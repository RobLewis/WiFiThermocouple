package net.grlewis.wifithermocouple;

// Class to store current PID appState and to use for a library of states
// We're using objects so the null state means "undefined"

import java.io.Serializable;

import static net.grlewis.wifithermocouple.Constants.*;

class PIDState implements Cloneable, Serializable {  // TODO: cloneable?
    
    private Float setPoint;
    private Float currentVariableValue;
    private Float previousVariableValue;
    private Float gain;
    private Float propCoeff;
    private Float intCoeff;
    private Float diffCoeff;
    private Boolean intClamped;  // is the integrator term clamped?
    private Float currentPctg;   // last value of % on time for each period
    private Float intAccum;      // the accumulated integral term
    private Float periodSecs;    // seconds for loop repeat interval
    private Boolean enabled;     // is the PID enabled?
    private Boolean reset;       // has it been reset?
    private Boolean outputOn;    // is the fan, heater, whatever currently on?
    private Float minOutPct;     // the minimum controlled output percentage that will cause turnon
    
    private Float analogInVolts; // 0.0-1.0 (-1 means not set)
    
    
    // constructor
    PIDState() {
        setPoint = DEFAULT_SETPOINT;  // can't run if we haven't defined this
        currentVariableValue = 0f;
        previousVariableValue = 0f;
        gain = DEFAULT_GAIN;
        propCoeff = DEFAULT_PROP_COEFF;
        intCoeff = DEFAULT_INT_COEFF;
        diffCoeff = DEFAULT_DIFF_COEFF;
        intClamped = false;
        currentPctg = 0f;
        intAccum = 0f;
        periodSecs = DEFAULT_PERIOD_SECS;
        enabled = false;
        reset = true;
        outputOn = false;
        minOutPct = DEFAULT_MIN_OUT_PCT;
        
        analogInVolts = -1f;
    }
    
    
    // GETTERS & SETTERS
    
    // reset controller
    Boolean isReset( ) {
        return reset;
    }
    void setReset( Boolean reset ) {
        this.reset = reset;
    }
    
    
    // The PID setpoint
    Float getSetPoint( ) {
        return setPoint;
    }
    void set( Float setPoint ) {
        this.setPoint = setPoint;
    }
    
    
    // current value of the controlled variable (i.e., temperature)
    Float getCurrentVariableValue( ) {
        return currentVariableValue;
    }
    void setCurrentVariableValue( Float currentVariableValue ) {
        this.currentVariableValue = currentVariableValue;
    }
    
    
    // previous value of the controlled variable (updated on each iteration)
    Float getPreviousVariableValue( ) {
        return previousVariableValue;
    }
    void setPreviousVariableValue( Float previousVariableValue ) {
        this.previousVariableValue = previousVariableValue;
    }
    
    
    // overall gain of PID
    Float getGain( ) {
        return gain;
    }
    void setGain( Float gain ) {
        this.gain = gain;
    }
    
    
    // coefficient of the proportional term
    Float getPropCoeff( ) {
        return propCoeff;
    }
    void setPropCoeff( Float propCoeff ) {
        this.propCoeff = propCoeff;
    }
    
    
    // coefficient of the integral term
    Float getIntCoeff( ) {
        return intCoeff;
    }
    void setIntCoeff( Float intCoeff ) {
        this.intCoeff = intCoeff;
    }
    
    
    // coefficient of the differential term
    Float getDiffCoeff( ) {
        return diffCoeff;
    }
    void setDiffCoeff( Float diffCoeff ) {
        this.diffCoeff = diffCoeff;
    }
    
    
    // whether the integrator is currently clamped
    Boolean intIsClamped( ) {
        return intClamped;
    }
    void setIntClamped( Boolean intClamped ) {
        this.intClamped = intClamped;
    }
    
    
    // most recent % of "full on" of the controlled device
    Float getCurrentPctg( ) {
        return currentPctg;
    }
    void setCurrentPctg( Float currentPctg ) {
        this.currentPctg = currentPctg;
    }
    
    
    // current value of the accumulated integral term
    Float getIntAccum( ) {
        return intAccum;
    }
    void setIntAccum( Float intAccum ) {
        this.intAccum = intAccum;
    }
    
    
    // current repeat interval in seconds
    Float getPeriodSecs( ) {
        return periodSecs;
    }
    void setPeriodSecs( Float periodSecs ) {
        this.periodSecs = periodSecs;
    }
    
    
    // whether PID operation is enabled
    Boolean isEnabled( ) {
        return enabled;
    }
    void setEnabled( Boolean enabled ) {  // TODO: shut heat off when disabling
        this.enabled = enabled;
    }
    
    
    // whether the output device (fan, heater, etc.) is currently on or off
    Boolean outputIsOn( ) { return outputOn; }
    void setOutputOn( Boolean outOn ) {
        this.outputOn = outOn;
    }
    
    Float getMinOutPctg( ) {
        return minOutPct;
    }
    void setMinOutPctg( Float minOutPct ) {
        this.minOutPct = minOutPct;
    }
    
    Float getAnalogInVolts( ) { return analogInVolts; }
    void setAnalogInVolts( Float analogVolts ) { this.analogInVolts = analogVolts; }
    
    
    @Override  // TODO: serialize before storing?
    protected PIDState clone( ) throws CloneNotSupportedException {
        PIDState theClone = new PIDState();
        
        theClone.setPoint = Float.valueOf( setPoint );  // just copying references? will we get NPE?
        theClone.currentVariableValue = Float.valueOf( currentVariableValue );
        theClone.previousVariableValue = Float.valueOf( previousVariableValue );
        theClone.gain = Float.valueOf( gain );
        theClone.propCoeff = Float.valueOf( propCoeff );
        theClone.intCoeff = Float.valueOf( intCoeff );
        theClone.diffCoeff = Float.valueOf( diffCoeff );
        theClone.intClamped = Boolean.valueOf( intClamped );
        theClone.currentPctg = Float.valueOf( currentPctg ) ;
        theClone.intAccum = Float.valueOf( intAccum );
        theClone.periodSecs = Float.valueOf( periodSecs );
        theClone.enabled = Boolean.valueOf( enabled );
        theClone.reset = Boolean.valueOf( reset );
        theClone.outputOn = Boolean.valueOf( outputOn );
        theClone.minOutPct = Float.valueOf( minOutPct );
        
        theClone.analogInVolts = Float.valueOf( analogInVolts );
        
        return theClone;
    }
    
    
    
}
