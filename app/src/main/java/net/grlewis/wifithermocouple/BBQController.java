package net.grlewis.wifithermocouple;

import android.os.Handler;
import android.util.Log;

import static net.grlewis.wifithermocouple.Constants.DEBUG;
import static net.grlewis.wifithermocouple.Constants.DEFAULT_SETPOINT;

class BBQController implements PIDController {
    
    final static String TAG = BBQController.class.getSimpleName( );
    
    private final ThermocoupleApp appInstance;
    private final PIDState pidState;
    private TestActivity testActivityRef;
    
    // PID parameters & variables are in pidState
    
    //private float proportionalTerm;
    //private float integralTerm;
    private boolean integralClamped;
    //private float differentialTerm;
    private float outputPercent;
    //private float error;
    
    private Handler pidHandler;
    final PIDLoopRunnable pidLoopRunnable;
    
    
    // constructor that makes a new Handler
    BBQController( ) {
        this( new Handler( ) );
    }
    
    // constructor that allows supplying a Handler
    BBQController( Handler newHandler ) {
        appInstance = ThermocoupleApp.getSoleInstance();
        pidState = appInstance.pidState;
        testActivityRef = appInstance.testActivityRef;  // TODO: need?
        pidHandler = newHandler;
        pidLoopRunnable = new PIDLoopRunnable( );
        pidState.set( DEFAULT_SETPOINT );  // need some defined setpoint or won't start
    }
    
    
    void setTestActivityRef( TestActivity testAct ) {
        testActivityRef = testAct;
    }
    
    // INTERFACE IMPLEMENTATION  \\
    
    @Override  // OK
    public synchronized void set( Float setPoint ) {  // set it null to indicate uninitialized?
        pidState.setPublishChanges( false );  // temporarily turn off publishing
        pidState.set( setPoint );             // when we change setpoint,
        pidState.setIntClamped( true );       // start clamped
        pidState.setIntAccum( 0f );           // with no integral term
        pidState.setPublishChanges( true );   // re-enable publishing
        pidState.setReset( false );           // and trigger publishing
    }
    
    @Override  // OK
    public Float getSetpoint( ) {  // null if not set yet
        return pidState.getSetPoint();
    }
    
    
    @Override  // TODO: does it make sense to be able to stop, and start with same values?  (see: restart())   OK
    public synchronized boolean start( ) {  // return false if setpoint hasn't been set
        if ( pidState.getSetPoint() == null /*|| pidState.isReset()*/ || pidState.getPeriodSecs() == null ) {  // TODO: other conditions
            pidState.setEnabled( false );
            if( DEBUG ) Log.d( TAG, "Can't start PID" );
            return false;
        }
        pidState.setPublishChanges( false );
        pidState.setEnabled( true );
        pidState.setReset( false );
        pidState.setIntClamped( true );
        pidState.setIntAccum( 0f );
        pidState.setPublishChanges( true );
        pidState.setPreviousVariableValue( pidState.getCurrentVariableValue() );  // TODO: right?
        //pidHandler.post( pidLoopRunnable );  // start the loop TODO: anything else needed before we start?
        if( DEBUG ) Log.d( TAG, "start() completed successfully(?), skipping .post()" );
        return true;
    }
    
    @Override  // OK
    public synchronized boolean stop( ) {
        pidHandler.removeCallbacks( pidLoopRunnable );  // cancel any pending loop run
        appInstance.wifiCommunicator.fanControlWithWarning( false ).subscribe( );
        pidState.setEnabled( false );  // loop keeps running
        return true;
    }
    
    @Override  // don't clear parameters--just re-enable  // OK?
    public synchronized boolean restart( ) {
        if ( pidState.getSetPoint() == null /*|| pidState.isReset()*/ || pidState.getPeriodSecs() == null ) {  // TODO: other conditions
            pidState.setEnabled( false );
            if( DEBUG ) Log.d( TAG, "Can't restart PID" );
            return false;
        }
        pidState.setEnabled( true );
        pidState.setReset( false );
        if( DEBUG ) Log.d( TAG, "restart() completed" );
        return true;
    }
    
    @Override  // OK
    public synchronized boolean isRunning( ) {
        return pidState.isEnabled();
    }
    
    @Override  // OK
    public synchronized boolean reset( ) {
        pidState.setPublishChanges( false );
        pidState.setReset( true );
        pidState.set( null );  // null the setpoint
        pidState.setIntClamped( false );
        pidState.setPublishChanges( true );
        pidState.setIntAccum( 0f );
        return stop();
    }
    
    @Override  // OK
    public synchronized boolean isReset( ) {
        return pidState.isReset();
    }
    
    @Override
    public void setClamped( boolean clamped ) {
        pidState.setIntClamped( clamped );
    }
    @Override
    public boolean isClamped( ) {
        return pidState.intIsClamped();
    }
    
    @Override  // OK
    public synchronized float getError( ) {  // current value - setPoint (no need to use a wrapper)
        return  pidState.getSetPoint() - pidState.getCurrentVariableValue();  // NI's odd choice of sign
    }
    
    
    @Override  // OK
    public boolean setPeriodMs( long ms ) {  // in pidState is stored as a Float seconds
        pidState.setPeriodSecs( ms/1000f );
        return true;
    }
    @Override  // OK
    public Long getPeriodMs( ) {
        return Long.valueOf( Math.round( pidState.getPeriodSecs()*1000d ) );
    }  // rounding double returns long
    
    
    // current value of the controlled variable
    public synchronized void setCurrentVariableValue( float value ) {
        pidState.setCurrentVariableValue( value );
    }
    public Float getCurrentVariableValue() { return pidState.getCurrentVariableValue(); }
    
    // state of controlled output
    
    
    @Override
    public void setOutputOn( boolean outputState ) {
        appInstance.wifiCommunicator.fanControlWithWarning( outputState )
                .retry( 3L )
                .subscribe(
                        response -> pidState.setOutputOn( outputState ),
                        fanSwitchErr -> { if( DEBUG ) Log.d( TAG, "Error trying to setOutputOn: " + outputState
                                + "; " + fanSwitchErr.getMessage() ); }
                );
    }
    @Override
    public boolean getOutputState( ) {
        return pidState.outputIsOn();
    }
    
    // overall gain of the PID controller
    public synchronized void setGain( float gain ) {
        pidState.setGain( gain );
    }
    public Float getGain( ){ return pidState.getGain(); }
    
    // proportional coefficient
    public synchronized void setPropCoeff( float propCoeff ) {
        pidState.setPropCoeff( propCoeff );
    }
    public Float getPropCoeff( ) { return pidState.getPropCoeff(); }
    
    // integral coefficient
    public synchronized void setIntCoeff( float intCoeff ) {
        pidState.setIntCoeff( intCoeff );
    }
    public Float getIntCoeff( ) { return pidState.getIntCoeff(); }
    
    // differential coefficient
    public synchronized void setDiffCoeff( float diffCoeff ) {
        pidState.setDiffCoeff( diffCoeff );
    }
    public Float getDiffCoeff( ) { return pidState.getDiffCoeff(); }
    
    // minimum controlled output %
    
    @Override
    public synchronized void setMinOutPctg( Float pctg ) {
        pidState.setMinOutPctg( pctg );
    }
    @Override
    public Float getMinOutPctg( ) {
        return pidState.getMinOutPctg();
    }
    
    @Override
    public void setDutyCyclePercent( float dcPct ) {
        pidState.setCurrentPctg( dcPct );
    }
    @Override
    public Float getDutyCyclePercent( ) {
        return pidState.getCurrentPctg();
    }
    
    // END OF INTERFACE IMPLEMENTATION  \\
    
    // Additional functionality not part of the PIDController interface
    public Float getControlSetting( ) {  // read the analog in voltage (knob): 0.0-1.0 volts
        return pidState.getAnalogInVolts();
    }
    
    
    
    
    public class PIDLoopRunnable implements Runnable {
        
        final String TAG = PIDLoopRunnable.class.getSimpleName();
        int iteration;  // holds > 600 years of 10-second cycles
        
        float error, proportionalTerm, integralTerm, differentialTerm, outputPercent;
        boolean clamped;
        
        public void run() {  // seems to always run on the correct thread
            
            iteration++;
            
            if( DEBUG ) Log.d( TAG, "Entering PID Control Loop iteration " + iteration + " on thread "
                    + Thread.currentThread().getName() + "; temp = " + getCurrentVariableValue()
                    + " setpoint = " + getSetpoint() );
            
            
            // schedule next loop run
            // in Service implementation, maybe let it run all the time but only operate the heater if enabled.
            pidHandler.postDelayed( this, Math.round( pidState.getPeriodSecs() * 1000d ) );  // rounding double returns long  TODO: getPeriodSecs?
            
            
            error = getError();  // odd choice of error sign  ('BBQController.this' isn't really needed)
            proportionalTerm = error * getPropCoeff();
            integralTerm = pidState.getIntAccum();  // TODO: .getIntAccum?
            if( !pidState.intIsClamped() ) {        // TODO: ?
                integralTerm += error * getIntCoeff();
            }
            { pidState.setPublishChanges( false );
                pidState.setIntAccum( integralTerm );  // FIXME: does this take care of int accum not updating?
                pidState.setPublishChanges( true ); }
            differentialTerm = ( getCurrentVariableValue() - pidState.getPreviousVariableValue() )  // TODO: ?
                    * getDiffCoeff();
            pidState.updatePreviousVariableValue();  // set Previous value equal to Current value (doesn't publish)  // TODO: ?
            //pidState.setPreviousVariableValue( pidState.getCurrentVariableValue() );  // FIXME: was crashing (do to publishing, somehow)
            
            outputPercent = getGain() * (proportionalTerm + integralTerm - differentialTerm);
            
            // clamped if output is out of range and the integral term and error have the same sign
            pidState.setIntClamped( (outputPercent > 100f || outputPercent < 0f) && ( (integralTerm * error) > 0f) );  // TODO: ?
            
            // probably don't want to manipulate UI here for Service version
            //appInstance.testActivityRef.pidButtonTextPublisher.onNext( appInstance.pidState.intIsClamped()?
            //        "PID is enabled (clamped)" : "PID is enabled" );
            
            outputPercent = outputPercent < 0f? 0f : outputPercent;
            outputPercent = outputPercent > 100f? 100f : outputPercent;
            setDutyCyclePercent( outputPercent );
            
            // actually do output control only if PID is enabled and % is > minimum that we act on
            if( isRunning() ) {
                if ( outputPercent >= getMinOutPctg( ) ) {  // want to enable output
                    
                    pidHandler.post( () -> setOutputOn( true ) );
//                    pidHandler.post( ( ) -> appInstance.wifiCommunicator.fanControlWithWarning( true ).subscribe(
//                            //response -> testActivityRef.fanButtonTextPublisher.onNext( "PID TURNED FAN ON" )
//                            response -> pidState.setOutputOn( true )  // TODO: ?
//                    ) );
                    if ( outputPercent < 100f ) { // schedule a turnoff if <100%
                        pidHandler.postDelayed( () -> setOutputOn( false ),
//                        ( ) -> appInstance.wifiCommunicator.fanControlWithWarning( false ).subscribe(
//                                        //response -> testActivityRef.fanButtonTextPublisher.onNext( "PID TURNED FAN OFF" )
//                                        response -> pidState.setOutputOn( false )  // TODO: ?
                                (long) ( getPeriodMs( ) * outputPercent / 100f) );  // delay time // TODO: ?
                    }
                } else {  // outputPercent < MIN_OUTPUT_PCT -- turn fan off just to be safe
                    pidHandler.post( () -> setOutputOn( false ) );
                }
            }  // if PID is enabled
            
            if( DEBUG ) Log.d( TAG, "Exiting PID Control Loop with outputPercent = " + outputPercent
                    + ", error = " + error + ", integral term = " + integralTerm );
        }  // .run()
    }  // PID loop Runnable
    
}
