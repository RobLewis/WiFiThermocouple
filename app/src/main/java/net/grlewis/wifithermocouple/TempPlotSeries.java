package net.grlewis.wifithermocouple;

import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import com.androidplot.Plot;
import com.androidplot.PlotListener;
import com.androidplot.xy.XYSeries;

import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.grlewis.wifithermocouple.Constants.DEBUG;
import static net.grlewis.wifithermocouple.Constants.HISTORY_BUFFER_SIZE;
import static net.grlewis.wifithermocouple.Constants.TEMP_UPDATE_SECONDS;

// implementation of XYSeries and PlotListener interfaces for AndroidPlot

class TempPlotSeries implements XYSeries, PlotListener {
    
    private final static String TAG = TempPlotSeries.class.getSimpleName();
    
    private Pair<Date, Float>[] plotArray;
    
    public TempPlotSeries( ) {  // constructor may solve crash of null array
        plotArray = new Pair[0];
    }
    
    void updatePlotData( ArrayBlockingQueue<Pair<Date, Float>> dataQueue ) throws InterruptedException {
        synchronized ( this ) {
            if( DEBUG ) Log.d( TAG, "updatePlotData entered with dataQueue size " + dataQueue.size() );
            //wait();       // don't update data until we're notified that current plot is done (& we can get lock) FIXME: elimination of wait/notify fixes app freeze
            plotArray = dataQueue.toArray( new Pair[0] );
            if( DEBUG ) Log.d( TAG, "updatePlotData run with " + plotArray.length + " data points" );
            //notifyAll();  // release lock & let other threads know they can continue
        }
    }
    
    // XYSeries implementation
    // note it's only the draw routines that will call these methods, and drawing is locked out while updating data
    // so shouldn't need any extra synchronization
    
    @Override
    public int size( ) {
        return plotArray.length;
    }
    
    @Override
    public Number getX( int index ) {
        return (index - HISTORY_BUFFER_SIZE) / (60/TEMP_UPDATE_SECONDS);  // e.g., -60 minutes at left edge of graph, -1/12 min at right
    }
    
    @Override
    public Number getY( int index ) {
        return plotArray[index].second;  // the temp value
    }
    
    @Override
    public String getTitle( ) {
        return "Temp History";
    }
    
    
    // PlotListener Implementation (supposed to write-lock data changes before redraw and release after)
    // it appears these must be called on background thread because doesn't freeze app?
    // eliminating sync/wait/notifyAll doesn't appear to help: still no drawing
    @Override
    public void onBeforeDraw( Plot source, Canvas canvas ) {
        synchronized ( this ) {
//            try {
//                wait();  // wait for data updating to finish if it's in progress on another thread
//            } catch ( InterruptedException e ) {
//                // unlikely to be interrupted?
//            }
        }
    }
    // between these 2 calls the plot is redrawn
    @Override
    public void onAfterDraw( Plot source, Canvas canvas ) {
        synchronized ( this ) {
//            notifyAll( );  // plot done, OK to update data
        }
    }
}
