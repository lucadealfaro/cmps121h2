package com.dealfaro.luca.clicker;

import com.google.gson.annotations.SerializedName;

/**
 * Created by logan6694 on 4/26/15.
 */
public class Message {

    @SerializedName("msg")
    String msg;

    @SerializedName("msgid")
    String msgid;

    @SerializedName("ts")
    String ts;

    public String getMsg() {
        return msg;
    }

    public String getMsgid() {
        return msgid;
    }

    public String getTs() {
        return ts;
    }
}
