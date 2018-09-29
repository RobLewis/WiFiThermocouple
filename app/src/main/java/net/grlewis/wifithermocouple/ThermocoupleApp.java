package net.grlewis.wifithermocouple;

import android.app.Activity;
import android.app.Application;


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
*
* */


public class ThermocoupleApp extends Application {
    
    private static ThermocoupleApp sAppInstance;  // the Singleton instance of this Application
    
    // Application Globals (instance variables of the Singleton)
    WiFiCommunicator wifiCommunicator;
    ApplicationState appState;
    PIDState pidState;
    
    TestActivity testActivityRef;
    BBQController bbqController;
    
    
    
    
    
    // retrieve the app instance to request access to its instance variables
    public static ThermocoupleApp getSoleInstance() { return sAppInstance; } // can use this to retrieve e.g. the sole rxBleClient
    
    //public RxBleClient getRxBleClient() { return rxBleClient; }  // instance method
    
    @Override
    public void onCreate() {
        super.onCreate( );
        sAppInstance = this;  // the created App instance stores a reference to itself in the static variable
        sAppInstance.initialize();
    }
    
    protected void initialize() {
        // do all your initialization in this instance method
        // (with instance members, not static)
        wifiCommunicator = new WiFiCommunicator();
        appState = new ApplicationState();
        pidState = new PIDState();
        bbqController = new BBQController();
    
    
    
    }
    
    void setTestActivityRef( TestActivity testActivity ) { testActivityRef = testActivity; }
    
    
}
