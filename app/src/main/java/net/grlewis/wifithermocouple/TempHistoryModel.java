package net.grlewis.wifithermocouple;

// attempt to use a ViewModel to persist temperature history data

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.util.Pair;

import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;

import static net.grlewis.wifithermocouple.Constants.HISTORY_MINUTES;
import static net.grlewis.wifithermocouple.Constants.TEMP_UPDATE_SECONDS;

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
    
    private ArrayBlockingQueue<Pair<Date,Float>> timestampedHistory;
    
    public ArrayBlockingQueue<Pair<Date,Float>> getTimestampedHistory() {
        if( timestampedHistory == null ) {
            timestampedHistory = new ArrayBlockingQueue<>( HISTORY_BUFFER_SIZE );
        }
        return timestampedHistory;
    }
    
    // Adds a value to the history queue, returning the updated number of values in the queue
    public int addHistoryValue( Pair<Date,Float> value ) {
        if( timestampedHistory == null ) {
            timestampedHistory = new ArrayBlockingQueue<>( HISTORY_BUFFER_SIZE );
        }
        while( timestampedHistory.remainingCapacity() < 1 ) {  // if queue is full
            timestampedHistory.poll();                         // discard items until it isn't
        }
        timestampedHistory.add( value );
        return timestampedHistory.size();
    }
    
    
    
}
