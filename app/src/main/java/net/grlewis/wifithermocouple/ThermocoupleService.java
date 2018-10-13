package net.grlewis.wifithermocouple;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import static android.support.v4.app.NotificationCompat.CATEGORY_SERVICE;
import static net.grlewis.wifithermocouple.Constants.SERVICE_NOTIFICATION_ID;

public class ThermocoupleService extends Service {
    
    private static final String TAG = ThermocoupleService.class.getSimpleName();
    
    public ThermocoupleService( ) {
    }
    
    
    
    @Override
    // called by Android every time a client calls Context.startService( Intent )
    // called with a null Intent if being restarted after killed (shouldn't happen)
    public int onStartCommand( Intent intent, int flags, int startId ) {  // Intent contains calling context and name of this class
        
        // if you want to keep the Service from being killed, must have an ongoing notification (Compat builder for API < 26)
        startForeground( SERVICE_NOTIFICATION_ID, new NotificationCompat.Builder( getApplicationContext(), "Channel 1" )
                .setContentTitle( "PID Service Running" )
                .setCategory( CATEGORY_SERVICE )
                .setOngoing( true )
                .build() );
        
        if( intent != null ) {  // this is an initial start, not a restart after killing
    
    
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


}
