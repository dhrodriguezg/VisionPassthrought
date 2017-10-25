package com.dhrodriguezg.halloween.visionpassthrought.entity;

import android.graphics.BitmapFactory;

import org.jboss.netty.buffer.ChannelBuffer;
import java.io.Serializable;

public class RemoteImage implements Serializable {

    private static final String TAG = "RemoteImage";
    private boolean isFlipped;
    private int offset, length;
    byte[] data;

    public RemoteImage(ChannelBuffer imageBuffer){
        setImageBuffer(imageBuffer);
    }

    public void setImageBuffer(ChannelBuffer imageBuffer){
        isFlipped = false;
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

    public boolean isFlipped() {
        return isFlipped;
    }

    public void setFlipped(boolean flipped) {
        isFlipped = flipped;
    }

}
