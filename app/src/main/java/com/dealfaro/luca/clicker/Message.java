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

    @SerializedName("dest")
    String dest;

    @SerializedName("conversation")
    Boolean conv;

    @SerializedName("userid")
    String userid;

    public String getMsg() {
        return msg;
    }

    public String getMsgid() {
        return msgid;
    }

    public String getTs() {
        return ts;
    }

    public Boolean getConv() { return conv; }

    public String getDest() { return dest; }

    public String getUserId() { return userid; }
}
