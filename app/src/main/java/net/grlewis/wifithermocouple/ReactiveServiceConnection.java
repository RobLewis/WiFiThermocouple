package net.grlewis.wifithermocouple;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.jakewharton.rxrelay2.BehaviorRelay;

import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/*
 * subscribe to bind, dispose to unbind
 *
 * emits service reference
 * completes if service goes away?
 * errors if service can't be bound
 * unbinds if disposed
 *
 * */

class ReactiveServiceConnection<S> implements ServiceConnection, ObservableSource<S> {  // S is the Service type
    // note ObservableSource calls the Observer's onSubscribe(), onNext(), onError(), and onComplete() methods
    
    private Observer subscriber;
    private RSCDisposable disposable;
    private boolean disposed;
    
    private S serviceRef;
    
    
    private class RSCDisposable implements Disposable {  // has methods .dispose() and .isDisposed()
        
        @Override // if called, unbind Service
        public void dispose( ) {
            disposed = true;
            ////////
        }
        
        @Override
        public boolean isDisposed( ) {
            return disposed;
        }
    }
    
    
    // constructor
    public ReactiveServiceConnection( ) {
        this.disposable = new RSCDisposable();
    }
    
    
    @Override
    // respond by calling observer.onSubscribe( Disposable d )
    public void subscribe( Observer<? super S> observer ) {
        subscriber = observer;
        observer.onSubscribe( disposable );
        ///////// bind service & emit connection
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
