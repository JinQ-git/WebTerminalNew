package me.developer.ws.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.google.gson.*;
import com.jcraft.jsch.*;

import me.developer.jsonrpc.*;
import me.developer.jsonrpc.params.ResizeParams;
import me.developer.jsonrpc.params.EncodingParams;

public class ChannelShellHandler extends OutputStream implements IWebSocketHandler {

    final private String                  mTerminalId;
    final private javax.websocket.Session mSessionWS;
    final private com.jcraft.jsch.Session mSessionSSH;
    final private ChannelShell            mChannel;
    final private OutputStream            mChannelOS;

    private Charset mCharset;
    private Gson    mGson;

    final private byte[] dummy = new byte[1];

    private boolean mIsExplicitExit = false;

    public ChannelShellHandler(javax.websocket.Session wSession, 
                               JSch jsch, 
                               com.jcraft.jsch.Session session, 
                               ChannelShell ch, 
                               Charset charset,
                               String terminalId ) throws IOException 
    {
        mSessionWS = wSession;
        mSessionSSH = session;
        mChannel = ch;
        mTerminalId = terminalId;

        mCharset = charset == null ? StandardCharsets.UTF_8 : charset;
        
        mChannel.setInputStream(null);
        mChannel.setOutputStream(this);

        try {
            mChannelOS = mChannel.getOutputStream();
        }
        catch(IOException ioe) {
            mIsExplicitExit = true;
            mChannel.setOutputStream(null);
            throw ioe;
        }

        GsonBuilder builder = new GsonBuilder();
        mGson = builder.create();
    }

    // MARK: - Impl. IWebSocketHandler

    @Override
    public void onMessageText(String message) {
        if( mChannel.isConnected() ) {
            try { 
                mChannelOS.write(message.getBytes(mCharset)); 
                mChannelOS.flush();
            }
            catch(IOException ioe) {
                // Channel may be closed
                ioe.printStackTrace();
            }
        }
    }

    @Override
    public void onMessageBinary(byte[] message) {
        if( mChannel.isConnected() ) {
            String jsonStr = new String(message, StandardCharsets.UTF_8); // decode message
            try {
                IMessageType msg = JsonRPCUtil.parseJsonRPC(jsonStr, mGson);
                if( msg.isNotification() ) { // Accept Notification Message Only
                    Notification notify = (Notification)msg;
                    switch(notify.method)
                    {
                        case "resizeTerminal":
                        {
                            ResizeParams params = mGson.fromJson(notify.params, ResizeParams.class);
                            if( params.terminalId.equals(mTerminalId) && 
                              ( params.cols != null && params.rows != null && params.cols > 0 && params.rows > 0 ) ) 
                            {
                                final int cols = params.cols.intValue();
                                final int rows = params.rows.intValue();

                                // NOTE: width, height pixel is meaning-less value...
                                final int widthPixel  = (params.wp != null && params.wp > 0) ? params.wp : cols * 8;
                                final int heightPixel = (params.hp != null && params.hp > 0) ? params.hp : rows * 8;

                                mChannel.setPtySize(cols, rows, widthPixel, heightPixel);
                            }
                        }
                        break;

                        case "changeEncoding":
                        {
                            EncodingParams params = mGson.fromJson(notify.params, EncodingParams.class);
                            if( params.terminalId.equals(mTerminalId) && params.encoding != null )
                            {
                                try {
                                    Charset target = Charset.forName( params.encoding );
                                    mCharset = target;
                                }
                                catch(Exception e) { // IllegalCharsetNameException, UnsupportCharsetException
                                    e.printStackTrace();
                                    // Ignore
                                }
                            }
                        }
                        break;
                    }
                }
            }
            catch(JsonSyntaxException jse) {
                // Ignore Message
                return;
            }
        }
    }

    @Override
    public void onClose() {
        mIsExplicitExit = true;

        if( mChannel.isConnected() ) {
            mChannel.disconnect();
        }

        if( mSessionSSH.isConnected() ) {
            mSessionSSH.disconnect();
        }
    }

    // MARK: - Impl. OutputStream

    @Override
    public void write(byte[] b) throws IOException {
        if( mSessionWS.isOpen() ) {
            mSessionWS.getBasicRemote().sendText(new String(b, mCharset));
            //mSession.getBasicRemote().sendBinary(ByteBuffer.wrap(b));
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if( mSessionWS.isOpen() ) {
            mSessionWS.getBasicRemote().sendText(new String(b, off, len, mCharset));
            //mSession.getBasicRemote().sendBinary(ByteBuffer.wrap(b, off, len));
        }
    }

    @Override
    public void write(int b) throws IOException {
        // Dummy Method: Generally Never Called
        synchronized(dummy) {
            dummy[0] = (byte)b;
            write(dummy);
        }
    }

    @Override
    public void close() {
        if( !mIsExplicitExit ) {
            mIsExplicitExit = true;
            Thread exitThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    // Channel is closed!!
                    if( mSessionWS.isOpen() ) { // Close Shell
                        try { mSessionWS.close(); } catch(IOException ignore) {}
                    }

                    int waitCount = 5;
                    while( waitCount-- > 0 && mChannel.isConnected() ) {
                        try{ Thread.sleep(100); }catch(InterruptedException ignore){}
                    }

                    mSessionSSH.disconnect();
                }
            });
            exitThread.run();
        }
    }
}
