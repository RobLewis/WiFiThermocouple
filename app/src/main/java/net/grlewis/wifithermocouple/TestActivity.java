package net.grlewis.wifithermocouple;

import android.annotation.SuppressLint;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.jakewharton.rxbinding2.InitialValueObservable;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxCompoundButton;
import com.jakewharton.rxbinding2.widget.RxSeekBar;
import com.jakewharton.rxbinding2.widget.SeekBarChangeEvent;
import com.jakewharton.rxbinding2.widget.SeekBarProgressChangeEvent;
import com.jakewharton.rxbinding2.widget.SeekBarStopChangeEvent;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

import static net.grlewis.wifithermocouple.Constants.DEBUG;

public class TestActivity extends AppCompatActivity implements ServiceConnection {
    
    static final String TAG = TestActivity.class.getSimpleName();
    
    ThermocoupleApp appInstance;                          // set in onCreate()
    
    Button updateTempButton;
    ToggleButton toggleFanButton;
    SeekBar tempSlider;
    Button setTempButton;
    ToggleButton togglePIDButton;
    
    InitialValueObservable<Boolean> fanToggleObservable;  // emits clicks on Fan On/Off button TODO: eventually eliminate?
    InitialValueObservable<Boolean> pidToggleObservable;  // emits clicks on PID enable/disable button
    Observable<Object> tempUpdateObservable;              // emits clicks on Update Current Temp button
    Observable<Object> tempSetObservable;                 // TODO: unused
    //InitialValueObservable<Integer> tempSliderObservable;  // replaced by... (TODO: replace slider?)
    InitialValueObservable<SeekBarChangeEvent> tempSliderEventObservable;  // captures value, start & stop, not just value
    
    Disposable fanToggleDisposable;
    Disposable pidToggleDisposable;
    Disposable tempUpdateDisposable;
    Disposable setTempButtonTextDisposable;
    Disposable tempSliderDisposable;
    Disposable fanButtonTextDisposable;
    Disposable tempButtonTextDisposable;
    Disposable tempFUpdaterDisposable;  // periodic updates
    Disposable pidButtonTextDisposable;
    Disposable watchdogEnableDisposable;  // disables watchdog when disposed (we hope)
    Disposable watchdogResetDisposable;
    Disposable watchdogStatusUpdatesDisposable;
    
    Disposable pidParameterChangesDisp;   // Service impl
    CompositeDisposable serviceImplDisp;  // Service impl
    
    // attempt at ViewModel to save temp history  TODO: dump? (all Rx)
    private UIStateModel uiStateModel;
    
    ThermocoupleService thermoServiceRef;           // set when Service is bound
    ThermocoupleService.LocalBinder thermoBinder;   // part of binding operation, not used otherwise
    ComponentName serviceComponentName;             // returned by .startService(); just logged
    boolean serviceBound;                           // did service binding succeed?
    
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        
        if( DEBUG ) Log.d( TAG, "Entering onCreate()" );
        
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_test );
        Toolbar toolbar = (Toolbar) findViewById( R.id.toolbar );  // TODO: use?
        setSupportActionBar( toolbar );
        
        appInstance = ThermocoupleApp.getSoleInstance();
        appInstance.setTestActivityRef( this );  // install a reference to this activity in main App TODO: need?
        appInstance.bbqController.setTestActivityRef( this );  // TODO: need?
        
        // should be OK for now
        updateTempButton = (Button) findViewById( R.id.temp_button );
        toggleFanButton = (ToggleButton) findViewById( R.id.fan_button );
        tempSlider = (SeekBar) findViewById( R.id.temp_slider );
        setTempButton = (Button) findViewById( R.id.setpoint_button );
        togglePIDButton = (ToggleButton) findViewById( R.id.pid_button );
        
        fanToggleObservable = RxCompoundButton.checkedChanges( toggleFanButton );  // emits clicks on Fan On/Off button TODO: eventually eliminate?
        pidToggleObservable = RxCompoundButton.checkedChanges( togglePIDButton );  // emits clicks on PID enable/disable button
        tempUpdateObservable = RxView.clicks( updateTempButton );                  // emits clicks on Update Current Temp button
        //tempSliderObservable = RxSeekBar.changes( tempSlider );                  // replaced by...
        tempSliderEventObservable = RxSeekBar.changeEvents( tempSlider );          // now emits all events, not just motion
        
        serviceImplDisp= new CompositeDisposable(  );      // for Service impl
        
        uiStateModel = ViewModelProviders.of(this).get( UIStateModel.class );  // TODO: dump?
        
        // Create the LiveData observer that updates the UI.  TODO: dump?
        final Observer<UIStateModel.UIState> uiStateObserver = new Observer<UIStateModel.UIState>() {
            @Override
            public void onChanged( @Nullable UIStateModel.UIState uiState ) {
                // update the UI
                // TODO: better to use setText or the Subjects?
                Log.d( TAG, "LiveData updated UI state with temp " + uiState.getUITempUpdate( ) );  // Works!!
            }
        };
        // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        uiStateModel.getCurrentUIState().observe(this, uiStateObserver );
        
        //appInstance.wifiCommunicator = new WiFiCommunicator();  // moved it from Application because Activity needs to exist first
        
        
        FloatingActionButton fab = (FloatingActionButton) findViewById( R.id.fab );  // what TODO with this?
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
    @SuppressLint( "CheckResult" )  // suppress "result of subscribe is not used" warnings
    protected void onStart( ) {
        
        if( DEBUG ) Log.d( TAG, "Entering onStart()" );
        
        super.onStart( );
        
        // part of Service implementation
        bindThermoServiceIntent = new Intent( getApplicationContext(), ThermocoupleService.class );
        if( !( serviceBound = bindService( bindThermoServiceIntent, /*ServiceConnection interface*/ this, Context.BIND_AUTO_CREATE ) ) ) // flag: create the service if it's bound
            throw new RuntimeException( TAG + ": bindService() call in onStart() failed" );
        serviceComponentName = getApplicationContext().startService( bindThermoServiceIntent );  // bind to it AND start it
        if( DEBUG ) Log.d( TAG, "Service running with ComponentName " + serviceComponentName.toShortString() );  // looks OK
        
        pidParameterChangesDisp = appInstance.pidState.pidStatePublisher
                .observeOn( AndroidSchedulers.mainThread() )  // don't forget!
                .subscribe(  // receive updated parameters & redraw UI
                        updatedParams -> {
                            updateTempButton.setText( "Current Temperature: " + updatedParams.currentVariableValue + "°F" );  // FIXME: not getting set?
                            togglePIDButton.setText( "PID enabled: " + updatedParams.enabled
                                    + (updatedParams.intClamped? " (clamped)" : "") );
                            toggleFanButton.setText( "PID output on: " + updatedParams.outputOn );
                            setTempButton.setText( "Current Setpoint: " + updatedParams.setPoint + "°F" );
                        }
                );
        serviceImplDisp.add( pidParameterChangesDisp );
        
        // above is part of Service implementation
        
        
        tempSlider.setProgress( Math.round( appInstance.bbqController.getSetpoint() ) );
        
        // set initial appState
        // make sure fan is off
        // TODO: Disposable?
        appInstance.wifiCommunicator.fanControlWithWarning( false )  // fan off  FIXME: first bad UUID?
                .retry( 2 )  // try up to 3 times
                //.observeOn( AndroidSchedulers.mainThread()  (already in the source)
                .subscribe(
                        httpResponse -> {
                            //fanButtonTextPublisher.onNext( "FAN IS INITIALLY OFF" );  // replaced by...
                            toggleFanButton.setText( "FAN IS INITIALLY OFF" );
                            //appInstance.pidState.setOutputOn( false );  // fanControlWithWarning takes care of this
                        },
                        httpError -> {
                            Toast.makeText( TestActivity.this, "Fan shutoff in onStart() failed after retries"
                                    + httpError.getMessage(), Toast.LENGTH_LONG ).show();
                            //fanButtonTextPublisher.onNext( "Error turning fan off in onStart()" );  // replaced by...
                            toggleFanButton.setText( "Error turning fan off in onStart()" );
                        } );
        
        
        
        // TODO: keep?
        //setTempButton.setText( "CURRENT TEMP SETTING: " + appInstance.pidState.getSetPoint().toString() + "°F" );
        //appInstance.bbqController.start();  // don't start ON
        
        
        if( DEBUG ) Log.d( TAG, "Exiting onStart()" );
    }
    
    @Override
    protected void onResume( ) {
        
        if( DEBUG ) Log.d( TAG, "Entering onResume()" );
        
        super.onResume( );
        
        // handle clicks on the Fan On/Off button: switch fan and update button text
        // TODO: not part of final system?
        fanToggleDisposable = fanToggleObservable
                //.skipInitialValue()  // discard the immediate emission on subscription TODO: does this work?
                .skip( 1L )
                .subscribe(
                        newFanState -> {  // fan on/off button clicked
                            //appInstance.pidState.setOutputOn( newFanState );
                            appInstance.bbqController.setOutputOn( newFanState );  // NEW
//                            appInstance.wifiCommunicator.fanControlWithWarning( newFanState )
//                                    .observeOn( AndroidSchedulers.mainThread() )
//                                    //.subscribe( httpResponse -> toggleFanButton.setText( newFanState? "FAN IS ON" : "FAN IS OFF" ) );
//                                    .subscribe(
//                                            response -> {
//                                                toggleFanButton.setText( newFanState? "Fan turned on with button" : "Fan turned off with button" );
//                                                if( DEBUG ) Log.d( TAG, newFanState? "Fan turned on with button" : "Fan turned off with button" );
//                                            },
//                                            fanSwitchErr -> Log.d( TAG, "Error manually switching fan: " + fanSwitchErr.getMessage() )
//                                    );
                        },
                        fanToggleErr -> {
                            Log.d( TAG, "Error getting fan button click: " + fanToggleErr.getMessage() );
                        }  // TODO (note we get a warning)
                );
        appInstance.onPauseDisposables.add( fanToggleDisposable );
        
        
        // handle click events for toggle button to enable/disable PID
        pidToggleDisposable = pidToggleObservable
                .observeOn( AndroidSchedulers.mainThread() )
                .subscribe(
                        newPidState -> {
                            //appInstance.appState.enablePid( newPidState );  // TODO: need? (just sets appState.pidEnabled)
//                            appInstance.pidState.setEnabled( newPidState );
                            if( newPidState ) {  // enable the PID
                                appInstance.bbqController.start();  // NEW
//                                if( appInstance.bbqController.start() ) {
//                                    pidButtonTextPublisher.onNext( appInstance.pidState.intIsClamped( )?
//                                            "PID is enabled (clamped)" : "PID is enabled" );
//                                    if ( DEBUG ) Log.d( TAG, "PID start command succeeded" );
//                                } else { // problem starting
//                                    if( DEBUG ) Log.d( TAG, "Attempt to start PID failed");
//                                }
                            } else {  // disable the PID
                                appInstance.bbqController.stop();  // remove handler scheduled tasks, stop fan, set disabled
                                if( DEBUG ) Log.d( TAG, "Attempting to stop PID" );
                            }
                        },
                        pidToggleErr -> { Log.d( TAG, "Error with pidToggleObservable: "
                                + pidToggleErr.getMessage(), pidToggleErr );}  // TODO
                );
        appInstance.onPauseDisposables.add( pidToggleDisposable );
        
        
        // handle clicks on the temperature update button (manual update)
        tempUpdateDisposable = tempUpdateObservable.subscribe(
                click -> appInstance.wifiCommunicator.tempFGetter.get()
                        .map( tempJSON -> (float) tempJSON.getDouble( "TempF" ) )  // NEW
                        .retry( 2 )  // NEW: get a bad reading occasionally
                        .observeOn( AndroidSchedulers.mainThread() )
                        .subscribe(
                                tempFloat -> {
                                    updateTempButton.setText( "LAST READING: " + tempFloat + "°F" );
                                    if( DEBUG ) Log.d( TAG, "Temp manually updated successfully" );
                                },
                                tempErr -> {
                                    updateTempButton.setText( "Manual temp update error: "
                                            + tempErr.getMessage() );
                                    if( DEBUG ) Log.d( TAG, "Error getting temp update: " + tempErr.getMessage() );
                                }
                        )
        );
        appInstance.onPauseDisposables.add( tempUpdateDisposable );
        
        
        
        tempSliderDisposable = tempSliderEventObservable
                .observeOn( AndroidSchedulers.mainThread() )
                .subscribe(  // now emits SeekBarChangeEvent
//                newValue -> { setTempButton.setText( "SETPOINT: " + String.valueOf( newValue ) + "°F" );
//                    appInstance.pidState.set( Float.valueOf( newValue ) );
//                }
                        newEvent -> {
                            if( newEvent instanceof SeekBarProgressChangeEvent ) {
                                setTempButton.setText( "SETPOINT: "
                                        + String.valueOf( ((SeekBarProgressChangeEvent) newEvent).progress() ) + "°F" );
                            } else if( newEvent instanceof SeekBarStopChangeEvent ) {
                                appInstance.bbqController.set( (float) tempSlider.getProgress() );  // FIXME: wasn't goin through BBQController
                                if( DEBUG ) Log.d( TAG, "Setpoint changed to " + tempSlider.getProgress() );
                            }
                        }
                );
        appInstance.onPauseDisposables.add( tempSliderDisposable );
        
        
        
        if( DEBUG ) Log.d( TAG, "Exiting onResume()" );
        
    }  // onResume
    
    
    @Override
    protected void onPause( ) {
        if( DEBUG ) Log.d( TAG, "Entering onPause()" );
        
//        fanToggleDisposable.dispose();   // TODO: combine these into a Composite Disposable
//        pidToggleDisposable.dispose();
//        tempUpdateDisposable.dispose();  // listener for clicks on temp button
//        tempSliderDisposable.dispose();
//        fanButtonTextDisposable.dispose();
//        tempButtonTextDisposable.dispose();
//        pidToggleDisposable.dispose();
//        setTempButtonTextDisposable.dispose();
        
        appInstance.onPauseDisposables.clear();
        
        super.onPause( );
        
        if( DEBUG ) Log.d( TAG, "Exiting onPause()" );
    }
    
    @Override
    protected void onStop( ) {
        if( DEBUG ) Log.d( TAG, "Entering onStop()" );
        
        //tempFUpdaterDisposable.dispose();           // turn off periodic temp updates  FIXME: crash NPE
        serviceImplDisp.clear();              // new composite for Service implementation
        
        //watchdogEnableDisposable.dispose();         // disable watchdog  FIXME: crash NPE
        //watchdogStatusUpdatesDisposable.dispose();  // stop getting watchdog status updates
        //watchdogResetDisposable.dispose();          // stop resetting watchdog timer
        super.onStop( );
        
        if( DEBUG ) Log.d( TAG, "Exiting onStop()" );
    }
    
    // TODO: unbind service onDestroy() (?)
    
    
    @Override
    protected void onDestroy( ) {
        if( serviceBound ) {
            getApplicationContext().unbindService( this );;     // try to avoid leaking Service ('this' is ServiceConnection)
        } else {
            if( DEBUG ) Log.d( TAG, "onDestroy() found that ThermocoupleService was not bound" );
        }
        super.onDestroy( );
    }
    
    /*---------------------------------SERVICE CONNECTION INTERFACE-----------------------------------*/
    // Created & used by onStart() in call to bindService()
    Intent bindThermoServiceIntent; // can pass extra data to the Service if we need to
    @Override
    // Here, the IBinder has a getService() method that returns a reference to the Service instance
    @SuppressWarnings( "static-access" )
    public void onServiceConnected( ComponentName className, IBinder service ) {
        Log.d( TAG, "Entering onServiceConnected()" );
        // We've bound to Service, cast the IBinder and get Service instance
        thermoBinder = (ThermocoupleService.LocalBinder) service;
        // (casting it makes compiler aware of existence of getService() method)
        thermoServiceRef = thermoBinder.getService();
        if( thermoServiceRef == null ) throw new RuntimeException( TAG
                + ": onServiceConnected returned null from getService()" );
        Log.d( TAG, "Finished onServiceConnected()" );
    }
    @Override // called when the connection with the service has been unexpectedly disconnected
    public void onServiceDisconnected( ComponentName name ) {  // component name of the service whose connection has been lost.
        Log.d( TAG, "Finished onServiceDisconnected()" );
    }
    /*------------------------------------------------------------------------------------------------*/
    
    
}
