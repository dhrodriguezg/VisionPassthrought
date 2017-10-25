package com.dhrodriguezg.halloween.visionpassthrought;

import android.app.Activity;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;

import com.dhrodriguezg.halloween.visionpassthrought.entity.Animation;
import com.dhrodriguezg.halloween.visionpassthrought.widget.CustomCameraView;

import java.util.ArrayList;
import java.util.List;

public class SingleCameraActivity extends Activity {

	private static final String TAG = "SingleCameraActivity";

    private int cWidth = 1920;
    private int cHeight = 1080;
    private int nCamera = 0;
    private CustomCameraView localView;
    private ImageView animationView;
    private Spinner spinnerResolution;
    private boolean isAppRunning = true;
    private Button nextCamera;
    private Animation animation = null;

    public SingleCameraActivity() {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_singlecamera);

        nextCamera = (Button) findViewById(R.id.next_camera_btn);
        nextCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nCamera++;
                if (localView.getCamera().getNumberOfCameras() > 1)
                    nCamera = nCamera % localView.getCamera().getNumberOfCameras();
                localView.selectCamera(nCamera);
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
        animationView = (ImageView) findViewById(R.id.imageAnimation);
        animationView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        localView = (CustomCameraView) findViewById(R.id.imageLocal);
        localView.setResolution(cWidth, cHeight);

        nCamera = localView.getCamera().getNumberOfCameras()-1;

        animation = new Animation(this);
        animation.setAnimation("heart");

        updateResolutions(localView.selectCamera(nCamera));
        updateAnimation();

    }

    @Override
    public void onResume() {
        super.onResume();
        isAppRunning = true;
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    }
    
    @Override
    public void onDestroy() {
        isAppRunning = false;
        if(animation != null)
            animation.stopAnimation();
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

    private void updateAnimation(){
        Thread threadUpdateAnimation = new Thread(){
            public void run(){
                try {
                    animation.startAnimation();
                    while(isAppRunning){
                        Thread.sleep(100);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                animationView.setImageBitmap(animation.nextFrame());

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