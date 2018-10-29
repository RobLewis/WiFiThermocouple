package net.grlewis.wifithermocouple;

interface PIDController {
    
    void set( Float setPoint );  // set the setpoint (wrapper object enables a null value)  OK
    Float getSetpoint();         // null if not set yet  OK
    
    boolean start();             // return false if setpoint hasn't been set
    boolean stop();              // always shut off the heat (control loop can keep running but not operate heater?)
    boolean restart();           // keep existing parameters, resume operation
    boolean isRunning();
    
    boolean reset();             // clear history etc.
    boolean isReset();           // true after reset until settings set etc.
    
    default float getError() {  // setPoint - current value (odd choice of sign, but it's NI's convention)
        return getSetpoint() - getCurrentVariableValue();
    }
    
    boolean setPeriodMs( long ms );
    Long getPeriodMs();
    
    
    // current value of the controlled variable
    void setCurrentVariableValue( float value );
    Float getCurrentVariableValue();
    
    // state of the controlled output
    void setOutputOn( boolean outputState );
    boolean getOutputState( );
    
    // overall gain of the PID controller
    void setGain( float gain );
    Float getGain( );
    
    // proportional coefficient
    void setPropCoeff( float propCoeff );
    Float getPropCoeff( );
    
    // integral coefficient
    void setIntCoeff( float intCoeff );
    Float getIntCoeff( );
    
    // differential coefficient
    void setDiffCoeff( float diffCoeff );
    Float getDiffCoeff( );
    
    // clamped state
    void setClamped( boolean clamped );
    boolean isClamped();
    
    // minimum control % (less than this ignored in controlled output)
    void setMinOutPctg( Float pctg );
    Float getMinOutPctg( );
    
    // output % duty cycle (0-100)
    void setDutyCyclePercent( float dcPct );
    Float getDutyCyclePercent();
    
    
    // New stuff to track temp high/low
    // When setpoint is changed we log previous results and start watching new ones
    // Wait until the temperature passes through the setpoint (in either direction) to start updating
    void resetHiLoTracking();
    Float getHiTemp();
    Float getLoTemp();
    
}
