package net.grlewis.wifithermocouple;

import org.json.JSONObject;

// Interface that handles the callback from the request JSON calls.
// Actual handlers implement handleJSON and optionally handleJSONError
interface JSONHandler {
    
    void handleJSON( JSONObject theJSON );
    
    default void handleJSONError( Throwable theError ) {  // hey, my first use of a default method in an interface
        theError.printStackTrace();
    }
    
}

