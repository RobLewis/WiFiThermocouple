package net.grlewis.wifithermocouple;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

/*
* Tracking Request UUID ranges:
*     0x1000: watchdog enabler
*     0x2000: watchdog feeder
*     0x3000: temperature updater
*     0x4000: fan control
*
* */

final class Constants {
    
    static final String TAG = Constants.class.getSimpleName();
    
    static boolean DEBUG = true;
    
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
    private static final String getInfo = "info";
    
    static JSONObject DEFAULT_TEMP_F;
    ;
    
    
    
    static URL RESET_DEFAULTS_URL;         //
    static URL TEMP_F_URL = null;          // JSON
    static URL TEMP_C_URL = null;          // JSON
    static URL BLUE_LED_ON_URL = null;     //
    static URL BLUE_LED_OFF_URL = null;    //
    static URL FAN_ON_URL = null;          //
    static URL FAN_OFF_URL = null;         //
    static URL FAN_DC_URL = null;          // JSON
    static URL FAN_SET_DC_URL = null;      //
    static URL FAN_SET_CL_URL = null;      //
    static URL FAN_DISABLE_DC_URL = null;  //
    static URL FAN_ENABLE_DC_URL = null;   //
    static URL CURRENT_SECONDS_URL = null; // JSON
    static URL ENABLE_WD_URL = null;       //
    static URL DISABLE_WD_URL = null;      //
    static URL WD_STATUS_URL = null;       // JSON
    static URL RESET_WD_URL = null;        //
    static URL READ_ANALOG_URL = null;     // JSON
    static URL GET_INFO_URL = null;        // JSON
    
    static {  // initializer
        
        try {  // need this to handle the possible MalformedURLException
            
            RESET_DEFAULTS_URL  = new URL( urlRoot + resetDefaults );
            TEMP_F_URL          = new URL( urlRoot + tempF );
            TEMP_C_URL          = new URL( urlRoot + tempC );
            BLUE_LED_ON_URL     = new URL( urlRoot + blueLedOn );
            BLUE_LED_OFF_URL    = new URL( urlRoot + blueLedOff );
            FAN_ON_URL          = new URL( urlRoot + fanOn );
            FAN_OFF_URL         = new URL( urlRoot + fanOff );
            FAN_DC_URL          = new URL( urlRoot + fanDc );
            FAN_SET_DC_URL      = new URL( urlRoot + setFanDc );
            FAN_SET_CL_URL      = new URL( urlRoot + setFanCl );
            FAN_DISABLE_DC_URL  = new URL( urlRoot + disableFanDc );
            FAN_ENABLE_DC_URL   = new URL( urlRoot + enableFanDc );
            CURRENT_SECONDS_URL = new URL( urlRoot + currentSeconds );
            ENABLE_WD_URL       = new URL( urlRoot + enableWatchdog );
            DISABLE_WD_URL      = new URL( urlRoot + disableWatchdog );
            WD_STATUS_URL       = new URL( urlRoot + watchdogStatus );  // JSON whether it's enabled and has expired
            RESET_WD_URL        = new URL( urlRoot + resetWatchdog );   // HTTP to feed the timer
            READ_ANALOG_URL     = new URL( urlRoot + readAnalog );
            GET_INFO_URL        = new URL( urlRoot + getInfo );
    
            DEFAULT_TEMP_F = new JSONObject( "{\"TempF\":-999}" );
    
        }
        catch( MalformedURLException m) {
            Log.d( "Constants", "initializer threw MalformedURLException: " + m.getMessage() );
        }
        catch( JSONException j) {
            Log.d( "Constants", "initializer threw JSONException: " + j.getMessage() );
        }
        
    }
    
    
    static final int TEMP_UPDATE_SECONDS = 5;       // seconds between temp polling (can be different from PID period)
    static final int HISTORY_MINUTES = 60;          // how many minutes of temp history to buffer
    final static int HISTORY_BUFFER_SIZE = 60/TEMP_UPDATE_SECONDS * HISTORY_MINUTES;  // should == 720
    
    static final int WATCHDOG_CHECK_SECONDS = 60;   // time between checks of enabled/expired JSON, if enabled
    static final int WATCHDOG_RESET_SECONDS = 40;
    static final int FAN_CONTROL_TIMEOUT_SECS = 5;  // can't wait around for fan commands TODO: was 2 too short?
    static final int ANALOG_IN_UPDATE_SECS = 1;
    
    static final String SOFTWARE_VERSION = "0.8";
    static final String HARDWARE_VERSION = "0.8";
    
    // New, for PIDState constructor
    static final float DEFAULT_SETPOINT = 250f;              // TODO: consider °F or °C? Set to current temp value on startup?
    static final float DEFAULT_GAIN = 1f;                    // TODO: value? (started at 2)
    static final float DEFAULT_PROP_COEFF = 16f;             // TODO: value? from AppleScript
    static final float DEFAULT_INT_COEFF = 2f;               // TODO: value?
    static final float DEFAULT_DIFF_COEFF = 3f;              // TODO: value?
    static final float DEFAULT_PERIOD_SECS = 10f;            // TODO: value?
    static final float DEFAULT_MIN_OUT_PCT = 5f;             // TODO: value?
    static final float DEFAULT_DUTY_CYCLE_PCT = 0f;          // TODO: value?
    static final boolean DEFAULT_PID_ENABLE_STATE = false;   // TODO: value?
    
    static final long WATCHDOG_ENABLE_UPPER_HALF = 0x1000;
    static final long WATCHDOG_FEED_UPPER_HALF   = 0x2000;
    static final long TEMP_GET_UPPER_HALF        = 0x3000;
    //static final long TEMP_UPDATE_UPPER_HALF     = 0x4000;
    static final long FAN_CONTROL_UPPER_HALF     = 0x5000;
    static final long WATCHDOG_STATUS_UPPER_HALF = 0x6000;
    static final long ANALOG_READ_UPPER_HALF     = 0x7000;
    
    // action names that describe tasks that the JobIntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    static final String ACTION_START_TEMP_UPDATES = "net.grlewis.packagename.action.START_TEMP_UPDATES";
    static final String ACTION_STOP_TEMP_UPDATES = "net.grlewis.packagename.action.STOP_TEMP_UPDATES";
    static final String ACTION_START_PID = "net.grlewis.packagename.action.START_PID";
    static final String ACTION_STOP_PID = "net.grlewis.packagename.action.STOP_PID";
    static final String ACTION_START_BG_SERVICE = "net.grlewis.packagename.action.START_BG_SERVICE";
    static final String ACTION_STOP_BG_SERVICE = "net.grlewis.packagename.action.STOP_BG_SERVICE";
    
    static final int SERVICE_NOTIFICATION_ID = 8266;  // unique id for ongoing background notification
    
    
    
    
}
