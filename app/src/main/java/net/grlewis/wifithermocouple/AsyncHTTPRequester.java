package net.grlewis.wifithermocouple;

import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONObject;

import java.net.URL;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.schedulers.Schedulers;

import java.io.IOException;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created on 2018-08-27?
 *
 * Modified from AsyncJSONGetter
 *
 *
 * myAsyncRequester = new AsyncHTTPRequester( URL targetURL, OkHttpClient httpClient );
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

public class AsyncHTTPRequester {  // based on AsyncJSONGetter
    
    private static final String TAG = AsyncHTTPRequester.class.getSimpleName( );
    
    private final Single<Response> httpRequest;
    private URL theURL;  // mutable
    private OkHttpClient client;
    private UUID requestUUID;
    
    
    // Internal class to implement SingleOnSubscribe and its subscribe() method
    class HTTPRequesterOnSubscribe implements SingleOnSubscribe<Response> {
        
        // conversion to RxJava2 says must implement subscribe(SingleEmitter<T>)
        //
        //    SingleObserver is the subscriber(s). 3 methods: onError, onSubscribe(returns Disposable), onSuccess
        //    SingleEmitter basically extends SingleObserver (AKA Subscriber) by allowing registration of a Disposable or Cancellable with it?????
        //        (while "hiding" SingleObserver's onSubscribe(Disposable d), which you aren't supposed to interact with????)
        //    SingleOnSubscribe is a functional interface with a single subscribe() method that receives a SingleEmitter
        //
        // SEE https://github.com/ReactiveX/RxJava/issues/4787
        
        @Override
        public void subscribe( final SingleEmitter<Response> emitter ) throws Exception {
            
            if( emitter == null ) throw new IllegalArgumentException( "Can't subscribe with a null SingleEmitter" );
            
            Request request = new Request.Builder( )
                    .url( theURL )
                    .tag( UUID.class, requestUUID )
                    .build( );
            
            Call call = client.newCall( request );
            emitter.setCancellable( call::cancel );  // attempt to cancel the call if requester is unsubscribed
            
            call.enqueue( new Callback( ) {
                // Note callback is made after the response headers are ready. Reading the response body may still block.
                
                @Override
                public void onFailure( @NonNull Call call, @NonNull IOException e ) {
                    if( !emitter.tryOnError( new IOException( TAG + ": onFailure Callback while starting HTTP request with UUID: "
                            + requestUUID.toString(), e ) ) ) {
                        Log.d( TAG, "HTTP request canceled before failure received" );
                    }
                }
                
                @Override
                public void onResponse( @NonNull Call call, @NonNull Response response ) {
                    if( !emitter.isDisposed() ) {  // perhaps the request was canceled before response received?
                        if ( !response.isSuccessful( ) ) {
                            emitter.onError( new IOException( TAG + ": HTTP request UUID " + requestUUID.toString()
                                    + " failed with HTTP status: " + response.message( ) ) );
                        } else {  // successful response
                            if ( response.code( ) != 200 ) {  // response was not "OK"
                                Log.d( TAG, "HTTP request UUID " + requestUUID.toString()
                                        + " response code was not 200 (OK): " + response.code( ) );
                                emitter.onError( new IOException( "HTTP request UUID " + requestUUID.toString()
                                        + " failed with response code: " + response.code( ) ) );
                            } else {  // HTTP response OK
                                emitter.onSuccess( response );
                            }
                            response.close( );  // need?
                        }  // else successful response
                    } else {  // emitter has been disposed
                        Log.d( TAG, "HTTP request subscription disposed before response received" );
                    }
                    response.close();  // always do this
                }  // onResponse()
            } );  // Callback & enqueue()
            
        }  // subscribe
    }
    
    
    // constructor
    public AsyncHTTPRequester( URL targetURL, OkHttpClient httpClient, UUID requestID ) {
        httpRequest = Single.create( new HTTPRequesterOnSubscribe( ) );
        theURL = targetURL;
        requestUUID = requestID;
        client = client == null?                    // if client is null
                httpClient == null?                 // and passed httpClient is also null
                        new OkHttpClient( ) :       // create a new client; if passed httpClient is not null
                        httpClient                  // use it
                :                                   // but if client is not null
                client;                             // keep it
    }
    
    public AsyncHTTPRequester( URL targetURL, OkHttpClient httpClient ) {
        // if no UUID is supplied, generate one
        this( targetURL, httpClient, UUID.randomUUID() );
    }
    
    /*
    Alternate:
    httpRequest = Single.create( emitter -> { <code that calls emitter.onSuccess() and emitter.onError()> } );
     */
    
    // convenience constructor that allows URL to be String (doesn't support requestID)
    public AsyncHTTPRequester( String theURLString, OkHttpClient httpClient )
            throws Exception {  // (malformed URL exception--subclass of IOException
        this( new URL( theURLString ), httpClient );
    }
    
    // after getting the Requester instance, subscribe to this to send the request. It emits Response
    public Single<Response> request( ) {
        return httpRequest.subscribeOn( Schedulers.io( ) );  // TODO: probably don't need the Schedulers.io bit (OkHttp3 manages?)
    }
    
    // change source URL: returns the getter so you can chain with .request()
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

