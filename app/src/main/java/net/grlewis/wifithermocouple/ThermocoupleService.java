package net.grlewis.wifithermocouple;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.SingleEmitter;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Cancellable;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import static android.support.v4.app.NotificationCompat.CATEGORY_SERVICE;
import static net.grlewis.wifithermocouple.Constants.DEBUG;
import static net.grlewis.wifithermocouple.Constants.ENABLE_WD_URL;
import static net.grlewis.wifithermocouple.Constants.RESET_WD_URL;
import static net.grlewis.wifithermocouple.Constants.SERVICE_NOTIFICATION_ID;
import static net.grlewis.wifithermocouple.Constants.TEMP_F_URL;
import static net.grlewis.wifithermocouple.Constants.TEMP_UPDATE_SECONDS;
import static net.grlewis.wifithermocouple.Constants.WATCHDOG_CHECK_SECONDS;


/*
 *
 * All the stuff that runs in the background
 *
 */


public class ThermocoupleService extends Service {
    
    private static final String TAG = ThermocoupleService.class.getSimpleName();
    
    public ThermocoupleService( ) {  } // required empty constructor?
    
    ThermocoupleApp appInstance;
    
    OkHttpClient client;
    
    
    Disposable watchdogEnableDisp;
    AsyncHTTPRequester watchdogEnabler;
    Disposable watchdogFeedingDisp;
    AsyncHTTPRequester watchdogFeeder;
    Observable<Response> watchdogIntervalFeeder;
    Disposable tempUpdateDisp;
    AsyncJSONGetter tempUpdater;
    Observable<Float> tempIntervalUpdater;
    
    
    
    CompositeDisposable serviceCompositeDisp;
    
    // create a Handler on a separate thread for PID
    HandlerThread pidHandlerThread;
    Looper pidLooper;
    Handler pidHandler;
    
    BBQController bbqController;
    BBQController.PIDLoopRunnable pidLoopRunnable;
    
    
    @Override
    public void onCreate( ) {
        super.onCreate( );
        
        appInstance = ThermocoupleApp.getSoleInstance();
        
        
    
        // create Handler
        pidHandlerThread = new HandlerThread( "PIDHandlerThread" );
        pidHandlerThread.start();
        pidLooper = pidHandlerThread.getLooper();
        pidHandler = new Handler( pidLooper );
        //pidHandler = Handler.createAsync( pidLooper );  // API 28 (no VBL sync)
        bbqController = new BBQController( pidHandler );  // bbqController.pidLoopRunnable should be created
        
        client = new OkHttpClient();
        
        watchdogEnabler = new AsyncHTTPRequester( ENABLE_WD_URL, client, new SerialUUIDSupplier( 0x1000 ) );
        watchdogFeeder = new AsyncHTTPRequester( RESET_WD_URL, client, new SerialUUIDSupplier( 0x2000 ) );
        watchdogIntervalFeeder = Observable.interval( WATCHDOG_CHECK_SECONDS, TimeUnit.SECONDS )
                .flatMapSingle( resetTime -> watchdogFeeder.request().retry( 5L ) );
        tempUpdater = new AsyncJSONGetter( TEMP_F_URL, client, new SerialUUIDSupplier( 0x3000 ) );
        tempIntervalUpdater = Observable.interval( TEMP_UPDATE_SECONDS, TimeUnit.SECONDS )
                .flatMapSingle( updateTime -> tempUpdater.get().retry( 2L ) )
                .map( jsonTemp -> (float) jsonTemp.getDouble( "TempF" ) );
        
    }
    
    @Override
    // called by Android every time a client calls Context.startService( Intent )
    // called with a null Intent if being restarted after killed (shouldn't happen)
    public int onStartCommand( Intent intent, int flags, int startId ) {  // Intent contains calling context and name of this class
        
        // if you want to keep the Service from being killed, must have an ongoing notification (Compat builder for API < 26)
        // the Notification doesn't seem to be working (there is one that the App is, so maybe that's it???)
        startForeground( SERVICE_NOTIFICATION_ID, new NotificationCompat.Builder( getApplicationContext(), "Channel 1" )
                .setContentTitle( "PID Service Running" )
                .setCategory( CATEGORY_SERVICE )
                .setOngoing( true )
                .build() );
        
        if( intent != null ) {  // this is an initial start, not a restart after killing
            
            // first thing to do is enable the watchdog timer
            watchdogEnableDisp = watchdogEnabler.request().retry( 5L ).subscribe(
                    okResponse -> {
                        if( DEBUG ) Log.d( TAG, "Watchdog enabled; "
                                + "watchdogEnableDisp = " + watchdogEnableDisp.toString() );  // logs "DISPOSED"
                    },
                    wdEnableErr -> {
                        if( DEBUG ) Log.d( TAG, "Error enabling watchdog; "
                                + "watchdogEnableDisp = " + watchdogEnableDisp.toString() );
                    }
            );
            serviceCompositeDisp.add( watchdogEnableDisp );
            
            // now start periodic resets of the watchdog
            watchdogFeedingDisp = watchdogIntervalFeeder.subscribe(
                    feedingTime -> { if( DEBUG ) Log.d( TAG, "Watchdog fed successfully" ); },
                    feedingErr -> { if( DEBUG ) Log.d( TAG, "Error feeding watchdog: " + feedingErr ); }
            );
            serviceCompositeDisp.add( watchdogFeedingDisp );
            
            // now start temperature updates
            tempUpdateDisp = tempIntervalUpdater.subscribe(
                    appInstance.pidState::setCurrentVariableValue,
                    tempErr -> { if( DEBUG ) Log.d( TAG, "Error updating temp: " + tempErr ); }
            );
            serviceCompositeDisp.add( tempUpdateDisp );
            
            
            // start the BBQ Controller loop (we think it's fixed to not change UI and run all the time)
            pidHandler.postDelayed( bbqController.pidLoopRunnable, 2000L );  // give it a couple seconds
            
            
            
        } else {  // this is a restart
        
        }
        
        super.onStartCommand( intent, flags, startId );
        return START_STICKY;  // keep it running
    }
    
    
    
    
    
    /*------------------------------- START OF SERVICE BINDING STUFF ---------------------------------*/
    // Binder given to clients for service--returns this instance of the Service class
    private final IBinder thermoBinder = new LocalBinder();
    // Class for binding Service
    public class LocalBinder extends Binder {
        ThermocoupleService getService() {
            Log.d( TAG, "Entering thermoBinder.getService()" );
            // return this instance of the Service so Clients can call public methods
            // (note you can call static methods with an instance identifier)
            return ThermocoupleService.this; // include class name; otherwise "this" == LocalBinder
        }
    }
    @Override // Service must implement this & return an IBinder
    public IBinder onBind( Intent intent ) {
        Log.d( TAG, "Entering onBind()" );
        // Here can fetch "Extra" info sent with the Intent
        return thermoBinder; // client can call the returned instance with .getService
    }
    /*-------------------------------- END OF SERVICE BINDING STUFF ----------------------------------*/
    
    
    
    // do we need this?
    class tempUpdateEmitter implements SingleEmitter<JSONObject> {
        
        Disposable tempUpdateEmitterDisp;
        boolean disposed;
        
        @Override
        public void onSuccess( JSONObject jsonObject ) {
            try {
                appInstance.pidState.setCurrentVariableValue( (float) jsonObject.getDouble( "TempF" ) );
            } catch ( JSONException j ) {
                this.onError( j );
            } finally {
                if( DEBUG ) Log.d( TAG, "tempUpdateEmitter onSuccess() called" );
                if( tempUpdateEmitterDisp != null ) tempUpdateEmitterDisp.dispose();
                disposed = true;
            }
        }
        
        @Override
        public void onError( Throwable t ) {
            if( DEBUG ) Log.d( TAG, "tempUpdateEmitter onError() called: " + t.getMessage() );
            if( tempUpdateEmitterDisp != null ) tempUpdateEmitterDisp.dispose();
        }
        
        @Override
        public void setDisposable( Disposable d ) {
            tempUpdateEmitterDisp = d;
        }
        
        @Override
        public void setCancellable( Cancellable c ) {
            throw new UnsupportedOperationException( "setCancellable is not implemented in tempUpdateEmitter" );
        }
        
        @Override
        public boolean isDisposed( ) {
            return disposed;
        }
        
        @Override
        public boolean tryOnError( Throwable t ) {
            if( disposed ) return false;
            this.onError( t );
            return true;
        }
    }
    
}
