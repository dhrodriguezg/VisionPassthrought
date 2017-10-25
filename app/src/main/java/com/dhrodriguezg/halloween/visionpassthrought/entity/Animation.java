package com.dhrodriguezg.halloween.visionpassthrought.entity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;

import com.dhrodriguezg.halloween.visionpassthrought.R;

/**
 * Created by Diego Rodriguez on 24/10/2017.
 */

public class Animation {

    private Activity activity;
    private int frame = 0;
    private Bitmap[] animation = null;
    private MediaPlayer soundPlayback = null;

    public Animation(Activity activity){
        this.activity = activity;
    }

    public void setAnimation(String name){
        if(name.equals("heart")){

            //taken from http://soundbible.com/1001-Heartbeat.html
            soundPlayback = MediaPlayer.create(activity.getApplicationContext() , R.raw.heartbeat);//R.music
            soundPlayback.setLooping(true);
            soundPlayback.setVolume(1f,1f);

            animation = new Bitmap[7];
            animation[0] = BitmapFactory.decodeResource(activity.getResources(),R.raw.heart_0);
            animation[1] = BitmapFactory.decodeResource(activity.getResources(),R.raw.heart_1);
            animation[2] = BitmapFactory.decodeResource(activity.getResources(),R.raw.heart_2);
            animation[3] = BitmapFactory.decodeResource(activity.getResources(),R.raw.heart_3);
            animation[4] = BitmapFactory.decodeResource(activity.getResources(),R.raw.heart_4);
            animation[5] = BitmapFactory.decodeResource(activity.getResources(),R.raw.heart_5);
            animation[6] = BitmapFactory.decodeResource(activity.getResources(),R.raw.heart_6);
        }else{
            //other animations
        }
    }

    public void startAnimation(){
        soundPlayback.start();
    }

    public void stopAnimation(){
        soundPlayback.stop();
    }

    public Bitmap nextFrame(){
        if(frame > animation.length - 1)
            frame=0;
        Bitmap bitmap = animation[frame];
        frame++;
        return bitmap;
    }

}
