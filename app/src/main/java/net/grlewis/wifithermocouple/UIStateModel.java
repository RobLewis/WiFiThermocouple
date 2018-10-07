package net.grlewis.wifithermocouple;

// attempt to use a ViewModel to persist temperature history data



/*
Analogy with docs at <https://developer.android.com/topic/libraries/architecture/livedata#java>

NameViewModel extends ViewModel          =         UIStateModel extends ViewModel
MutableLiveData<String> mCurrentName     =         MutableLiveData<UIState> uiLiveData
<String>                                 =         <UIState>
MutableLiveData<String> getCurrentName() =         MutableLiveData<UIState> getCurrentUIState()
    (returns (if null, new) mCurrentName)              (returns uiLiveData)


NameViewModel mModel                     =          UIStateModel uiStateModel
mModel = ViewModelProviders.of(this).get(NameViewModel.class)
                                         =          uiStateModel = ViewModelProviders.of(this).get( UIStateModel.class )
nameObserver                             =          uiStateObserver
Observer<String> nameObserver = new Observer<String> = Observer<UIStateModel.UIState> uiStateObserver = new Observer<UIStateModel.UIState>
onChanged(@Nullable final String newName)=          onChanged( @Nullable UIStateModel.UIState uiState )
mModel.getCurrentName().observe(this, nameObserver) = uiStateModel.getCurrentUIState().observe(this, uiStateObserver )

String anotherName = "John Doe"          =          (call a Setter in UIState)
mModel.getCurrentName().postValue(anotherName) =     uiStateModel.getCurrentUIState().postValue( uiState instance )

*/





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

public class UIStateModel extends ViewModel {
    
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
    
        // setters that cause a live UI update (we hope)
        public void updateUISetpoint( Float setpoint ) {
            this.setpoint = setpoint;
            uiLiveData.postValue( this );            // TODO: trigger the live update(?)
        }
    
        public void updateUITemp( Float temp ) {
            this.currentTemp = temp;
            uiLiveData.postValue( this );            // TODO: trigger the live update(?)
        }
        public Float getUITempUpdate() { return currentUIState.currentTemp; }
    
        public void updateUIPIDEnabled( Boolean enabled ) {
            currentUIState.pidEnabled = enabled;
            uiLiveData.postValue( currentUIState );            // TODO: trigger the live update(?)
        }
        public void updateUIHeaterOn( Boolean heaterState ) {
            currentUIState.heaterOn = heaterState;
            uiLiveData.postValue( currentUIState );            // TODO: trigger the live update(?)
        }
        public void updateUIDutyCyclePct( Float percent ) {
            currentUIState.dutyCyclePct = percent;
            uiLiveData.postValue( currentUIState );            // TODO: trigger the live update(?)
        }
        public void updateUIClamped( Boolean clamped ) {
            currentUIState.clamped = clamped;
            uiLiveData.postValue( currentUIState );            // TODO: trigger the live update(?)
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
            uiLiveData.postValue( currentUIState );            // TODO: trigger the live update(?)
            return currentUIState.timestampedHistory.size();  // TODO: probably let LiveData handle updating?
        }
        
    }
    
    
    // instance members
    private UIState currentUIState;
    private MutableLiveData<UIState> uiLiveData;
    
    
    // Model constructor
    public UIStateModel() {
        if( currentUIState == null ) currentUIState = new UIState();
        uiLiveData = new MutableLiveData<>();
    }
    
    
    
    public ArrayBlockingQueue<Pair<Date,Float>> getTimestampedHistory() {
        if( currentUIState.timestampedHistory == null ) {
            currentUIState.timestampedHistory = new ArrayBlockingQueue<>( HISTORY_BUFFER_SIZE );
        }
        return currentUIState.timestampedHistory;
    }
    
    
    
    // to post updated UI data from an Activity, call uiLiveData.postValue( currentUIState )
    // evidently it will be propagated to observers
    // TODO: docs say that normally the model keeps mutable data but publishes it as immutable
    public UIState getUIStateObject() { return currentUIState; }
    MutableLiveData<UIState> getCurrentUIState() { return uiLiveData; }  // TODO: OK?
    
    
    // called after the owning activity calls .finish() and is destroyed
    @Override
    protected void onCleared( ) {
        super.onCleared( );
        currentUIState = null;
        uiLiveData = null;
    }
}
