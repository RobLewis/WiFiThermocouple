package net.grlewis.wifithermocouple;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.jakewharton.rxrelay2.BehaviorRelay;

import io.reactivex.ObservableSource;
import io.reactivex.Observer;

/*
* subscribe to bind, dispose to unbind
*
* emits service reference
* completes if service goes away?
* errors if service can't be bound
*
* */

class ReactiveServiceConnection<Service> implements ServiceConnection, ObservableSource<Service> {
    
    
    Observer subscriber;
    
    
    @Override  // respond by calling observer.onSubscribe( Disposable d )
    public void subscribe( Observer<? super Service> observer ) {
        subscriber = observer;
        /////////
    }
    
    @Override
    public void onServiceConnected( ComponentName name, IBinder service ) {
    
    }
    
    @Override
    public void onServiceDisconnected( ComponentName name ) {
    
    }
    
    @Override
    public void onBindingDied( ComponentName name ) {
    
    }
}
