package net.grlewis.wifithermocouple;

import android.app.Application;

import java.util.ArrayList;
import java.util.List;

public class ThermocoupleApp extends Application {
    
    private static ThermocoupleApp sAppInstance;  // the Singleton instance of this Application
    
    // Application Globals (instance variables of the Singleton)
    private RxBleClient rxBleClient;
    private List<BlueDotDevice> deviceList;
    private MainActivity mainActivityRef;
    private ScanActivity scanActivityRef;
    private ScanInteractor scanInteractorRef;
    private ScanPresenter scanPresenterRef;
    
    // retrieve the app instance to get access to its instance variables
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
        //rxBleClient = RxBleClient.create( this );  // use the Application context
        deviceList = new ArrayList<>( );
        scanInteractorRef = new ScanInteractor( new BleScannerImpl( ) );
        scanPresenterRef = new ScanPresenter();
    }
    
}
