package net.grlewis.wifithermocouple;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;

import io.reactivex.disposables.CompositeDisposable;

import static net.grlewis.wifithermocouple.Constants.DEBUG;


/*
*
* should be possible to access from an Activity with something like
*   ThermocoupleApp appInstance = (ThermocoupleApp)getApplication().getSoleInstance();
*
*
* Communication scheme
*
*   This app has a temperature polling Observable built on Observable.interval() with, say, a 5-second period
*   The app subscribes to it to start polling and disposes to stop it.
*   At each polling time, use AsyncHTTPRequester to request the temperature (by .request().subscribe()
*     The Subscriber's .onSuccess( JSONObject ) handles the returned temperature.
*
*
*   A second Observable.interval() with perhaps a 40-second interval handles the watchdog timer (200 sec timeout)
*   For safety, we want the watchdog timer always enabled (shut down fan & warn if it ever elapses)
*   At each polling time, use AsyncHTTPRequester
*     Request watchdog status with Single timeout of, say, 10 seconds: .timeout( long, TimeUnit )
*       If we request a TimeoutException error or WatchdogAlarm || !WatchdogEnabled, shut down and alarm
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
    
    TestActivity testActivityRef;
    
    SerialUUIDSupplier httpUUIDSupplier;
    SerialUUIDSupplier jsonUUIDSupplier;
    
    CompositeDisposable onPauseDisposables;
    CompositeDisposable onStopDisposables;
    
    Intent startServiceIntent;
    
    
    
    // retrieve the app instance to request access to its instance variables
    public static ThermocoupleApp getSoleInstance() { return sAppInstance; } // can use this to retrieve e.g. the sole rxBleClient
    
    @Override
    public void onCreate() {
        
        if( DEBUG ) Log.d( TAG, "Entering onCreate()");
        
        super.onCreate( );
        sAppInstance = this;  // the created App instance stores a reference to itself in the static variable
        sAppInstance.initialize();
    
        if( DEBUG ) Log.d( TAG, "Exiting onCreate()");
    }
    
    protected void initialize() {
        // do all your initialization in this instance method
        // (with instance members, not static)
        wifiCommunicator = new WiFiCommunicator();  // TODO: is this right?
        appState = new ApplicationState();  // TODO: need? (maybe if we engage controller's internal DC etc.)
        pidState = new PIDState();
        bbqController = new BBQController();
        
        onPauseDisposables = new CompositeDisposable(  );
        onStopDisposables = new CompositeDisposable(  );
        
    
    }
    
    
    void setTestActivityRef( TestActivity testActivity ) { testActivityRef = testActivity; }
    
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
