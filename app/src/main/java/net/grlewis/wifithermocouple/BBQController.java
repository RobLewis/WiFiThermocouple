package net.grlewis.wifithermocouple;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;

import static net.grlewis.wifithermocouple.Constants.MIN_OUTPUT_PCT;

class BBQController implements PIDController {
    
    final static String TAG = BBQController.class.getSimpleName( );
    
    private final ThermocoupleApp appInstance;
    private final PIDState pidState;
    private final TestActivity testActivity;
    
    // PID parameters & variables are in pidState
    
    private float proportionalTerm;
    private float integralTerm;
    private boolean integralClamped;
    private float differentialTerm;
    private float outputPercent;
    private float error;
    
    private final Handler pidHandler;
    private final PIDLoopRunnable pidLoopRunnable;
    
    
    // constructor
    BBQController( ) {
        appInstance = ThermocoupleApp.getSoleInstance();
        pidState = appInstance.pidState;
        testActivity = appInstance.testActivityRef;
        pidHandler = new Handler( );
        pidLoopRunnable = new PIDLoopRunnable( );    }
    
    
    
    // INTERFACE IMPLEMENTATION  \\
    
    @Override  // OK
    public void set( float setPoint ) {  // TODO: guess it's OK to set it null to indicate uninitialized?
        pidState.set( setPoint );
        pidState.setReset( false );      // TODO: can it run with just this value set?
    }
    
    @Override  // OK
    public Float getSetpoint( ) {  // null if not set yet
        return pidState.getSetPoint();
    }
    
    
    @Override  //
    public boolean start( ) {  // return false if setpoint hasn't been set
        if ( pidState.getSetPoint() == null || pidState.isReset() || pidState.getPeriodSecs() == null ) {  // TODO: other conditions
            pidState.setEnabled( false );
            return false;
        }
        pidState.setEnabled( true );
        pidState.setReset( false );
        pidHandler.post( pidLoopRunnable );  // start the loop TODO: anything else needed before we start?
        return true;
    }
    
    @Override  // OK
    public boolean stop( ) {
        pidHandler.removeCallbacks( pidLoopRunnable );  // cancel any pending loop run
        appInstance.wifiCommunicator.fanControlWithWarning( false ).subscribe( );
        pidState.setEnabled( false );
        return true;
    }
    
    @Override  // OK
    public boolean isRunning( ) {
        return pidState.isEnabled();
    }
    
    @Override  // OK
    public boolean reset( ) {
        pidState.setReset( true );
        pidState.set( null );  // null the setpoint
        stop();
        return true;
    }
    
    @Override  // OK
    public boolean isReset( ) {
        return pidState.isReset();
    }
    
    @Override  // OK
    public float getError( ) {  // current value - setPoint
        return  pidState.getCurrentVariableValue() - pidState.getSetPoint();
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
    public void setCurrentVariableValue( float value ) {
        pidState.setCurrentVariableValue( value );
    }
    public Float getCurrentVariableValue() { return pidState.getCurrentVariableValue(); }
    
    // overall gain of the PID controller
    public void setGain( float gain ) {
        pidState.setGain( gain );
    }
    public Float getGain( ){ return pidState.getGain(); }
    
    // proportional coefficient
    public void setPropCoeff( float propCoeff ) {
        pidState.setPropCoeff( propCoeff );
    }
    public Float getPropCoeff( ) { return pidState.getPropCoeff(); }
    
    // integral coefficient
    public void setIntCoeff( float intCoeff ) {
        pidState.setIntCoeff( intCoeff );
    }
    public Float getIntCoeff( ) { return pidState.getIntCoeff(); }
    
    // differential coefficient
    public void setDiffCoeff( float diffCoeff ) {
        pidState.setDiffCoeff( diffCoeff );
    }
    public Float getDiffCoeff( ) { return pidState.getDiffCoeff(); }
    
    
    // END OF INTERFACE IMPLEMENTATION  \\
    
    
    
    
    
    
    class PIDLoopRunnable implements Runnable {
        
        final String TAG = PIDLoopRunnable.class.getSimpleName(); 
        
        public void run() {
            
            Log.d( TAG, "Entering PID Control Loop" );
            
            if( pidState.isEnabled() ) {
                // schedule next loop run
                pidHandler.postDelayed( this, Math.round( pidState.getPeriodSecs() * 1000d ) );  // rounding double returns long
                
                //error = setPoint - currentVariableValue;  // odd choice of sign
                error = pidState.getSetPoint() - pidState.getCurrentVariableValue();
                proportionalTerm = error * pidState.getPropCoeff();
                if( !pidState.intIsClamped() ) {
                    integralTerm += error * pidState.getIntCoeff();
                }
                differentialTerm = ( pidState.getCurrentVariableValue() - pidState.getPreviousVariableValue() )
                        * pidState.getDiffCoeff();
                pidState.setPreviousVariableValue( pidState.getCurrentVariableValue() );
                
                outputPercent = pidState.getGain() * (proportionalTerm + integralTerm - differentialTerm);
                
                pidState.setIntClamped( (outputPercent > 100f || outputPercent < 0f) && (integralTerm * error > 0f) );
                
                outputPercent = outputPercent < 0f? 0f : outputPercent;
                outputPercent = outputPercent > 100f? 100f : outputPercent;
                pidState.setCurrentPctg( outputPercent );
                
                if( outputPercent >= MIN_OUTPUT_PCT ) {
                    pidHandler.post( () ->  appInstance.wifiCommunicator.fanControlWithWarning( true ).subscribe(
                            response -> testActivity.fanButtonTextPublisher.onNext( "PID TURNED FAN ON" )
                    ) );
                    pidHandler.postDelayed( () -> appInstance.wifiCommunicator.fanControlWithWarning( false ).subscribe(
                            response -> testActivity.fanButtonTextPublisher.onNext( "PID TURNED FAN OFF" )
                            ),
                            (long)(pidState.getPeriodSecs() * 1000f * outputPercent/100f) );
                }
            }  // if enabled
        }  // .run()
    }  // PID loop Runnable
    
}
