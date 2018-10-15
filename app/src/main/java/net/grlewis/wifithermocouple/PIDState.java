package net.grlewis.wifithermocouple;

// Class to store current PID appState and to use for a library of states
// We're using objects so the null state means "undefined"

import java.io.Serializable;

import io.reactivex.subjects.BehaviorSubject;

import static net.grlewis.wifithermocouple.Constants.*;

class PIDState implements Cloneable, Serializable {
    
    private Parameters parameters;
    public BehaviorSubject<Parameters> pidStatePublisher;
    private boolean publishChanges;
    private Float previousAnalogIn;
    
    class Parameters implements Cloneable, Serializable {
        
         Float setPoint;
         Float currentVariableValue;
        private Float previousVariableValue;
        private Float gain;
        private Float propCoeff;
        private Float intCoeff;
        private Float diffCoeff;
         Boolean intClamped;  // is the integrator term clamped?
        private Float currentPctg;   // last value of % on time for each period
        private Float intAccum;      // the accumulated integral term
        private Float periodSecs;    // seconds for loop repeat interval
         Boolean enabled;     // is the PID enabled?
        private Boolean reset;       // has it been reset?
         Boolean outputOn;    // is the fan, heater, whatever currently on?
        private Float minOutPct;     // the minimum controlled output percentage that will cause turnon
        
        private Float analogInVolts; // 0.0-1.0 (-1 means not set)
        
        Parameters() {  // constructor to set initial values
            setPoint = DEFAULT_SETPOINT;  // can't run if we haven't defined this
            currentVariableValue = 0f;
            previousVariableValue = 0f;
            gain = DEFAULT_GAIN;
            propCoeff = DEFAULT_PROP_COEFF;
            intCoeff = DEFAULT_INT_COEFF;
            diffCoeff = DEFAULT_DIFF_COEFF;
            intClamped = true;  // TODO: better?
            currentPctg = 0f;
            intAccum = 0f;
            periodSecs = DEFAULT_PERIOD_SECS;
            enabled = false;
            reset = true;
            outputOn = false;
            minOutPct = DEFAULT_MIN_OUT_PCT;
            
            analogInVolts = -1f;
        }
        
        @Override  // don't have to implement because all contents are immutable
        public Parameters clone() throws CloneNotSupportedException {
            return (Parameters) super.clone();
        }
    }  // class Parameters
    
    
    
    // constructor
    PIDState() {
        parameters = new Parameters();
        pidStatePublisher = BehaviorSubject.createDefault( parameters );
        publishChanges = true;  // by default, publish changes
    }
    
    
    // GETTERS & SETTERS
    
    // publishing control
    public boolean publishingChanges( ) {
        return publishChanges;
    }
    public void setPublishChanges( boolean publishChanges ) {
        this.publishChanges = publishChanges;
    }
    
    // The PID setpoint
    Float getSetPoint( ) {
        return parameters.setPoint;
    }
    void set( Float setPoint ) {
        this.parameters.setPoint = setPoint;
        pidStatePublisher.onNext( parameters );
    }
    
    // current value of the controlled variable (i.e., temperature)
    Float getCurrentVariableValue( ) {
        return parameters.currentVariableValue;
    }
    void setCurrentVariableValue( Float currentVariableValue ) {
        this.parameters.currentVariableValue = currentVariableValue;
        pidStatePublisher.onNext( parameters );
    }
    
    // previous value of the controlled variable (updated on each iteration)
    Float getPreviousVariableValue( ) {
        return parameters.previousVariableValue;
    }
    void setPreviousVariableValue( Float previousVariableValue ) {
        this.parameters.previousVariableValue = previousVariableValue;
        pidStatePublisher.onNext( parameters );
    }
    void updatePreviousVariableValue( ) {  // sets it equal to CurrentVariableValue (FIXME: avoiding a crash?)
        this.parameters.previousVariableValue = this.parameters.currentVariableValue;
        //pidStatePublisher.onNext( parameters );  // TODO: do we need this?
    }
    
    // overall gain of PID
    Float getGain( ) {
        return parameters.gain;
    }
    void setGain( Float gain ) {
        this.parameters.gain = gain;
        pidStatePublisher.onNext( parameters );
    }
    
    // coefficient of the proportional term
    Float getPropCoeff( ) {
        return parameters.propCoeff;
    }
    void setPropCoeff( Float propCoeff ) {
        this.parameters.propCoeff = propCoeff;
        pidStatePublisher.onNext( parameters );
    }
    
    // coefficient of the integral term
    Float getIntCoeff( ) {
        return parameters.intCoeff;
    }
    void setIntCoeff( Float intCoeff ) {
        this.parameters.intCoeff = intCoeff;
        pidStatePublisher.onNext( parameters );
    }
    
    // coefficient of the differential term
    Float getDiffCoeff( ) {
        return parameters.diffCoeff;
    }
    void setDiffCoeff( Float diffCoeff ) {
        this.parameters.diffCoeff = diffCoeff;
        pidStatePublisher.onNext( parameters );
    }
    
    // whether the integrator is currently clamped
    Boolean intIsClamped( ) {
        return parameters.intClamped;
    }
    void setIntClamped( Boolean intClamped ) {
        this.parameters.intClamped = intClamped;
        pidStatePublisher.onNext( parameters );
    }
    
    // most recent % of "full on" of the controlled device
    Float getCurrentPctg( ) {
        return parameters.currentPctg;
    }
    void setCurrentPctg( Float currentPctg ) {
        this.parameters.currentPctg = currentPctg;
        pidStatePublisher.onNext( parameters );
    }
    
    // current value of the accumulated integral term
    Float getIntAccum( ) {
        return parameters.intAccum;
    }
    void setIntAccum( Float intAccum ) {
        this.parameters.intAccum = intAccum;
        pidStatePublisher.onNext( parameters );
    }
    
    // current repeat interval in seconds
    Float getPeriodSecs( ) {
        return parameters.periodSecs;
    }
    void setPeriodSecs( Float periodSecs ) {
        this.parameters.periodSecs = periodSecs;
        pidStatePublisher.onNext( parameters );
    }
    
    // whether PID operation is enabled
    Boolean isEnabled( ) {
        return parameters.enabled;
    }
    void setEnabled( Boolean enabled ) {  // TODO: shut heat off when disabling
        this.parameters.enabled = enabled;
        pidStatePublisher.onNext( parameters );
    }
    
    // reset controller
    Boolean isReset( ) {
        return parameters.reset;
    }
    void setReset( Boolean reset ) {
        this.parameters.reset = reset;
        pidStatePublisher.onNext( parameters );
    }
    
    // whether the output device (fan, heater, etc.) is currently on or off
    Boolean outputIsOn( ) { return parameters.outputOn; }
    void setOutputOn( Boolean outOn ) {
        this.parameters.outputOn = outOn;
        pidStatePublisher.onNext( parameters );
    }
    
    Float getMinOutPctg( ) { return parameters.minOutPct; }
    void setMinOutPctg( Float minOutPct ) {
        this.parameters.minOutPct = minOutPct;
        pidStatePublisher.onNext( parameters );
    }
    
    Float getAnalogInVolts( ) { return parameters.analogInVolts; }
    void setAnalogInVolts( Float analogVolts ) {
        this.parameters.analogInVolts = analogVolts;
        if( !analogVolts.equals( previousAnalogIn ) ) {  // only publish if value changed
            pidStatePublisher.onNext( parameters );
            previousAnalogIn = analogVolts;
        }
    }
    
    
    @Override
    // If all you do is implement Cloneable, only subclasses and members of the same package
    // will be able to invoke clone() on the object. To enable any class in any package
    // to access the clone() method, you have to override it and declare it public
    // So this whole thing (except 'implements Cloneable') may be unnecessary.
    // (one source indicated this method should return Object (cast to PIDState))
    public PIDState clone( ) throws CloneNotSupportedException {
        PIDState theClone = (PIDState) super.clone();  // Object.clone() returns an Object

//  Since boxed primitives are immutable, this apparently is not necessary:
//        theClone.setPoint = Float.valueOf( setPoint );  // just copying references? will we get NPE?
//        theClone.currentVariableValue = Float.valueOf( currentVariableValue );
//        theClone.previousVariableValue = Float.valueOf( previousVariableValue );
//        theClone.gain = Float.valueOf( gain );
//        theClone.propCoeff = Float.valueOf( propCoeff );
//        theClone.intCoeff = Float.valueOf( intCoeff );
//        theClone.diffCoeff = Float.valueOf( diffCoeff );
//        theClone.intClamped = Boolean.valueOf( intClamped );
//        theClone.currentPctg = Float.valueOf( currentPctg ) ;
//        theClone.intAccum = Float.valueOf( intAccum );
//        theClone.periodSecs = Float.valueOf( periodSecs );
//        theClone.enabled = Boolean.valueOf( enabled );
//        theClone.reset = Boolean.valueOf( reset );
//        theClone.outputOn = Boolean.valueOf( outputOn );
//        theClone.minOutPct = Float.valueOf( minOutPct );
//
//        theClone.analogInVolts = Float.valueOf( analogInVolts );
        
        return theClone;
    }
}
