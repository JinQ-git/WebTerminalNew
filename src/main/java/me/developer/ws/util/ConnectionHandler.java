package me.developer.ws.util;

import java.util.concurrent.locks.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import javax.websocket.CloseReason.CloseCodes;

import com.google.gson.*;
import com.jcraft.jsch.UserInfo;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UIKeyboardInteractive;

import me.developer.jsonrpc.*;
import me.developer.jsonrpc.params.*;

public class ConnectionHandler implements IWebSocketHandler, UserInfo, UIKeyboardInteractive, Runnable {

    public interface IConnectionCompleteHandler {
        public void onConnectionComplete(ChannelShellHandler newHandler);
    }

    private final Lock      mLock          = new ReentrantLock();
    private final Condition mWaitInputCond = mLock.newCondition();
    private final Session   mSessionWS;
    private final IConnectionCompleteHandler mCCHandler;

    private Gson mGson;

    private JsonPrimitive mConnectId = null;
    private String mTerminalId = null;
    private ConnectParams mConnectParams = null;
    private Thread mConnectThread = null;

    // Impl. UserInfo, UIKeyboardInterfactive Interface(s)
    private Long    mPromptId    = null;
    private String  mPromptInput = null;

    private String  mPassword   = null;
    private String  mPassphrase = null;
    private boolean mForceYes   = false;

    private boolean mShouldCancelInput = false;

    public ConnectionHandler(Session wSession, IConnectionCompleteHandler handler ) {
        mSessionWS = wSession;
        mCCHandler = handler;

        GsonBuilder builder = new GsonBuilder();
        mGson = builder.create();
    }

    public void setPassword(String passwd) {
        mPassword = passwd;
    }

    public void setPassphrase(String passphrase) {
        mPassphrase = passphrase;
    }

    public void setForceYes(boolean yes) {
        mForceYes = yes;
    }

    private String waitForInput(PromptParams params) {
        mLock.lock();
        try {
            if( mShouldCancelInput ) { return null; }
            if( mPromptId == null) {
                long id = System.currentTimeMillis(); // use timestamp as prompt id

                StringBuffer sb = new StringBuffer(256);
                sb.append("{\"jsonrpc\":\"2.0\",\"id\":").append(id)
                .append(",\"method\":\"prompt\",\"params\":")
                .append(mGson.toJson(params)).append("}");

                // Send Message to Client
                String req = sb.toString();
                mSessionWS.getBasicRemote().sendBinary(ByteBuffer.wrap( req.getBytes(StandardCharsets.UTF_8) ));

                mPromptId = id;

                // Wait Message From Client
                mWaitInputCond.await();

                mPromptId = null;

                String input = mPromptInput;
                mPromptInput = null; // consume
                return input;
            }
            // Unexpected method call
        }
        catch(IOException ioe) { // Websocket Write Error
            ioe.printStackTrace();
        }
        catch(InterruptedException ie) { // Interrupted
            ie.printStackTrace(); 
        }
        finally {
            mLock.unlock();
        }
        return null; // Error or Cancel
    }

    private void sendJsonRPC(String jsonStr) {
        ByteBuffer buffer = ByteBuffer.wrap( jsonStr.getBytes(StandardCharsets.UTF_8) );
        try { mSessionWS.getBasicRemote().sendBinary( buffer ); }
        catch(IOException ioe) { // Websocket session may be disconnected...
            ioe.printStackTrace();
        }
    }

    private void sendResp(ResponseSuccess resp) { sendJsonRPC(mGson.toJson(resp)); }

    private void sendError(ResponseError error) { sendJsonRPC(mGson.toJson(error)); }

    // MARK: - Impl. IWebSocketHandler

    @Override
    public void onMessageText(String message) {
        // Not used...
    }

    @Override
    public void onMessageBinary(byte[] message) {
        String jsonStr = new String(message, StandardCharsets.UTF_8); // decode message

        // Validate
        try {
            IMessageType msg = JsonRPCUtil.parseJsonRPC(jsonStr, mGson);

            if( msg.isRequest() )
            {
                // Only Accept "connect" method
                Request req = (Request)msg;
                if( !req.method.equals("connect") ) {
                    sendError(ResponseError.MethodNotFound(req.id, "[" + req.method + "] is not available"));
                    return;
                }
                else if( mConnectId != null ) {
                    sendError(ResponseError.InvalidRequest(req.id, "Invalid Request: 'connect' request is already received before"));
                    return;
                }

                // Handle Connect Message
                ConnectParams params = null;
                try { params = mGson.fromJson( req.params, ConnectParams.class ); }
                catch(JsonSyntaxException jse) {
                    sendError(ResponseError.InvalidParams(req.id, "Invalid 'connect' request params"));
                    return;
                }

                mConnectId  = req.id;
                mTerminalId = params.getTermianlId();
                mConnectParams = params;

                mConnectThread = new Thread(this);
                mConnectThread.start();
            }
            else if (msg.isResponse() )
            {
                if( msg.isResponseSuccess() )
                {
                    ResponseSuccess resp = (ResponseSuccess)msg;

                    mLock.lock();
                    if( mPromptId != null )
                    {
                        // Check Id
                        if( resp.id.isNumber() && resp.id.getAsLong() == mPromptId ) {
                            if( resp.result.isJsonPrimitive() ) {
                                JsonPrimitive result = resp.result.getAsJsonPrimitive();
                                mPromptInput = result.getAsString(); // result.isString()
                            } // result may JsonNull if user cancel the prompt input
                            else {
                                mPromptInput = null;
                            }
                            mWaitInputCond.signal();
                        }
                    }
                    mLock.unlock();
                }
                else if( msg.isResponseError() )
                {
                    // ResponseError resp = (ResponseError)msg;
                    // if( resp.id.isJsonPrimitive() )
                    // {
                    //     JsonPrimitive respId = resp.id.getAsJsonPrimitive();

                    //     mLock.lock();
                    //     if( mPromptId != null ) {
                    //         // Check Id
                    //         if( respId.isNumber() && respId.getAsLong() == mPromptId ) {
                    //             // Error -> Cancel Input
                    //             mPromptInput = null;
                    //             mWaitInputCond.signal();
                    //         }
                    //     }
                    //     mLock.unlock();
                    // }
                }
            }
            else // if( msg.isNotification() )
            {
                // Ignore Notification
            }
        }
        catch(JsonSyntaxException jse) {
            // JSON Parse Error
            sendError(ResponseError.ParseError(jse.getMessage()));
            return;
        }
    }

    @Override
    public void onClose() {
        // While Connecting to the Server
        if( mConnectThread != null && mConnectThread.isAlive() )
        {
            mLock.lock();
            mShouldCancelInput = true;
            mPromptInput = null;
            mWaitInputCond.signalAll();
            mLock.unlock();

            try { mConnectThread.join(); }
            catch(InterruptedException ignore) {}
        }
    }

    // MARK: - Impl. Runnable: NOTE: Do not run this method directly
    @Override
    public void run()
    {
        // NOTE: Do not call this method directly, this will be called at thread automatically.
        // NOTE: mConnParams must not be null and valid instance

        // assert( mConnectParmas != null && mConnectParams.isValid() )

        // Connect to SSH Server
        JSch jsch = new JSch();
        com.jcraft.jsch.Session sessionSSH = null;
        ChannelShell chShell = null;
        try {
            sessionSSH = jsch.getSession(mConnectParams.getUser(), 
                                         mConnectParams.getHost(), 
                                         mConnectParams.getPort());

            if(!mConnectParams.getStrictHostCheck()) {
                sessionSSH.setConfig("StrictHostKeyChecking", "no");
            }
            sessionSSH.setUserInfo(ConnectionHandler.this);

            final int timeout = mConnectParams.getTimeout();
            sessionSSH.connect(timeout);

            chShell = (ChannelShell)sessionSSH.openChannel("shell");
            // setPtyType(ptyType) here...: `ptyType` is one of [ "vt100", "vt102", "xterm", "xterm-256color", "linux", "screen", ...]
            chShell.setPtyType("xterm-256color"); // default is "vt100"; Terminal app of macOS and Windows use "xterm-256color"

            Integer termianlRows = mConnectParams.getTermainlRows();
            Integer terminalCols = mConnectParams.getTermainlCols();
            if( termianlRows != null && terminalCols != null && termianlRows > 0 && terminalCols > 0 ) {
                final int cols = terminalCols.intValue();
                final int rows = termianlRows.intValue();

                // NOTE: width, height pixel is meaning-less value...
                final int widthPixel = cols * 8;
                final int heightPixel = rows * 8;

                chShell.setPtySize(cols, rows, widthPixel, heightPixel);
            }
        }
        catch(JSchException je) {
            // Connect Fail Response Error
            sendError(ResponseError.create( mConnectId, 1, je.getMessage() ));
            try { mSessionWS.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, je.getMessage())); }
            catch(IOException ignore) {}

            if( sessionSSH != null && sessionSSH.isConnected() ) {
                sessionSSH.disconnect();
            }

            mConnectId = null;
            mTerminalId = null;
            mConnectParams = null;
            return;
        }

        ChannelShellHandler newHandler = null;
        try {
            newHandler = new ChannelShellHandler(mSessionWS, jsch, sessionSSH, chShell, mConnectParams.getEncodingCharset(), mTerminalId);
        }
        catch(IOException ioe) {
            // Connect Fail Response Error
            sendError(ResponseError.create( mConnectId, 2, ioe.getMessage() ));
            try { mSessionWS.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, ioe.getMessage())); }
            catch(IOException ignore) {}

            if(chShell != null && chShell.isConnected()) {
                chShell.setOutputStream(null);
                chShell.disconnect();
            }

            if( sessionSSH != null && sessionSSH.isConnected() ) {
                sessionSSH.disconnect();
            }

            mConnectId = null;
            mTerminalId = null;
            mConnectParams = null;
            return;
        }

        try {
            final int timeout = mConnectParams.getTimeout();
            chShell.connect(timeout);
        }
        catch(JSchException jse) {
            // Connect Fail Response Error
            sendError(ResponseError.create( mConnectId, 3, jse.getMessage() ));
            try { mSessionWS.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, jse.getMessage())); }
            catch(IOException ignore) {}

            newHandler.onClose();
            // This close() method is meaning-less code, just add for remove warning message from java compiler.
            // Do not call close() method directly, this method will be called from ChannelShell class.
            newHandler.close(); 

            mConnectId = null;
            mTerminalId = null;
            mConnectParams = null;
            return;
        }

        // Open Shell Channel
        if( mCCHandler != null ) {
            mCCHandler.onConnectionComplete(newHandler);
        }

        // Send Sucess Response
        sendResp(ResponseSuccess.create(mConnectId, new JsonPrimitive(true)));
    }

    // MARK: - Impl. UIKeyboardInteractive

    @Override
    public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt, boolean[] echo) {
        String[] results = new String[prompt.length];

        for(int i = 0; i < prompt.length; i++) {
            String ret = waitForInput( 
                new PromptParams( mTerminalId, 
                                  prompt[i], 
                                  echo[i] ? PromptParams.PromptType.StringType : PromptParams.PromptType.PasswordType )
            );
            mShouldCancelInput = ret == null;
            results[i] = ret;
        }

        return results;
    }

    // MARK: - Impl. UserInfo

    @Override
    public String getPassphrase() { return mPassphrase; }

    @Override
    public String getPassword() { 
        String ret = mPassword;
        mPassword = null; // reset password -> ask again if password is incorrect!!
        return ret; 
    }

    @Override
    public boolean promptPassword(String message) {
        if(mPassword != null) { return true; }
        mPassword = waitForInput( new PromptParams(mTerminalId, message, PromptParams.PromptType.PasswordType) );
        mShouldCancelInput = mPassword == null;
        return mPassword != null;
    }

    @Override
    public boolean promptPassphrase(String message) {
        if(mPassphrase != null) { return true; }
        mPassphrase = waitForInput( new PromptParams(mTerminalId, message, PromptParams.PromptType.PasswordType) );
        mShouldCancelInput = mPassphrase == null;
        return mPassphrase != null;
    }

    @Override
    public boolean promptYesNo(String message) {
        if( mForceYes ) { return true; }
        String in = waitForInput( new PromptParams(mTerminalId, message, PromptParams.PromptType.BooleanType) );
        mShouldCancelInput = in == null;
        return in != null && ( in.equalsIgnoreCase("Y") || in.equalsIgnoreCase("yes") );
    }

    @Override
    public void showMessage(String message) {
        try { mSessionWS.getBasicRemote().sendText(message); }
        catch(IOException ignore) {}
    }
}
