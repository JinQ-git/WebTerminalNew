package me.developer.ws;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

import me.developer.ws.util.ChannelShellHandler;
import me.developer.ws.util.ConnectionHandler;
import me.developer.ws.util.IWebSocketHandler;

@ServerEndpoint(value="/ws/ssh")
public class SSHWebSocket implements ConnectionHandler.IConnectionCompleteHandler {
    private final Object handlerMutex = new Object();
    // Current Websocket Instance
    //private javax.websocket.Session mSessionWS = null;
    private IWebSocketHandler mHandler = null;

    @OnOpen
    public void onOpenWebSocket(javax.websocket.Session wSession) {
        //mSessionWS = wSession;
        synchronized(handlerMutex) {
            mHandler = new ConnectionHandler(wSession, this);
        }
    }

    @OnClose
    public void onCloseWebSocket(javax.websocket.Session wSession) {
        synchronized(handlerMutex) {
            if( mHandler != null ) {
                mHandler.onClose();
            }
        }
    }

    @OnMessage // Text Version
    public void onTextMessage(String msg) {
        synchronized(handlerMutex) {
            if( mHandler != null ) { mHandler.onMessageText(msg); }
        }
    }

    @OnMessage // Binary Version
    public void onBinaryMessage(byte[] msg) {
        synchronized(handlerMutex) {
            if( mHandler != null ) { mHandler.onMessageBinary(msg); }
        }
    }

    @OnError
    public void onError(Throwable e) {
        e.printStackTrace();
    }

    // MARK: - Impl. ConnectionHandler.IConnectionCompleteHandler

    @Override
    public void onConnectionComplete(ChannelShellHandler newHandler) {
        synchronized(handlerMutex) {
            mHandler = newHandler;
        }
    }
}
