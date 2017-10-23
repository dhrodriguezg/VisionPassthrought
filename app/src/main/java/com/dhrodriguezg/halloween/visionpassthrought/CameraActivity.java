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
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;


import java.util.ArrayList;
import java.util.List;

import com.dhrodriguezg.halloween.visionpassthrought.entity.TCPClient;
import com.dhrodriguezg.halloween.visionpassthrought.entity.TCPServer;
import com.dhrodriguezg.halloween.visionpassthrought.widget.CustomCameraView;

import org.jboss.netty.buffer.ChannelBuffer;

public class CameraActivity extends Activity {

	private static final String TAG = "CameraActivity";

    private CustomCameraView localView;
    private ImageView remoteView;
    private Spinner spinnerResolution;
    private boolean running = true;
    private Switch cameraSwitch;
    private TCPClient tcpClient;
    private TCPServer tcpServer;
    private ChannelBuffer remoteImageBuffer = null;

    public CameraActivity() {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {

    	Intent intent = getIntent();
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);

        if(MainActivity.PREFERENCES.getProperty(getString(R.string.comm_mode_name)).equals(getString(R.string.comm_server_name))){
            tcpServer = new TCPServer(this);
            tcpServer.initControllerService();
            tcpServer.initStreamService();
            tcpClient = null;
        } else {
            tcpServer = null;
            tcpClient = new TCPClient(this);
            tcpClient.initControllerService();
            tcpClient.initStreamService();
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
        localView.setResolution(640, 360);

        remoteView = (ImageView) findViewById(R.id.imageRemote);
        remoteView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        updateResolutions(localView.selectCamera(0));

        Thread threadTarget = new Thread(){
            public void run(){
                try {


                    while(running){

                        while(!localView.hasImageChanged()){
                            Thread.sleep(1);
                        }

                        if(tcpServer != null){//
                            tcpServer.updateDownImage(localView.getImage());
                            remoteImageBuffer=tcpServer.getUpImageBuffer();
                        }
                        if(tcpClient != null){
                            tcpClient.updateUpImage(localView.getImage());
                            tcpClient.updateController(1);
                            remoteImageBuffer=tcpClient.getDownImageBuffer();
                        }


                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //byte[] data = localView.getImage().array();
                                //remoteView.setImageBitmap(BitmapFactory.decodeByteArray(data, localView.getImage().arrayOffset(), localView.getImage().readableBytes()));
                                if(remoteImageBuffer != null){
                                    byte[] data = remoteImageBuffer.array();
                                    remoteView.setImageBitmap(BitmapFactory.decodeByteArray(data, remoteImageBuffer.arrayOffset(), remoteImageBuffer.readableBytes()));

                                }
                            }
                        });

                        localView.setImageChanged(false);
                        Thread.sleep(10);
                    }
                } catch (InterruptedException e) {
                    e.getStackTrace();
                }

            }
        };
        threadTarget.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        running=true;
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    }
    
    @Override
    public void onDestroy() {
        running=false;
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

}
