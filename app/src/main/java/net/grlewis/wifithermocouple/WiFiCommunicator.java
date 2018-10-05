package net.grlewis.wifithermocouple;

import android.annotation.TargetApi;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import static net.grlewis.wifithermocouple.Constants.DEBUG;
import static net.grlewis.wifithermocouple.Constants.DISABLE_WD_URL;
import static net.grlewis.wifithermocouple.Constants.ENABLE_WD_URL;
import static net.grlewis.wifithermocouple.Constants.FAN_CONTROL_TIMEOUT_SECS;
import static net.grlewis.wifithermocouple.Constants.FAN_OFF_URL;
import static net.grlewis.wifithermocouple.Constants.FAN_ON_URL;
import static net.grlewis.wifithermocouple.Constants.RESET_WD_URL;
import static net.grlewis.wifithermocouple.Constants.TEMP_F_URL;
import static net.grlewis.wifithermocouple.Constants.TEMP_UPDATE_SECONDS;
import static net.grlewis.wifithermocouple.Constants.WATCHDOG_CHECK_SECONDS;
import static net.grlewis.wifithermocouple.Constants.WD_STATUS_URL;

class WiFiCommunicator {  // should probably be a Singleton (it is: see ThermocoupleApp)
    
    // if we ever have multiple instances of this class, they will share these:
    private final static String TAG = WiFiCommunicator.class.getSimpleName();
    private final static OkHttpClient client;
    private final static OkHttpClient eagerClient;  // 5 sec?
    private final static ThermocoupleApp appInstance;
    
    // TestActivity also uses this in onStart() to get initial reading and onResume() to manually update current temp
    AsyncJSONGetter tempFGetter = new AsyncJSONGetter( TEMP_F_URL, client );
    
    AsyncJSONGetter watchdogStatusGetterSingle = new AsyncJSONGetter( WD_STATUS_URL, client );
    
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
        
        
        
        enableWatchdogObservable = watchdogEnableSingle.request()  // returns the Single
                .toObservable()  // convert to Observable that emits one item, then completes.
                .doOnDispose( () -> {
                    watchdogDisableSingle.request().subscribe();
                    if( DEBUG ) Log.d( TAG, "enableWatchdogObservable disposed to disable watchdog" );
                } );
    
//        enableAndResetWatchdogSubject = PublishSubject.create()
//        .doOnSubscribe(
//                disposable -> {
//
//                }
//        )
        ;
    }
    
    
    
    
    
    // generic method to get JSON from a URL
    // passes a callback handler that implements JSONHandler interface (with handleJSON() and handleJSONError() methods)
    void getJSONFromURL( final URL theURL, final JSONHandler jsonHandler ) {  // TODO: will this hold reference to jsonGetter as long as needed?
        AsyncJSONGetter jsonGetter = new AsyncJSONGetter( theURL, client );
        Disposable getJSONDisposable = jsonGetter.get().subscribe(  // subscribe to the Single (uses Schedulers.io)
                jsonHandler::handleJSON,
                jsonHandler::handleJSONError
        );
        //getJSONDisposable.dispose();  // TODO: Single is automatically disposed when it emits?
    }
    
    
    
    
    // Observable to request Watchdog status JSON and reset every 40 seconds
    // note AsyncJSONGrtter is a Single; this combines the series of Single outputs into an Observable
    // the .onNext() handler will receive the JSON object
    Observable<JSONObject> watchdogStatusUpdates = Observable.interval( WATCHDOG_CHECK_SECONDS, TimeUnit.SECONDS )
            .flatMapSingle( checkWatchdogNow ->  watchdogStatusGetterSingle.get() );  // creates Observable that emits status updates
    
    
    
    
    
    // Observable to request Temperature JSON (°F)
    // subscribing to this will periodically update the ApplicationState temperature values (C and F)
    // we should be able to ignore the JSON it emits
    // note AsyncJSONGetter is a Single; this combines the series of Single outputs into an Observable stream
    // TODO: maybe add watchdog status query?
    Observable<JSONObject> tempFUpdater = Observable.interval( TEMP_UPDATE_SECONDS, TimeUnit.SECONDS )
            .flatMapSingle( getTempFNow -> tempFGetter.get() )  // combines outputs of Singles into an Observable stream
            .doOnNext( jsonF -> {
                appInstance.pidState.setCurrentVariableValue( (float)(jsonF.getDouble( "TempF" ) ) );
                appInstance.testActivityRef.updateTempButtonTextPublisher.onNext( "tempFUpdater set new value: "
                        + String.valueOf( jsonF.getDouble( "TempF" ) ) );
            } )
            .doOnTerminate( () -> fanControlSingle( false ).request().subscribe(
                    response -> { },  // successful OK response to fan shutoff
                    fanError -> { }// TODO: advise of possible emergency}
            ) );  // if this stops for any reason, shut off fan
    
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
            return new AsyncHTTPRequester( FAN_ON_URL, client );
        }
        else {
            appInstance.appState.setFanState( false );
            return new AsyncHTTPRequester( FAN_OFF_URL, client );
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
    Single<Response> fanControlWithWarning( boolean fanState ) {
        URL fanURL;
        if( fanState ) {
            fanURL = FAN_ON_URL;
        } else {
            fanURL = FAN_OFF_URL;
        }
        return new AsyncHTTPRequester( fanURL, eagerClient )
                .request()
                .retry( 1 )  // new
                .observeOn( AndroidSchedulers.mainThread() )  // must use UI thread to show a Toast
                .doOnSuccess( response -> appInstance.pidState.setOutputOn( fanState ) )
                .doOnError(
                        fanError -> {
                            Toast.makeText( appInstance, "Error controlling fan: " + fanError.getMessage(),
                                    Toast.LENGTH_SHORT ).show();
                        }
                );
    }
    
    // Version that allows passing an error handling Consumer (with an accept() method, returning no result)
    // TODO: needs updating
    @TargetApi( 24 )  // Consumer requires API 24
    Single<Response> fanControlWithWarning( boolean fanState, Consumer<Throwable> errorHandler ) {
        URL fanURL;
        if( fanState ) {
            fanURL = FAN_ON_URL;
        } else {
            fanURL = FAN_OFF_URL;
        }
        appInstance.appState.setFanState( fanState );
        return new AsyncHTTPRequester( fanURL, eagerClient )
                .request()
                .doOnError(
                        errorHandler::accept  // custom error handler TODO: "Consumer" requires API 24 (Android 7)
                );
    }
    
    
    
}
