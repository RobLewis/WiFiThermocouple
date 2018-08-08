package net.grlewis.wifithermocouple;

import java.net.MalformedURLException;
import java.net.URL;

final class Constants {
    
    // URLs to control the device
    
    static final String urlRoot = "http://wifitempsensor.lan/";
    
    static final String resetDefaults = "defaults";
    static final String tempF = "temperature/F";
    static final String tempC = "temperature/C";
    static final String blueLedOn = "blueled/on";
    static final String blueLedOff = "blueled/off";
    static final String fanOn = "fan/on";
    static final String fanOff = "fan/off";
    static final String fanDc = "fan/dc";
    static final String setFanDc = "fan/?dc=";
    static final String setFanCl = "fan/?cl=";
    static final String disableFanDc = "fan/dcdisable";
    static final String enableFanDc = "fan/dcenable";
    static final String currentSeconds = "time/CurrentSeconds";
    static final String enableWatchdog = "time/watchdogenable";
    static final String disableWatchdog = "time/watchdogdisable";
    static final String watchdogStatus = "time/watchdogstatus";
    static final String resetWatchdog = "time/watchdogreset";
    
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
        }
        catch( MalformedURLException m) { }
        
    }
    
    
    
    
    
    
    
}
