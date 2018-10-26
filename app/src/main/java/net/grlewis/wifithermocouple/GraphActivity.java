package net.grlewis.wifithermocouple;

import android.annotation.SuppressLint;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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

import com.androidplot.Plot;
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

import static net.grlewis.wifithermocouple.Constants.DEBUG;

import com.androidplot.xy.*;

public class GraphActivity extends AppCompatActivity implements ServiceConnection {
    
    static final String TAG = GraphActivity.class.getSimpleName();
    
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
    
    Disposable fanToggleDisp;
    Disposable pidToggleDisp;
    Disposable tempUpdateDisp;
    Disposable tempSliderDisp;
    Disposable pidParameterChangesDisp;   // Service impl
    Disposable graphDataUpdateDisp;       // relay of graphing data
    
    CompositeDisposable onPauseDisp;
    CompositeDisposable onStopDisp;
    
    // attempt at ViewModel to save temp history  TODO: dump? (all Rx)
    private UIStateModel uiStateModel;
    
    // Created & used by onStart() in call to bindService()
    Intent bindThermoServiceIntent; // can pass extra data to the Service if we need to
    ThermocoupleService thermoServiceRef;           // set when Service is bound
    ThermocoupleService.LocalBinder thermoBinder;   // part of binding operation, not used otherwise
    ComponentName serviceComponentName;             // returned by .startService(); just logged
    boolean serviceBound;                           // did service binding succeed?
    
    ThermocoupleServiceConnection thermocoupleServiceConnection;  // NEW
    ThermocoupleService thermocoupleServiceRef;
    
    private XYPlot tempHistoryPlot;
    private TempPlotSeries tempPlotSeries;          // implements XYSeries
    LineAndPointFormatter tempGraphFormatter;
    XYGraphWidget tempGraphWidget;
    
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        
        if( DEBUG ) Log.d( TAG, "Entering onCreate()" );
        
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_graph );
        Toolbar toolbar = (Toolbar) findViewById( R.id.toolbar );  // TODO: use?
        setSupportActionBar( toolbar );
        
        appInstance = ThermocoupleApp.getSoleInstance();
        
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
        
        onPauseDisp = new CompositeDisposable( );
        onStopDisp  = new CompositeDisposable( );
        
        
        // NEW: initialize our XYPlot reference & components:
        tempHistoryPlot = (XYPlot) findViewById(R.id.temp_history);  // "cast is redundant"?
        tempPlotSeries = new TempPlotSeries();
        tempHistoryPlot.setRenderMode( Plot.RenderMode.USE_BACKGROUND_THREAD );
        tempGraphFormatter = new LineAndPointFormatter(this, R.xml.line_point_formatter_with_labels);
        tempHistoryPlot.addSeries(tempPlotSeries, tempGraphFormatter);
        tempGraphWidget = tempHistoryPlot.getGraph();
        
        
        
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
        
        
        // Moved to ThermocoupleApp (service binding)
        
        thermocoupleServiceConnection = new ThermocoupleServiceConnection();  // NEW
        
        bindThermoServiceIntent = new Intent( getApplicationContext(), ThermocoupleService.class );  // FIXME: this also doesn't work for context
        serviceBound = bindService( bindThermoServiceIntent, thermocoupleServiceConnection, Context.BIND_AUTO_CREATE );  // NEW ServiceConnection impl
        if( DEBUG ) {
            if( serviceBound ) {
                Log.d( TAG, "bindService() reports that ThermocoupleService is bound");  // always reports bound
                thermocoupleServiceRef = thermocoupleServiceConnection.getThermocoupleService();
                if( DEBUG ) Log.d( TAG, "thermocoupleServiceRef = " + thermocoupleServiceRef.toString() );  // FIXME: NPE
            } else {
                Log.d( TAG, "bindService() reports that ThermocoupleService is NOT bound");
            }
        }
        
        
        if( DEBUG ) Log.d( TAG, "Exiting onCreate()" );
    }  // onCreate
    
    
    @Override
    @SuppressLint( "CheckResult" )  // suppress "result of subscribe is not used" warnings
    protected void onStart( ) {
        
        if( DEBUG ) Log.d( TAG, "Entering onStart()" );
        super.onStart( );
        
        // part of Service implementation (onStart() is a good place to bind Services)
/*  try moving to onCreate()
        bindThermoServiceIntent = new Intent( getApplicationContext(), ThermocoupleService.class );
        if( !( serviceBound = bindService( bindThermoServiceIntent, */
        /*ServiceConnection*//*
 this, Context.BIND_AUTO_CREATE ) ) ) // flag: create the service if it's bound
            throw new RuntimeException( TAG + ": bindService() call in onStart() failed" );
        // Note: startForegroundService requires API 26
        serviceComponentName = getApplicationContext().startService( bindThermoServiceIntent );  // bind to it AND start it
        if( DEBUG ) Log.d( TAG, "Service running with ComponentName " + serviceComponentName.toShortString() );  // looks OK
*/
        
        
        // This updates the UI except for the (NEW) temp history graph and perhaps other stuff
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
        onStopDisp.add( pidParameterChangesDisp );  // TODO: dispose in both onStop() and onDestroy() (?)
        
        // above is part of Service implementation
        
        
        tempSlider.setProgress( Math.round( appInstance.bbqController.getSetpoint() ) );
        
        // set initial appState
        // make sure fan is off
        appInstance.wifiCommunicator.fanControlWithWarning( false )  // fan off  FIXME: first bad UUID?
                .retry( 2 )  // try up to 3 times
                .subscribe(
                        httpResponse -> {
                            toggleFanButton.setText( "FAN IS INITIALLY OFF" );
                        },
                        httpError -> {
                            Toast.makeText( GraphActivity.this, "Fan shutoff in onStart() failed after retries"
                                    + httpError.getMessage(), Toast.LENGTH_LONG ).show();
                            toggleFanButton.setText( "Error turning fan off in onStart()" );
                        } );
        
        if( DEBUG ) Log.d( TAG, "Exiting onStart()" );
    }
    
    @Override
    protected void onResume( ) {
        
        if( DEBUG ) Log.d( TAG, "Entering onResume()" );
        super.onResume( );
        
        // handle clicks on the Fan On/Off button: switch fan and update button text
        // TODO: not part of final system?
        fanToggleDisp = fanToggleObservable
                //.skipInitialValue()  // discard the immediate emission on subscription TODO: does this work?
                .skip( 1L )
                .subscribe(
                        newFanState -> {  // fan on/off button clicked
                            //appInstance.pidState.setOutputOn( newFanState );
                            appInstance.bbqController.setOutputOn( newFanState );  // NEW
                        },
                        fanToggleErr -> {
                            Log.d( TAG, "Error getting fan button click: " + fanToggleErr.getMessage() );
                        }  // TODO (note we get a warning)
                );
        onPauseDisp.add( fanToggleDisp );
        
        // handle click events for toggle button to enable/disable PID
        pidToggleDisp = pidToggleObservable
                .observeOn( AndroidSchedulers.mainThread() )
                .subscribe(
                        newPidState -> {
                            if( newPidState ) {  // enable the PID
                                appInstance.bbqController.start();  // NEW
                            } else {  // disable the PID
                                appInstance.bbqController.stop();  // remove handler scheduled tasks, stop fan, set disabled
                                if( DEBUG ) Log.d( TAG, "Attempting to stop PID" );
                            }
                        },
                        pidToggleErr -> {
                            Log.d( TAG, "Error with pidToggleObservable: "
                                    + pidToggleErr.getMessage(), pidToggleErr );
                        }
                );
        onPauseDisp.add( pidToggleDisp );
        
        // handle clicks on the temperature update button (manual update) TODO: so far only affects UI, not PID
        tempUpdateDisp = tempUpdateObservable.subscribe(
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
                                    if( DEBUG ) Log.d( TAG, "Error getting manual temp update: " + tempErr.getMessage() );
                                }
                        )
        );
        onPauseDisp.add( tempUpdateDisp );
        
        
        
        tempSliderDisp = tempSliderEventObservable
                .observeOn( AndroidSchedulers.mainThread() )
                .subscribe(  // now emits SeekBarChangeEvent
                        newEvent -> {
                            if( newEvent instanceof SeekBarProgressChangeEvent ) {
                                setTempButton.setText( "SETPOINT: "
                                        + String.valueOf( ((SeekBarProgressChangeEvent) newEvent).progress() ) + "°F" );
                            } else if( newEvent instanceof SeekBarStopChangeEvent ) {
                                appInstance.bbqController.set( (float) tempSlider.getProgress() );  // FIXME: wasn't going through BBQController
                                if( DEBUG ) Log.d( TAG, "Setpoint changed to " + tempSlider.getProgress() );
                            }
                        }
                );
        onPauseDisp.add( tempSliderDisp );
        
        // subscribe to updates in temp history for graphing
        
        if( appInstance.thermocoupleService == null ) throw new NullPointerException( "appInstance.thermocoupleService returns null" );  // FIXME: returning null
//        if( thermoServiceRef.tempHistRelay == null ) throw new NullPointerException( "tempHistRelay returns null" );
        
        graphDataUpdateDisp = appInstance.thermocoupleService.tempHistRelay.subscribe(
                newTempHistory -> {
                    tempPlotSeries.updatePlotData( newTempHistory );  // with sync
                    // TODO: redraw the graph
                    tempHistoryPlot.redraw();
                }
        );
        onPauseDisp.add( graphDataUpdateDisp );
        
        
        if( DEBUG ) Log.d( TAG, "Exiting onResume()" );
    }  // onResume
    
    
    @Override
    protected void onPause( ) {
        if( DEBUG ) Log.d( TAG, "Entering onPause()" );
        onPauseDisp.clear();
        super.onPause( );
        if( DEBUG ) Log.d( TAG, "Exiting onPause()" );
    }
    
    
    @Override
    protected void onStop( ) {
        if( DEBUG ) Log.d( TAG, "Entering onStop()" );
        onStopDisp.clear();              // new composite for Service implementation
        super.onStop( );
        if( serviceBound ) {  // try it here instead of onDestroy
            getApplicationContext().unbindService( this );     // try to avoid leaking Service ('this' is ServiceConnection impl)
            serviceBound = false;
            thermoServiceRef = null;  // NEW
        } else {
            if( DEBUG ) Log.d( TAG, "onDestroy() found that ThermocoupleService was not bound" );
        }
        if( DEBUG ) Log.d( TAG, "Exiting onStop()" );
    }
    
    // TODO: unbind service onDestroy() (?)
    
    
    @Override
    protected void onDestroy( ) {
        super.onDestroy( );  // FIXME: does putting this first fix the "can't destroy activity; service is not registered" crash? No.
        if( DEBUG ) Log.d( TAG, "onDestroy() entered");
        onStopDisp.clear();  // because apparently onStop() isn't always called
    }
    
    /*---------------------------------SERVICE CONNECTION INTERFACE-----------------------------------*/
    @Override
    // Here, the IBinder has a getService() method that returns a reference to the Service instance
    @SuppressWarnings( "static-access" )
    public void onServiceConnected( ComponentName className, IBinder service ) {  // service is an IBinder (interface implemented by Binder)
        Log.d( TAG, "Entering onServiceConnected()" );
        // We've bound to Service, cast the IBinder and get Service instance
        thermoBinder = (ThermocoupleService.LocalBinder) service;                 // FIXME:
        // (casting it makes compiler aware of existence of getService() method)
        thermoServiceRef = thermoBinder.getService();                             // FIXME: null
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
