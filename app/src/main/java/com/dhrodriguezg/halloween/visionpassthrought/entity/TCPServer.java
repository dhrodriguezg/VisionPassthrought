package com.dhrodriguezg.halloween.visionpassthrought.entity;

import android.app.Activity;
import android.util.Log;

import com.dhrodriguezg.halloween.visionpassthrought.MainActivity;
import com.dhrodriguezg.halloween.visionpassthrought.R;

import org.jboss.netty.buffer.ChannelBuffer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer {

    private static final String TAG = "TCPServer";
    private static final int STREAM_UP_PORT = 7775;
    private static final int STREAM_DOWN_PORT = 7776;
    private static final int CONTROLLER_PORT = 7777;

    private ServerSocket streamupServerSocket = null;
    private ServerSocket streamdownServerSocket = null;
    private ServerSocket controllerServerSocket = null;

    private boolean serverOnline = false;

    private Socket streamDownSocket = null;
    private ObjectInputStream objectStreamDownInput = null;
    private ObjectOutputStream objectStreamDownOutput = null;
    private boolean isTransferingStreamDown = false;
    private boolean streamDownOnline = false;

    private Socket streamUpSocket = null;
    private ObjectInputStream objectStreamUpInput = null;
    private ObjectOutputStream objectStreamUpOutput = null;
    private boolean isTransferingStreamUp = false;
    private boolean streamUpOnline = false;

    private Socket controllerSocket = null;
    private ObjectInputStream objectControllerInput = null;
    private ObjectOutputStream objectControllerOutput = null;
    private boolean isTransferingController = false;
    private boolean controllerOnline = false;

    private ChannelBuffer downImageBuffer = null;
    private ChannelBuffer upImageBuffer = null;
    private int controllerBuffer;

    private long startingStreamDownTime = 0;
    private long startingStreamUpTime = 0;
    private long startingControllerTime = 0;

    private Activity activity;

    public TCPServer(Activity activity){
        this.activity=activity;
        serverOnline = true;
    }

    public void initStreamService(){
        Thread threadStreamUp = new Thread() {
            public void run() {
                try {
                    streamupServerSocket = new ServerSocket(STREAM_UP_PORT);
                    while(serverOnline){
                        streamUpSocket = streamupServerSocket.accept(); // This is blocking. It will wait.
                        objectStreamUpOutput = new ObjectOutputStream(streamUpSocket.getOutputStream());
                        objectStreamUpInput = new ObjectInputStream(streamUpSocket.getInputStream());
                        streamUpSocket.setKeepAlive(true);
                        streamUpOnline = true;
                        transferUpImage();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error on stream up");
                    streamUpOnline = false;
                    e.printStackTrace();
                }
            }
        };
        threadStreamUp.start();

        Thread threadStreamDown = new Thread() {
            public void run() {
                try {
                    streamdownServerSocket = new ServerSocket(STREAM_DOWN_PORT);
                    while(serverOnline){
                        streamDownSocket = streamdownServerSocket.accept(); // This is blocking. It will wait.
                        objectStreamDownOutput = new ObjectOutputStream(streamDownSocket.getOutputStream());
                        objectStreamDownInput = new ObjectInputStream(streamDownSocket.getInputStream());
                        streamDownSocket.setKeepAlive(true);
                        streamDownOnline = true;
                        transferDownImage();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error on stream down");
                    streamDownOnline = false;
                    e.printStackTrace();
                }
            }
        };
        threadStreamDown.start();

    }

    public void initControllerService(){
        Thread thread = new Thread() {
            public void run() {

                try {
                    controllerServerSocket = new ServerSocket(CONTROLLER_PORT);
                    while(serverOnline){
                        controllerSocket = controllerServerSocket.accept(); // This is blocking. It will wait.
                        objectControllerOutput = new ObjectOutputStream(controllerSocket.getOutputStream());
                        objectControllerInput = new ObjectInputStream(controllerSocket.getInputStream());
                        controllerSocket.setKeepAlive(true);
                        controllerOnline = true;
                        transferController();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error on controller");
                    controllerOnline = false;
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    public ChannelBuffer getUpImageBuffer(){
        return upImageBuffer;
    }

    public boolean updateDownImage(ChannelBuffer imageBuffer){
        if(isTransferingStreamDown)
            return false; //Cannot send right now, busy.
        downImageBuffer = imageBuffer;
        return true;
    }

    public boolean updateController(int code){
        if(isTransferingController)
            return false; //Cannot send right now, busy.
        controllerBuffer = code;
        return true;
    }

    private void transferDownImage( ){

        while (serverOnline){ //while the app is running, keep trying.

            try {

                //waiting for image to send
                while(downImageBuffer == null){
                    Thread.sleep(10);
                }

                if(!streamDownOnline)
                    return;

                isTransferingStreamDown = true;
                startingStreamDownTime = System.currentTimeMillis();
                objectStreamDownOutput.writeObject(downImageBuffer);
                objectStreamDownOutput.flush();
                isTransferingStreamDown = false;

                downImageBuffer = null;
            } catch (IOException e) {
                streamDownOnline = false;
                Log.i(TAG,e.getMessage());
            } catch (InterruptedException e) {
                streamDownOnline = false;
                Log.i(TAG, e.getMessage());
            }
        }

    }

    private void transferUpImage( ){

        while (serverOnline){ //while the app is running, keep trying.

            try {

                if(!streamUpOnline)
                    return;

                startingStreamUpTime = System.currentTimeMillis();

                while(objectStreamUpInput.available()==0){//waiting for next image
                    Thread.sleep(5);
                }

                isTransferingStreamUp = true;
                upImageBuffer=(ChannelBuffer) objectStreamUpInput.readObject();
                isTransferingStreamUp = false;

            } catch (IOException e) {
                streamUpOnline = false;
                Log.i(TAG,e.getMessage());
            } catch (ClassNotFoundException e) {
                streamUpOnline = false;
                Log.i(TAG, e.getMessage());
            } catch (InterruptedException e) {
                streamUpOnline = false;
                Log.i(TAG, e.getMessage());
            }
        }

    }

    public void transferController(){

        while (serverOnline){
            try {

                if(!controllerOnline)
                    return;

                while(objectControllerInput.available()==0){ //waiting for data
                    Thread.sleep(5);
                }

                isTransferingController = true;

                switch (objectControllerInput.readInt()) {
                    case -1:
                        //close all connections
                        objectControllerOutput.writeInt(0);
                        objectControllerOutput.flush();
                        shutdown();
                        break;
                    case 1:
                        //send server meta
                        objectControllerOutput.writeObject( MainActivity.PREFERENCES.get(activity.getString(R.string.comm_config_name)) );
                        objectControllerOutput.flush();
                        break;
                    case 2:
                        //use heart animation
                        objectControllerOutput.writeInt(0);
                        objectControllerOutput.flush();
                        break;
                    default:
                        //confirm anything
                        objectControllerOutput.writeInt(0);
                        objectControllerOutput.flush();
                        break;
                }

                isTransferingController = false;

            } catch (IOException e) {
                controllerOnline = false;
                Log.i(TAG,e.getMessage());
            } catch (InterruptedException e) {
                controllerOnline = false;
                Log.i(TAG, e.getMessage());
            }
        }

    }

    private void checkConnection(){
        Thread thread = new Thread() {
            public void run() {
                while(serverOnline){
                    try {
                        Thread.sleep(2000);
                        if(System.currentTimeMillis()- startingStreamDownTime > 4000){
                            if(streamDownOnline)
                                resetStreaming();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();

    }

    private void resetStreaming(){
        try {
            if(!controllerSocket.isClosed())
                controllerSocket.close();
            if(!streamDownSocket.isClosed())
                streamDownSocket.close();
            if(!streamUpSocket.isClosed())
                streamUpSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown(){
        serverOnline = false;
        try {
            streamupServerSocket.close();
        } catch (IOException e) {}
        try {
            streamdownServerSocket.close();
        } catch (IOException e) {}
        try {
            controllerServerSocket.close();
        } catch (IOException e) {}
    }


    public boolean isServerOnline() {
        return serverOnline;
    }


    public void setServerOnline(boolean serverOnline) {
        this.serverOnline = serverOnline;
    }

    public boolean isStreamDownOnline() {
        return streamDownOnline;
    }

    public void setStreamDownOnline(boolean clientOnline) {
        this.streamDownOnline = clientOnline;
    }

    public boolean isControllerOnline() {
        return controllerOnline;
    }

    public void setControllerOnline(boolean controllerOnline) {
        this.controllerOnline = controllerOnline;
    }

    public boolean isTransferingController() {
        return isTransferingController;
    }

    public void setTransferingController(boolean isTransferingController) {
        this.isTransferingController = isTransferingController;
    }

}