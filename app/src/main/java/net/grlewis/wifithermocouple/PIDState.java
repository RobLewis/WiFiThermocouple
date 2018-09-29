package net.grlewis.wifithermocouple;

// Class to store current PID appState and to use for a library of states
// We're using objects so the null state means "undefined"

import static net.grlewis.wifithermocouple.Constants.*;

class PIDState implements Cloneable {  // TODO: cloneable?
    
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
    
    
    // constructor
    PIDState() {
        setPoint = DEFAULT_SETPOINT;
        currentVariableValue = null;
        previousVariableValue = null;
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
    }
    
    
    // GETTERS & SETTERS
    
    // reset controller
    public Boolean isReset( ) {
        return reset;
    }
    public void setReset( Boolean reset ) {
        this.reset = reset;
    }
    
    
    // The PID setpoint
    public Float getSetPoint( ) {
        return setPoint;
    }
    public void set( Float setPoint ) {
        this.setPoint = setPoint;
    }
    
    
    // current value of the controlled variable (i.e., temperature)
    public Float getCurrentVariableValue( ) {
        return currentVariableValue;
    }
    public void setCurrentVariableValue( Float currentVariableValue ) {
        this.currentVariableValue = currentVariableValue;
    }
    
    
    // previous value of the controlled variable (updated on each iteration)
    public Float getPreviousVariableValue( ) {
        return previousVariableValue;
    }
    public void setPreviousVariableValue( Float previousVariableValue ) {
        this.previousVariableValue = previousVariableValue;
    }
    
    
    // overall gain of PID
    public Float getGain( ) {
        return gain;
    }
    public void setGain( Float gain ) {
        this.gain = gain;
    }
    
    
    // coefficient of the proportional term
    public Float getPropCoeff( ) {
        return propCoeff;
    }
    public void setPropCoeff( Float propCoeff ) {
        this.propCoeff = propCoeff;
    }
    
    
    // coefficient of the integral term
    public Float getIntCoeff( ) {
        return intCoeff;
    }
    public void setIntCoeff( Float intCoeff ) {
        this.intCoeff = intCoeff;
    }
    
    
    // coefficient of the differential term
    public Float getDiffCoeff( ) {
        return diffCoeff;
    }
    public void setDiffCoeff( Float diffCoeff ) {
        this.diffCoeff = diffCoeff;
    }
    
    
    // whether the integrator is currently clamped
    public Boolean intIsClamped( ) {
        return intClamped;
    }
    public void setIntClamped( Boolean intClamped ) {
        this.intClamped = intClamped;
    }
    
    
    // most recent % of "full on" of the controlled device
    public Float getCurrentPctg( ) {
        return currentPctg;
    }
    public void setCurrentPctg( Float currentPctg ) {
        this.currentPctg = currentPctg;
    }
    
    
    // current value of the accumulated integral term
    public Float getIntAccum( ) {
        return intAccum;
    }
    public void setIntAccum( Float intAccum ) {
        this.intAccum = intAccum;
    }
    
    
    // current repeat interval in seconds
    public Float getPeriodSecs( ) {
        return periodSecs;
    }
    public void setPeriodSecs( Float periodSecs ) {
        this.periodSecs = periodSecs;
    }
    
    
    // whether PID operation is enabled
    public Boolean isEnabled( ) {
        return enabled;
    }
    public void setEnabled( Boolean enabled ) {  // TODO: shut heat off when disabling
        this.enabled = enabled;
    }
    
    
    // whether the output device (fan, heater, etc.) is currently on or off
    public Boolean outputIsOn( ) {return outputOn; }
    public void setOutputOn( Boolean outOn ) {
        this.outputOn = outOn;
    }
    
    
    
    @Override  // TODO: serialize before storing?
    protected PIDState clone( ) throws CloneNotSupportedException {
        PIDState theClone = new PIDState();
        theClone.setPoint = setPoint;  // just copying references? will we get NPE?
        theClone.currentVariableValue = currentVariableValue;
        theClone.previousVariableValue = previousVariableValue;
        theClone.gain = gain;
        theClone.propCoeff = propCoeff;
        theClone.intCoeff = intCoeff;
        theClone.diffCoeff = diffCoeff;
        theClone.intClamped = intClamped;
        theClone.currentPctg = currentPctg ;
        theClone.intAccum = intAccum;
        theClone.periodSecs = periodSecs;
        theClone.enabled = enabled;
        
        return theClone;
    }
    
    
    
}
