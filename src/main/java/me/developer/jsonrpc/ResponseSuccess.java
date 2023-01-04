package me.developer.jsonrpc;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class ResponseSuccess implements IMessageType 
{
    public String        jsonrpc;
    public JsonPrimitive id; // String or Number
    public JsonElement   result;

    public static ResponseSuccess create(JsonPrimitive id, JsonElement result) {
        ResponseSuccess ret = new ResponseSuccess();
        ret.jsonrpc = "2.0";
        ret.id = id;
        ret.result = result;

        return ret;
    }

    @Override
    public boolean isValid() {
        return jsonrpc.equals("2.0") && id != null && (id.isString() || id.isNumber() && result != null);
    }

    @Override
    public boolean isRequest() { return false; }

    @Override
    public boolean isResponse() { return true; }

    @Override
    public boolean isResponseSuccess() { return true; }

    @Override
    public boolean isResponseError() { return false; }

    @Override
    public boolean isNotification() { return false; }
}
