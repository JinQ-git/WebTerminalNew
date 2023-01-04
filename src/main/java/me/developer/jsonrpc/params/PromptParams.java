package me.developer.jsonrpc.params;

import com.google.gson.annotations.SerializedName;

public class PromptParams {
    public enum PromptType {
        @SerializedName("boolean")  BooleanType,
        @SerializedName("string")   StringType,
        @SerializedName("password") PasswordType
    }

    public String terminalId;
    public String message;
    public PromptType type;

    public PromptParams(String tid, String msg, PromptType typ) {
        terminalId = tid;
        message = msg;
        type = typ;
    }
}
