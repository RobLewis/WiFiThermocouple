package net.grlewis.wifithermocouple;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import static net.grlewis.wifithermocouple.Constants.*;



public class ThermocoupleIntentService extends JobIntentService {
    
    private final static String TAG = ThermocoupleIntentService.class.getSimpleName();
    private final static int JOB_ID = 2018_10_08;    // unique job ID for this Service
    private ComponentName thermocoupleComponentName;
    
    private boolean running;
    
    Disposable tempUpdateDisp, watchdogEnableDisp, watchdogResetDisp;
    CompositeDisposable serviceCompDisp;
    Observable<Float> tempUpdateObs;
    AsyncJSONGetter tempGetter;
    AsyncHTTPRequester watchdogEnabler;
    AsyncHTTPRequester watchdogResetter;
    OkHttpClient client;
    ThermocoupleApp appInstance;
    Observable<Response> watchdogResetObs;
    
    
    // constructor -- may not be necesssary
    public ThermocoupleIntentService() {
        super( );
    }

// Auto-created Intent Service stuff
    ////////////// below stuff auto-created for IntentService //////////////
    
    /**
     * An {@link IntentService} subclass for handling asynchronous task requests in
     * a service on a separate handler thread.
     * <p>
     * TODO: Customize class - update intent actions, extra parameters and static
     * helper methods.
     */
    
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_FOO = "net.grlewis.wifithermocouple.action.FOO";
    private static final String ACTION_BAZ = "net.grlewis.wifithermocouple.action.BAZ";
    
    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "net.grlewis.wifithermocouple.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "net.grlewis.wifithermocouple.extra.PARAM2";
    
    //public ThermocoupleIntentService( ) { super( "ThermocoupleIntentService" ); }
    
    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionFoo( Context context, String param1, String param2 ) {
        Intent intent = new Intent( context, ThermocoupleIntentService.class );
        intent.setAction( ACTION_FOO );
        intent.putExtra( EXTRA_PARAM1, param1 );
        intent.putExtra( EXTRA_PARAM2, param2 );
        context.startService( intent );
    }
    
    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionBaz( Context context, String param1, String param2 ) {
        Intent intent = new Intent( context, ThermocoupleIntentService.class );
        intent.setAction( ACTION_BAZ );
        intent.putExtra( EXTRA_PARAM1, param1 );
        intent.putExtra( EXTRA_PARAM2, param2 );
        context.startService( intent );
    }
    
    //@Override (was giving an error)
    protected void onHandleIntent( Intent intent ) {
        if ( intent != null ) {
            final String action = intent.getAction( );
            if ( ACTION_FOO.equals( action ) ) {
                final String param1 = intent.getStringExtra( EXTRA_PARAM1 );
                final String param2 = intent.getStringExtra( EXTRA_PARAM2 );
                handleActionFoo( param1, param2 );
            } else if ( ACTION_BAZ.equals( action ) ) {
                final String param1 = intent.getStringExtra( EXTRA_PARAM1 );
                final String param2 = intent.getStringExtra( EXTRA_PARAM2 );
                handleActionBaz( param1, param2 );
            }
        }
    }
    
    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo( String param1, String param2 ) {
        // TODO: Handle action Foo
        throw new UnsupportedOperationException( "Not yet implemented" );
    }
    
    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionBaz( String param1, String param2 ) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException( "Not yet implemented" );
    }
    
    
    ////////////// above stuff auto-created for IntentService //////////////
    
    
    
    /**
     * A Service subclass for handling asynchronous task requests in
     * a service on a separate handler thread.
     * <p>
     
     * NOTE: this is a JobIntentService which differs from an IntentService (see folded stuff above)
     * It must define onHandleWork( Intent )
     *
     * Methods:
     *  static void enqueueWork()     -- start work (pre-O) or enqueue as a Job (O+)
     *  boolean isStopped()           -- true if onStopCurrentWork() has been called (poll during execution of work to see if it should be stopped)
     *  IBinder onBind()              -- returns IBinder for the JobServiceEngine when running as a JobService on O+
     *  void onCreate() & void onDestroy() [no info]
     *  int onStartCommand()          -- Processes start commands when running as a pre-O service, enqueueing them to be later dispatched in onHandleWork(Intent)
     *  boolean onStopCurrentWork()   -- called if JobScheduler has decided to stop this job (generally only if it exceeds maximum execution time)
     *                                   return true to request rescheduling this and queued work (the normal and default case)
     *  void setInterruptIfStopped()  -- control whether code executing in onHandleWork(Intent) will be interrupted if the job is stopped. Default false.
     *                                   If set true, any time onStopCurrentWork() is called, the class will first call AsyncTask.cancel(true) to interrupt the running task.
     *  abstract void onHandleWork()  -- called on background thread for each work dispatched; when running as a Job, limited by the max job execution time
     *
     *  (plus methods inherited from Service, ContextWrapper, and Context)
     *
     */
    
    
    // action names that describe tasks that this Service can perform, e.g. ACTION_FETCH_NEW_ITEMS
    // (moved to Constants class)
    
    // parameters (Intent extras)
    private static final String EXTRA_SERVICE_UUID  = "net.grlewis.packagename.extra.SERVICE_UUID";
    
    
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate() of ThermocoupleIntentService called");
        appInstance = ThermocoupleApp.getSoleInstance();
        thermocoupleComponentName = new ComponentName( this, "ThermocoupleIntentService" );
        appInstance.serviceComponentName = thermocoupleComponentName;  // set name to access service with
        
        client = new OkHttpClient();  //TODO: add eager client
        serviceCompDisp = new CompositeDisposable( );
        watchdogEnabler = new AsyncHTTPRequester( ENABLE_WD_URL, client, new SerialUUIDSupplier( 0x1000 ) );
        watchdogResetter = new AsyncHTTPRequester( RESET_WD_URL, client, new SerialUUIDSupplier( 0x2000 ) );
        tempGetter = new AsyncJSONGetter( TEMP_F_URL, client, new SerialUUIDSupplier( 0x3000 ) );  // spurious
        tempUpdateObs = Observable.interval( TEMP_UPDATE_SECONDS, TimeUnit.SECONDS )
                .flatMapSingle( timeToUpdate -> tempGetter.get() )
                .retry( 2 )
                .map( tempJSON -> (float) tempJSON.getDouble( "TempF" ) );
        watchdogResetObs = Observable.interval( WATCHDOG_CHECK_SECONDS, TimeUnit.SECONDS )
                .flatMapSingle( timeToReset -> watchdogResetter.request() );
    }
    
    @Override
    public void onDestroy() {
        Log.i( TAG, "onDestroy() of ThermocoupleIntentService called: work complete" );
        super.onDestroy();
    }
    
    
    // static convenience method for enqueueing work into this Service
    // context is the context this is being called FROM
    static void enqueueWork( Context context, Intent work ) {
        enqueueWork( context, ThermocoupleIntentService.class, JOB_ID, work );
    }
    // also possible:
    // component is the published ComponentName of the class this work should be dispatched to.
    //public static void enqueueWork(@NonNull Context callerContext, @NonNull ComponentName component, int jobId, @NonNull Intent work) { }
    
    
    // This method must be implemented for JobIntentService
    // called on a background thread; upon returning from it the work is considered done:
    // can run indefinitely but may be stopped & rescheduled by Android
    // either next queued work is dispatched or the service is destroyed if nothing left to do
    // (might want to never return, normally)
    @Override
    protected void onHandleWork( Intent intent ) {
        
        // system already holding a wake lock for us so we can just go
        Log.i(TAG, "Executing work: " + intent + " " + intent.getAction() );
        
        // here we analyze the Intent to see what ACTION to perform
        if (intent != null) {
            
            switch ( intent.getAction() ) {
                
                case ACTION_START_TEMP_UPDATES:
                    final String uuidString = intent.getStringExtra(EXTRA_SERVICE_UUID);  // get parameter from Intent
                    handleActionScanService( uuidString );
                    //
                    break;
                
                case ACTION_STOP_TEMP_UPDATES:
                    break;
                
                case ACTION_START_PID:
                    break;
                
                case ACTION_STOP_PID:
                    break;
                    
                case ACTION_START_BG_SERVICE:
                    startBgService();
                    break;
                    
                case ACTION_STOP_BG_SERVICE:
                    stopBgService();
                    break;
    
                default:
                    return;  // this should cause Service to be destroyed
                
            }  // switch
        }
    }
    
    
    /**
     * Handle action SCAN_SERVICE in the provided background thread with the provided
     * UUID String
     */
    private void handleActionScanService ( String serviceUUID ){
        // TODO: Handle action Scan Service by UUID
        if( isStopped() ) { /* poll during work to find out if Android wants us to stop */ }
        throw new UnsupportedOperationException( "Not yet implemented" );
    }
    
    
    @Override  // called if the JobScheduler has decided to stop this job; return default true to request rescheduling so no work is lost
    public boolean onStopCurrentWork() {
        Log.i( TAG, "JobScheduler has decided to stop this job; we are requesting rescheduling" );
        return true;
    }
    
    
    // method to start background service
    // enables temperature updates, watchdog, and watchdog resetting
    private void startBgService() {
        // start subscriptions: temp updates, watchdog reset,
        if( !isStopped() ) {
            tempUpdateDisp = tempUpdateObs.subscribe(
                    appInstance.pidState::setCurrentVariableValue
                    // TODO: handle temp update fetch error
            );
            serviceCompDisp.add( tempUpdateDisp );
            
            watchdogEnableDisp = watchdogEnabler.request().retry( 5L ).subscribe(
                    response -> { if( DEBUG ) Log.d( TAG, "Watchdog enabled" ); }
                    // TODO: handle watchdog enable failure
            );
            serviceCompDisp.add( watchdogEnableDisp );
            
            watchdogResetDisp = watchdogResetter.request().subscribe(
                    response -> { if( DEBUG ) Log.d( TAG, "Watchdog reset" ); }
                    // TODO: handle watchdog reset failure
            );
            serviceCompDisp.add( watchdogResetDisp );
            
        }
    }
    
    private void stopBgService() {
        // dispose subscriptions
        serviceCompDisp.clear();
    }
    
    
}
