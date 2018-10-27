package net.grlewis.wifithermocouple;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import static net.grlewis.wifithermocouple.Constants.DEBUG;

/*
 *
 * Desperation move trying to get the ThermocoupleService to start and bind
 *
 */

class ThermocoupleServiceConnection implements ServiceConnection {
    
    private static final String TAG = ThermocoupleServiceConnection.class.getSimpleName();
    
    private ThermocoupleService.LocalBinder scBinder;
    private ThermocoupleService thermocoupleService;
    
    ThermocoupleServiceConnection() {
        if( DEBUG ) Log.d( TAG, "exiting constructor" );
    }
    
    @Override
    public void onBindingDied( ComponentName name ) {
        if( DEBUG ) Log.d( TAG, "onBindingDied() called for component " + name.toShortString() );
    }
    
    @Override  // FIXME: supposed to be called when Service connects, not happening
    public void onServiceConnected( ComponentName name, IBinder service ) {
        if( DEBUG ) Log.d( TAG, "onServiceConnected() called for component " + name.toShortString()
                + " with Binder " + service.toString() );
        scBinder = (ThermocoupleService.LocalBinder) service;
        thermocoupleService = scBinder.getService();
    }
    
    @Override
    public void onServiceDisconnected( ComponentName name ) {
        if( DEBUG ) Log.d( TAG, "onServiceDisconnected() called for component " + name.toShortString() );
    }
    
    ThermocoupleService getThermocoupleService() { return thermocoupleService; }
}
