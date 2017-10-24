package com.dhrodriguezg.halloween.visionpassthrought;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.dhrodriguezg.halloween.visionpassthrought.entity.RemoteImage;
import com.dhrodriguezg.halloween.visionpassthrought.entity.TCPClient;
import com.dhrodriguezg.halloween.visionpassthrought.entity.TCPServer;
import com.dhrodriguezg.halloween.visionpassthrought.widget.CustomCameraView;

public class CameraActivity extends Activity {

	private static final String TAG = "CameraActivity";

    private int cWidth = 320;
    private int cHeight = 240;
    private int frame = 0;
    private CustomCameraView localView;
    private ImageView remoteView;
    private ImageView animationView;
    private Spinner spinnerResolution;
    private boolean isAppRunning = true;
    private boolean isUpdatingRemoteImg = false;
    private Switch cameraSwitch;
    private TCPClient tcpClient;
    private TCPServer tcpServer;
    private RemoteImage remoteImageBuffer = null;
    private Bitmap[] animation = null;

    public CameraActivity() {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);

        if(MainActivity.PREFERENCES.getProperty(getString(R.string.comm_mode_name)).equals(getString(R.string.comm_server_name))){
            Log.i(TAG,"I'M THE SERVER");
            tcpServer = new TCPServer(this);
            tcpServer.initServices();
            tcpClient = null;
        } else {
            Log.i(TAG,"I'M A CLIENT");
            tcpClient = new TCPClient(this);
            tcpClient.initServices();
            tcpServer = null;
        }

        cameraSwitch = (Switch) findViewById(R.id.cameraSwitch);
        cameraSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                if (isChecked)
                    localView.selectCamera(0);
                else
                    localView.selectCamera(1);
            }
        });

        spinnerResolution = (Spinner) findViewById(R.id.resolutionSpinner);
        spinnerResolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String[] resolution = ((String) adapterView.getItemAtPosition(i)).split("x");
                localView.setResolution(Integer.parseInt(resolution[0]), Integer.parseInt(resolution[1]));
                localView.updateCamera();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        localView = (CustomCameraView) findViewById(R.id.imageLocal);
        localView.setResolution(cWidth, cHeight);

        remoteView = (ImageView) findViewById(R.id.imageRemote);
        remoteView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        animationView = (ImageView) findViewById(R.id.imageAnimation);
        remoteView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        animation = new Bitmap[7];
        animation[0] = BitmapFactory.decodeResource(getResources(),R.raw.heart_0);
        animation[1] = BitmapFactory.decodeResource(getResources(),R.raw.heart_1);
        animation[2] = BitmapFactory.decodeResource(getResources(),R.raw.heart_2);
        animation[3] = BitmapFactory.decodeResource(getResources(),R.raw.heart_3);
        animation[4] = BitmapFactory.decodeResource(getResources(),R.raw.heart_4);
        animation[5] = BitmapFactory.decodeResource(getResources(),R.raw.heart_5);
        animation[6] = BitmapFactory.decodeResource(getResources(),R.raw.heart_6);

        updateResolutions(localView.selectCamera(0));
        updateStreamImages();
        updateAnimation();

    }

    @Override
    public void onResume() {
        super.onResume();
        isAppRunning =true;
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    }
    
    @Override
    public void onDestroy() {
        isAppRunning =false;
        if(tcpServer != null)
            tcpServer.shutdown();
        if(tcpClient != null)
            tcpClient.shutdown();
        super.onDestroy();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }


    private void updateResolutions(List<Camera.Size> resolutions){
        int index = 0;
        Camera.Size customSize = localView.getPreviewSize();
        final List<String> resolutionList = new ArrayList<String>();

        for(int n=0; n < resolutions.size(); n++){
            Camera.Size resolution = resolutions.get(n);
            resolutionList.add(resolution.width+"x"+resolution.height);
            if( resolution.equals(customSize))
                index = n;
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, resolutionList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final int pos = index;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinnerResolution.setAdapter(adapter);
                spinnerResolution.setSelection(pos);
            }
        });
    }

    private void updateStreamImages(){
        Thread threadUpdateRmtImage = new Thread(){
            public void run(){
                try {

                    while(isAppRunning){

                        while( !localView.hasImageChanged() || isUpdatingRemoteImg){
                            Thread.sleep(2);
                        }

                        if(tcpServer != null){//
                            tcpServer.updateDownImage(new RemoteImage(localView.getImage()));
                            remoteImageBuffer=tcpServer.getUpImageBuffer();
                        }
                        if(tcpClient != null){
                            tcpClient.updateUpImage(new RemoteImage(localView.getImage()));
                            tcpClient.updateController(1);
                            remoteImageBuffer=tcpClient.getDownImageBuffer();
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                isUpdatingRemoteImg=true;
                                if(remoteImageBuffer != null){
                                    byte[] data = remoteImageBuffer.array();
                                    remoteView.setImageBitmap(BitmapFactory.decodeByteArray(data, remoteImageBuffer.arrayOffset(), remoteImageBuffer.readableBytes()));
                                    remoteImageBuffer = null;
                                }
                                isUpdatingRemoteImg=false;
                            }
                        });
                        localView.setImageChanged(false);
                    }
                } catch (InterruptedException e) {
                    e.getStackTrace();
                }

            }
        };
        threadUpdateRmtImage.start();
    }

    private void updateAnimation(){
        Thread threadUpdateAnimation = new Thread(){
            public void run(){
                try {
                    while(isAppRunning){
                        Thread.sleep(100);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                if(frame > animation.length - 1)
                                    frame=0;
                                animationView.setImageBitmap(animation[frame]);
                                frame++;
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    e.getStackTrace();
                }
            }
        };
        threadUpdateAnimation.start();
    }

}