package net.grlewis.wifithermocouple;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ButtonBarLayout;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.jakewharton.rxbinding2.InitialValueObservable;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxCompoundButton;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class TestActivity extends AppCompatActivity {
    
    ThermocoupleApp appInstance;
    
    Button updateTempButton;
    ToggleButton toggleFanButton;
    Button setTempButton;
    ToggleButton togglePIDButton;
    
    InitialValueObservable<Boolean> fanToggleObservable;
    InitialValueObservable<Boolean> pidToggleObservable;
    Observable<Object> tempUpdateObservable;
    Observable<Object> tempSetObservable;
    
    Disposable fanToggleDisposable;
    Disposable pidToggleDisposable;
    Disposable tempUpdateDisposable;
    Disposable tempSetDisposable;
    
    
    
    
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_test );
        Toolbar toolbar = (Toolbar) findViewById( R.id.toolbar );
        setSupportActionBar( toolbar );
        
        appInstance = ThermocoupleApp.getSoleInstance();
        
        updateTempButton = (Button) findViewById( R.id.temp_button );
        toggleFanButton = (ToggleButton) findViewById( R.id.fan_button );
        setTempButton = (Button) findViewById( R.id.setpoint_button );
        togglePIDButton = (ToggleButton) findViewById( R.id.pid_button );
        
        fanToggleObservable = RxCompoundButton.checkedChanges( toggleFanButton );
        pidToggleObservable = RxCompoundButton.checkedChanges( togglePIDButton );
        tempUpdateObservable = RxView.clicks( updateTempButton );
        
        
        
        
        
        
        
        
        FloatingActionButton fab = (FloatingActionButton) findViewById( R.id.fab );
        fab.setOnClickListener( new View.OnClickListener( ) {
            @Override
            public void onClick( View view ) {
                Snackbar.make( view, "Replace with your own action", Snackbar.LENGTH_LONG )
                        .setAction( "Action", null ).show( );
            }
        } );
    }  // onCreate
    
    
    @Override
    protected void onStart( ) {
        super.onStart( );
        // set initial state
        appInstance.wifiCommunicator.fanControlWithWarning( false )  // fan off
                .observeOn( AndroidSchedulers.mainThread() )
                .subscribe( httpResponse -> {
                    toggleFanButton.setText( "FAN IS INITIALLY OFF" );
                    appInstance.state.setFanState( false );
                } );
        
        appInstance.wifiCommunicator.tempFGetter.get()
                .observeOn( AndroidSchedulers.mainThread() )
                .subscribe( tempJSON -> {
                            float tempF = (float)tempJSON.getDouble( "TempF" );
                            updateTempButton.setText( "INITIAL READING: " + String.valueOf( tempF ) + "°F" );
                            appInstance.state.setCurrentTempF( tempF );
                        }
                );
        appInstance.state.enablePid( false );
        togglePIDButton.setText( "PID IS INITIALLY DISABLED:" );
    }
    
    @Override
    protected void onResume( ) {
        super.onResume( );
        
        fanToggleDisposable = fanToggleObservable.subscribe(
                newFanState -> {
                    appInstance.state.setFanState( newFanState );
                    appInstance.wifiCommunicator.fanControlWithWarning( newFanState )
                            .observeOn( AndroidSchedulers.mainThread() )
                            .subscribe( httpResponse -> toggleFanButton.setText( newFanState? "FAN IS ON" : "FAN IS OFF" ) );
                },
                fanToggleErr -> {}  // TODO (but note we get a warning)
        );
        pidToggleDisposable = pidToggleObservable
                .observeOn( AndroidSchedulers.mainThread() )
                .subscribe(
                        newPidState -> {
                            appInstance.state.enablePid( newPidState );
                            togglePIDButton.setText( newPidState? "PID IS ENABLED" : "PID IS DISABLED" );
                        },
                        pidToggleErr -> {}  // TODO
                );
        tempUpdateDisposable = tempUpdateObservable.subscribe(
                click -> appInstance.wifiCommunicator.tempFGetter.get()
                        .observeOn( AndroidSchedulers.mainThread() )
                        .subscribe(
                                tempJSON -> updateTempButton.setText( "LAST READING: "
                                        + String.valueOf( tempJSON.getDouble( "TempF" ) ) + "°F" )
                        )
        );
        
    }  // onResume
    
    
    @Override
    protected void onPause( ) {
        fanToggleDisposable.dispose();
        pidToggleDisposable.dispose();
        tempUpdateDisposable.dispose();
        super.onPause( );
    }
}
