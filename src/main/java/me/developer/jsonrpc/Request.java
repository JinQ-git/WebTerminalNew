package me.developer.jsonrpc;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class Request implements IMessageType 
{
    public String        jsonrpc;
    public JsonPrimitive id; // String or Number
    public String        method;
    public JsonElement   params;

    @Override
    public boolean isValid() {
        return jsonrpc.equals("2.0") && method != null && id != null && (id.isString() || id.isNumber());
    }

    @Override
    public boolean isRequest() { return true; }

    @Override
    public boolean isResponse() { return false; }

    @Override
    public boolean isResponseSuccess() { return false; }

    @Override
    public boolean isResponseError() { return false; }

    @Override
    public boolean isNotification() { return false; }
}
