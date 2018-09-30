package net.grlewis.wifithermocouple;

import android.annotation.TargetApi;
import android.widget.Toast;

import org.json.JSONObject;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import static net.grlewis.wifithermocouple.Constants.FAN_CONTROL_TIMEOUT_SECS;
import static net.grlewis.wifithermocouple.Constants.FAN_OFF_URL;
import static net.grlewis.wifithermocouple.Constants.FAN_ON_URL;
import static net.grlewis.wifithermocouple.Constants.SOFTWARE_VERSION;
import static net.grlewis.wifithermocouple.Constants.TEMP_F_URL;
import static net.grlewis.wifithermocouple.Constants.TEMP_UPDATE_SECONDS;
import static net.grlewis.wifithermocouple.Constants.WATCHDOG_CHECK_SECONDS;
import static net.grlewis.wifithermocouple.Constants.WD_STATUS_URL;

class WiFiCommunicator {  // should probably be a Singleton (it is: see ThermocoupleApp)
    
    // if we ever have multiple instances of this class, they will share these:
    private final static String TAG = WiFiCommunicator.class.getSimpleName();
    private final static OkHttpClient client = new OkHttpClient();  // supposed to only have one
    private final static OkHttpClient eagerClient = client.newBuilder().readTimeout( FAN_CONTROL_TIMEOUT_SECS, TimeUnit.SECONDS ).build();  // 2 sec
    private final static ThermocoupleApp appInstance = ThermocoupleApp.getSoleInstance();
    
    
    AsyncJSONGetter tempFGetter = new AsyncJSONGetter( TEMP_F_URL, client );  //
    
    
    
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
    
    
    
    
    // Observable to request Watchdog status JSON
    // note AsyncJSONGrtter is a Single; this combines the series of Single outputs into an Observable
    // the .onNext() handler will receive the JSON object
    Observable<JSONObject> getWatchdogStatus = Observable.interval( WATCHDOG_CHECK_SECONDS, TimeUnit.SECONDS )
            .flatMapSingle( checkWatchdogNow -> new AsyncJSONGetter( WD_STATUS_URL, client ).get() );
    
    
    
    // Observable to request Temperature JSON (°F)
    // subscribing to this will periodically update the ApplicationState temperature values (C and F)
    // we should be able to ignore the JSON it emits
    // note AsyncJSONGetter is a Single; this combines the series of Single outputs into an Observable stream
    Observable<JSONObject> tempFUpdater = Observable.interval( TEMP_UPDATE_SECONDS, TimeUnit.SECONDS )
            .flatMapSingle( getTempFNow -> tempFGetter.get() )  // combines outputs of Singles into an Observable stream
            .doOnNext( jsonF -> {
                appInstance.pidState.setCurrentVariableValue( (float)(jsonF.getDouble( "TempF" ) ) );
                appInstance.testActivityRef.tempButtonTextPublisher.onNext( "tempFUpdater set new value: "
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
    
    
    
    // subscribe to the returned requester to get a warning of any problems with fan
    // the Single emits the HTTP Response
    Single<Response> fanControlWithWarning( boolean fanState ) {
        URL fanURL;
        if( fanState ) {
            fanURL = FAN_ON_URL;
        } else {
            fanURL = FAN_OFF_URL;
        }
        //appInstance.appState.setFanState( fanState );  // need?
        //appInstance.pidState.setOutputOn( fanState );
        return new AsyncHTTPRequester( fanURL, eagerClient )
                .request()
                .observeOn( AndroidSchedulers.mainThread() )  // must use UI thread to show a Toast
                .doOnSuccess( response -> appInstance.pidState.setOutputOn( fanState ) )
                .doOnError(
                        fanError -> {
                            Toast.makeText( appInstance, "Error controlling fan: " + fanError.getMessage(),
                                    Toast.LENGTH_SHORT ).show();
                        }  // TODO: issue warning (Toast or whatever; do we need UI thread?)
                );
    }
    
    // Version that allows passing an error handling Consumer (with an accept() method, returning no result)
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
