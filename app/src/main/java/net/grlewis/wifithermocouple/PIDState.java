package net.grlewis.wifithermocouple;

// Class to store current PID applicationState and to use for a library of states
// We're using objects so the null state means "undefined"

import android.util.Log;

import java.io.Serializable;

import io.reactivex.subjects.BehaviorSubject;

import static net.grlewis.wifithermocouple.Constants.*;

class PIDState implements Cloneable, Serializable {
    
    private final static String TAG = PIDState.class.getSimpleName();
    
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
         Boolean intClamped;         // is the integrator term clamped?
        private Float currentPctg;   // last value of % on time for each period
        private Float intAccum;      // the accumulated integral term
        private Float periodSecs;    // seconds for loop repeat interval
         Boolean enabled;            // is the PID enabled?
        private Boolean reset;       // has it been reset?
         Boolean outputOn;           // is the fan, heater, whatever currently on?
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
        if( DEBUG ) Log.d( TAG, "exiting constructor" );
    }
    
    
    // GETTERS & SETTERS
    
    // publishing control (not part of PIDController interface)
    public boolean publishingChanges( ) {
        return publishChanges;
    }
    public void setPublishChanges( boolean publishChanges ) {  // called from BBQController
        this.publishChanges = publishChanges;
    }
    
    // not in interface, called from PID Control Loop
    void updatePreviousVariableValue( ) {  // sets it equal to CurrentVariableValue (FIXME: avoiding a crash?)
        this.parameters.previousVariableValue = this.parameters.currentVariableValue;
        //pidStatePublisher.onNext( parameters );  // TODO: do we need this?
    }
    
    
    // Methods handling calls forwarded from BBQController implementation of PIDController
    
    // The PID setpoint
    Float getSetPoint( ) {
        return parameters.setPoint;
    }       // called from BBQController
    void set( Float setPoint ) {                               // called from BBQController
        this.parameters.setPoint = setPoint;
        pidStatePublisher.onNext( parameters );
    }
    
    // current value of the controlled variable (i.e., temperature)
    Float getCurrentVariableValue( ) { return parameters.currentVariableValue; }    // called from BBQController
    void setCurrentVariableValue( Float currentVariableValue ) {          // called from BBQController
        this.parameters.currentVariableValue = currentVariableValue;
        pidStatePublisher.onNext( parameters );
    }
    
    // previous value of the controlled variable (updated on each iteration)
    Float getPreviousVariableValue( ) {                               // called from BBQController
        return parameters.previousVariableValue;
    }
    void setPreviousVariableValue( Float previousVariableValue ) {    // called from BBQController
        this.parameters.previousVariableValue = previousVariableValue;
        pidStatePublisher.onNext( parameters );
    }
   
    
    // overall gain of PID
    Float getGain( ) {
        return parameters.gain;
    }          // called from BBQController
    void setGain( Float gain ) {                          // called from BBQController
        this.parameters.gain = gain;
        pidStatePublisher.onNext( parameters );
    }
    
    // coefficient of the proportional term
    Float getPropCoeff( ) {
        return parameters.propCoeff;
    }    // called from BBQController
    void setPropCoeff( Float propCoeff ) {                    // called from BBQController
        this.parameters.propCoeff = propCoeff;
        pidStatePublisher.onNext( parameters );
    }
    
    // coefficient of the integral term
    Float getIntCoeff( ) {
        return parameters.intCoeff;
    }      // called from BBQController
    void setIntCoeff( Float intCoeff ) {                      // called from BBQController
        this.parameters.intCoeff = intCoeff;
        pidStatePublisher.onNext( parameters );
    }
    
    // coefficient of the differential term
    Float getDiffCoeff( ) {
        return parameters.diffCoeff;
    }    // called from BBQController
    void setDiffCoeff( Float diffCoeff ) {                    // called from BBQController
        this.parameters.diffCoeff = diffCoeff;
        pidStatePublisher.onNext( parameters );
    }
    
    // whether the integrator is currently clamped
    Boolean intIsClamped( ) {
        return parameters.intClamped;
    }       // called from BBQController
    void setIntClamped( Boolean intClamped ) {                      // called from BBQController
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
    void setIntAccum( Float intAccum ) {                      // called from BBQController
        this.parameters.intAccum = intAccum;
        pidStatePublisher.onNext( parameters );
    }
    
    // current repeat interval in seconds
    Float getPeriodSecs( ) {
        return parameters.periodSecs;
    }  // called from BBQController
    void setPeriodSecs( Float periodSecs ) {                  // called from BBQController
        this.parameters.periodSecs = periodSecs;
        pidStatePublisher.onNext( parameters );
    }
    
    // whether PID operation is enabled
    Boolean isEnabled( ) { return parameters.enabled; }          // called from BBQController
    void setEnabled( Boolean enabled ) {  // TODO: shut heat off when disabling     // called from BBQController
        this.parameters.enabled = enabled;
        pidStatePublisher.onNext( parameters );
    }
    
    // reset controller
    Boolean isReset( ) {
        return parameters.reset;
    }              // called from BBQController
    void setReset( Boolean reset ) {                             // called from BBQController
        this.parameters.reset = reset;
        pidStatePublisher.onNext( parameters );
    }
    
    // whether the output device (fan, heater, etc.) is currently on or off
    Boolean outputIsOn( ) { return parameters.outputOn; }       // called from BBQController
    void setOutputOn( Boolean outOn ) {                         // called from BBQController
        this.parameters.outputOn = outOn;
        pidStatePublisher.onNext( parameters );
    }
    
    Float getMinOutPctg( ) { return parameters.minOutPct; }     // called from BBQController
    void setMinOutPctg( Float minOutPct ) {                     // called from BBQController
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
