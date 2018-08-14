package net.grlewis.wifithermocouple;

import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Headers;
import okhttp3.ResponseBody;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created on 2018-03-27.
 *
 * Modified from AsyncDownloader
 *
 *
 * myJsonGetter = new AsyncJSONGetter( URL jsonURL, OkHttpClient httpClient );
 * myJsonGetter.get().subscribe( theJSON -> { handle it }, theThrowable -> { handle failure } );
 *
 * since you're only supposed to have 1 instance of OkHttpClient, you pass it in
 * however if you pass null for the client, we create one
 *
 * Changed to make setSourceURL return the Getter so you can chain with .get()
 *
 * 2018-08: updating to RxJava2
 *
 *
 */

public class AsyncJSONGetter {  // updating from GlucometerApp version to use RxJava2
    
    private static final String TAG = AsyncJSONGetter.class.getSimpleName( );
    
    private final Single<JSONObject> getJSON;
    private URL sourceURL;  // mutable
    private final OkHttpClient client;
    
    
    class JSONGetterOnSubscribe implements SingleOnSubscribe<JSONObject> {
        
        // conversion to RxJava2 says must implement subscribe(SingleEmitter<T>)
        //
        //    SingleObserver is the subscriber(s). 3 methods: onError, onSubscribe(returns Disposable), onSuccess
        //    SingleEmitter basically extends SingleObserver (AKA Subscriber) by allowing registration of a Disposable or Cancellable with it?????
        //        (while "hiding" SingleObserver's onSubscribe(Disposable d), which you aren't supposed to interact with????)
        //    SingleOnSubscribe is a functional interface with a single subscribe() method that receives a SingleEmitter
        //
        // SEE https://github.com/ReactiveX/RxJava/issues/4787
        
        @Override
        public void subscribe( final SingleEmitter<JSONObject> emitter ) {
    
            Request request = new Request.Builder( )
                    .url( sourceURL )
                    .build( );
    
            client.newCall( request ).enqueue( new Callback( ) {
                // Note callback is made after the response headers are ready. Reading the response body may still block.
        
                @Override
                public void onFailure( @NonNull Call call, @NonNull IOException e ) {
                    emitter.onError( new IOException( TAG + ": onFailure Callback while starting JSON fetch: ", e ) );
                }
        
                @Override
                public void onResponse( @NonNull Call call, @NonNull Response response ) {
                    if ( !response.isSuccessful( ) ) {
                        emitter.onError( new IOException( TAG + ": JSON Fetch failed with HTTP status: " + response.message( ) ) );
                    } else {  // successful response
                        try {
                            ResponseBody responseBody = response.body( );
                            Headers headers = response.headers( );
                            response.close( );
                    
                            String contentType;
                            JSONObject returnedJSON = null;
                    
                            if ( (contentType = headers.get( "Content-type" )).equalsIgnoreCase( "application/json" ) ) {
                                try {
                                    returnedJSON = new JSONObject( responseBody.string( ) );
                                } catch ( JSONException j ) {
                                    emitter.onError( new JSONException( TAG + ": Invalid JSON returned from fetch: " + j.getMessage( ) ) );
                                    return;  // new
                                }
                                emitter.onSuccess( returnedJSON );
                            } else {  // content type not JSON
                                emitter.onError( new JSONException(
                                        TAG + ": Returned content type header is not JSON but " + contentType ) );
                            }
                        } catch ( IOException e ) {
                            emitter.onError( new IOException( TAG + ": Error fetching JSON "
                                    + "from URL " + sourceURL.toString( ), e ) );
                        }
                    }  // else successful response
                }  // onResponse()
            } );  // Callback & enqueue()
        }
        
        
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
                            
                            if ( (contentType = headers.get( "Content-type" )).equalsIgnoreCase( "application/json" ) ) {
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
    public AsyncJSONGetter( URL jsonURL, OkHttpClient httpClient ) {
        getJSON = Single.create( new JSONGetterOnSubscribe( ) );
        sourceURL = jsonURL;
        client = httpClient == null? new OkHttpClient( ) : httpClient;  // if we weren't passed a client, create one
    }
    // should we have a no-arg version?
    
    /*
    Alternate:
    getJSON = Single.create( emitter -> { <code that calls emitter.onSuccess() and emitter.onError()> } );
     */
    
    // convenience constructor that allows URL to be String
    public AsyncJSONGetter( String jsonURLString, OkHttpClient httpClient )
            throws Exception {  // (malformed URL exception--subclass of IOException
        this( new URL( jsonURLString ), httpClient );
    }
    
    // after getting the JSON Getter instance, subscribe to this to do the get
    public Single<JSONObject> get( ) {
        return getJSON.subscribeOn( Schedulers.io( ) );
    }
    
    // change source URL: returns the getter so you can chain with .get()
    public AsyncJSONGetter setSourceURL( URL newURL ) {
        sourceURL = newURL;
        return this;
    }
    
    public AsyncJSONGetter setSourceURL( String newURLString ) throws Exception { // malformed URL exception
        sourceURL = new URL( newURLString );
        return this;
    }
}

