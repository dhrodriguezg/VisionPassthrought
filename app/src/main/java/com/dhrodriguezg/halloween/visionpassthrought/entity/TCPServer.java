package com.dhrodriguezg.halloween.visionpassthrought.entity;

import android.app.Activity;
import android.util.Log;

import com.dhrodriguezg.halloween.visionpassthrought.MainActivity;
import com.dhrodriguezg.halloween.visionpassthrought.R;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
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

    private RemoteImage downImageBuffer = null;
    private RemoteImage upImageBuffer = null;
    private int controllerBuffer;

    private Activity activity;

    public TCPServer(Activity activity){
        this.activity=activity;
        serverOnline = true;
    }

    public void initServices(){
        Thread threadStreamUp = new Thread() {
            public void run() {
                try {
                    streamupServerSocket = new ServerSocket(STREAM_UP_PORT);
                    while(serverOnline){
                        Log.i(TAG,"Waiting for new connections to streamup");
                        streamUpSocket = streamupServerSocket.accept(); // This is blocking. It will wait.
                        Log.i(TAG,"New client connected to streamup");
                        objectStreamUpOutput = new ObjectOutputStream(streamUpSocket.getOutputStream());
                        objectStreamUpInput = new ObjectInputStream(streamUpSocket.getInputStream());
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
                    streamdownServerSocket = new ServerSocket(STREAM_DOWN_PORT);
                    while(serverOnline){
                        Log.i(TAG,"Waiting for new connections to streamdown");
                        streamDownSocket = streamdownServerSocket.accept(); // This is blocking. It will wait.
                        Log.i(TAG,"New client connected to streamdown");
                        objectStreamDownOutput = new ObjectOutputStream(streamDownSocket.getOutputStream());
                        objectStreamDownInput = new ObjectInputStream(streamDownSocket.getInputStream());
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
                    controllerServerSocket = new ServerSocket(CONTROLLER_PORT);
                    while(serverOnline){
                        Log.i(TAG,"Waiting for new connections to controller");
                        controllerSocket = controllerServerSocket.accept(); // This is blocking. It will wait.
                        Log.i(TAG,"New client connected to controller");
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

    public RemoteImage getUpImageBuffer(){
        return upImageBuffer;
    }

    public boolean updateDownImage(RemoteImage imageBuffer){
        if(isTransferingStreamDown)
            return false; //Cannot update it right now, currently sending.
        downImageBuffer = imageBuffer;
        return true;
    }

    public boolean updateController(int code){
        if(isTransferingController)
            return false; //Cannot update it right now, currently sending.
        controllerBuffer = code;
        return true;
    }

    private void transferDownImage( ){

        while (serverOnline){ //while the app is running, keep trying.

            try {

                if(!streamDownOnline)
                    return; // there was an exception in previous connections, cancel.

                //waiting for image to send
                while(downImageBuffer == null){
                    Thread.sleep(10);
                }

                isTransferingStreamDown = true;
                objectStreamDownOutput.writeObject(downImageBuffer);
                objectStreamDownOutput.flush();
                downImageBuffer = null;
                isTransferingStreamDown = false;

            } catch (IOException e) {
                streamDownOnline = false;
                printException(e);
            } catch (InterruptedException e) {
                streamDownOnline = false;
                printException(e);
            }
        }
    }

    private void transferUpImage( ){

        while (serverOnline){ //while the app is running, keep trying.

            try {

                if(!streamUpOnline)
                    return; // there was an exception in previous connections, cancel.

                isTransferingStreamUp = true;
                upImageBuffer = (RemoteImage)objectStreamUpInput.readObject();
                isTransferingStreamUp = false;

            } catch (IOException e) {
                streamUpOnline = false;
                printException(e);
            } catch (ClassNotFoundException e) {
                streamUpOnline = false;
                printException(e);
            }
        }
    }

    public void transferController(){

        while (serverOnline){
            try {

                if(!controllerOnline)
                    return;

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
                printException(e);
            }
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

    private void printException(Exception e){
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        Log.e(TAG, errors.toString());
    }

}