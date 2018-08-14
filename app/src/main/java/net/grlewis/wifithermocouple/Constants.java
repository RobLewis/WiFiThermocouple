package net.grlewis.wifithermocouple;

import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

final class Constants {
    
    // URLs to control the device
    
    private static final String urlRoot = "http://wifitempsensor.lan/";
    
    private static final String resetDefaults = "defaults";
    private static final String tempF = "temperature/F";
    private static final String tempC = "temperature/C";
    private static final String blueLedOn = "blueled/on";
    private static final String blueLedOff = "blueled/off";
    private static final String fanOn = "fan/on";
    private static final String fanOff = "fan/off";
    private static final String fanDc = "fan/dc";
    private static final String setFanDc = "fan/?dc=";
    private static final String setFanCl = "fan/?cl=";
    private static final String disableFanDc = "fan/dcdisable";
    private static final String enableFanDc = "fan/dcenable";
    private static final String currentSeconds = "time/CurrentSeconds";
    private static final String enableWatchdog = "time/watchdogenable";
    private static final String disableWatchdog = "time/watchdogdisable";
    private static final String watchdogStatus = "time/watchdogstatus";
    private static final String resetWatchdog = "time/watchdogreset";
    private static final String readAnalog = "analog/in";
    
    
    static URL RESET_DEFAULTS_URL;         //
    static URL TEMP_F_URL = null;          //
    static URL TEMP_C_URL = null;          //
    static URL BLUE_LED_ON_URL = null;     //
    static URL BLUE_LED_OFF_URL = null;    //
    static URL FAN_ON_URL = null;          //
    static URL FAN_OFF_URL = null;         //
    static URL FAN_DC_URL = null;          //
    static URL FAN_SET_DC_URL = null;      //
    static URL FAN_SET_CL_URL = null;      //
    static URL FAN_DISABLE_DC_URL = null;  //
    static URL FAN_ENABLE_DC_URL = null;   //
    static URL CURRENT_SECONDS_URL = null; //
    static URL ENABLE_WD_URL = null;       //
    static URL DISABLE_WD_URL = null;      //
    static URL WD_STATUS_URL = null;       //
    static URL RESET_WD_URL = null;        //
    static URL READ_ANALOG_URL = null;     //
    
    static {  // initializer
        
        try {  // need this to handle the possible MalformedURLException
            RESET_DEFAULTS_URL = new URL( urlRoot + resetDefaults );
            TEMP_F_URL = new URL( urlRoot + tempF );
            TEMP_C_URL = new URL( urlRoot + tempC );
            BLUE_LED_ON_URL = new URL( urlRoot + blueLedOn );
            BLUE_LED_OFF_URL = new URL( urlRoot + blueLedOff );
            FAN_ON_URL = new URL( urlRoot + fanOn );
            FAN_OFF_URL = new URL( urlRoot + fanOff );
            FAN_DC_URL = new URL( urlRoot + fanDc );
            FAN_SET_DC_URL = new URL( urlRoot + setFanDc );
            FAN_SET_CL_URL = new URL( urlRoot + setFanCl );
            FAN_DISABLE_DC_URL = new URL( urlRoot + disableFanDc );
            FAN_ENABLE_DC_URL = new URL( urlRoot + enableFanDc );
            CURRENT_SECONDS_URL = new URL( urlRoot + currentSeconds );
            ENABLE_WD_URL = new URL( urlRoot + enableWatchdog );
            DISABLE_WD_URL = new URL( urlRoot + disableWatchdog );
            WD_STATUS_URL = new URL( urlRoot + watchdogStatus );
            RESET_WD_URL = new URL( urlRoot + resetWatchdog );
            READ_ANALOG_URL = new URL( urlRoot + readAnalog);
        }
        catch( MalformedURLException m) {
            Log.d( "Constants", "initializer threw MalformedURLException: " + m.getMessage() );
        }
        
    }
    
    
    
    
    
    
    
}
