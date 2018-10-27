package net.grlewis.wifithermocouple;


import android.util.Log;

import java.util.Date;

import static net.grlewis.wifithermocouple.Constants.DEBUG;
import static net.grlewis.wifithermocouple.Constants.HARDWARE_VERSION;
import static net.grlewis.wifithermocouple.Constants.SOFTWARE_VERSION;

class ApplicationState

{
    private static final String TAG = ApplicationState.class.getSimpleName();
    
    // instance variables
    
    // TODO: figure out what needs to be here (most can go in PIDState?)
    
    // appState of the peripheral device & firmware (initially null)
    //private Float currentTempF;           // last temperature reading from the  (currentVariableValue)
    //private Float currentTempC;           // converted/synced to/from C/F as needed (no equivalent)
    private Float lastCurrentSeconds;       // last elapsed seconds (since startup) read from the peripheral's clock (0.1 sec resolution) (no equivalent)
    private Date lastSecondsTimestamp;      // system time when lastCurrentSeconds was last sampled (no equivalent)
    //private Float lastAnalogInputVolts;   // last setting read from the analog pot (0.0 – 1.0) (analogInVolts)
    private Boolean blueLEDState;           // true if blue LED is on (no equivalent)
    //private Boolean fanState;             // true if fan is on (outputOn)
    private Float fanDutyCyclePct;          // 0.0 – 100.0% for the peripheral's built-in fan control (no equivalent)
    private Float fanDutyCycleSecs;         // length of the built-in fan duty cycle period in seconds  (no equivalent)
    private Boolean fanDutyCycleEnabled;    // true if the peripheral's built-in fan duty cycling is enabled (no equivalent)
    private Boolean watchdogEnabled;        // true if the peripheral's watchdog timer is enabled (no equivalent)
    private Boolean watchdogAlarmed;        // true if the watchdog timer has expired (no equivalent)
    private String model;                   // device model read from device (no equivalent)
    private String hwVersion;               // hardware version read from device (no equivalent)
    private String swVersion;               // software version read from device (no equivalent)
    
    
    // appState of the PID BBQ controller (this app's, not the peripheral's internal one, if any)
    private boolean pidEnabled;
    
    
    // constructor
    // note boxed versions of variables should auto initialize null
    ApplicationState() {
        if( DEBUG ) Log.d( TAG, "exiting constructor" );
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
    
    
    // Note these methods merely record and report the appState of the app variables; they do nothing to bring it about
    
    
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
    
    
    public boolean blueLEDIsOn() {
        return blueLEDState;
    }
    public void setBlueLEDState( boolean blueLEDOn ) {
        this.blueLEDState = blueLEDOn;
    }
    
    
    public boolean pidIsEnabled( ) { return pidEnabled; }
    public void enablePid( boolean enabled ) { pidEnabled = enabled; }
    
    
}
