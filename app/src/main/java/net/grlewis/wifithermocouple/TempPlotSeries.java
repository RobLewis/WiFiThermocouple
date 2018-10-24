package net.grlewis.wifithermocouple;

import android.util.Pair;

import com.androidplot.xy.XYSeries;

import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;

import static net.grlewis.wifithermocouple.Constants.HISTORY_BUFFER_SIZE;
import static net.grlewis.wifithermocouple.Constants.TEMP_UPDATE_SECONDS;

// implementation of XYSeries interface for AndroidPlot

class TempPlotSeries implements XYSeries {
    
    Pair<Date, Float>[] plotArray;
    
    void updatePlotData( ArrayBlockingQueue<Pair<Date, Float>> dataQueue ) {
        plotArray = dataQueue.toArray( new Pair[0] );
    }
    
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
}
