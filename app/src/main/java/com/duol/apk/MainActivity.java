package com.duol.apk;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Random;

public class MainActivity extends Activity{
    private static final String[] REQUIRED_PERMISSIONS =
            new String[] {
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            };
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    private boolean startConfirm = false;
    private Boolean sensorToggled = true;

    private TextView updateDefenseText;
    private TextView defenseStatement;

    private static final Random rand = new Random();

    private Intent sensorIntent;

    Vibrator v;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("result"));

        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE); //initializes vibration

        sensorIntent = new Intent(MainActivity.this, SensorActivity.class);
        startService(sensorIntent);

        //STOPS THIS ACTIVITY AND STARTS ENDLESS MODE
        Button singleplayerButton = findViewById(R.id.startSingleplayer);
        singleplayerButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startSingleplayer();
            }
        });

        //STOPS THIS ACTIVITY AND STARTS MULTIPLAYER MODE
        Button multiplayerButton = findViewById(R.id.startMultiplayer);
        multiplayerButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startMultiplayer();
            }
        });

    }

    //start singleplayer (triggered by button)
    private void startSingleplayer(){
        Intent i = new Intent(MainActivity.this, EndlessActivity.class);
        //i.putExtra()
        startActivity(i);

        //close this activity
        finish();
    }

    //start multiplayer (triggered by button)
    private void startMultiplayer(){
        Intent i = new Intent(MainActivity.this, MainActivity_Multi.class);
        //i.putExtra()
        startActivity(i);

        //close this activity
        finish();
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            int result = intent.getIntExtra("result", 1);

        }
    };

}