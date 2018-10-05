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
    private URL theURL;  // mutable
    private OkHttpClient client;
    private UUID requestUUID;
    
    private Call savedCall;       // new stuff to implement Disposable
    private boolean disposed;
    private Disposable disposable;
    
    
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
        public void subscribe( final SingleEmitter<Response> emitter ) throws Exception {
            
            if( emitter == null ) throw new NullPointerException( TAG + " Can't subscribe with a null SingleEmitter" );
            
            Request request = new Request.Builder( )
                    .url( theURL )
                    .tag( UUID.class, requestUUID )
                    .build( );
            
            //Call call = client.newCall( request );
            savedCall = client.newCall( request );
            
            //emitter.setCancellable( call::cancel );  // attempt to cancel the call if requester is unsubscribed
            emitter.setDisposable( disposable );       // is there a default implementation if you use lambdas?
            
            savedCall.enqueue( new Callback( ) {
                // Note callback is made after the response headers are ready. Reading the response body may still block.
                
                @Override
                public void onFailure( @NonNull Call call, @NonNull IOException e ) {
                    if( !emitter.tryOnError( new IOException( TAG + ": onFailure Callback while starting HTTP request with UUID: "
                            + requestUUID.toString() + ": " + e.getMessage(), e ) ) ) {
                        Log.d( TAG, "HTTP request UUID " + requestUUID.toString()
                                + " canceled before failure received" );
                    } else {  // Throwable was emitted because sequence still alive
                        disposed = true;  // TODO: right? Error disposes?
                        if( DEBUG ) Log.d( TAG, "onFailure callback for request UUID "
                                + requestUUID.toString() + "signaled IOException: " + e.getMessage() );
                    }
                }
                
                @Override
                public void onResponse( @NonNull Call call, @NonNull Response response ) {
                    if( !emitter.isDisposed() ) {  // perhaps the request was canceled before response received?
                        if ( !response.isSuccessful( ) ) {
                            emitter.onError( new IOException( TAG + ": HTTP request UUID " + requestUUID.toString()
                                    + " failed with HTTP status: " + response.message( ) ) );
                            disposed = true;  // TODO: right?
                        } else {  // successful response
                            if ( response.code( ) != 200 ) {  // response was not "OK"
                                Log.d( TAG, "HTTP request UUID " + requestUUID.toString()
                                        + " response code was not 200 (OK); was : " + response.code( ) );
                                emitter.onError( new IOException( "HTTP request UUID " + requestUUID.toString()
                                        + " failed with response code: " + response.code( ) ) );
                                disposed = true;  // TODO: right?
                            } else {  // HTTP response OK
                                emitter.onSuccess( response );
                            }
                        }  // else successful response
                    } else {  // emitter has been disposed
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
        client = client == null?                    // if client is null
                httpClient == null?                 // and passed httpClient is also null
                        new OkHttpClient( ) :       // create a new default client; if passed httpClient is not null
                        httpClient                  // use it
                :                                   // but if client is not null
                client;                             // keep it
        disposable = new Disposable() {
            @Override
            public void dispose() {
                if( !savedCall.isExecuted() ) savedCall.cancel();  // TODO: this seems to have fixed crash (but why is .dispose() getting called twice for some requests?
                disposed = true;
                if( DEBUG ) Log.d( TAG, ".dispose() called for request ID " + requestID.toString()
                        + "; savedCall executed? " + savedCall.isExecuted() );
            }
            @Override
            public boolean isDisposed() {
                if( DEBUG ) Log.d( TAG, ".isDisposed() returning " + disposed + " for request ID " + requestUUID.toString() );
                return disposed;
            }
        };  // disposable
    }  // primary constructor
    
    
    // constructor that supplies a random request UUID if we don't
    public AsyncHTTPRequester( URL targetURL, OkHttpClient httpClient ) {
        // if no UUID is supplied, generate one
        this( targetURL, httpClient, UUID.randomUUID() );
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
    
    
    // change source URL: returns the requester so you can chain with .request()
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
    
}

