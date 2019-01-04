package com.duol.apk;

import android.app.Service;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import java.util.Timer;
import java.util.TimerTask;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import android.content.Intent;

public class SensorActivity extends Service implements SensorEventListener{

    private Sensor mySensor;
    private SensorManager SM;
    public Boolean inverted = false;

    private static double THRESHOLD = 1.5;
    private static int COOLDOWN = 250;

    private Boolean sendEnabled = true;

    private Timer timer;
    private TimerTask timerTaskCD;

    @Override
    public void onCreate() {
        super.onCreate();

        // Create our Sensor Manager
        SM = (SensorManager)getSystemService(SENSOR_SERVICE);

        // Accelerometer Sensor
        mySensor = SM.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        // Register sensor Listener
        SM.registerListener(this, mySensor, SensorManager.SENSOR_DELAY_NORMAL);

        timer = new Timer();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not in use
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if(event.values[0] < -THRESHOLD)
        {
            if(!inverted) {
                sendDirection(1);
            }
            else {
                sendDirection(3);
            }

        }
        if(event.values[2] > THRESHOLD)
        {
            if(!inverted) {
                sendDirection(2);
            }
            else {
                sendDirection(4);
            }
        }
        if(event.values[0] > THRESHOLD)
        {
            if(!inverted) {
                sendDirection(3);
            }
            else {
                sendDirection(1);
            }
        }
        if(event.values[2] < -THRESHOLD)
        {
            if(!inverted) {
                sendDirection(4);
            }
            else {
                sendDirection(2);
            }
        }


    }

    public void sendDirection(int direction){
        if(sendEnabled){
            Intent returnIntent = new Intent("result");
            returnIntent.putExtra("result", direction);
            LocalBroadcastManager.getInstance(this).sendBroadcast(returnIntent);
            coolDown();
        }

        //Log.d("help", "help");
    }

    public void coolDown(){
        sendEnabled = false;
        timerTaskCD = new TimerTask() {

            @Override
            public void run() {
                sendEnabled = true;
            }
        };

        timer.schedule(timerTaskCD, COOLDOWN);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        SM.unregisterListener(this);

    }
    //commited at 11:53
}