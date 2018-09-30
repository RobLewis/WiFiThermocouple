package net.grlewis.wifithermocouple;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.ToggleButton;

import com.jakewharton.rxbinding2.InitialValueObservable;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxCompoundButton;
import com.jakewharton.rxbinding2.widget.RxSeekBar;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

import static net.grlewis.wifithermocouple.Constants.DEBUG;

public class TestActivity extends AppCompatActivity {
    
    static final String TAG = TestActivity.class.getSimpleName();
    
    ThermocoupleApp appInstance;
    
    Button updateTempButton;
    ToggleButton toggleFanButton;
    SeekBar tempSlider;
    Button setTempButton;
    ToggleButton togglePIDButton;
    
    InitialValueObservable<Boolean> fanToggleObservable;
    InitialValueObservable<Boolean> pidToggleObservable;
    Observable<Object> tempUpdateObservable;
    Observable<Object> tempSetObservable;
    InitialValueObservable<Integer> tempSliderObservable;
    
    Disposable fanToggleDisposable;
    Disposable pidToggleDisposable;
    Disposable tempUpdateDisposable;
    Disposable tempSetDisposable;
    Disposable tempSliderDisposable;
    Disposable fanButtonTextDisposable;
    Disposable tempButtonTextDisposable;
    Disposable tempFUpdaterDisposable;  // periodic updates
    
    Subject<String> fanButtonTextPublisher;   // called to emit text to be displayed by fan state button (or other subs)
    Subject<String> tempButtonTextPublisher;  // called to emit text to be displayed by temp update button (or other subs)
    
    
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        
        if( DEBUG ) Log.d( TAG, "Entering onCreate()" );
        
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_test );
        Toolbar toolbar = (Toolbar) findViewById( R.id.toolbar );
        setSupportActionBar( toolbar );
        
        appInstance = ThermocoupleApp.getSoleInstance();
        appInstance.setTestActivityRef( this );  // install a reference to this activity in main App
        
        appInstance.bbqController.setTestActivityRef( this );
        updateTempButton = (Button) findViewById( R.id.temp_button );
        toggleFanButton = (ToggleButton) findViewById( R.id.fan_button );
        tempSlider = (SeekBar) findViewById( R.id.temp_slider );
        setTempButton = (Button) findViewById( R.id.setpoint_button );
        togglePIDButton = (ToggleButton) findViewById( R.id.pid_button );
        
        fanToggleObservable = RxCompoundButton.checkedChanges( toggleFanButton );
        pidToggleObservable = RxCompoundButton.checkedChanges( togglePIDButton );
        tempUpdateObservable = RxView.clicks( updateTempButton );
        tempSliderObservable = RxSeekBar.changes( tempSlider );
        
        fanButtonTextPublisher = BehaviorSubject.createDefault( "Uninitialized" ).toSerialized();   // thread safe
        tempButtonTextPublisher = BehaviorSubject.createDefault( "Uninitialized" ).toSerialized();  // thread safe
        
        
        
        
        
        
        
        FloatingActionButton fab = (FloatingActionButton) findViewById( R.id.fab );
        fab.setOnClickListener( new View.OnClickListener( ) {
            @Override
            public void onClick( View view ) {
                Snackbar.make( view, "Replace with your own action", Snackbar.LENGTH_LONG )
                        .setAction( "Action", null ).show( );
            }
        } );
        if( DEBUG ) Log.d( TAG, "Exiting onCreate()" );
    }  // onCreate
    
    
    @Override
    protected void onStart( ) {
        
        if( DEBUG ) Log.d( TAG, "Entering onStart()" );
        
        super.onStart( );
        
        float startingSetpoint = appInstance.pidState.getSetPoint();
        setTempButton.setText( "INITIAL TEMP SETTING: " + startingSetpoint );
        tempSlider.setProgress( Math.round( appInstance.pidState.getSetPoint() ) );
        
        // set initial appState
        appInstance.wifiCommunicator.fanControlWithWarning( false )  // fan off
                .observeOn( AndroidSchedulers.mainThread() )
                .subscribe( httpResponse -> {
                    toggleFanButton.setText( "FAN IS INITIALLY OFF" );
                    appInstance.appState.setFanState( false );  // TODO: need?
                    appInstance.pidState.setOutputOn( false );
                } );
        
        appInstance.wifiCommunicator.tempFGetter.get()
                .observeOn( AndroidSchedulers.mainThread() )
                .subscribe( tempJSON -> {
                            float tempF = (float)tempJSON.getDouble( "TempF" );
                            updateTempButton.setText( "INITIAL READING: " + String.valueOf( tempF ) + "°F" );
                            appInstance.appState.setCurrentTempF( tempF );  // TODO: need?
                            appInstance.pidState.setCurrentVariableValue( tempF );
                        }
                );
        tempFUpdaterDisposable = appInstance.wifiCommunicator.tempFUpdater.subscribe();
        appInstance.appState.enablePid( false );  // TODO: need?
        togglePIDButton.setText( "PID IS INITIALLY DISABLED:" );
        
        setTempButton.setText( "CURRENT TEMP SETTING: " + appInstance.pidState.getSetPoint().toString() + "°F" );
        
        appInstance.bbqController.start();
        if( DEBUG ) Log.d( TAG, "Exiting onStart()" );
        
    }
    
    @Override
    protected void onResume( ) {
        
        if( DEBUG ) Log.d( TAG, "Entering onResume()" );
        
        super.onResume( );
        
        fanToggleDisposable = fanToggleObservable.subscribe(
                newFanState -> {
                    appInstance.appState.setFanState( newFanState );  // TODO: need?
                    appInstance.pidState.setOutputOn( newFanState );
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
                            appInstance.appState.enablePid( newPidState );  // TODO: need? (just sets appState.pidEnabled)
                            appInstance.pidState.setEnabled( newPidState );
                            togglePIDButton.setText( newPidState? "PID IS ENABLED" : "PID IS DISABLED" );
                            if( newPidState ) {  // enable the PID
                                appInstance.bbqController.start();
                                if( DEBUG ) Log.d( TAG, "Attempting to start PID" );
                            } else {  // disable the PID
                                appInstance.bbqController.stop();
                                if( DEBUG ) Log.d( TAG, "Attempting to stop PID" );
                            }
                        },
                        pidToggleErr -> { Log.d( TAG, "Error with pidToggleObservable: "
                                + pidToggleErr.getMessage(), pidToggleErr );}  // TODO
                );
        tempUpdateDisposable = tempUpdateObservable.subscribe(
                click -> appInstance.wifiCommunicator.tempFGetter.get()
                        .observeOn( AndroidSchedulers.mainThread() )
                        .subscribe(
                                tempJSON -> updateTempButton.setText( "LAST READING: "
                                        + String.valueOf( tempJSON.getDouble( "TempF" ) ) + "°F" )
                        )
        );
        tempSliderDisposable = tempSliderObservable.subscribe(  // emits Integer updates
                newValue -> { setTempButton.setText( "SETPOINT: " + String.valueOf( newValue ) + "°F" );
                    appInstance.pidState.set( Float.valueOf( newValue ) );
                }
        );
        fanButtonTextDisposable = fanButtonTextPublisher.subscribe(
                buttonText -> toggleFanButton.setText( buttonText )
        );
        tempButtonTextDisposable = tempButtonTextPublisher
                .observeOn( AndroidSchedulers.mainThread() )  // else can't touch UI
                .subscribe(
                        buttonText -> updateTempButton.setText( buttonText ),
                        buttTextErr -> Log.d( TAG, "Error trying to receive tempButton text update", buttTextErr )
                );
        
        if( DEBUG ) Log.d( TAG, "Exiting onResume()" );
        
    }  // onResume
    
    
    @Override
    protected void onPause( ) {
        if( DEBUG ) Log.d( TAG, "Entering onPause()" );
        
        fanToggleDisposable.dispose();   // TODO: combine these into a Composite Disposable
        pidToggleDisposable.dispose();
        tempUpdateDisposable.dispose();  // listener for clicks on temp button
        tempSliderDisposable.dispose();
        fanButtonTextDisposable.dispose();
        tempButtonTextDisposable.dispose();
        super.onPause( );
        
        if( DEBUG ) Log.d( TAG, "Exiting onPause()" );
    }
    
    @Override
    protected void onStop( ) {
        tempFUpdaterDisposable.dispose();  // turn off periodic temp updates
        super.onStop( );
    }
}
