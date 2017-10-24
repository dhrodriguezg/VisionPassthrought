package com.dhrodriguezg.halloween.visionpassthrought.entity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
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

    public void initServices(){
        Thread threadStreamUp = new Thread() {
            public void run() {
                try {

                    while(serverOnline){
                        Log.i(TAG,"Connecting to streamup");
                        streamUpSocket = new Socket(serverInfo.getIp(), STREAM_UP_PORT);
                        Log.i(TAG,"Connected to streamup");
                        objectStreamUpOutput = new ObjectOutputStream(streamUpSocket.getOutputStream());
                        objectStreaUpInput = new ObjectInputStream(streamUpSocket.getInputStream());
                        streamUpSocket.setKeepAlive(true);
                        streamUpOnline = true;
                        transferUpImage();
                    }
                } catch (IOException e) {
                    streamUpOnline = false;
                    printException(e);
                }
            }
        };
        threadStreamUp.start();

        Thread threadStreamDown = new Thread() {
            public void run() {
                try {
                    while(serverOnline){
                        Log.i(TAG,"Connecting to streamdown");
                        streamDownSocket = new Socket(serverInfo.getIp(), STREAM_DOWN_PORT);
                        Log.i(TAG,"Connected to streamdown");
                        objectStreamDownOutput = new ObjectOutputStream(streamDownSocket.getOutputStream());
                        objectStreaDownInput = new ObjectInputStream(streamDownSocket.getInputStream());
                        streamDownSocket.setKeepAlive(true);
                        streamDownOnline = true;
                        transferDownImage();
                    }
                } catch (IOException e) {
                    streamDownOnline = false;
                    printException(e);
                }
            }
        };
        threadStreamDown.start();

        Thread threadController = new Thread() {
            public void run() {

                try {
                    while(serverOnline){
                        Log.i(TAG,"Connecting to controller");
                        controllerSocket = new Socket(serverInfo.getIp(), CONTROLLER_PORT);
                        Log.i(TAG,"Connected to controller");
                        objectControllerOutput = new ObjectOutputStream(controllerSocket.getOutputStream());
                        objectControllerInput = new ObjectInputStream(controllerSocket.getInputStream());
                        controllerSocket.setKeepAlive(true);
                        controllerOnline = true;
                        transferController();
                    }
                } catch (IOException e) {
                    controllerOnline = false;
                    printException(e);
                }
            }
        };
        threadController.start();

    }

    public boolean updateUpImage(RemoteImage imageBuffer){
        if(isTransferingStreamUp)
            return false; //Cannot update it right now, currently sending.
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

        while (serverOnline){ //while the app is running, keep trying.

            try {

                if(!streamUpOnline)
                    return; // there was an exception in previous connections, cancel.

                //waiting for image to send
                while(upImageBuffer == null){
                    Thread.sleep(10);
                }

                isTransferingStreamUp = true;
                objectStreamUpOutput.writeObject(upImageBuffer);
                objectStreamUpOutput.flush();
                upImageBuffer = null;
                isTransferingStreamUp = false;

            } catch (IOException e) {
                streamUpOnline = false;
                printException(e);
            } catch (InterruptedException e) {
                streamUpOnline = false;
                printException(e);
            }
        }
    }

    private void transferDownImage( ){

        while (serverOnline){ //while the app is running, keep trying.

            try {

                if(!streamDownOnline)
                    return; // there was an exception in previous connections, cancel.

                isTransferingStreamDown = true;
                downImageBuffer=(RemoteImage) objectStreaDownInput.readObject();
                isTransferingStreamDown = false;

            } catch (IOException e) {
                streamDownOnline = false;
                printException(e);
            } catch (ClassNotFoundException e) {
                streamDownOnline = false;
                printException(e);
            }
        }
    }

    public void transferController(){

        while (serverOnline){
            try {

                if(!controllerOnline)
                    return; // there was an exception in previous connections, cancel.

                isTransferingController = true;

                int controller_code=controllerBuffer;
                objectControllerOutput.writeInt(controller_code);
                objectControllerOutput.flush();

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
                printException(e);
            } catch (InterruptedException e) {
                controllerOnline = false;
                printException(e);
            } catch (ClassNotFoundException e) {
                controllerOnline = false;
                printException(e);
            }
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

    private void printException(Exception e){
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        Log.e(TAG, errors.toString());
    }

}