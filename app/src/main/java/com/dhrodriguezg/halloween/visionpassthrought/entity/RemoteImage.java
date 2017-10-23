package com.dhrodriguezg.halloween.visionpassthrought.entity;

import android.graphics.BitmapFactory;

import org.jboss.netty.buffer.ChannelBuffer;
import java.io.Serializable;

public class RemoteImage implements Serializable {

    private static final String TAG = "RemoteImage";

    private int offset, length;
    byte[] data;

    public RemoteImage(ChannelBuffer imageBuffer){
        setImageBuffer(imageBuffer);
    }

    public void setImageBuffer(ChannelBuffer imageBuffer){
        data=imageBuffer.array();
        offset=imageBuffer.arrayOffset();
        length=imageBuffer.readableBytes();
    }

    public byte[] array() {
        return data;
    }

    public int arrayOffset() {
        return offset;
    }

    public int readableBytes() {
        return length;
    }



}
