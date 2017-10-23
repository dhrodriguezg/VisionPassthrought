package com.dhrodriguezg.halloween.visionpassthrought.entity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import android.app.Activity;
import android.util.Log;

import com.dhrodriguezg.halloween.visionpassthrought.MainActivity;
import com.dhrodriguezg.halloween.visionpassthrought.R;

public class TCPClient {

    private static final String TAG = "TCPClient";
    private static final int STREAM_UP_PORT = 7775;
    private static final int STREAM_DOWN_PORT = 7776;
    private static final int CONTROLLER_PORT = 7777;

    private boolean serverOnline = false;

    private Socket streamDownSocket = null;
    private ObjectInputStream objectStreaDownInput = null;
    private ObjectOutputStream objectStreamDownOutput = null;
    private boolean isTransferingStreamDown = false;
    private boolean streamDownOnline = false;

    private Socket streamUpSocket = null;
    private ObjectInputStream objectStreaUpInput = null;
    private ObjectOutputStream objectStreamUpOutput = null;
    private boolean isTransferingStreamUp = false;
    private boolean streamUpOnline = false;

    private Socket controllerSocket = null;
    private ObjectInputStream objectControllerInput = null;
    private ObjectOutputStream objectControllerOutput = null;
    private boolean isTransferingController = false;
    private boolean controllerOnline = false;

    private RemoteImage downImageBuffer = null;
    private RemoteImage upImageBuffer = null;
    private int controllerBuffer;

    private long startingStreamDownTime = 0;
    private long startingStreamUpTime = 0;
    private long startingControllerTime = 0;

    private Activity activity;
    private Server serverInfo;


    public TCPClient(Activity activity){
        this.activity=activity;
        serverInfo = (Server) MainActivity.PREFERENCES.get(activity.getString(R.string.comm_config_name));
        serverOnline = true;
    }

    public void initStreamService(){
        Thread threadStreamUp = new Thread() {
            public void run() {
                try {

                    while(serverOnline){
                        Log.i(TAG, "UP is up");
                        streamUpSocket = new Socket(serverInfo.getIp(), STREAM_UP_PORT);
                        objectStreamUpOutput = new ObjectOutputStream(streamUpSocket.getOutputStream());
                        objectStreaUpInput = new ObjectInputStream(streamUpSocket.getInputStream());
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
                    while(serverOnline){
                        Log.i(TAG, "Down is up");
                        streamDownSocket = new Socket(serverInfo.getIp(), STREAM_DOWN_PORT);
                        objectStreamDownOutput = new ObjectOutputStream(streamDownSocket.getOutputStream());
                        objectStreaDownInput = new ObjectInputStream(streamDownSocket.getInputStream());
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
                    while(serverOnline){
                        Log.i(TAG, "Contr is up");
                        controllerSocket = new Socket(serverInfo.getIp(), CONTROLLER_PORT);
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

    public boolean updateUpImage(RemoteImage imageBuffer){
        if(isTransferingStreamUp)
            return false; //Cannot send right now, busy.
        upImageBuffer = imageBuffer;
        return true;
    }

    public RemoteImage getDownImageBuffer(){
        return downImageBuffer;
    }

    public boolean updateController(int code){
        controllerBuffer = code;
        return true;
    }

    private void transferUpImage( ){

        Log.i(TAG, "Up.0");

        while (serverOnline){ //while the app is running, keep trying.

            try {

                Log.i(TAG, "Up.1");


                //waiting for image to send
                while(upImageBuffer == null){
                    Thread.sleep(10);
                }

                Log.i(TAG, "Up.2");

                if(!streamUpOnline)
                    return;

                Log.i(TAG, "Up.3");

                isTransferingStreamUp = true;
                startingStreamUpTime = System.currentTimeMillis();
                objectStreamUpOutput.writeObject(upImageBuffer);
                objectStreamUpOutput.flush();
                isTransferingStreamUp = false;

                Log.i(TAG, "Up.4");
                upImageBuffer = null;
            } catch (IOException e) {
                streamUpOnline = false;
                Log.i(TAG,e.getMessage());
            } catch (InterruptedException e) {
                e.printStackTrace();
                streamUpOnline = false;
                Log.i(TAG, e.getMessage());
            }
        }

    }

    private void transferDownImage( ){

        while (serverOnline){ //while the app is running, keep trying.

            try {

                if(!streamDownOnline)
                    return;

                startingStreamDownTime = System.currentTimeMillis();

                while(objectStreaDownInput.available()==0){//waiting for next image
                    Thread.sleep(5);
                }

                isTransferingStreamDown = true;
                downImageBuffer=(RemoteImage) objectStreaDownInput.readObject();
                isTransferingStreamDown = false;

            } catch (IOException e) {
                streamDownOnline = false;
                Log.i(TAG,e.getMessage());
            } catch (ClassNotFoundException e) {
                streamDownOnline = false;
                Log.i(TAG, e.getMessage());
            } catch (InterruptedException e) {
                streamDownOnline = false;
                Log.i(TAG, e.getMessage());
            }
        }

    }

    public void transferController(){

        while (serverOnline){
            try {


                if(!controllerOnline)
                    return;

                isTransferingController = true;

                Log.i(TAG, "updating controller");

                int controller_code=controllerBuffer;
                objectControllerOutput.writeInt(controller_code);
                objectControllerOutput.flush();

                while(objectControllerInput.available()==0){ //waiting for data
                    Thread.sleep(1);
                }

                switch (controller_code) {
                    case -1:
                        //close all connections
                        objectControllerInput.readInt();
                        shutdown();
                        break;
                    case 1:
                        //send server meta
                        serverInfo = (Server) objectControllerInput.readObject();
                        MainActivity.PREFERENCES.put(activity.getString(R.string.comm_config_name), serverInfo);
                        Log.i(TAG,serverInfo.getIp());
                        break;
                    case 2:
                        //use heart animation
                        objectControllerInput.readInt();
                        break;
                    default:
                        //confirm anything
                        objectControllerInput.readInt();
                        break;
                }

                isTransferingController = false;
                controllerBuffer=0;
                Thread.sleep(100);

            } catch (IOException e) {
                controllerOnline = false;
                Log.i(TAG,e.getMessage());
            } catch (InterruptedException e) {
                controllerOnline = false;
                Log.i(TAG, e.getMessage());
            } catch (ClassNotFoundException e) {
                controllerOnline = false;
                Log.i(TAG,e.getLocalizedMessage());
                Log.i(TAG, e.getException().getMessage());
            }
        }
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
            streamDownSocket.close();
        } catch (IOException e) {}
        try {
            streamUpSocket.close();
        } catch (IOException e) {}
        try {
            controllerSocket.close();
        } catch (IOException e) {}
    }
}