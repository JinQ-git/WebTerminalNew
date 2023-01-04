package me.developer.jsonrpc;

import com.google.gson.JsonElement;

public class Notification implements IMessageType 
{
    public String        jsonrpc;
    public String        method;
    public JsonElement   params;

    @Override
    public boolean isValid() {
        return jsonrpc.equals("2.0") && method != null;
    }

    @Override
    public boolean isRequest() { return false; }

    @Override
    public boolean isResponse() { return false; }

    @Override
    public boolean isResponseSuccess() { return false; }

    @Override
    public boolean isResponseError() { return false; }

    @Override
    public boolean isNotification() { return true; }
}
