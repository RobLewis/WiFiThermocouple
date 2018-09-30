package net.grlewis.wifithermocouple;

interface PIDController {
    
    void set( Float setPoint );  // set the setpoint (wrapper object enables a null value)
    Float getSetpoint();  // null if not set yet
    
    boolean start(); // return false if setpoint hasn't been set
    boolean stop();  // always shut off the heat
    boolean isRunning();
    
    boolean reset();  // clear history etc.
    boolean isReset();  // true after reset until settings set etc.
    
    float getError();  // setPoint - current value (odd choice of sign, but it's NI's convention)
    
    boolean setPeriodMs( long ms );
    Long getPeriodMs();
    
    
    // current value of the controlled variable
    void setCurrentVariableValue( float value );
    Float getCurrentVariableValue();
    
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
    
    // minimum control % (less than this ignored in controlled output)
    void setMinOutPctg( Float pctg );
    Float getMinOutPctg( );
    
    
    
}
