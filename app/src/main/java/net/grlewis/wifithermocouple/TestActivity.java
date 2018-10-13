package net.grlewis.wifithermocouple;

import android.annotation.SuppressLint;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
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
    
    InitialValueObservable<Boolean> fanToggleObservable;  // emits clicks on Fan On/Off button
    InitialValueObservable<Boolean> pidToggleObservable;
    Observable<Object> tempUpdateObservable;
    Observable<Object> tempSetObservable;
    //InitialValueObservable<Integer> tempSliderObservable;
    InitialValueObservable<SeekBarChangeEvent> tempSliderEventObservable;  // captures start & stop, not just value
    
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
    
    Subject<String> fanButtonTextPublisher;         // called to emit text to be displayed by fan state button (or other subs)
    Subject<String> updateTempButtonTextPublisher;  // called to emit text to be displayed by temp update button (or other subs)
    Subject<String> pidButtonTextPublisher;         // called to emit text to be displayed by PID enable/disable button (or other subs)
    Subject<String> setTempButtonTextPublisher;     // called to emit text to be displayed by PID enable/disable button (or other subs)
    
    // attempt at ViewModel to save temp history
    private UIStateModel uiStateModel;
    
    
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        
        if( DEBUG ) Log.d( TAG, "Entering onCreate()" );
        
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_test );
        Toolbar toolbar = (Toolbar) findViewById( R.id.toolbar );  // TODO: use?
        setSupportActionBar( toolbar );
        
        appInstance = ThermocoupleApp.getSoleInstance();
        appInstance.setTestActivityRef( this );  // install a reference to this activity in main App
        appInstance.bbqController.setTestActivityRef( this );  // TODO: need?
        
        updateTempButton = (Button) findViewById( R.id.temp_button );
        toggleFanButton = (ToggleButton) findViewById( R.id.fan_button );
        tempSlider = (SeekBar) findViewById( R.id.temp_slider );
        setTempButton = (Button) findViewById( R.id.setpoint_button );
        togglePIDButton = (ToggleButton) findViewById( R.id.pid_button );
        
        fanToggleObservable = RxCompoundButton.checkedChanges( toggleFanButton );
        pidToggleObservable = RxCompoundButton.checkedChanges( togglePIDButton );
        tempUpdateObservable = RxView.clicks( updateTempButton );
        //tempSliderObservable = RxSeekBar.changes( tempSlider );
        tempSliderEventObservable = RxSeekBar.changeEvents( tempSlider );  // now detect all events, not just motion
        
        
        // BehaviorSubject emits its last observed value plus future values to each new subscriber
        // .toSerialized() converts any kind of Subject to a plain Subject (see above declarations)
        fanButtonTextPublisher = BehaviorSubject.createDefault( "Uninitialized" ).toSerialized();   // thread safe TODO: need serialized? always UI thread?
        updateTempButtonTextPublisher = BehaviorSubject.createDefault( "Uninitialized" ).toSerialized();  // thread safe
        pidButtonTextPublisher = BehaviorSubject.createDefault( "Uninitialized" ).toSerialized();   // thread safe
        setTempButtonTextPublisher = BehaviorSubject.createDefault( "Uninitialized" ).toSerialized();   // thread safe
        
        uiStateModel = ViewModelProviders.of(this).get( UIStateModel.class );
        
        // Create the LiveData observer that updates the UI.
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
        
        appInstance.wifiCommunicator = new WiFiCommunicator();  // moved it from Application because Activity needs to exist first
        
        
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
        
        float startingSetpoint = appInstance.pidState.getSetPoint();  // DEFAULT_SETPOINT constant
        updateTempButtonTextPublisher.onNext( "INITIAL TEMP SETTING: " + startingSetpoint );
        tempSlider.setProgress( Math.round( appInstance.pidState.getSetPoint() ) );
        pidButtonTextPublisher.onNext( "PID IS INITIALLY DISABLED:" );
        
        // set initial appState
        // make sure fan is off
        // TODO: Disposable?
        appInstance.wifiCommunicator.fanControlWithWarning( false )  // fan off
                .retry( 2 )  // try up to 3 times
                .observeOn( AndroidSchedulers.mainThread() )
                .subscribe(
                        httpResponse -> {
                            fanButtonTextPublisher.onNext( "FAN IS INITIALLY OFF" );
                            //appInstance.pidState.setOutputOn( false );  // fanControlWithWarning takes care of this
                        },
                        httpError -> {
                            Toast.makeText( TestActivity.this, "Fan shutoff in onStart() failed after retries"
                                    + httpError.getMessage(), Toast.LENGTH_SHORT ).show();
                            fanButtonTextPublisher.onNext( "Error turning fan off in onStart()" );
                        } );
        
        // get initial temperature reading
        // TODO: Disposable?
        appInstance.wifiCommunicator.tempFGetter.get()
                .observeOn( AndroidSchedulers.mainThread() )
                .subscribe( tempJSON -> {
                            float tempF = (float)tempJSON.getDouble( "TempF" );
                            updateTempButtonTextPublisher.onNext( "INITIAL READING: " + String.valueOf( tempF ) + "°F" );
                            appInstance.pidState.setCurrentVariableValue( tempF );
                        },
                        tempErr -> Log.d( TAG, "Error getting initial temp reading: " + tempErr.getMessage() )
                );
        
        
        // initiate periodic temperature updates
        tempFUpdaterDisposable = appInstance.wifiCommunicator.tempFUpdater
                .retry( 3 )  // TODO: does this help with failures returning null?
                .subscribe(
                jsonF -> { },
                tempErr -> Log.d( TAG, "Error from tempFUpdater: " + tempErr.getMessage() )  // TODO: was this the missing error handler?
        );  // keeps pidState updated too
        appInstance.onStopDisposables.add( tempFUpdaterDisposable );
        
        
        // enable watchdog timer
        /*appInstance.wifiCommunicator.watchdogEnableSingle.request()  // returns Single
                .observeOn( AndroidSchedulers.mainThread() )
                .subscribe(
                        response -> { if( DEBUG ) Log.d( TAG, "Watchdog timer enabled" ); },
                        wdEnableErr -> { if( DEBUG ) Log.d( TAG, "Error enabling watchdog", wdEnableErr );
                            Toast.makeText( TestActivity.this,
                                    "Error enabling watchdog: " + wdEnableErr.getMessage(), Toast.LENGTH_SHORT ).show(); }
                );  // TODO: make sure it worked*/
        
        watchdogEnableDisposable = appInstance.wifiCommunicator.enableWatchdogObservable  // dispose to disable
                .observeOn( AndroidSchedulers.mainThread() )  // added
                .subscribe( response -> { if( DEBUG ) Log.d( TAG, "Watchdog timer enabled" ); },
                        wdEnableErr -> { if( DEBUG ) Log.d( TAG, "Error enabling watchdog", wdEnableErr );
                            Toast.makeText( TestActivity.this,
                                    "Error enabling watchdog: " + wdEnableErr.getMessage(), Toast.LENGTH_SHORT ).show(); });
        appInstance.onStopDisposables.add( watchdogEnableDisposable );
    
    
    
        // initiate periodic watchdog status checks
        watchdogStatusUpdatesDisposable = appInstance.wifiCommunicator.watchdogStatusUpdates
                .observeOn( AndroidSchedulers.mainThread() )
                .subscribe(
                        wdJSON -> {
                            if( wdJSON.getBoolean( "WatchdogAlarm" ) ) {  // true if watchdog has timed out
                                appInstance.wifiCommunicator.fanControlWithWarning( false )
                                        .retry( 3 )
                                        .subscribe();  // shut off fan
                                Toast.makeText( TestActivity.this, "Watchdog timed out -- aborting!", Toast.LENGTH_LONG ).show();
                                pidButtonTextPublisher.onNext( "Watchdog timed out -- aborted!" );
                            } else {  // watchdog is OK
                                appInstance.wifiCommunicator.watchdogResetSingle
                                        .request()
                                        .subscribe();
                            }
                        },
                        wdStatusError -> { }  // TODO: error handler
                );
        appInstance.onStopDisposables.add( watchdogStatusUpdatesDisposable );
    
    
    
    
    // TODO: keep?
        setTempButton.setText( "CURRENT TEMP SETTING: " + appInstance.pidState.getSetPoint().toString() + "°F" );
        //appInstance.bbqController.start();  // don't start ON
        
        
        watchdogResetDisposable = appInstance.wifiCommunicator.watchdogResetObservable.subscribe(
                response -> { if( DEBUG ) Log.d( TAG, "Watchdog timer reset" ); }
        );  // start resetting watchdog timer periodically
        appInstance.onStopDisposables.add( watchdogResetDisposable );
    
    
        if( DEBUG ) Log.d( TAG, "Exiting onStart()" );
    }
    
    @Override
    protected void onResume( ) {
        
        if( DEBUG ) Log.d( TAG, "Entering onResume()" );
        
        super.onResume( );
        
        // handle clicks on the Fan On/Off button: switch fan and update button text
        fanToggleDisposable = fanToggleObservable
                //.skipInitialValue()  // discard the immediate emission on subscription TODO: does this work?
                .skip( 1L )
                .subscribe(
                        newFanState -> {  // fan on/off button clicked
                            appInstance.pidState.setOutputOn( newFanState );
                            appInstance.wifiCommunicator.fanControlWithWarning( newFanState )
                                    .observeOn( AndroidSchedulers.mainThread() )
                                    //.subscribe( httpResponse -> toggleFanButton.setText( newFanState? "FAN IS ON" : "FAN IS OFF" ) );
                                    .subscribe(
                                            response -> {
                                                fanButtonTextPublisher.onNext( newFanState? "Fan turned on with button" : "Fan turned off with button" );
                                                if( DEBUG ) Log.d( TAG, newFanState? "Fan turned on with button" : "Fan turned off with button" );
                                            },
                                            fanSwitchErr -> Log.d( TAG, "Error manually switching fan: " + fanSwitchErr.getMessage() )
                                    );
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
                            appInstance.pidState.setEnabled( newPidState );
                            if( newPidState ) {  // enable the PID
                                if( appInstance.bbqController.start() ) {
                                    pidButtonTextPublisher.onNext( appInstance.pidState.intIsClamped( )?
                                            "PID is enabled (clamped)" : "PID is enabled" );
                                    if ( DEBUG ) Log.d( TAG, "PID start command succeeded" );
                                } else { // problem starting
                                    if( DEBUG ) Log.d( TAG, "Attempt to start PID failed");
                                }
                            } else {  // disable the PID
                                appInstance.bbqController.stop();  // remove handler scheduled tasks, stop fan, set disabled
                                pidButtonTextPublisher.onNext( "PID is disabled" );
                                fanButtonTextPublisher.onNext( "PID turned fan off" );
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
                        .observeOn( AndroidSchedulers.mainThread() )
                        .subscribe(
                                tempJSON -> {
                                    updateTempButtonTextPublisher.onNext("LAST READING: "
                                            + String.valueOf( tempJSON.getDouble( "TempF" ) ) + "°F" );
                                    if( DEBUG ) Log.d( TAG, "Temp manually updated successfully" );
                                },
                                tempJSONErr -> {
                                    updateTempButtonTextPublisher.onNext( "Manual temp update error: "
                                            + tempJSONErr.getMessage() );
                                    if( DEBUG ) Log.d( TAG, "Error getting JSON temp update: " + tempJSONErr.getMessage() );
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
                                setTempButtonTextPublisher.onNext( "SETPOINT: "
                                        + String.valueOf( ((SeekBarProgressChangeEvent) newEvent).progress() ) + "°F" );
                            } else if( newEvent instanceof SeekBarStopChangeEvent ) {
                                appInstance.pidState.set( (float) tempSlider.getProgress() );
                                if( DEBUG ) Log.d( TAG, "Setpoint changed to " + tempSlider.getProgress() );
                            }
                        }
                );
        appInstance.onPauseDisposables.add( tempSliderDisposable );
    
        fanButtonTextDisposable = fanButtonTextPublisher
                .observeOn( AndroidSchedulers.mainThread() )
                .subscribe(
                        buttonText -> toggleFanButton.setText( buttonText )
                );
        appInstance.onPauseDisposables.add( fanButtonTextDisposable );
    
        tempButtonTextDisposable = updateTempButtonTextPublisher
                .observeOn( AndroidSchedulers.mainThread() )  // else can't touch UI
                .subscribe(
                        buttonText -> updateTempButton.setText( buttonText ),
                        buttTextErr -> Log.d( TAG, "Error trying to receive tempButton text update", buttTextErr )
                );
        appInstance.onPauseDisposables.add( tempButtonTextDisposable );
    
        pidToggleDisposable = pidButtonTextPublisher  // receive and display updates to PID on/off button text
                .observeOn( AndroidSchedulers.mainThread() )
                .subscribe(
                        buttonText -> togglePIDButton.setText( buttonText )
                );
        appInstance.onPauseDisposables.add( pidToggleDisposable );
    
        setTempButtonTextDisposable = setTempButtonTextPublisher
                .observeOn( AndroidSchedulers.mainThread() )
                .subscribe(
                        setTempButton::setText
                );
        appInstance.onPauseDisposables.add( setTempButtonTextDisposable );
    
    
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
        pidToggleDisposable.dispose();
        setTempButtonTextDisposable.dispose();
        
        appInstance.onPauseDisposables.clear();
        
        super.onPause( );
        
        if( DEBUG ) Log.d( TAG, "Exiting onPause()" );
    }
    
    @Override
    protected void onStop( ) {
        if( DEBUG ) Log.d( TAG, "Entering onStop()" );
    
        tempFUpdaterDisposable.dispose();           // turn off periodic temp updates
        watchdogEnableDisposable.dispose();         // disable watchdog
        watchdogStatusUpdatesDisposable.dispose();  // stop getting watchdog status updates
        watchdogResetDisposable.dispose();          // stop resetting watchdog timer
        super.onStop( );
    
        if( DEBUG ) Log.d( TAG, "Exiting onStop()" );
    }
}
