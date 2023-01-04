package me.developer.jsonrpc;

public interface IMessageType 
{
    public boolean isValid();
    public boolean isRequest();
    public boolean isResponse();
    public boolean isResponseSuccess();
    public boolean isResponseError();
    public boolean isNotification();
}
