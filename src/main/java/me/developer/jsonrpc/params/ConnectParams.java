package me.developer.jsonrpc.params;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

import com.google.gson.JsonPrimitive;

public class ConnectParams {
    public class TerminalSize {
        public JsonPrimitive cols;
        public JsonPrimitive rows;
    }

    public JsonPrimitive terminalId;
    public JsonPrimitive user;
    public JsonPrimitive host;
    public JsonPrimitive port;
    public JsonPrimitive strictHostCheck;
    public JsonPrimitive timeout;
    public JsonPrimitive encoding; // charset name
    public TerminalSize  size;

    public boolean isValid() {
        return (terminalId != null && terminalId.isString())
            && (user != null && user.isString())
            && (host == null || host.isString())
            && (port == null || port.isNumber())
            && (strictHostCheck == null || strictHostCheck.isBoolean())
            && (timeout == null || timeout.isNumber())
            && (encoding == null || encoding.isString())
            && (size == null || (size.cols != null && size.cols.isNumber() && size.rows != null && size.rows.isNumber()));
    }

    public String getTermianlId() { return terminalId.getAsString(); }
    public String getUser() { return user.getAsString(); }
    public String getHost() { return host == null ? "localhost" : host.getAsString(); }
    public int    getPort() {
        if( port != null ) {
            try {
                int val = port.getAsInt();
                if( val > 0 && val < 65536 ) { return val; }
            } catch(NumberFormatException ignore) {}
        }
        return 22;
    }
    public boolean getStrictHostCheck() { return strictHostCheck == null ? true : strictHostCheck.getAsBoolean(); }
    public int     getTimeout() { 
        if( timeout != null ) {
            try {
                int val = timeout.getAsInt();
                if( val > 0 ) {
                    return val;
                }
            }catch(NumberFormatException ignore) {}
        }
        return 5000; // default
    }
    public Charset getEncodingCharset() { 
        if(encoding != null) {
            try {
                return Charset.forName(encoding.getAsString());
            }
            catch(IllegalCharsetNameException icne) {

            }
            catch(UnsupportedCharsetException uce) {

            }
        }
        return StandardCharsets.UTF_8;
    }
    public Integer getTermainlCols() {
        return size == null ? null : size.cols.getAsInt();
    }
    public Integer getTermainlRows() {
        return size == null ? null : size.rows.getAsInt();
    }
}
