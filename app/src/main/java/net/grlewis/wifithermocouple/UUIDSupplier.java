package net.grlewis.wifithermocouple;

import android.util.Log;

import java.util.Iterator;
import java.util.UUID;

import static net.grlewis.wifithermocouple.Constants.DEBUG;

class UUIDSupplier {
    
    private long upperBits;
    private long lowerBits;
    
    Iterator<UUID> iterator;
    
    
    class UUIDIterator implements Iterator<UUID> {
    
        @Override
        public boolean hasNext( ) {
            return true;  // unlikely to run out of longs
        }
    
        @Override
        public UUID next( ) {
            UUID nextUUID = new UUID( upperBits, ++lowerBits );
            if( DEBUG ) Log.d( "UUIDSupplier", "next UUID: " + nextUUID.toString() );
            return nextUUID;
        }
    }
    
    
    // constructor
    UUIDSupplier( long upperHalf ) {
        upperBits = upperHalf;
        lowerBits = 0L;
        iterator = new UUIDIterator();
    }
    
}
