package com.dhrodriguezg.halloween.visionpassthrought.entity;

import java.io.Serializable;

import javax.net.ssl.HttpsURLConnection;

public class Server implements Serializable {

    private static final long serialVersionUID = -5399605122490343339L;
    private static final String TAG = "Server";

    private String ip;
    private String name;
    private int streamUpPort;
    private int streamDownPort;
    private int controlPort;

    public Server() {
    }

    public Server(String ip, String name, int streamUpPort, int streamDownPort, int controlPort) {
        this.ip = ip;
        this.name = name;
        this.streamUpPort = streamUpPort;
        this.streamDownPort = streamDownPort;
        this.controlPort = controlPort;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStreamUpPort() {
        return streamUpPort;
    }

    public void setStreamUpPort(int streamUpPort) {
        this.streamUpPort = streamUpPort;
    }

    public int getStreamDownPort() {
        return streamDownPort;
    }

    public void setStreamDownPort(int streamDownPort) {
        this.streamDownPort = streamDownPort;
    }

    public int getControlPort() {
        return controlPort;
    }

    public void setControlPort(int controlPort) {
        this.controlPort = controlPort;
    }
}
