package net.grlewis.wifithermocouple;

import android.os.Handler;

import static net.grlewis.wifithermocouple.Constants.MIN_OUTPUT_PCT;
import static net.grlewis.wifithermocouple.Constants.PID_LOOP_INTERVAL_SECS;

class BBQController implements PIDController {
    
    final static String TAG = BBQController.class.getSimpleName( );
    
    final ThermocoupleApp appInstance;
    
    // PID parameters & variables
    private volatile Float setPoint;  // null means uninitialized
    private volatile Float currentVariableValue;  // current value of the temperature being controlled
    private float previousVariableValue;
    private volatile Float error;  // sign seems backwards but this is NI's convention
    private volatile Float gain;
    private volatile Float propCoeff;
    private volatile Float intCoeff;
    private volatile Float diffCoeff;
    
    private float proportionalTerm;
    private float integralTerm;
    private boolean integralClamped;
    private float differentialTerm;
    private float outputPercent;
    
    private volatile boolean running;
    private volatile Long periodMs;  // milliseconds between iterations
    private volatile boolean reset;  // has a reset happened? (if true you can't actually run)
    
    
    private Handler pidHandler = new Handler( );
    private PIDLoopHandler pidLoopHandler = new PIDLoopHandler( );
    
    
    // constructor
    BBQController( ) {
        appInstance = ThermocoupleApp.getSoleInstance();
    }
    
    
    @Override  // OK
    public void set( float setPoint ) {  // TODO: guess it's OK to set it null to indicate uninitialized?
        this.setPoint = setPoint;
        reset = false;
    }
    
    @Override  // OK
    public Float getSetpoint( ) {  // null if not set yet
        return setPoint;
    }
    
    
    @Override  // OK
    public boolean start( ) {  // return false if setpoint hasn't been set
        if ( setPoint == null || reset || periodMs == null ) {  // TODO: other conditions
            running = false;
            return false;
        }
        running = true;
        reset = false;
        pidHandler.post( pidLoopHandler );  // start the loop
        return true;
    }
    
    @Override  // OK
    public boolean stop( ) {
        pidHandler.removeCallbacks( pidLoopHandler );  // cancel any pending loop run
        appInstance.wifiCommunicator.fanControlWithWarning( false ).subscribe( );
        running = false;
        return true;
    }
    
    @Override  // OK
    public boolean isRunning( ) {
        return running;
    }
    
    @Override  // OK
    public void reset( ) {
        reset = true;
        setPoint = null;
        stop();
    }
    
    @Override  // OK
    public boolean isReset( ) {
        return reset;
    }
    
    @Override  // OK
    public float getError( ) {  // current value - setPoint
        return  error;
    }
    
    
    
    @Override  // OK
    public boolean setPeriod( long ms ) {
        periodMs = ms;
        return true;
    }
    
    
    
    // current value of the controlled variable
    public void setCurrentVariableValue( float value ) {
        currentVariableValue = value;
    }
    public float getCurrentVariableValue() { return currentVariableValue; }
    
    // overall gain of the PID controller
    public void setGain( float gain ) {
        this.gain = gain;
    }
    public float getGain( ){ return gain; }
    
    // proportional coefficient
    public void setPropCoeff( float propCoeff ) {
        this.propCoeff = propCoeff;
    }
    public float getProfCoeff( ) { return propCoeff; }
    
    // integral coefficient
    public void setIntCoeff( float intCoeff ) {
        this.intCoeff = intCoeff;
    }
    public float getIntCoeff( ) { return intCoeff; }
    
    // differential coefficient
    public void setDiffCoeff( float diffCoeff ) {
        this.diffCoeff = diffCoeff;
    }
    public float getDiffCoeff( ) { return diffCoeff; }
    
    
    
    
    
    
    
    class PIDLoopHandler implements Runnable {
        
        public void run() {
            
            if( isRunning() ) {
                pidHandler.postDelayed( this, PID_LOOP_INTERVAL_SECS * 1000L );  // schedule next loop run
                
                error = setPoint - currentVariableValue;  // odd choice of sign
                proportionalTerm = error * propCoeff;
                if( !integralClamped ) {
                    integralTerm += error * intCoeff;
                }
                differentialTerm = ( currentVariableValue - previousVariableValue ) * diffCoeff;
                previousVariableValue = currentVariableValue;
                outputPercent = gain * ( proportionalTerm + integralTerm - differentialTerm );  // percent (duty cycle) of full-on heater
                integralClamped = (outputPercent > 100f || outputPercent < 0f) && (integralTerm * error > 0f);  // out of range & same sign?
                outputPercent = outputPercent < 0f? 0f : outputPercent;
                outputPercent = outputPercent > 100f? 100f : outputPercent;
                if( outputPercent >= MIN_OUTPUT_PCT ) {
                    pidHandler.post( () -> appInstance.wifiCommunicator.fanControlWithWarning( true ).subscribe() );
                    pidHandler.postDelayed( () -> appInstance.wifiCommunicator.fanControlWithWarning( false ).subscribe(),
                            (long)(PID_LOOP_INTERVAL_SECS * 1000f * outputPercent/100f) );
                }
            }
        }  // .run()
    }  // PID loop Runnable
    
}
