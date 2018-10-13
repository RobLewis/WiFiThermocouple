package net.grlewis.wifithermocouple;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import static android.support.v4.app.NotificationCompat.CATEGORY_SERVICE;
import static net.grlewis.wifithermocouple.Constants.SERVICE_NOTIFICATION_ID;

public class ThermocoupleService extends Service {
    public ThermocoupleService( ) {
    }
    
    @Override
    public IBinder onBind( Intent intent ) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException( "Not yet implemented" );
    }
    
    
    @Override
    public int onStartCommand( Intent intent, int flags, int startId ) {
        
        // if you want to keep the Service from being killed, must have an ongoing notification (Compat builder for API < 26)
        startForeground( SERVICE_NOTIFICATION_ID, new NotificationCompat.Builder( getApplicationContext(), "Channel 1" )
                .setContentTitle( "PID Service Running" )
                .setCategory( CATEGORY_SERVICE )
                .setOngoing( true )
                .build());
        
        return super.onStartCommand( intent, flags, startId );
    }
}
