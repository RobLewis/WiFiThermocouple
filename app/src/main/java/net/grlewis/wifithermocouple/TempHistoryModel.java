package net.grlewis.wifithermocouple;

// attempt to use a ViewModel to persist temperature history data

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.util.Pair;

import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;

import static net.grlewis.wifithermocouple.Constants.DEFAULT_DUTY_CYCLE_PCT;
import static net.grlewis.wifithermocouple.Constants.DEFAULT_PID_ENABLE_STATE;
import static net.grlewis.wifithermocouple.Constants.HISTORY_MINUTES;
import static net.grlewis.wifithermocouple.Constants.TEMP_UPDATE_SECONDS;

/*
 *
 * Apparently it's called a ViewModel because its purpose is to hold the state of the UI
 *
 * UI elements we care about:
 *     --setpoint
 *     --current temp
 *     --PID enable state (maybe display "disabled" where setpoint would be displayed?
 *     --heater on/off
 *     --current % duty cycle
 *     --clamped?
 *     --temp history
 *
 */

public class TempHistoryModel extends ViewModel {
    
    private final static int HISTORY_BUFFER_SIZE = 60/TEMP_UPDATE_SECONDS * HISTORY_MINUTES;  // should == 720

    
/*
    private MutableLiveData<ArrayBlockingQueue<Pair<Date,Float>>> timestampedHistory;
    public LiveData<ArrayBlockingQueue<Pair<Date,Float>>> getTempHistory() {
        if( timestampedHistory == null ) {
            timestampedHistory = new MutableLiveData<>();
        }
        return timestampedHistory;
    }
*/
    
    
    class UIState {  // the observed object(?)
        
        private Float setpoint;
        private Float currentTemp;
        private Boolean pidEnabled;
        private Boolean heaterOn;
        private Float dutyCyclePct;
        private Boolean clamped;
        private ArrayBlockingQueue<Pair<Date, Float>> timestampedHistory;
        
        UIState() {
            setpoint = Constants.DEFAULT_SETPOINT;
            currentTemp = 0f;
            pidEnabled = DEFAULT_PID_ENABLE_STATE;  // normally false
            heaterOn = false;  // seems pretty safe
            dutyCyclePct = DEFAULT_DUTY_CYCLE_PCT;  // normally 0
            clamped = false;
            timestampedHistory = new ArrayBlockingQueue<>( HISTORY_BUFFER_SIZE );
        }
        
    }
    
    
    // instance members
    private UIState currentUIState;
    private MutableLiveData<UIState> uiLiveData;
    
    
    // Model constructor
    public TempHistoryModel() {
        
        if( currentUIState == null ) currentUIState = new UIState();
        uiLiveData = new MutableLiveData<>();
    }
    
    
    // to post updated UI data, call uiLiveData.setValue( currentUIState )
    // evidently it will be propagated to observers
    
    
    
    public ArrayBlockingQueue<Pair<Date,Float>> getTimestampedHistory() {
        if( currentUIState.timestampedHistory == null ) {
            currentUIState.timestampedHistory = new ArrayBlockingQueue<>( HISTORY_BUFFER_SIZE );
        }
        return currentUIState.timestampedHistory;
    }
    
    
    
    
    
    // setters that cause a live UI update (we hope)
    public void updateUISetpoint( Float setpoint ) {
        currentUIState.setpoint = setpoint;
        uiLiveData.setValue( currentUIState );            // TODO: trigger the live update(?)
    }
    public void updateUITemp( Float temp ) {
        currentUIState.currentTemp = temp;
        uiLiveData.setValue( currentUIState );            // TODO: trigger the live update(?)
    }
    public void updateUIPIDEnabled( Boolean enabled ) {
        currentUIState.pidEnabled = enabled;
        uiLiveData.setValue( currentUIState );            // TODO: trigger the live update(?)
    }
    public void updateUIHeaterOn( Boolean heaterState ) {
        currentUIState.heaterOn = heaterState;
        uiLiveData.setValue( currentUIState );            // TODO: trigger the live update(?)
    }
    public void updateUIDutyCyclePct( Float percent ) {
        currentUIState.dutyCyclePct = percent;
        uiLiveData.setValue( currentUIState );            // TODO: trigger the live update(?)
    }
    public void updateUIClamped( Boolean clamped ) {
        currentUIState.clamped = clamped;
        uiLiveData.setValue( currentUIState );            // TODO: trigger the live update(?)
    }
    // Adds a value to the history queue, returning the updated number of values in the queue
    public int addHistoryValue( Pair<Date,Float> value ) {
        if( currentUIState.timestampedHistory == null ) {
            currentUIState.timestampedHistory = new ArrayBlockingQueue<>( HISTORY_BUFFER_SIZE );
        }
        while( currentUIState.timestampedHistory.remainingCapacity() < 1 ) {  // if queue is full
            currentUIState.timestampedHistory.poll();                         // discard items until it isn't
        }
        currentUIState.timestampedHistory.add( value );
        uiLiveData.setValue( currentUIState );            // TODO: trigger the live update(?)
        return currentUIState.timestampedHistory.size();  // TODO: probably let LiveData handle updating?
    }
    
    
    
    
    // called after the owning activity calls .finish() and is destroyed
    @Override
    protected void onCleared( ) {
        super.onCleared( );
        currentUIState = null;
        uiLiveData = null;
    }
}
