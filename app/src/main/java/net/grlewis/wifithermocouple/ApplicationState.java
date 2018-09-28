package net.grlewis.wifithermocouple;


import java.util.Date;

import static net.grlewis.wifithermocouple.Constants.HARDWARE_VERSION;
import static net.grlewis.wifithermocouple.Constants.SOFTWARE_VERSION;

class ApplicationState

{
    
    // instance variables
    
    // state of the peripheral device & firmware (initially null)
    private Float currentTempF;           // last temperature reading from the thermocouple
    private Float currentTempC;           // converted/synced to/from C/F as needed
    private Float lastCurrentSeconds;     // last elapsed seconds (since startup) read from the peripheral's clock (0.1 sec resolution)
    private Date lastSecondsTimestamp;    // system time when lastCurrentSeconds was last sampled
    private Float lastAnalogInputVolts;   // last setting read from the analog pot (0.0 – 1.0)
    private Boolean blueLEDState;         // true if blue LED is on
    private Boolean fanState;             // true if fan is on
    private Float fanDutyCyclePct;        // 0.0 – 100.0% for the peripheral's built-in fan control
    private Float fanDutyCycleSecs;       // length of the fan duty cycle period in seconds
    private Boolean fanDutyCycleEnabled;  // true if the peripheral's built-in fan duty cycling is enabled
    private Boolean watchdogEnabled;      // true if the peripheral's watchdog timer is enabled
    private Boolean watchdogAlarmed;      // true if the watchdog timer has expired
    private String model;                 // device model read from device
    private String hwVersion;             // hardware version read from device
    private String swVersion;             // software version read from device
    
    
    // state of the PID BBQ controller (this app's, not the peripheral's internal one, if any)
    private boolean pidEnabled;
    
    
    // constructor
    // note boxed versions of variables should auto initialize null
    ApplicationState() {
    
    }
    
    
    
    
    
    
    public String getSoftwareVersion( ) {  // can be null
        return swVersion;
    }
    public void setSoftwareVersion( String version ) {
        swVersion = version;
    }
    public String getHardwareVersion( ) {  // can be null
        return hwVersion;
    }
    public void setHardwareVersion( String version ) {
        hwVersion = version;
    }
    
    
    
    
    
    // Note these methods merely record and report the state of the app variables; they do nothing to bring it about
    
    
    public  float getCurrentTempF( ) {
        return currentTempF;
    }
    public  void setCurrentTempF( float currentTempF ) {
        this.currentTempF = currentTempF;
        this.currentTempC = ((currentTempF + 40f) * 5f/9f) - 40f;
    }
    
    public float getCurrentTempC( ) {
        return currentTempC;
    }
    public void setCurrentTempC( float currentTempC ) {
        this.currentTempC = currentTempC;
        this.currentTempF = ((currentTempC + 40f) * 9f/5f) - 40f;
    }
    
    
    public float getLastCurrentSeconds( ) {
        return lastCurrentSeconds;
    }
    public void setLastCurrentSeconds( float lastCurrentSeconds ) {
        this.lastCurrentSeconds = lastCurrentSeconds;
        this.lastSecondsTimestamp = new Date( );
    }
    public Date getLastSecondsTimestamp() {
        return lastSecondsTimestamp;
    }
    
    
    public float getLastAnalogInputVolts( ) {
        return lastAnalogInputVolts;
    }
    public void setLastAnalogInputVolts( float lastAnalogInputVolts ) {
        this.lastAnalogInputVolts = lastAnalogInputVolts;
    }
    
    
    public float getFanDutyCycleSecs( ) {
        return fanDutyCycleSecs;
    }
    public void setFanDutyCycleSecs( float dutyCycleLengthSecs ) {
        this.fanDutyCycleSecs = dutyCycleLengthSecs;
    }
    
    
    public float getDutyCycleOnPercent( ) {
        return fanDutyCyclePct;
    }
    public void setDutyCycleOnPercent( float dutyCycleOnPercent ) {
        this.fanDutyCyclePct = dutyCycleOnPercent;
    }
    
    
    public boolean dutyCycleIsEnabled( ) {
        return fanDutyCycleEnabled;
    }
    public void setDutyCycleEnabled( boolean dutyCycleEnabled ) {
        this.fanDutyCycleEnabled = dutyCycleEnabled;
    }
    
    
    public boolean watchdogIsEnabled( ) {
        return watchdogEnabled;
    }
    public void setWatchdogEnabled( boolean watchdogEnabled ) {
        this.watchdogEnabled = watchdogEnabled;
    }
    
    
    public boolean watchDogHasAlarmed( ) {
        return watchdogAlarmed;
    }
    public void setWatchDogAlarmed( boolean watchDogAlarmed ) {
        this.watchdogAlarmed = watchDogAlarmed;
    }
    
    
    public boolean fanIsOn( ) {
        return fanState;
    }
    public void setFanState( boolean fanOn ) {
        this.fanState = fanOn;
    }
    
    public boolean blueLEDIsOn() {
        return blueLEDState;
    }
    public void setBlueLEDState( boolean blueLEDOn ) {
        this.blueLEDState = blueLEDOn;
    }
    
    
    
    public boolean pidIsEnabled( ) { return pidEnabled; }
    public void enablePid( boolean enabled ) { pidEnabled = enabled; }
    
    
}
