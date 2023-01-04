package me.developer.ws.util;

public interface IWebSocketHandler
{
    public void onMessageText(String message);
    public void onMessageBinary(byte[] message);
    public void onClose();
}