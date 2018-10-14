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
        pidState.set( setPoint );
        pidState.setIntClamped( true );  // TODO: shouldn't we set these too?
        pidState.setIntAccum( 0f );
        pidState.setPublishChanges( true );  // re-enable
        pidState.setReset( false );
    }
    
    @Override  // OK
    public Float getSetpoint( ) {  // null if not set yet
        return pidState.getSetPoint();
    }
    
    
    @Override  //
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
        pidState.setEnabled( false );
        return true;
    }
    
    @Override  // OK
    public synchronized boolean isRunning( ) {
        return pidState.isEnabled();
    }
    
    @Override  // OK
    public synchronized boolean reset( ) {
        pidState.setReset( true );
        //pidState.set( null );  // null the setpoint TODO: ?
        pidState.setIntClamped( false );
        pidState.setIntAccum( 0f );
        return stop();
    }
    
    @Override  // OK
    public synchronized boolean isReset( ) {
        return pidState.isReset();
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
    
    
    // END OF INTERFACE IMPLEMENTATION  \\
    
    
    
    
    
    
    public class PIDLoopRunnable implements Runnable {
        
        final String TAG = PIDLoopRunnable.class.getSimpleName();
        int iteration;  // holds > 600 years of 10-second cycles
        
        float error, proportionalTerm, integralTerm, differentialTerm, outputPercent;
        boolean clamped;
        
        public void run() {
            
            iteration++;
            
            if( DEBUG ) Log.d( TAG, "Entering PID Control Loop iteration " + iteration + "; temp = " + pidState.getCurrentVariableValue()
                    + " setpoint = " + pidState.getSetPoint() );
            
            
            //if( pidState.isEnabled() ) {
            // schedule next loop run
            // in Service implementation, maybe let it run all the time but only operate the heater if enabled.
            pidHandler.postDelayed( this, Math.round( pidState.getPeriodSecs() * 1000d ) );  // rounding double returns long
            
            
            error = BBQController.this.getError();  // odd choice of error sign  ('BBQController.this' isn't really needed)
            proportionalTerm = error * pidState.getPropCoeff();
            integralTerm = pidState.getIntAccum();
            if( !pidState.intIsClamped() ) {
                integralTerm += error * pidState.getIntCoeff();
            }
            { pidState.setPublishChanges( false );
                pidState.setIntAccum( integralTerm );  // FIXME: does this take care of int accum not updating?
                pidState.setPublishChanges( true ); }
            differentialTerm = ( pidState.getCurrentVariableValue() - pidState.getPreviousVariableValue() )
                    * pidState.getDiffCoeff();
            pidState.updatePreviousVariableValue();  // set Previous value equal to Current value (doesn't publish)
            //pidState.setPreviousVariableValue( pidState.getCurrentVariableValue() );  // FIXME: was crashing (do to publishing, somehow)
            
            outputPercent = pidState.getGain() * (proportionalTerm + integralTerm - differentialTerm);
            
            // clamped if output is out of range and the integral term and error have the same sign
            pidState.setIntClamped( (outputPercent > 100f || outputPercent < 0f) && ( (integralTerm * error) > 0f) );
            
            // probably don't want to manipulate UI here for Service version
            //appInstance.testActivityRef.pidButtonTextPublisher.onNext( appInstance.pidState.intIsClamped()?
            //        "PID is enabled (clamped)" : "PID is enabled" );
            
            outputPercent = outputPercent < 0f? 0f : outputPercent;
            outputPercent = outputPercent > 100f? 100f : outputPercent;
            pidState.setCurrentPctg( outputPercent );
            
            // actually do output control only if PID is enabled and % is > minimum that we act on
            if( pidState.isEnabled() ) {
                if ( outputPercent >= pidState.getMinOutPctg( ) ) {  // want to enable output
                    pidHandler.post( ( ) -> appInstance.wifiCommunicator.fanControlWithWarning( true ).subscribe(
                            //response -> testActivityRef.fanButtonTextPublisher.onNext( "PID TURNED FAN ON" )
                            response -> pidState.setOutputOn( true )
                            // TODO: error handler?
                    ) );
                    if ( outputPercent < 100f ) { // schedule a turnoff if <100%
                        pidHandler.postDelayed(
                                ( ) -> appInstance.wifiCommunicator.fanControlWithWarning( false ).subscribe(
                                        //response -> testActivityRef.fanButtonTextPublisher.onNext( "PID TURNED FAN OFF" )
                                        response -> pidState.setOutputOn( false )
                                ),  // TODO: error handler?
                                (long) (pidState.getPeriodSecs( ) * 1000f * outputPercent / 100f) );  // delay time
                    }
                } else {  // outputPercent < MIN_OUTPUT_PCT -- turn fan off just to be safe
                    pidHandler.post( ( ) -> appInstance.wifiCommunicator.fanControlWithWarning( false ).subscribe(
                            //response -> testActivityRef.fanButtonTextPublisher.onNext( "PID TURNED FAN OFF" ) ) );
                            response -> pidState.setOutputOn( false ) ) );
                    // TODO: error handler?
                }
            }  // if PID is enabled
            
            if( DEBUG ) Log.d( TAG, "Exiting PID Control Loop with outputPercent = " + outputPercent
                    + ", error = " + error + ", integral term = " + integralTerm );
        }  // .run()
    }  // PID loop Runnable
    
}
