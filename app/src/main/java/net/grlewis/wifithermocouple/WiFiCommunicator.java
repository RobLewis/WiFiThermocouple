package net.grlewis.wifithermocouple;

import android.annotation.TargetApi;
import android.arch.core.util.Function;
import android.os.SystemClock;
import android.util.Pair;
import android.widget.Toast;

import org.json.JSONObject;

import java.net.URL;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import static net.grlewis.wifithermocouple.Constants.ANALOG_IN_UPDATE_SECS;
import static net.grlewis.wifithermocouple.Constants.ANALOG_READ_UPPER_HALF;
import static net.grlewis.wifithermocouple.Constants.DISABLE_WD_URL;
import static net.grlewis.wifithermocouple.Constants.ENABLE_WD_URL;
import static net.grlewis.wifithermocouple.Constants.FAN_CONTROL_TIMEOUT_SECS;
import static net.grlewis.wifithermocouple.Constants.FAN_CONTROL_UPPER_HALF;
import static net.grlewis.wifithermocouple.Constants.FAN_OFF_URL;
import static net.grlewis.wifithermocouple.Constants.FAN_ON_URL;
import static net.grlewis.wifithermocouple.Constants.READ_ANALOG_URL;
import static net.grlewis.wifithermocouple.Constants.RESET_WD_URL;
import static net.grlewis.wifithermocouple.Constants.TEMP_F_URL;
import static net.grlewis.wifithermocouple.Constants.TEMP_GET_UPPER_HALF;
import static net.grlewis.wifithermocouple.Constants.TEMP_UPDATE_SECONDS;
import static net.grlewis.wifithermocouple.Constants.TEMP_UPDATE_UPPER_HALF;
import static net.grlewis.wifithermocouple.Constants.WATCHDOG_CHECK_SECONDS;
import static net.grlewis.wifithermocouple.Constants.WATCHDOG_ENABLE_UPPER_HALF;
import static net.grlewis.wifithermocouple.Constants.WATCHDOG_FEED_UPPER_HALF;
import static net.grlewis.wifithermocouple.Constants.WATCHDOG_STATUS_UPPER_HALF;
import static net.grlewis.wifithermocouple.Constants.WD_STATUS_URL;

class WiFiCommunicator {  // should probably be a Singleton (it is: see ThermocoupleApp)
    
    // if we ever have multiple instances of this class, they will share these:
    private final static String TAG = WiFiCommunicator.class.getSimpleName();
    private final static OkHttpClient client;
    private final static OkHttpClient eagerClient;  // 5 sec?
    private final static ThermocoupleApp appInstance;
    
    // buffer for 1 hour of temp history
    private ArrayBlockingQueue<Float> tempHistoryBuffer;  // (use .toArray() to get the whole thing for graphing)
    private ArrayBlockingQueue<Pair<Date,Float>> timestampedHistory;  // with timestamp
    
    private UIStateModel uiStateModel;
    
    //private Function<URL,UUID> httpUUIDSupplier;
    //private Function<URL,UUID> jsonUUIDSupplier;
    private Function<URL,UUID> watchdogEnableUUIDSupplier;
    private Function<URL,UUID> watchdogFeedUUIDSupplier;
    private Function<URL,UUID> tempGetUUIDSupplier;
    private Function<URL,UUID> tempUpdateUUIDSupplier;
    private Function<URL,UUID> fanControlUUIDSupplier;
    private Function<URL,UUID> watchdogStatusUUIDSupplier;
    private Function<URL,UUID> analogReadUUIDSupplier;
    
    
    // NEW organization
    AsyncJSONGetter tempFGetter;               // Single that fetches a JSON tempF value
    Observable<JSONObject> tempFUpdater;       // combines period values into an Observable
    AsyncJSONGetter watchdogStatusGetter;      // Single that fetches a JSON watchdog status report (enabled, expired)
    AsyncJSONGetter analogReader;              // Single that fetches a JSON report of control setting (0.0-1.0V)
    Observable<JSONObject> analogInUpdater;
    
    AsyncHTTPRequester watchdogEnabler;
    AsyncHTTPRequester watchdogDisabler;
    AsyncHTTPRequester watchdogFeeder;
    Observable<Response> watchdogFeedObservable;
    Observable<JSONObject> watchdogStatusUpdater;
    
    AsyncHTTPRequester fanTurnon;
    AsyncHTTPRequester fanTurnoff;
    
    
    
    
    // enable watchdog timer (dispose to disable)
    Observable<Response> enableWatchdogObservable;
    PublishSubject<Response> enableAndResetWatchdogSubject;  // subscribe to enable watchdog & start resetting, dispose to disable/stop
    
    
    static {  // initializer
        
        appInstance = ThermocoupleApp.getSoleInstance();
        
        client = new OkHttpClient.Builder()     // recommended to have only one
                .retryOnConnectionFailure( true )  // this supposedly defaults true but trying it to fix "Socket closed" errors
                //.connectTimeout( 10L, TimeUnit.SECONDS )  // the default is said to be 10 seconds
                .build();
        
        eagerClient = client.newBuilder()  // TODO: re-evaluate?
                .readTimeout( FAN_CONTROL_TIMEOUT_SECS, TimeUnit.SECONDS )  // 5 sec? (default is 10)
                .build();
    }
    
    
    // constructor
    WiFiCommunicator() {
        
        // providers of custom UUIDs for JSON and HTTP requests (passed the URL, which we ignore and return serialized UUIDs)
        tempGetUUIDSupplier = new SerialUUIDSupplier( TEMP_GET_UPPER_HALF, "Temp Getter" );          // 0x3000
        tempUpdateUUIDSupplier = new SerialUUIDSupplier( TEMP_UPDATE_UPPER_HALF, "Temp Updater" );       // 0x4000
        watchdogStatusUUIDSupplier = new SerialUUIDSupplier( WATCHDOG_STATUS_UPPER_HALF, "Watchdog Status Checker" );   // 0x6000
        watchdogEnableUUIDSupplier = new SerialUUIDSupplier( WATCHDOG_ENABLE_UPPER_HALF, "Watchdog Enabler" );   // 0x1000
        watchdogFeedUUIDSupplier = new SerialUUIDSupplier( WATCHDOG_FEED_UPPER_HALF, "Watchdog Feeder" );     // 0x2000
        analogReadUUIDSupplier = new SerialUUIDSupplier( ANALOG_READ_UPPER_HALF, "Analog Reader" );       // 0x7000
        
        fanControlUUIDSupplier = new SerialUUIDSupplier( FAN_CONTROL_UPPER_HALF, "Fan Controller" );       // 0x5000
        
        SystemClock.sleep( 2000L );  // FIXME: is it conceivable we have to wait for these constructors?
        
        // TestActivity also uses this in onStart() to get initial reading and onResume() to manually update current temp
        tempFGetter = new AsyncJSONGetter( TEMP_F_URL, client, tempGetUUIDSupplier );
        tempFUpdater = Observable.interval( TEMP_UPDATE_SECONDS, TimeUnit.SECONDS )  // currently 5 seconds
                .flatMapSingle( getTempFNow -> tempFGetter.get( ) );  // combines outputs of Singles into an Observable stream
        
        watchdogStatusGetter = new AsyncJSONGetter( WD_STATUS_URL, client, watchdogStatusUUIDSupplier );
        watchdogStatusUpdater = Observable.interval( WATCHDOG_CHECK_SECONDS, TimeUnit.SECONDS )
                .flatMapSingle( checkWatchdogNow -> watchdogStatusGetter.get( ) );
        
        watchdogEnabler = new AsyncHTTPRequester( ENABLE_WD_URL, client, watchdogEnableUUIDSupplier );
        watchdogDisabler = new AsyncHTTPRequester( DISABLE_WD_URL, client, watchdogEnableUUIDSupplier );  // same supplier
        watchdogFeeder = new AsyncHTTPRequester( RESET_WD_URL, client, watchdogFeedUUIDSupplier );
        watchdogFeedObservable = Observable.interval( WATCHDOG_CHECK_SECONDS, TimeUnit.SECONDS )  // 40
                .startWith( -1L )  // kick it off right away TODO: needed?
                .flatMapSingle( resetNow -> watchdogFeeder.request( ) );
        
        analogReader = new AsyncJSONGetter( READ_ANALOG_URL, client, analogReadUUIDSupplier );
        analogInUpdater = Observable.interval( ANALOG_IN_UPDATE_SECS, TimeUnit.SECONDS )
                .flatMapSingle( tick -> analogReader.get( ) );
        
        fanTurnon = new AsyncHTTPRequester( FAN_ON_URL, client, fanControlUUIDSupplier );
        fanTurnoff = new AsyncHTTPRequester( FAN_OFF_URL, client, fanControlUUIDSupplier );
        
        
    }
    
    
    // method that returns a requester to turn the fan on or off
    AsyncHTTPRequester fanController( boolean fanState ) {
        return fanState? fanTurnon : fanTurnoff;
    }
    
    // subscribe to the returned requester to get a Toast warning of any problems with fan
    // the Single emits the HTTP Response
    Single<Response> fanControlWithWarning( boolean fanState ) {
        return fanController( fanState )
                .request()
                .retry( 2 )  // new
                .observeOn( AndroidSchedulers.mainThread() )  // must use UI thread to show a Toast
                .doOnError(
                        fanError -> {
                            Toast.makeText( appInstance, "Error controlling fan: " + fanError.getMessage(),
                                    Toast.LENGTH_SHORT ).show();
                        }
                );
    }


        
/*
//        tempHistoryBuffer = new ArrayBlockingQueue<>( (60/TEMP_UPDATE_SECONDS) * HISTORY_MINUTES );  // should == 720
//        timestampedHistory = new ArrayBlockingQueue<>( (60/TEMP_UPDATE_SECONDS) * HISTORY_MINUTES );  // should == 720
        
        // TODO: create Observable that you subscribe to to enable the watchdog, and dispose to disable. It never completes.
        // (like RxBle .establishconnection())
        
        Observable<Optional<Integer>> watchDogObservable() {
            
            return Observable.never( )
                    .startWith( Integer.valueOf( 1 ) )
                    .doOnSubscribe( one -> watchdogEnabler.request().subscribe( ) )
                    .doOnDispose( () -> watchdogDisabler.request().subscribe( ) )
                    .map( one -> Optional.empty() );
            
        }
*/

    
    
    // Version that allows passing an error handling Consumer (with an accept() method that returns no result)
    // TODO: needs updating if we want to use it
    @TargetApi( 24 )  // Consumer requires API 24 (maybe reactivex doesn't)
    Single<Response> fanControlWithWarning( boolean fanState, Consumer<Throwable> errorHandler ) {
        URL fanURL;
        if( fanState ) {
            fanURL = FAN_ON_URL;
        } else {
            fanURL = FAN_OFF_URL;
        }
        appInstance.appState.setFanState( fanState );
        return new AsyncHTTPRequester( fanURL, eagerClient, fanControlUUIDSupplier )  // generate serialized UUIDs
                .request()
                .doOnError(
                        errorHandler::accept  // custom error handler TODO: "Consumer" requires API 24 (Android 7)
                );
    }
    
}
