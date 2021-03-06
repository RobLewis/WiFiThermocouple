package net.grlewis.wifithermocouple;

import android.arch.core.util.Function;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Cancellable;
import okhttp3.Headers;
import okhttp3.ResponseBody;
import java.io.IOException;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static net.grlewis.wifithermocouple.Constants.DEBUG;

/**
 * Created on 2018-03-27.
 *
 * Modified from AsyncDownloader
 *
 *
 * myJsonGetter = new AsyncHTTPRequester( URL jsonURL, OkHttpClient httpClient );
 * myJsonGetter.get().subscribe( theJSON -> { handle it }, theThrowable -> { handle failure } );
 *
 * Since you're only supposed to have 1 instance of OkHttpClient, you pass it in
 * however if you pass null for the client, we create one
 *
 * Changed to make setURL return the Getter so you can chain with .request()
 *
 * 2018-08: updating to RxJava2
 *
 * * 2018-09-28: added request UUID to error messages
 *               pass or create new request UUID when setting a new URL
 *
 *                * 2018-10-12: rethink constructor parameters, esp. with regard to UUIDs
 *  *             desired options
 *  *                 --a single UUID for all requests, either random or supplied
 *  *                 --a new UUID for each .subscribe()
 *  *                     --supplied (can this be done?)
 *  *                     --random
 *  *                     --generated by a UUID Supplier
 *  *             Constructor argument overloads:
 *  *                 --( URL )  send all requests with the same random UUID and default client
 *  *                 --( URL, client )  send all requests with the same random UUID and supplied client
 *  *                 --( URL, client, UUID ) send all requests with the same supplied UUID and supplied client
 *  *                 --( URL, client, Function<URL, UUID> uuidSupplier ) called before each request, passing URL & returning a UUID
 *  *
 *  *             (also have setURL and setUUID methods)
 *
 *  2018-10-19: back-porting changes/fixes from HTTP requester
 */

public class AsyncJSONGetter {  // updating from GlucometerApp version to use RxJava2
    
    private static final String TAG = AsyncJSONGetter.class.getSimpleName( );
    
    private final Single<JSONObject> getJSON;
    private URL theURL;  // mutable
    private final OkHttpClient client;  // created if not passed
    private UUID requestUUID;     // to identify the request that produced result (copied from HTTP Requester)
    private int successes;
    private int failures;
    
    private Call savedCall;       // new stuff to implement Disposable
    private boolean disposed;
    private Disposable disposable;
    private Cancellable cancellable;  // NEW
    private JSONGetterOnSubscribe requesterOnSubscribe;  // NEW
    
    private Function<URL,UUID> uuidSupplier;  // non-null means use it
    
    class JSONGetterOnSubscribe implements SingleOnSubscribe<JSONObject> {
        
        // conversion to RxJava2 says must implement subscribe(SingleEmitter<T>)
        //
        //    SingleObserver is the subscriber(s). 3 methods: onError, onSubscribe(returns Disposable), onSuccess
        //    SingleEmitter basically extends SingleObserver (AKA Subscriber) by allowing registration of a Disposable or Cancellable with it
        //        (while "hiding" SingleObserver's onSubscribe(Disposable d), which you aren't supposed to interact with????)
        //    SingleOnSubscribe is a functional interface with a single subscribe() method that receives a SingleEmitter
        //
        // SEE https://github.com/ReactiveX/RxJava/issues/4787
        
        @Override  // define what happens when you subscribe to the Single being created, supplying a SingleEmitter
        // Note that Emitter adds setCancellable(), setDisposable(), isDisposed() and tryOnError() to the basic Observer
        // subscribe() returns void, not a Disposable
        public void subscribe( final SingleEmitter<JSONObject> emitter ) throws Exception {
    
            if( DEBUG ) Log.d( TAG, "Is this a subscribe retry? emitter class is " + emitter.getClass().getName() );  // FIXME: remove
            
            if( emitter == null ) throw new NullPointerException( TAG + "Can't subscribe with a null SingleEmitter" );
            if( uuidSupplier != null ) {
                requestUUID = uuidSupplier.apply( theURL ); // generate a custom UUID if available
            } else {    // UUIDSupplier is null
                if( DEBUG ) Log.d( TAG, "JSONGetter subscribing with null uuidSupplier; UUID is " + requestUUID.toString() );
            }
            
            Request request = new Request.Builder( )
                    .url( theURL )
                    .tag( UUID.class, requestUUID )
                    .build( );
            disposed = false;
            savedCall = client.newCall( request );
            //emitter.setDisposable( disposable );       // is there a default implementation if you use lambdas?
            emitter.setCancellable( cancellable );       // NEW
            if( DEBUG ) Log.d( TAG, "About to enqueue JSON request UUID " + requestUUID.toString()
            + " from Supplier " + ((SerialUUIDSupplier)uuidSupplier).getName() );  // FIXME: remove cast when debugged
            
            savedCall.enqueue( new Callback( ) {
                // Note callback is made after the response headers are ready. Reading the response body may still block.
                // Note a Single can only have onSuccess() and onError() outcomes. Both should dispose the subscription, right?
                // This should mean our Disposable is called on every request, no?
                // Perhaps only our Disposable should set the disposed flag (it can only be disposed by subscriber, right?)
                
                @Override
                // Called when the request could not be executed due to cancellation, a connectivity problem or timeout.
                // Because networks can fail during an exchange, it is possible that the remote server accepted the request before the failure.
                // does emitter call the Disposabale we gave it here? Seems likely
                public void onFailure( @NonNull Call call, @NonNull IOException e ) {
                    failures++;
                    // tryOnError() returns true if the error was signaled,
                    // false if sequence has been cancelled by downstream, or otherwise terminated
                    // (meaning that the downstream is not able to accept further events)
                    if( !emitter.tryOnError( new IOException( TAG + ": onFailure Callback while starting JSON request with UUID: "
                            + requestUUID.toString() + ": " + e.getMessage(), e ) ) ) {
                        if( DEBUG ) Log.d( TAG, "JSON request UUID " + requestUUID.toString()
                                + " canceled before failure received" );
                    } else {  // Throwable was emitted because sequence still alive
                        //disposed = true;  // TODO: right? Error disposes?
                        if( DEBUG ) Log.d( TAG, "onFailure callback for request UUID "
                                + requestUUID.toString() + "signaled IOException: " + e.getMessage() );
                    }
                }
                
                @Override
                // called when the server returns a (presumably well-formed) response (which may not be a success)
                public void onResponse( @NonNull Call call, @NonNull Response response ) {
                    if( !emitter.isDisposed() ) {  // perhaps the request was canceled before response received?
                        if ( !response.isSuccessful( ) ) {
                            emitter.onError( new IOException( TAG + ": JSON request UUID " + requestUUID.toString()
                                    + " failed with HTTP status: " + response.message( ) ) );
                            if( DEBUG) Log.d( TAG, "JSON request UUID " + requestUUID.toString()
                                    + " failed with HTTP status: " + response.message( ) );
                            //disposed = true;  // TODO: right? maybe not--leave it to our Disposable?
                            failures++;
                        } else {  // successful response
                            try {
                                ResponseBody responseBody = response.body( );
                                Headers headers = response.headers( );
                                
                                String contentType;
                                JSONObject returnedJSON = null;
                                
                                if ( (contentType = headers.get( "Content-type" )).equalsIgnoreCase( "application/json" ) ) {
                                    try /*( response )*/ {  // FIXME: apparent bug, "try with resources not supported"
                                        returnedJSON = new JSONObject( responseBody.string( ) );
                                        returnedJSON.put( "RequestUUID", requestUUID.toString() );  // add the request UUID
                                    } catch ( JSONException j ) {
                                        emitter.onError( new JSONException( TAG + ": Invalid JSON returned from fetch (request UUID "
                                                + requestUUID.toString() + "): " + j.getMessage( ) ) );
                                        if( DEBUG ) Log.d( TAG, "Invalid JSON returned from fetch (request UUID "
                                                + requestUUID.toString() + "): " + j.getMessage( ) );
                                    }
                                    emitter.onSuccess( returnedJSON );  // Pay dirt!
                                    successes++;
                                } else {  // content type not JSON (or missing header, I guess)
                                    emitter.onError( new JSONException(
                                            TAG + ": Returned content type header (request UUID "
                                                    + requestUUID.toString() + ") is not JSON but " + contentType ) );
                                    if( DEBUG ) Log.d( TAG, "Returned content type header (request UUID "
                                            + requestUUID.toString() + ") is not JSON  but " + contentType );
                                    failures++;
                                }
                            } catch ( IOException e ) {
                                emitter.onError( new IOException( TAG + ": Error fetching JSON from URL "
                                        + theURL.toString( ) + " (request UUID: " + requestUUID.toString() + ") ", e ) );
                                if( DEBUG ) Log.d( TAG, "Error fetching JSON from URL " + theURL.toString( )
                                        + " (request UUID: " + requestUUID.toString() + ") ", e );
                                failures++;
                            }
                        }  // else successful response
                    } else {  // emitter has been disposed (TODO: not a success or a failure?)
                        if( DEBUG ) Log.d( TAG, "JSON request UUID " + requestUUID.toString()
                                + " subscription disposed before response received?  Emitter disposed? " + emitter.isDisposed() );
                    }
                    response.close();  // always do this
                }  // onResponse()
            } );  // Callback & enqueue()
            
            
        }  // subscribe
        
        
/*        // invoked when Single.execute() is called (i.e., when it is subscribed to)
        // passed the Subscriber to the Single
        // here it should return a JSON object by calling the Subscriber's onSuccess( JSONObject )
        @Override
        public void call( SingleSubscriber<? super JSONObject> singleSubscriber ) {
            
            Request request = new Request.Builder( )
                    .url( theURL )
                    .build( );
            
            client.newCall( request ).enqueue( new Callback( ) {
                // Note callback is made after the response headers are ready. Reading the response body may still block.
                
                @Override
                public void onFailure( @NonNull Call call, @NonNull IOException e ) {
                    singleSubscriber.onError( new IOException( TAG + ": onFailure Callback while starting JSON fetch: ", e ) );
                }
                
                @Override
                public void onResponse( @NonNull Call call, @NonNull Response response ) *//*throws IOException*//* {
                    if ( !response.isSuccessful( ) ) {
                        singleSubscriber.onError( new IOException( TAG + ": JSON Fetch failed with HTTP status: " + response.message( ) ) );
                    } else {  // successful response
                        try {
                            
                            ResponseBody responseBody = response.body( );
                            Headers headers = response.headers( );
                            response.close( );
                            
                            String contentType;
                            JSONObject returnedJSON = null;
                            
                            if ( (contentType = headers.request( "Content-type" )).equalsIgnoreCase( "application/json" ) ) {
                                try {
                                    returnedJSON = new JSONObject( responseBody.string( ) );
                                } catch ( JSONException j ) {
                                    singleSubscriber.onError( new JSONException( TAG + ": Invalid JSON returned from fetch: " + j.getMessage( ) ) );
                                }
                                singleSubscriber.onSuccess( returnedJSON );
                            } else {  // content type not JSON
                                singleSubscriber.onError( new JSONException(
                                        TAG + ": Returned content type header is not JSON but " + contentType ) );
                            }
                            
                        } catch ( IOException e ) {
                            singleSubscriber.onError( new IOException( TAG + ": Error fetching JSON "
                                    + "from URL " + theURL.toString( ), e ) );
//                        } catch( NullPointerException n ) {  // .source() can apparently return null
//                            singleSubscriber.onError( new NullPointerException( TAG + ": Call to .source() returned null" ) );
                        }
                        
                    }  // else successful response
                }  // onResponse()
            } );  // Callback & enqueue()
        }  // call()*/
        
        
    }  // internal class JSONGetterOnSubscribe
    
    
    // constructors
    
    public AsyncJSONGetter( URL jsonURL, OkHttpClient httpClient, UUID requestID ) {
        requesterOnSubscribe = new JSONGetterOnSubscribe();  // NEW
        getJSON = Single.create( requesterOnSubscribe );
        theURL = jsonURL;
        requestUUID = requestID;
        uuidSupplier = null;
        successes = failures = 0;
        client = httpClient == null?            // and passed httpClient is also null
                new OkHttpClient( ) :           // create a new default client; if passed httpClient is not null
                httpClient;                     // use it
        disposable = new Disposable() {  // TODO: eliminate (?)
            @Override
            public void dispose() {
                if( !savedCall.isExecuted() ) savedCall.cancel();  // TODO: this seems to have fixed crash (but why is .dispose() getting called twice for some requests?
                disposed = true;
                if( DEBUG ) Log.d( TAG, ".dispose() called for JSON request ID " + requestUUID.toString()
                        + " to URL " + theURL.toString() + "; savedCall executed? " + savedCall.isExecuted() );
            }
            @Override
            public boolean isDisposed() {
                if( DEBUG ) Log.d( TAG, ".isDisposed() returning " + disposed + " for JSON request ID " + requestUUID.toString() );
                return disposed;
            }
        };  // disposable
        cancellable = new Cancellable( ) {  // NEW try this?
            @Override
            public void cancel( ) throws Exception {
                if( !savedCall.isExecuted() ) savedCall.cancel();
                if( DEBUG ) Log.d( TAG, ".cancel() called for JSON request ID " + requestUUID.toString()
                        + " to URL " + theURL.toString() + "; savedCall executed? " + savedCall.isExecuted() );  // FIXME:
            }
        };  // cancellable
    }  // primary constructor
    
    
    // NEW: constructor that specifies that request IDs should be generated on each .subscribe() with supplied function
    public AsyncJSONGetter( URL jsonURL, OkHttpClient httpClient, @NonNull Function<URL, UUID> supplier ) {
        this( jsonURL, httpClient, new UUID( 0xeeee, 0xeeee ) );   // makes a UUID which is not (supposed to be) used
        if( supplier == null ) {
            throw new IllegalArgumentException( "****** Goddammit, the UUID Supplier is null! ******" );
        } else {
            if( DEBUG ) Log.d( TAG, "Constructor passed supplier " + ((SerialUUIDSupplier)supplier).getName() ); // TODO: elim cast
        }
        Log.d( TAG, "UUID supplier class is " + supplier.getClass().getName() );
        uuidSupplier = supplier;       // non-null means use it
    }
    
    
    // constructor that supplies a random request UUID if we don't
    public AsyncJSONGetter( URL jsonURL, OkHttpClient httpClient ) {
        this( jsonURL, httpClient, UUID.randomUUID() /*new UUID(0xcccc, 0xdddd )*/ );  // diagnostic UUID
    }
    
    /*
    Alternate with lambdas etc. might work:
    getJSON = Single.create( emitter -> { <code that calls emitter.onSuccess() and emitter.onError()> } );
     */
    
    
    // after getting the JSON Getter instance, subscribe to this to do the request. It emits JSON
    public Single<JSONObject> get( ) {
        return getJSON;  // OkHttp3 manages its own threads, I think
    }
    
    
    // change source URL: returns the getter so you can chain with .get()
    public AsyncJSONGetter setURL( URL newURL ) {
        theURL = newURL;
        return this;
    }
    // set a new Request UUID if desired (as when changing URL)
    public AsyncJSONGetter setRequestUUID( UUID requestID ) {
        requestUUID = requestID;
        return this;
    }
    
    public UUID getRequestUUID( ) { return requestUUID; }
    public URL getURL( ) { return theURL; }
    
    public int getSuccessCount() { return successes; }
    public int getFailureCount() { return failures; }
    
}

