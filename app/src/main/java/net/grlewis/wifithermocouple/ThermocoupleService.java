package net.grlewis.wifithermocouple;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Pair;

import com.jakewharton.rxrelay2.BehaviorRelay;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
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
import static net.grlewis.wifithermocouple.Constants.HISTORY_BUFFER_SIZE;
import static net.grlewis.wifithermocouple.Constants.RESET_WD_URL;
import static net.grlewis.wifithermocouple.Constants.SERVICE_NOTIFICATION_ID;
import static net.grlewis.wifithermocouple.Constants.TEMP_F_URL;
import static net.grlewis.wifithermocouple.Constants.TEMP_GET_UPPER_HALF;
import static net.grlewis.wifithermocouple.Constants.TEMP_UPDATE_SECONDS;
import static net.grlewis.wifithermocouple.Constants.WATCHDOG_ENABLE_UPPER_HALF;
import static net.grlewis.wifithermocouple.Constants.WATCHDOG_FEED_UPPER_HALF;
import static net.grlewis.wifithermocouple.Constants.WATCHDOG_RESET_SECONDS;


/*
 *
 * All the stuff that runs in the background
 *
 * PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
        "MyApp::MyWakelockTag");
wakeLock.acquire();
 *
 */


public class ThermocoupleService extends Service {
    
    private static final String TAG = ThermocoupleService.class.getSimpleName();
    
    public ThermocoupleService( ) {  } // required empty constructor?  (same crash with or without)
    
    ThermocoupleApp appInstance;
    
    Disposable tempUpdateDisp;
    Disposable watchdogMaintainDisp;  // NEW: to enable, feed, and disable watchdog
    CompositeDisposable serviceCompositeDisp;
    
    Notification runningNotification;
    
    // create a Handler on a separate thread for PID
    HandlerThread pidHandlerThread;
    Looper pidLooper;
    Handler pidHandler;
    
    private ArrayBlockingQueue<Pair<Date, Float>> timestampedHistory;
    BehaviorRelay<ArrayBlockingQueue<Pair<Date, Float>>> tempHistRelay;
    
    private IBinder thermoBinder;
    
    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;
    
    
    /*------------------------------- START OF SERVICE BINDING STUFF ---------------------------------*/
    // Binder given to clients for service--returns this instance of the Service class
    class LocalBinder extends Binder {
        ThermocoupleService getService() {
            Log.d( TAG, "Entering thermoBinder.getService()" );
            return ThermocoupleService.this; // include class name; otherwise "this" == LocalBinder
        }
    }
    
    @Override // Service must implement this & return an IBinder
    public IBinder onBind( Intent intent ) {
        Log.d( TAG, "Entering onBind()" );
        // Here can fetch "Extra" info sent with the Intent
        return thermoBinder; // client can call the returned instance with .getService
    }
    
    @Override
    public boolean onUnbind( Intent intent ) {  // FIXME: NEW (not printing message)
        if( DEBUG ) Log.d( TAG, "onUnbind() called with Intent " + intent.toString() );
        return super.onUnbind( intent );
    }
    /*-------------------------------- END OF SERVICE BINDING STUFF ----------------------------------*/
    
    
    
    
    
    @Override
    public void onCreate( ) {
        super.onCreate( );
    
        if( DEBUG ) Log.d( TAG, "entering onCreate()");
    
        appInstance = ThermocoupleApp.getSoleInstance();
        if( DEBUG ) Log.d( TAG, "set appInstance to sole instance of App singleton");
    
        serviceCompositeDisp = new CompositeDisposable(  );  // TODO; does this fix NPE?
        
        // create Handler
        pidHandlerThread = new HandlerThread( "PIDHandlerThread" );
        pidHandlerThread.start();
        pidLooper = pidHandlerThread.getLooper();
        pidHandler = new Handler( pidLooper );
        //pidHandler = Handler.createAsync( pidLooper );  // API 28 (no VBL sync)
        appInstance.bbqController = new BBQController( pidHandler );  // bbqController.pidLoopRunnable should be created
        
        timestampedHistory = new ArrayBlockingQueue<>( HISTORY_BUFFER_SIZE );  // 720
        tempHistRelay = BehaviorRelay.create();
    
        thermoBinder = new LocalBinder();
        
        appInstance.setServiceRef( this );  // FIXME?: desperation when binding wont work
        
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":WakeLock" );
        wakeLock.acquire( 12 * 60 * 60 * 1000L );  // 12 hour timeout
        if( DEBUG ) Log.d( TAG, "WakeLock acquired for 12 hours");
        
        if( DEBUG ) Log.d( TAG, "exiting onCreate()");
    
    }
    
    @Override
    // called by Android every time a client calls Context.startService( Intent )
    // called with a null Intent if being restarted after killed (shouldn't happen)
    public int onStartCommand( Intent intent, int flags, int startId ) {  // Intent contains calling context and name of this class
    
        super.onStartCommand( intent, flags, startId );  // TODO: try putting this first, not last--no change
    
        if( DEBUG ) Log.d( TAG, "onStartCommand() entered");  // never prints
        
        // commenting out this section doesn't change null pointer crash
        // if you want to keep the Service from being killed, must have an ongoing notification (Compat builder for API < 26)
        // the Notification doesn't seem to be working (there is one that the App is, so maybe that's it???)
        runningNotification = new NotificationCompat.Builder( getApplicationContext(), "Channel 1" )
                .setContentTitle( "PID Service Running" )
                .setCategory( CATEGORY_SERVICE )
                .setOngoing( true )
                .build();
        startForeground( SERVICE_NOTIFICATION_ID, runningNotification );  // NEW need to cancel on destroy
        if( DEBUG ) Log.d( TAG, "returned from startForeground()");
        
        if( intent != null ) {  // this is an initial start, not a restart after killing  // FIXME: removing this if doesn't change crash
            
            // NEW: try the new combined "maintain" for the watchdog to enable, feed, and eventually disable
            watchdogMaintainDisp = appInstance.wifiCommunicator.watchDogMaintainObservable.subscribe();
            if( watchdogMaintainDisp != null ) serviceCompositeDisp.add( watchdogMaintainDisp );
            
            // now start temperature updates
            tempUpdateDisp = appInstance.wifiCommunicator.tempFUpdater  // emits JSON temp every 5 seconds
                    .retry( 3L)
                    .map( jsonTemp -> (float) jsonTemp.getDouble( "TempF" ) )
                    .doOnNext( temp -> {
                        while( timestampedHistory.remainingCapacity() < 1 ) timestampedHistory.poll();
                        timestampedHistory.add( new Pair<>( new Date( ), temp ) );  // TODO: make it an ImmutableTriple with %DC?
                        tempHistRelay.accept( timestampedHistory );  // relay the new history to UI or anyone listening
                    })
                    .subscribe(
                            // TODO: add smoothing?
                            appInstance.bbqController::setCurrentVariableValue,
                            tempErr -> { if( DEBUG ) Log.d( TAG, "****** Error updating temp: " + tempErr.getMessage() + " ******"); }
                    );
            serviceCompositeDisp.add( tempUpdateDisp );
            
            // start the BBQ Controller loop (we think it's fixed to not change UI and run all the time)
            pidHandler.removeCallbacksAndMessages( null );  // cancel any pending loop run (flush everything)
            pidHandler.postDelayed( appInstance.bbqController.pidLoopRunnable, 2000L );  // give it a couple seconds
            
        } else {  // this is a restart
        
        }
        
        return START_STICKY;  // keep it running
    }
    
    @Override
    public void onDestroy( ) {
        serviceCompositeDisp.clear();   //  kill all the subscriptions
        appInstance.bbqController.stop();  // remove callbacks etc.
        stopForeground( true );  // remove the Notification
        wakeLock.release();
        stopSelf();  // NEW
        super.onDestroy( );
    }
    

    
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
