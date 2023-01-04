package me.developer.jsonrpc;

import com.google.gson.JsonSyntaxException;

import com.google.gson.*;

public class JsonRPCUtil {
    public static IMessageType parseJsonRPC(String jsonStr, Gson gson) throws JsonSyntaxException {
        JsonObject jsonrpc = gson.fromJson(jsonStr, JsonObject.class);
        IMessageType ret = null;

        if( jsonrpc.get("id") != null ) // Request or Response
        {
            if(jsonrpc.get("method") != null) {
                ret = gson.fromJson( jsonrpc, Request.class );
            }
            else if(jsonrpc.get("result") != null) {
                ret = gson.fromJson( jsonrpc, ResponseSuccess.class );
            }
            else if(jsonrpc.get("error") != null) {
                ret = gson.fromJson(jsonrpc, ResponseError.class);
            }
            else {
                throw new JsonSyntaxException("Invalid JSON-RPC message format");
            }
        }
        else { // Notification
            ret = gson.fromJson(jsonrpc, Notification.class);
        }

        if( !ret.isValid() ) {
            throw new JsonSyntaxException("Invalid JSON-RPC message format");
        }

        return ret;
    }
}
