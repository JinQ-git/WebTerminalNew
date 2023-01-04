package me.developer.jsonrpc;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

public class ResponseError implements IMessageType 
{
    // JSON-RPC defined error codes
    public static class ErrorCodes
    {
        public final static int ParseError = -32700;
        public final static int InvalidRequest = -32600;
        public final static int MethodNotFound = -32601;
        public final static int InvalidParams = -32602;
        public final static int InternalError = -32603;
    }

    public static class Error {
        public int code;
        public String message;
        public JsonElement data; // optional
    }

    public String        jsonrpc;
    public JsonElement   id; // String or Number or JsonNull
    public Error         error;

    public static ResponseError create(JsonPrimitive id, int errorCode, String message) {
        ResponseError ret = new ResponseError();
        ret.jsonrpc = "2.0";
        ret.id = id;
        ret.error = new Error();
        ret.error.code = errorCode;
        ret.error.message = message == null ? "Error" : message;
        ret.error.data = null;

        return ret;
    }

    private static ResponseError create(int errorCode, String message) {
        ResponseError ret = new ResponseError();
        ret.jsonrpc = "2.0";
        ret.id = JsonNull.INSTANCE;
        ret.error = new Error();
        ret.error.code = errorCode;
        ret.error.message = message == null ? "Error" : message;
        ret.error.data = null;

        return ret;
    }

    public static ResponseError ParseError(String message) { return create(ErrorCodes.ParseError, message); }

    public static ResponseError InvalidRequest(JsonPrimitive id) { return create(id, ErrorCodes.InvalidRequest, "Invalid Request"); }
    public static ResponseError InvalidRequest(JsonPrimitive id, String message) { return create(id, ErrorCodes.InvalidRequest, message == null ? "Invalid Request" : message); }

    public static ResponseError MethodNotFound(JsonPrimitive id) { return create(id, ErrorCodes.InvalidRequest, "Method Not Found"); }
    public static ResponseError MethodNotFound(JsonPrimitive id, String message) { return create(id, ErrorCodes.InvalidRequest, message == null ? "Method Not Found" : message); }

    public static ResponseError InvalidParams(JsonPrimitive id) { return create(id, ErrorCodes.InvalidParams, "Invalid Params"); }
    public static ResponseError InvalidParams(JsonPrimitive id, String message) { return create(id, ErrorCodes.InvalidParams, message == null ? "Invalid Params" : message); }

    @Override
    public boolean isValid() {
        boolean ret = jsonrpc.equals("2.0") 
                    && id != null 
                    && (id.isJsonPrimitive() || id.isJsonNull()) 
                    && error != null;
        if( id.isJsonPrimitive() ) {
            JsonPrimitive p = id.getAsJsonPrimitive();
            ret = ret && (p.isString() || p.isNumber());
        }
        return ret;
    }

    @Override
    public boolean isRequest() { return false; }

    @Override
    public boolean isResponse() { return true; }

    @Override
    public boolean isResponseSuccess() { return false; }

    @Override
    public boolean isResponseError() { return true; }

    @Override
    public boolean isNotification() { return false; }
}
