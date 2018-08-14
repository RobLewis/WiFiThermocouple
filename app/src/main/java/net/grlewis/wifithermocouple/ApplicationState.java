package net.grlewis.wifithermocouple;

class ApplicationState

{
    
    
    private float currentTempF;
    private float currentTempC;
    
    private float lastCurrentSeconds;
    private float lastAnalogInputVolts;
    
    private float dutyCycleLengthSecs;
    private float dutyCycleOnPercent;
    private float currentPercent;  // ??
    
    private boolean dutyCycleEnabled;
    private boolean watchdogEnabled;
    private boolean watchDogAlarmed;
    
    private boolean fanOn;
    
    
    
    // default constructor
    ApplicationState() {
    
    }
    
    
    
    
    
    
    public float getCurrentTempF( ) {
        return currentTempF;
    }
    public void setCurrentTempF( float currentTempF ) {
        this.currentTempF = currentTempF;
        this.currentTempC = ((currentTempF + 40) * 5/9) - 40;
    }
    
    
    public float getCurrentTempC( ) {
        return currentTempC;
    }
    public void setCurrentTempC( float currentTempC ) {
        this.currentTempC = currentTempC;
        this.currentTempF = ((currentTempC + 40) * 9/5) - 40;
    }
    
    
    public float getLastCurrentSeconds( ) {
        return lastCurrentSeconds;
    }
    public void setLastCurrentSeconds( float lastCurrentSeconds ) {
        this.lastCurrentSeconds = lastCurrentSeconds;
    }
    
    
    public float getLastAnalogInputVolts( ) {
        return lastAnalogInputVolts;
    }
    public void setLastAnalogInputVolts( float lastAnalogInputVolts ) {
        this.lastAnalogInputVolts = lastAnalogInputVolts;
    }
    
    
    public float getDutyCycleLengthSecs( ) {
        return dutyCycleLengthSecs;
    }
    public void setDutyCycleLengthSecs( float dutyCycleLengthSecs ) {
        this.dutyCycleLengthSecs = dutyCycleLengthSecs;
    }
    
    
    public float getDutyCycleOnPercent( ) {
        return dutyCycleOnPercent;
    }
    public void setDutyCycleOnPercent( float dutyCycleOnPercent ) {
        this.dutyCycleOnPercent = dutyCycleOnPercent;
    }
    
    
    public float getCurrentPercent( ) {
        return currentPercent;
    }
    public void setCurrentPercent( float currentPercent ) {
        this.currentPercent = currentPercent;
    }
    
    
    public boolean dutyCycleIsEnabled( ) {
        return dutyCycleEnabled;
    }
    public void setDutyCycleEnabled( boolean dutyCycleEnabled ) {
        this.dutyCycleEnabled = dutyCycleEnabled;
    }
    
    
    public boolean watchdogIsEnabled( ) {
        return watchdogEnabled;
    }
    public void setWatchdogEnabled( boolean watchdogEnabled ) {
        this.watchdogEnabled = watchdogEnabled;
    }
    
    
    public boolean watchDogIsAlarmed( ) {
        return watchDogAlarmed;
    }
    public void setWatchDogAlarmed( boolean watchDogAlarmed ) {
        this.watchDogAlarmed = watchDogAlarmed;
    }
    
    
    public boolean fanIsOn( ) {
        return fanOn;
    }
    public void setFanOn( boolean fanOn ) {
        this.fanOn = fanOn;
    }
    
    
}
