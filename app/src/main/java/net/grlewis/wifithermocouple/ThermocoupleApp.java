package net.grlewis.wifithermocouple;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;

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
    ApplicationState appState;
    PIDState pidState;
    BBQController bbqController;
    ComponentName serviceComponentName;
    ThermocoupleService thermocoupleService;
    GraphActivity graphActivity;
    
    Intent startThermoServiceIntent;
    
    // retrieve the app instance to request access to its instance variables
    public static ThermocoupleApp getSoleInstance() { return sAppInstance; }
    
    @Override
    public void onCreate() {
        
        if( DEBUG ) Log.d( TAG, "Entering onCreate()");
        
        super.onCreate( );
        sAppInstance = this;  // the created App instance stores a reference to itself in the static variable
        sAppInstance.initialize();
        
        if( DEBUG ) Log.d( TAG, "Exiting onCreate()");
    }
    
    protected void initialize() {
        // do initialization in this instance method (with instance members, not static)
        if( DEBUG ) Log.d( TAG, "App initialize() entered");
        
        startThermoServiceIntent = new Intent( getApplicationContext(), ThermocoupleService.class );
        // Note: startForegroundService() requires API 26
        serviceComponentName = getApplicationContext().startService( startThermoServiceIntent );  // bind to it AND start it
        if( DEBUG ) {  // seems to never fail to start
            if( serviceComponentName != null ) {  // ComponentName doesn't contain any real info
                Log.d( TAG, "Service running with ComponentName " + serviceComponentName.toShortString( ) );  // looks OK
            }else throw new NullPointerException( "Attempt to start Service failed" );
        }
        
        wifiCommunicator = new WiFiCommunicator();  // TODO: is this right?
        appState = new ApplicationState();  // TODO: need? (maybe if we engage controller's internal DC etc.)
        pidState = new PIDState();
        bbqController = new BBQController();
    
        //SystemClock.sleep( 2000L );  // TODO: desperation (didn't help)
        
        if( DEBUG ) Log.d( TAG, "App initialize() exited");
    }
    
    
    
    void setGraphActivity( GraphActivity graphActivity ) { this.graphActivity = graphActivity; }
    
    void setServiceRef( ThermocoupleService serviceRef ) { thermocoupleService = serviceRef; }  // FIXME: desperation
    
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
