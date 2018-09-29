package net.grlewis.wifithermocouple;

interface PIDController {
    
    void set( float setPoint );  // set the setpoint
    Float getSetpoint();  // null if not set yet
    
    boolean start(); // return false if setpoint hasn't been set
    boolean stop();  // always shut off the heat
    boolean isRunning();
    
    boolean reset();  // clear history etc.
    boolean isReset();  // true after reset until settings set etc.
    
    float getError();  // current value - setPoint
    
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
    
    //
    
    
    
    
}
