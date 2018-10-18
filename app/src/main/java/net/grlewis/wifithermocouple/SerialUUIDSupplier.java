package net.grlewis.wifithermocouple;

import android.arch.core.util.Function;
import android.util.Log;

import java.net.URL;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.UUID;

import static net.grlewis.wifithermocouple.Constants.DEBUG;

// class that implements the Function that, given a URL for a request, returns a UUID to tag it with
// here we ignore the URL and just return a different set of serialized values for different instances
class SerialUUIDSupplier implements Function<URL,UUID> {  // (this Function is from android.arch.core.util, not the usual)
    
    private static final String TAG = SerialUUIDSupplier.class.getSimpleName();
    private static int instanceNo = 1;  // for creating default names
    
    private final long upperBits;       // upper 64 bits of the 128-bit UUID that identifies the set of values
    private long lowerBits;             // lower 64, which we increment to produce serialized values
    private final boolean initialized;  // have to give it a value for upper bits ("blank final" booleans are false until set)
    private final String name;          // you can give a custom name to ihe instance when creating it
    
    private Iterator<UUID> iterator;
    
    class UUIDIterator implements Iterator<UUID> {
        
        @Override
        public boolean hasNext( ) { return initialized; }  // unlikely to run out of longs
        
        @Override
        public UUID next( ) {
            if ( initialized ) {
                UUID nextUUID = new UUID( upperBits, ++lowerBits );
                if ( DEBUG ) Log.d( TAG, SerialUUIDSupplier.this.getName() + " next UUID: " + nextUUID.toString( ) );
                return nextUUID;
            } else
                throw new NoSuchElementException( "Can't use a SerialUUIDSupplier until it is initialized!" );
        }
    }  // internal class UUIDIterator
    
    
    // functional interface
    public synchronized UUID apply( URL theURL ) {  // implement the Function interface
        return this.iterator.next( );
    }
    
    // constructor that allows passing a name
    SerialUUIDSupplier( long upperHalf, String supplierName ) {
        upperBits = upperHalf;
        lowerBits = 0L;
        iterator = new UUIDIterator( );
        name = supplierName;
        instanceNo++;            // bump instance no. even if not using it to generate names
        initialized = true;
        if( DEBUG ) Log.d( TAG, "created " + supplierName );
    }
    
    // constructor that uses default names of "SerialUUIDSupplier 1" etc.
    SerialUUIDSupplier( long upperHalf ) {
        this( upperHalf, TAG + " " + instanceNo );
    }
    
    // get the last UUID returned
    public synchronized UUID getLastSerializedUUID( ) {
        return new UUID( upperBits, lowerBits );
    }
    
    // get the name of this Supplier
    public String getName() { return name; }
    
    // return the iterator if you want to use it directly
    public Iterator<UUID> getSerialUUIDIterator( ) { return iterator; }
    
}

