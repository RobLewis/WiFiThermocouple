package net.grlewis.wifithermocouple;

import android.annotation.TargetApi;
import android.arch.core.util.Function;
import android.arch.lifecycle.ViewModelProviders;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.PublishSubject;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import static net.grlewis.wifithermocouple.Constants.ANALOG_IN_UPDATE_SECS;
import static net.grlewis.wifithermocouple.Constants.ANALOG_READ_UPPER_HALF;
import static net.grlewis.wifithermocouple.Constants.DEBUG;
import static net.grlewis.wifithermocouple.Constants.DISABLE_WD_URL;
import static net.grlewis.wifithermocouple.Constants.ENABLE_WD_URL;
import static net.grlewis.wifithermocouple.Constants.FAN_CONTROL_TIMEOUT_SECS;
import static net.grlewis.wifithermocouple.Constants.FAN_CONTROL_UPPER_HALF;
import static net.grlewis.wifithermocouple.Constants.FAN_OFF_URL;
import static net.grlewis.wifithermocouple.Constants.FAN_ON_URL;
import static net.grlewis.wifithermocouple.Constants.HISTORY_MINUTES;
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
    
    AsyncJSONGetter tempFGetter;
    AsyncJSONGetter watchdogStatusGetterSingle;
    AsyncJSONGetter analogReaderSingle;
    
    Observable<Float> analogInObservable;
    
    // enable watchdog timer (dispose to disable)
    Observable<Response> enableWatchdogObservable;
    PublishSubject<Response> enableAndResetWatchdogSubject;  // subscribe to enable watchdog & start resetting, dispose to disable/stop
    
    
    static {  // initializer
        
        appInstance = ThermocoupleApp.getSoleInstance();
        
        client = new OkHttpClient.Builder()     // recommended to have only one
                .retryOnConnectionFailure( true )  // this supposedly defaults true but trying it to fix "Socket closed" errors
                //.connectTimeout( 10L, TimeUnit.SECONDS )  // the default is said to be 10 seconds
                .build();
        
        eagerClient = client.newBuilder()
                .readTimeout( FAN_CONTROL_TIMEOUT_SECS, TimeUnit.SECONDS )  // 5 sec? (default is 10)
                .build();
    }
    
    
    // constructor
    WiFiCommunicator() {
    
        // providers of custom UUIDs for JSON and HTTP requests (passed the URL, which we ignore and return serialized UUIDs)
        watchdogEnableUUIDSupplier = new SerialUUIDSupplier( WATCHDOG_ENABLE_UPPER_HALF );   // 0x1000
        watchdogFeedUUIDSupplier   = new SerialUUIDSupplier( WATCHDOG_FEED_UPPER_HALF );     // 0x2000
        tempGetUUIDSupplier        = new SerialUUIDSupplier( TEMP_GET_UPPER_HALF );          // 0x3000
        tempUpdateUUIDSupplier     = new SerialUUIDSupplier( TEMP_UPDATE_UPPER_HALF );       // 0x4000
        fanControlUUIDSupplier     = new SerialUUIDSupplier( FAN_CONTROL_UPPER_HALF );       // 0x5000
        watchdogStatusUUIDSupplier = new SerialUUIDSupplier( WATCHDOG_STATUS_UPPER_HALF );   // 0x6000
        analogReadUUIDSupplier     = new SerialUUIDSupplier( ANALOG_READ_UPPER_HALF );       // 0x7000
        
    
        // TestActivity also uses this in onStart() to get initial reading and onResume() to manually update current temp
        tempFGetter = new AsyncJSONGetter( TEMP_F_URL, client, tempGetUUIDSupplier );
        
        watchdogStatusGetterSingle = new AsyncJSONGetter( WD_STATUS_URL, client, watchdogStatusUUIDSupplier );
        analogReaderSingle = new AsyncJSONGetter( READ_ANALOG_URL, client, analogReadUUIDSupplier );
    
        analogInObservable = Observable.interval( ANALOG_IN_UPDATE_SECS, TimeUnit.SECONDS )
                .flatMapSingle( tick -> analogReaderSingle.get() )
                .retry( 2L )  // NEW (for errors producing HTTP Response)
                .map( json -> (float) json.getDouble( "AnalogVoltsIn" ) )
                .retry( 2L )  // NEW (for JSON errors)
                .doOnNext( volts -> appInstance.pidState.setAnalogInVolts( volts ) );
                
    
        tempHistoryBuffer = new ArrayBlockingQueue<>( (60/TEMP_UPDATE_SECONDS) * HISTORY_MINUTES );  // should == 720
        timestampedHistory = new ArrayBlockingQueue<>( (60/TEMP_UPDATE_SECONDS) * HISTORY_MINUTES );  // should == 720
        
        enableWatchdogObservable = watchdogEnableSingle.request()  // returns the Single
                .toObservable()  // convert to Observable that emits one item, then completes.
                .doOnDispose( () -> {
                    watchdogDisableSingle.request().subscribe();
                    if( DEBUG ) Log.d( TAG, "enableWatchdogObservable disposed to disable watchdog" );
                } );
        
        // TODO: does this work?
        if( appInstance.testActivityRef == null ) Log.d( TAG, "appInstance.testActivityRef is null" );  // says it's null
        uiStateModel = ViewModelProviders.of(appInstance.testActivityRef).get( UIStateModel.class );  // NPE
        
    }
    
    
    
    
    
    
    // Observable to request Watchdog status JSON and reset every 40 seconds
    // note AsyncJSONGrtter is a Single; this combines the series of Single outputs into an Observable
    // the .onNext() handler will receive the JSON object
    // TODO: need?
    Observable<JSONObject> watchdogStatusUpdates = Observable.interval( WATCHDOG_CHECK_SECONDS, TimeUnit.SECONDS )
            .flatMapSingle( checkWatchdogNow ->  watchdogStatusGetterSingle.get() );  // creates Observable that emits status updates
    
    
    
    // Observable to request Temperature JSON (°F)
    // subscribing to this will periodically update the ApplicationState temperature values (C and F)
    // we should be able to ignore the JSON it emits
    // note AsyncJSONGetter is a Single; this combines the series of Single outputs into an Observable stream
    // TODO: maybe add watchdog status query?
    Observable<JSONObject> tempFUpdater = Observable.interval( TEMP_UPDATE_SECONDS, TimeUnit.SECONDS )  // currently 5 seconds
            .flatMapSingle( getTempFNow -> tempFGetter.get() )  // combines outputs of Singles into an Observable stream
            .doOnNext( jsonF -> {
                
                float currentTempF;
                try {
                    currentTempF = (float) jsonF.getDouble( "TempF" );
                } catch ( JSONException j ) {
                    if( DEBUG ) Log.d( TAG, "JSONException on TempF fetch: " + j.getMessage()
                            + "; JSON: \n" + jsonF.toString( 4 )
                            + "\nRequest UUID: " + jsonF.optString( "RequestUUID", "(no request UUID in JSON)" ) );
                    currentTempF = appInstance.pidState.getCurrentVariableValue();  // repeat last value if a problem with new one
                } finally {
                }
                
                // another idea
                //currentTempF = (float) jsonF.optDouble( "TempF", appInstance.pidState.getCurrentVariableValue() );  // fallback
                
                appInstance.pidState.setCurrentVariableValue( currentTempF );
                appInstance.testActivityRef.updateTempButtonTextPublisher.onNext( "tempFUpdater set new value: "
                        + String.valueOf( currentTempF ) );

//                if( appInstance.pidState.isEnabled() ) {  // only keep history if PID is running TODO: (?)
//                    if ( tempHistoryBuffer.remainingCapacity( ) < 1 )
//                        tempHistoryBuffer.poll( );  // if buffer is full, discard oldest value
//                    tempHistoryBuffer.add( currentTempF );  // insert the new value at end of the queue
//                    if ( DEBUG )
//                        Log.d( TAG, "History buffer now contains " + tempHistoryBuffer.size( ) + " values" );
//                }
//
//                // alternative keeps value history with timestamps, for now even if PID is disabled.
//                if( timestampedHistory.remainingCapacity() < 1 ) {  // queue is full
//                    timestampedHistory.poll();                      // so discard oldest value
//                }
//                timestampedHistory.add( new Pair<>( new Date(), currentTempF ) );  // add latest to end of queue
//                if ( DEBUG ) Log.d( TAG, "timestampedHistory now contains " + timestampedHistory.size( ) + " values" );
                
                // alternative with ViewModel TODO: does it work?
                UIStateModel.UIState uiState = uiStateModel.getUIStateObject();
                uiState.updateUITemp( currentTempF );  // try to send UI new value
                int tempHistSize = uiState.addHistoryValue( new Pair<>( new Date(), currentTempF ) );
                uiStateModel.getCurrentUIState().postValue( uiState );  // can't use .setValue() on a background thread
                if( DEBUG ) Log.d( TAG, "History queue now contains " + tempHistSize + " values" );
            } )
            .doOnTerminate( () -> {
                if( DEBUG ) Log.d( TAG, "tempFUpdater terminated with " + tempFGetter.getSuccessCount()
                        + " successes and " + tempFGetter.getFailureCount() + " failures");
                fanControlSingle( false ).request()
                        .retry( 5 )  // lots of retries because it's vital
                        .subscribe(  // if this stops for any reason, shut off fan
                                response -> { if( DEBUG ) Log.d( TAG, "Successful fan shutoff after tempFUpdater termination"); },  // successful OK response to fan shutoff
                                fanError -> { if( DEBUG ) Log.d( TAG, "Error shutting off fan (after retries) after tempFUpdater termination: "
                                        + fanError.getMessage() ); }// TODO: advise of possible emergency}
                        );
            })
            .observeOn( AndroidSchedulers.mainThread() );
    
    /*
     * Usage
     * Disposable getTempFDisp = getTemperatureF. subscribe(
     *         jsonTempF -> setCurrentTempF( jsonTempF.getDouble( "TempF" ),  // (there's no getFloat())
     *         jsonTempFErr -> Log.D( TAG, "Error getting temperature: " + jsonTempFErr.getMessage() );
     *     )
     *
     * */
    
    
    // subscribe to the returned requester to turn fan on or off
    // TODO: interaction with pidState (see version with warning below, if we even need this)
    AsyncHTTPRequester fanControlSingle( boolean fanState ) {
        if( fanState ) {
            appInstance.appState.setFanState( true );
            return new AsyncHTTPRequester( FAN_ON_URL, client, httpUUIDSupplier );  // trial of auto-generate new UUID per request
        }
        else {
            appInstance.appState.setFanState( false );
            return new AsyncHTTPRequester( FAN_OFF_URL, client, httpUUIDSupplier );  // trial of auto-generate new UUID per request
        }
    }
    
    
    AsyncHTTPRequester watchdogEnableSingle = new AsyncHTTPRequester( ENABLE_WD_URL, client );
    AsyncHTTPRequester watchdogDisableSingle = new AsyncHTTPRequester( DISABLE_WD_URL, client );
    AsyncHTTPRequester watchdogResetSingle = new AsyncHTTPRequester( RESET_WD_URL, client );
    
    // subscribe to enable the periodic reset of the watchdog timer
    Observable<Response> watchdogResetObservable = Observable.interval( WATCHDOG_CHECK_SECONDS, TimeUnit.SECONDS )  // 40
            .startWith( -1L )  // kick it off right away TODO: needed?
            .flatMapSingle( resetNow -> watchdogResetSingle.request() );
    
    
    
    
    
    // subscribe to the returned requester to get a warning of any problems with fan
    // the Single emits the HTTP Response
    // TODO: this one method refactored for Service (so far)
    Single<Response> fanControlWithWarning( boolean fanState ) {
        URL fanURL = fanState? FAN_ON_URL : FAN_OFF_URL;
        return new AsyncHTTPRequester( fanURL, eagerClient, httpUUIDSupplier )  // generate (serialized) UUIDs FIXME: problem
                .request()
                .retry( 2 )  // new
                .observeOn( AndroidSchedulers.mainThread() )  // must use UI thread to show a Toast
                .doOnSuccess( response -> appInstance.pidState.setOutputOn( fanState ) )
                .doOnError(
                        fanError -> {
                            Toast.makeText( appInstance, "Error controlling fan: " + fanError.getMessage(),
                                    Toast.LENGTH_SHORT ).show();
                        }
                );
    }
    
    // Version that allows passing an error handling Consumer (with an accept() method that returns no result)
    // TODO: needs updating if we want to use it
    @TargetApi( 24 )  // Consumer requires API 24
    Single<Response> fanControlWithWarning( boolean fanState, Consumer<Throwable> errorHandler ) {
        URL fanURL;
        if( fanState ) {
            fanURL = FAN_ON_URL;
        } else {
            fanURL = FAN_OFF_URL;
        }
        appInstance.appState.setFanState( fanState );
        return new AsyncHTTPRequester( fanURL, eagerClient, httpUUIDSupplier )  // generate serialized UUIDs
                .request()
                .doOnError(
                        errorHandler::accept  // custom error handler TODO: "Consumer" requires API 24 (Android 7)
                );
    }
    
    
    
    
    
}
