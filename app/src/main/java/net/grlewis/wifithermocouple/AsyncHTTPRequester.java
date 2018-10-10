package net.grlewis.wifithermocouple;

import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONObject;

import java.net.URL;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.io.IOException;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static net.grlewis.wifithermocouple.Constants.DEBUG;

/**
 * Created on 2018-08-27?
 *
 * Modified from AsyncJSONGetter
 *
 *
 * myAsyncRequester = new AsyncHTTPRequester( URL targetURL, OkHttpClient httpClient [, UUID requestID] );
 * myAsyncRequester.request().subscribe( theResponse -> { handle it }, theThrowable -> { handle failure } );
 *
 * Since you're only supposed to have 1 instance of OkHttpClient, you pass it in
 * however if you pass null for the client, we create one
 *
 * setURL returns the Requester so you can chain with .request()
 *
 * 2018-09-26: added random UUID tag to request. Can be retrieved with mResponse.request().tag(UUID.class)
 *             added emitter.setCancellable()
 *
 * 2018-09-28: added request UUID to error messages
 *             pass or create new request UUID when setting a new URL
 *
 *
 */

public class AsyncHTTPRequester {  // based on AsyncJSONGetter (now back-porting enhancements to it)
    
    private static final String TAG = AsyncHTTPRequester.class.getSimpleName( );
    
    private final Single<Response> httpRequest;
    private URL theURL;           // mutable
    private final OkHttpClient client;
    private UUID requestUUID;     // mutable
    private int successes;
    private int failures;
    
    private Call savedCall;       // new stuff to implement Disposable
    private boolean disposed;
    private Disposable disposable;
    
    private boolean newIdOnSubscribe = false;  // new to auto-generate ID on each .subscribe
    
    
    // Internal class to implement SingleOnSubscribe [and its sole method, void subscribe( SingleEmitter )],
    // which is passed to Single.create( <something that implements SingleOnSubscribe> ) [this is the only form of Single.create()]
    class HTTPRequesterOnSubscribe implements SingleOnSubscribe<Response> {
        
        // conversion to RxJava2 says must implement subscribe(SingleEmitter<T>)
        //
        //    SingleObserver declares the subscriber(s)' 3 methods: onError, onSubscribe(returns Disposable), onSuccess
        
        //    SingleEmitter basically extends SingleObserver (AKA Subscriber) by allowing registration of a Disposable or Cancellable with it
        //        (while "hiding" SingleObserver's onSubscribe(Disposable d), which you aren't supposed to interact with????
        //         Usually .subscribe() calls .onSubscribe(Disposable) but in this case .subscribe() is void)
        //        setDisposable() and setCancellable() provide Actions to be performed when the downstream cancels the operation
        //        Besides setDisposable() and setCancellable(), SingleEmitter adds
        //            boolean isDisposed() which returns true if it's been disposed or terminated
        //            boolean tryOnError( Throwable ) which is like onError() but returns false if it can't emit the Throwable
        //                because the sequence is cancelled or otherwise terminated.
        
        //    SingleOnSubscribe is a functional interface with a single void subscribe() method that receives a SingleEmitter.
        //        Unlike many versions of subscribe(), this returns void instead of a Disposable.
        //    This class can of course do other things besides define the void subscribe() method
        //
        // SEE https://github.com/ReactiveX/RxJava/issues/4787
        
        @Override  // define what happens when you subscribe to the Single being created, supplying a SingleEmitter
        // Note that Emitter adds setCancellable(), setDisposable(), isDisposed() and tryOnError() to the basic Observer
        // subscribe() returns void, not a Disposable
        public void subscribe( final SingleEmitter<Response> emitter ) throws Exception {
            
            if( emitter == null ) throw new NullPointerException( TAG + " Can't subscribe with a null SingleEmitter" );
            
            if( newIdOnSubscribe ) // requestUUID = UUID.randomUUID();
                requestUUID = ThermocoupleApp.getSoleInstance().httpUUIDSupplier.iterator.next();  // TODO: remove after debugging
    
            Request request = new Request.Builder( )
                    .url( theURL )
                    .tag( UUID.class, requestUUID )
                    .build( );
            
            savedCall = client.newCall( request );
            
            emitter.setDisposable( disposable );       // is there a default implementation if you use lambdas?
            
            savedCall.enqueue( new Callback( ) {
                // Note callback is made after the response headers are ready. Reading the response body may still block.
                
                @Override
                // Called when the request could not be executed due to cancellation, a connectivity problem or timeout.
                // Because networks can fail during an exchange, it is possible that the remote server accepted the request before the failure.
                public void onFailure( @NonNull Call call, @NonNull IOException e ) {
                    failures++;
                    // TODO:  should we set disposed here? Maybe not--retries should be possible, right?
                    // tryOnError() returns false if sequence has been cancelled by downstream, or otherwise terminated
                    if( !emitter.tryOnError( new IOException( TAG + ": onFailure Callback while starting HTTP request with UUID: "
                            + requestUUID.toString() + ": " + e.getMessage(), e ) ) ) {
                        Log.d( TAG, "HTTP request UUID " + requestUUID.toString()
                                + " canceled before failure received" );
                    } else {  // Throwable was emitted because sequence still alive
                        disposed = true;  // TODO: right? Error disposes? Maybe not? Retries should be possible? But Observer cancels on onError()?
                        if( DEBUG ) Log.d( TAG, "onFailure callback for request UUID "
                                + requestUUID.toString() + "signaled IOException: " + e.getMessage() );
                    }
                }
                
                @Override
                public void onResponse( @NonNull Call call, @NonNull Response response ) {
                    if( !emitter.isDisposed() ) {  // perhaps the request was canceled before response received?
                        if ( !response.isSuccessful( ) ) {  // evidently "successful" just means we got an intelligible response
                            emitter.onError( new IOException( TAG + ": HTTP request UUID " + requestUUID.toString()
                                    + " failed with HTTP status: " + response.message( ) ) );
                            failures++;
                            disposed = true;  // TODO: right?
                        } else {  // successful response
                            if ( response.code( ) != 200 ) {  // response was not "OK"
                                failures++;
                                Log.d( TAG, "HTTP request UUID " + requestUUID.toString()
                                        + " response code was not 200 (OK); was : " + response.code( ) );
                                emitter.onError( new IOException( "HTTP request UUID " + requestUUID.toString()
                                        + " failed with response code: " + response.code( ) ) );
                                disposed = true;  // TODO: right?
                            } else {  // HTTP response OK
                                successes++;
                                emitter.onSuccess( response );
                            }
                        }  // else successful response
                    } else {  // emitter has been disposed TODO: neither success nor failure, right?
                        if( DEBUG ) Log.d( TAG, "HTTP request UUID " + requestUUID.toString()
                                + " subscription disposed before response received" );
                        disposed = true;  // TODO: right?
                    }
                    response.close();  // always do this
                }  // onResponse()
            } );  // Callback & enqueue()
            
            disposed = false;  // initially after subscribing, it is not disposed
            
        }  // subscribe
        // anything else we want this class to do? Maybe not; how would we access?
    }  // internal class HTTPRequesterOnSubscribe
    
    
    // constructor
    public AsyncHTTPRequester( URL targetURL, OkHttpClient httpClient, UUID requestID ) {
        httpRequest = Single.create( new HTTPRequesterOnSubscribe( ) );
        theURL = targetURL;
        requestUUID = requestID;
        successes = failures = 0;
        client = httpClient == null?        // if passed httpClient is null
                new OkHttpClient( ) :       // create a new default client; if not
                httpClient                  // use the supplied one
        ;
        disposable = new Disposable() {
            @Override
            public void dispose() {
                if( !savedCall.isExecuted() ) savedCall.cancel();  // TODO: 'if' seems to have fixed crash (but why is .dispose() getting called twice for some requests?
                disposed = true;
                if( DEBUG ) Log.d( TAG, ".dispose() called for HTTP request ID " + requestID.toString()
                        + "; savedCall executed? " + savedCall.isExecuted() );
            }
            @Override
            public boolean isDisposed() {
                if( DEBUG ) Log.d( TAG, ".isDisposed() returning " + disposed + " for HTTP request ID " + requestUUID.toString() );
                return disposed;
            }
        };  // disposable
    }  // primary constructor
    
    
    // NEW: constructor that specifies that request IDs should be auto-generated on each .subscribe()
    public AsyncHTTPRequester( URL targetURL, OkHttpClient httpClient, boolean generateIDs ) {
        this( targetURL, httpClient );
        newIdOnSubscribe = true;
    }
    
    
    // constructor that supplies a random request UUID if we don't (client can be null)
    public AsyncHTTPRequester( URL targetURL, OkHttpClient httpClient ) {
        this( targetURL, httpClient, UUID.randomUUID() );  // if no UUID is supplied, generate one
    }
    
    // constructor that takes just the URL
    public AsyncHTTPRequester( URL targetURL ) {
        this( targetURL, null, UUID.randomUUID() );  // create client and request ID
    }
    
    /*
    Alternate with lambdas etc. might work:
    httpRequest = Single.create( emitter -> { <code that calls emitter.onSuccess() and emitter.onError()> } );
     */
    
    // convenience constructor that allows URL to be String (doesn't support requestID)
    public AsyncHTTPRequester( String theURLString, OkHttpClient httpClient )
            throws Exception {  // (malformed URL exception--subclass of IOException
        this( new URL( theURLString ), httpClient );
    }
    
    
    
    // after getting the Requester instance, subscribe to this to send the request. It emits Response
    public Single<Response> request( ) {
        return httpRequest /*.subscribeOn( Schedulers.io( ) )*/;  // probably don't need the Schedulers.io bit (OkHttp3 manages?)
    }
    
    
    
    // change source URL (and create new request ID): returns the requester so you can chain with .request()
    public AsyncHTTPRequester setURL( URL newURL ) {
        theURL = newURL;
        requestUUID = UUID.randomUUID();
        return this;
    }
    
    public AsyncHTTPRequester setURL( URL newURL, UUID newRequestUUID ) {
        theURL = newURL;
        requestUUID = newRequestUUID;
        return this;
    }
    
    public AsyncHTTPRequester setURL( String newURLString ) throws Exception { // malformed URL exception
        theURL = new URL( newURLString );
        return this;
    }
    
    
    // set a new Request UUID if desired (as when changing URL)
    public AsyncHTTPRequester setRequestUUID( UUID requestID ) {
        requestUUID = requestID;
        return this;
    }
    public UUID getRequestUUID( ) {return requestUUID; }
    
    public int getSuccessCount() { return successes; }
    public int getFailureCount() { return failures; }
    
}

