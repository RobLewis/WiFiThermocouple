package net.grlewis.wifithermocouple;

import android.arch.core.util.Function;
import android.util.Log;

import java.net.URL;
import java.util.Iterator;
import java.util.UUID;

import static net.grlewis.wifithermocouple.Constants.DEBUG;

// class that implements the Function that, given a URL for a request, returns a UUID to tag it with
// here we just return a different set of serialized values for JSON and HTTP requests
class SerialUUIDSupplier implements Function<URL,UUID> {  // (this Function is from android.arch.core.util, not the usual)
    
    private long upperBits;       // upper 64 bits of the 128-bit UUID
    private long lowerBits;       // lower 64, which we increment to produce serialized values
    private boolean initialized;  // have to give it a value for upper bits
    
    private Iterator<UUID> iterator;
    
    
    class UUIDIterator implements Iterator<UUID> {
        
        @Override
        public boolean hasNext( ) { return initialized; }  // unlikely to run out of longs
        
        @Override
        public UUID next( ) {
            if ( initialized ) {
                UUID nextUUID = new UUID( upperBits, ++lowerBits );
                if ( DEBUG ) Log.d( "SerialUUIDSupplier", "next UUID: " + nextUUID.toString( ) );
                return nextUUID;
            } else
                throw new IllegalStateException( "Can't use a SerialUUIDSupplier until it is initialized!" );
        }
    }  // internal class UUIDIterator
    
    
    // functional interface
    public synchronized UUID apply( URL theURL ) {  // implement the Function interface
        return this.iterator.next( );
    }
    
    
    // constructor
    SerialUUIDSupplier( long upperHalf ) {
        upperBits = upperHalf;
        lowerBits = 0L;
        iterator = new UUIDIterator( );
        initialized = true;
    }
    
    // get the last UUID returned
    public synchronized UUID getLastSerializedUUID( ) {
        return new UUID( upperBits, lowerBits );
    }
    
    // return the iterator if you want to use it directly
    public Iterator<UUID> getSerialUUIDIterator( ) { return iterator; }
    
}

