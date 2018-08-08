package net.grlewis.wifithermocouple;

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
    
    static final URL RESET_DEFAULTS_URL = null;
    static final URL TEMP_F_URL = null;
    static final URL TEMP_C_URL = null;
    static final URL BLUE_LED_ON_URL = null;
    static final URL BLUE_LED_OFF_URL = null;
    static final URL FAN_ON_URL = null;
    static final URL FAN_OFF_URL = null;
    static final URL FAN_DC_URL = null;
    static final URL FAN_SET_DC_URL = null;
    static final URL FAN_SET_CL_URL = null;
    static final URL FAN_DISABLE_DC_URL = null;
    static final URL FAN_ENABLE_DC_URL = null;
    static final URL CURRENT_SECONDS_URL = null;
    static final URL ENABLE_WD_URL = null;
    static final URL DISABLE_WD_URL = null;
    static final URL WD_STATUS_URL = null;
    static final URL RESET_WD_URL = null;
    
    static {  // initializer
        
        try {
            URL RESET_DEFAULTS_URL = new URL( urlRoot + resetDefaults );
            URL TEMP_F_URL = new URL( urlRoot + tempF );
            URL TEMP_C_URL = new URL( urlRoot + tempC );
            URL BLUE_LED_ON_URL = new URL( urlRoot + blueLedOn );
            URL BLUE_LED_OFF_URL = new URL( urlRoot + blueLedOff );
            URL FAN_ON_URL = new URL( urlRoot + fanOn );
            URL FAN_OFF_URL = new URL( urlRoot + fanOff );
            URL FAN_DC_URL = new URL( urlRoot + fanDc );
            URL FAN_SET_DC_URL = new URL( urlRoot + setFanDc );
            URL FAN_SET_CL_URL = new URL( urlRoot + setFanCl );
            URL FAN_DISABLE_DC_URL = new URL( urlRoot + disableFanDc );
            URL FAN_ENABLE_DC_URL = new URL( urlRoot + enableFanDc );
            URL CURRENT_SECONDS_URL = new URL( urlRoot + currentSeconds );
            URL ENABLE_WD_URL = new URL( urlRoot + enableWatchdog );
            URL DISABLE_WD_URL = new URL( urlRoot + disableWatchdog );
            URL WD_STATUS_URL = new URL( urlRoot + watchdogStatus );
            URL RESET_WD_URL = new URL( urlRoot + resetWatchdog );
        }
        catch( MalformedURLException m) { }
        
    }
    
    
    
    
    
    
    
}
