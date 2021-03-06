package net.grlewis.wifithermocouple;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;

import com.jakewharton.rxrelay2.BehaviorRelay;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

import static net.grlewis.wifithermocouple.Constants.DEBUG;


/*
 *
 *   2018-10: updated firmware to 0.7 to try to work around "nan" temp readings
 *
 *
 * */


public class ThermocoupleApp extends Application {
    
    static final String TAG = ThermocoupleApp.class.getSimpleName();
    
    private static ThermocoupleApp sAppInstance;  // the Singleton instance of this Application
    
    // Application Globals (instance variables of the Singleton)
    WiFiCommunicator wifiCommunicator;
    ApplicationState applicationState;
    PIDState pidState;
    BBQController bbqController;
    ThermocoupleService thermocoupleService;  // set by ThermocoupleService onCreate() calling appInstance.setServiceRef( this )
    GraphActivity graphActivity;
    ComponentName serviceComponentName;
    
    BehaviorRelay<Boolean> serviceConnectionState;  // extends Relay, which extends Observable and implements Consumer TODO: need?
    
    Intent startThermoServiceIntent;
    
    // retrieve the app instance to request access to its instance variables
    public static ThermocoupleApp getSoleInstance() { return sAppInstance; }
    
    @Override
    public void onCreate() {
        
        if( DEBUG ) Log.d( TAG, "Entering onCreate()");
        
        super.onCreate( );
        sAppInstance = this;  // the created App instance stores a reference to itself in the static variable
        sAppInstance.initialize();
        
        serviceConnectionState = BehaviorRelay.createDefault( false );  // TODO: need?
        
        if( DEBUG ) Log.d( TAG, "Exiting onCreate()");
    }
    
    protected void initialize() {
        // do initialization in this instance method (with instance members, not static)
        if( DEBUG ) Log.d( TAG, "App initialize() entered");
    
        wifiCommunicator = new WiFiCommunicator();  // TODO: is this right?  NEW: moved ahead of starting service
        applicationState = new ApplicationState();  // TODO: need? (maybe if we engage controller's internal DC etc.)
        pidState = new PIDState();
        bbqController = new BBQController();
        
        // start Service (not usable until onServiceConnected() callback
        startThermoServiceIntent = new Intent( getApplicationContext(), ThermocoupleService.class );
        // Note: startForegroundService() requires API 26
        serviceComponentName = getApplicationContext().startService( startThermoServiceIntent );
        if( DEBUG ) {  // seems to never fail to start
            if( serviceComponentName != null ) {  // ComponentName doesn't contain any real info
                Log.d( TAG, "Service running with ComponentName " + serviceComponentName.toShortString( ) );  // looks OK
            }else throw new NullPointerException( "Attempt to start Service failed" );
        }
        
        //SystemClock.sleep( 2000L );  // TODO: desperation (didn't help) [if we were going to do this, try after bindService() call]
        
        if( DEBUG ) Log.d( TAG, "App initialize() exited");
    }
    
    
    
    void setGraphActivity( GraphActivity graphActivity ) { this.graphActivity = graphActivity; }  // TODO: need?
    
    void setServiceRef( ThermocoupleService serviceRef ) { thermocoupleService = serviceRef; }  // FIXME: desperation; elim? need?
    
/*
    @Override
    public void registerActivityLifecycleCallbacks( ActivityLifecycleCallbacks callback ) {
        super.registerActivityLifecycleCallbacks( callback );
    }
    
    
    class ThermocoupleLifecycleCallbacks implements ActivityLifecycleCallbacks {
        @Override
        public void onActivityStopped( Activity activity ) {
        
        }
        // have to implement all the Lifecycle methods and register this with the Application
        @Override
        public void onActivityDestroyed( Activity activity ) {
        
        }
    }
*/



}
