package net.grlewis.wifithermocouple;

interface PIDController {
    
    
    void set( float setPoint );
    Float getSetpoint();  // null if not set yet
    
    boolean start(); // return false if setpoint hasn't been set
    boolean stop();  // always shut off the heat
    boolean isRunning();
    
    void reset();  // clear history etc.
    boolean isReset();  // true after reset until settings set etc.
    
    float getError();  // current value - setPoint
    
    boolean setPeriod( long ms );
    
    
    // current value of the controlled variable
    void setCurrentVariableValue( float value );
    float getCurrentVariableValue();
    
    // overall gain of the PID controller
    void setGain( float gain );
    float getGain( );
    
    // proportional coefficient
    void setPropCoeff( float propCoeff );
    float getProfCoeff( );
    
    // integral coefficient
    void setIntCoeff( float intCoeff );
    float getIntCoeff( );
    
    // differential coefficient
    void setDiffCoeff( float diffCoeff );
    float getDiffCoeff( );
    
    //
    
    
    
    
}
