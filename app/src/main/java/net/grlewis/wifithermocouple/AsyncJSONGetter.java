package net.grlewis.wifithermocouple;

import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
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
 */

public class AsyncJSONGetter {  // updating from GlucometerApp version to use RxJava2
    
    private static final String TAG = AsyncHTTPRequester.class.getSimpleName( );
    
    private final Single<JSONObject> getJSON;
    private URL sourceURL;  // mutable
    private OkHttpClient client;  // created if not passed
    private UUID requestUUID;     // to identify the request that produced result (copied from HTTP Requester)
    private int successes;
    private int failures;
    
    private Call savedCall;       // new stuff to implement Disposable
    private boolean disposed;
    private Disposable disposable;
    
    private boolean newIdOnSubscribe = false;  // new to auto-generate ID on each .subscribe
    
    
    class JSONGetterOnSubscribe implements SingleOnSubscribe<JSONObject> {
        
        // conversion to RxJava2 says must implement subscribe(SingleEmitter<T>)
        //
        //    SingleObserver is the subscriber(s). 3 methods: onError, onSubscribe(returns Disposable), onSuccess
        //    SingleEmitter basically extends SingleObserver (AKA Subscriber) by allowing registration of a Disposable or Cancellable with it
        //        (while "hiding" SingleObserver's onSubscribe(Disposable d), which you aren't supposed to interact with????)
        //    SingleOnSubscribe is a functional interface with a single subscribe() method that receives a SingleEmitter
        //
        // SEE https://github.com/ReactiveX/RxJava/issues/4787
        
        @Override
        public void subscribe( final SingleEmitter<JSONObject> emitter ) throws Exception {
            
            if( emitter == null ) throw new NullPointerException( "Can't subscribe with a null SingleEmitter" );
            
            if( newIdOnSubscribe ) //requestUUID = UUID.randomUUID();  // make a new ID for each .subscribe()
                requestUUID = ThermocoupleApp.getSoleInstance().jsonUUIDSupplier.iterator.next();
            
            Request request = new Request.Builder( )
                    .url( sourceURL )
                    .tag( UUID.class, requestUUID )  // added from HTTP Requester
                    .build( );
            
            //Call call = client.newCall( request );
            savedCall = client.newCall( request );
            
            //emitter.setCancellable( call::cancel );  // attempt to cancel the call if requester is unsubscribed
            emitter.setDisposable( disposable );       // is there a default implementation if you use lambdas?
            
            if( DEBUG ) Log.d( TAG, "About to enqueue JSON request UUID " + requestUUID.toString() );
            
            savedCall.enqueue( new Callback( ) {
                // Note callback is made after the response headers are ready. Reading the response body may still block.
                
                @Override
                public void onFailure( @NonNull Call call, @NonNull IOException e ) {
                    if( !emitter.tryOnError( new IOException( TAG + ": onFailure Callback while starting JSON request with UUID: "
                            + requestUUID.toString() + ": " + e.getMessage(), e ) ) ) {
                        Log.d( TAG, "JSON request UUID " + requestUUID.toString()
                                + " canceled before failure received" );
                    } else {  // Throwable was emitted because sequence still alive
                        disposed = true;  // TODO: right? Error disposes?
                        if( DEBUG ) Log.d( TAG, "onFailure callback for request UUID "
                                + requestUUID.toString() + "signaled IOException: " + e.getMessage() );
                    }
                    failures++;
                }
                
                @Override
                public void onResponse( @NonNull Call call, @NonNull Response response ) {
                    if( !emitter.isDisposed() ) {  // perhaps the request was canceled before response received?
                        if ( !response.isSuccessful( ) ) {
                            emitter.onError( new IOException( TAG + ": JSON request UUID " + requestUUID.toString()
                                    + " failed with HTTP status: " + response.message( ) ) );
                            disposed = true;  // TODO: right?
                            failures++;
                        } else {  // successful response
                            try {
                                ResponseBody responseBody = response.body( );
                                Headers headers = response.headers( );
                                
                                String contentType;
                                JSONObject returnedJSON = null;
                                
                                if ( (contentType = headers.get( "Content-type" )).equalsIgnoreCase( "application/json" ) ) {
                                    try /*( response )*/ {  // apparent bug, "not supported"
                                        returnedJSON = new JSONObject( responseBody.string( ) );
                                        returnedJSON.put( "RequestUUID", requestUUID.toString() );  // add the request UUID
                                    } catch ( JSONException j ) {
                                        emitter.onError( new JSONException( TAG + ": Invalid JSON returned from fetch (request UUID "
                                                + requestUUID.toString() + "): " + j.getMessage( ) ) );
                                    }
                                    emitter.onSuccess( returnedJSON );
                                    successes++;
                                } else {  // content type not JSON
                                    emitter.onError( new JSONException(
                                            TAG + ": Returned content type header (request UUID "
                                                    + requestUUID.toString() + "): is not JSON but " + contentType ) );
                                    failures++;
                                }
                            } catch ( IOException e ) {
                                emitter.onError( new IOException( TAG + ": Error fetching JSON from URL "
                                        + sourceURL.toString( ) + " (request UUID: " + requestUUID.toString() + ")", e ) );
                                failures++;
                            }
                        }  // else successful response
                    } else {  // emitter has been disposed (TODO: not a success or a failure?)
                        Log.d( TAG, "HTTP request UUID " + requestUUID.toString()
                                + " subscription disposed before response received" );
                    }
                    response.close();  // always do this
                }  // onResponse()
            } );  // Callback & enqueue()
            
            disposed = false;
            
        }  // subscribe
        
        
/*        // invoked when Single.execute() is called (i.e., when it is subscribed to)
        // passed the Subscriber to the Single
        // here it should return a JSON object by calling the Subscriber's onSuccess( JSONObject )
        @Override
        public void call( SingleSubscriber<? super JSONObject> singleSubscriber ) {
            
            Request request = new Request.Builder( )
                    .url( sourceURL )
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
                                    + "from URL " + sourceURL.toString( ), e ) );
//                        } catch( NullPointerException n ) {  // .source() can apparently return null
//                            singleSubscriber.onError( new NullPointerException( TAG + ": Call to .source() returned null" ) );
                        }
                        
                    }  // else successful response
                }  // onResponse()
            } );  // Callback & enqueue()
        }  // call()*/
        
        
    }  // class JSONGetterOnSubscribe
    
    
    // constructor
    public AsyncJSONGetter( URL jsonURL, OkHttpClient httpClient, UUID requestID ) {
        getJSON = Single.create( new JSONGetterOnSubscribe( ) );
        sourceURL = jsonURL;
        requestUUID = requestID;
        successes = failures = 0;
        client = client == null?               // if client is null
                httpClient == null?            // and passed httpClient is also null
                        new OkHttpClient( ) :  // create a new default client; if passed httpClient is not null
                        httpClient             // use it
                :                              // but if client is not null
                client;                        // keep it
        disposable = new Disposable() {
            @Override
            public void dispose() {
                if( !savedCall.isExecuted() ) savedCall.cancel();  // TODO: this seems to have fixed crash (but why is .dispose() getting called twice for some requests?
                disposed = true;
                if( DEBUG ) Log.d( TAG, ".dispose() called for JSON request ID " + requestID.toString()
                        + "; savedCall executed? " + savedCall.isExecuted() );
            }
            @Override
            public boolean isDisposed() {
                if( DEBUG ) Log.d( TAG, ".isDisposed() returning " + disposed + " for JSON request ID " + requestUUID.toString() );
                return disposed;
            }
        };  // disposable
    }  // primary constructor
    
    
    // NEW: constructor that specifies that request IDs should be auto-generated on each .subscribe()
    public AsyncJSONGetter( URL jsonURL, OkHttpClient httpClient, boolean generateIDs ) {
        this( jsonURL, httpClient );
        newIdOnSubscribe = true;
    }
    
    
    
    
    // constructor that supplies a random request UUID if we don't
    public AsyncJSONGetter( URL jsonURL, OkHttpClient httpClient ) {
        this( jsonURL, httpClient, UUID.randomUUID() );
    }
    
    /*
    Alternate with lambdas etc. might work:
    getJSON = Single.create( emitter -> { <code that calls emitter.onSuccess() and emitter.onError()> } );
     */
    
    // convenience constructor that allows URL to be String (doesn't support requestID)
    public AsyncJSONGetter( String jsonURLString, OkHttpClient httpClient )
            throws Exception {  // (malformed URL exception--subclass of IOException
        this( new URL( jsonURLString ), httpClient );
    }
    
    
    // after getting the JSON Getter instance, subscribe to this to do the request. It emits JSON
    public Single<JSONObject> get( ) {
        return getJSON;  // OkHttp3 manages its own threads, I think
    }
    
    
    // change source URL: returns the getter so you can chain with .get()
    public AsyncJSONGetter setSourceURL( URL newURL ) {
        sourceURL = newURL;
        requestUUID = UUID.randomUUID();
        return this;
    }
    
    public AsyncJSONGetter setSourceURL( URL newURL, UUID newRequestUUID ) {
        sourceURL = newURL;
        requestUUID = newRequestUUID;
        return this;
    }
    
    
    public AsyncJSONGetter setSourceURL( String newURLString ) throws Exception { // malformed URL exception
        sourceURL = new URL( newURLString );
        return this;
    }
    
    
    // set a new Request UUID if desired (as when changing URL)
    public AsyncJSONGetter setRequestUUID( UUID requestID ) {
        requestUUID = requestID;
        return this;
    }
    public AsyncJSONGetter setRequestUUID( ) {  // if no UUID supplied, create a random one
        requestUUID = UUID.randomUUID();
        return this;
    }
    
    public UUID getRequestUUID( ) {return requestUUID; }
    
    public int getSuccessCount() { return successes; }
    public int getFailureCount() { return failures; }
    
}

