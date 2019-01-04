package com.duol.apk;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MotionEvent;
import android.content.Intent;
import android.util.Log;

import android.Manifest;

import android.widget.Switch;
import android.widget.Toast;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Timer;
import java.util.TimerTask;

import android.content.pm.PackageManager;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate.Status;
import com.google.android.gms.nearby.connection.Strategy;


public class MainActivity_Multi extends Activity{
    Vibrator v;

    public Boolean attacking;
    public int result = 0;
    public String direction;
    public String oppdir;

    public static boolean STARTING = true; //default true

    private final String codeName = "testuser";
    private static final String[] REQUIRED_PERMISSIONS =
            new String[] {
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            };
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    private static final Strategy STRATEGY = Strategy.P2P_STAR;

    public TextView defenseStatement;

    public Switch startSwitch;
    public Switch invertSwitch;

    private ConnectionsClient connectionsClient;
    private int myScore;
    private String opponentEndpointId;
    private String opponentName;
    private int opponentScore;

    private TextView opponentText;
    private TextView statusText;
    private TextView scoreText;
    private Button findOpponentButton;
    private Button disconnectButton;

    public Boolean inverted = false;

    public Boolean sensorToggled = false;

    public static int TIMING_WINDOW = 3000;

    public Timer timer = new Timer();

    public TimerTask timerTaskEnd;

    public Intent sensorIntent;

    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    if(attacking == false){
                        Log.d("MainActivity_Multi", "got payload");
                        if(oppdir == null){
                            oppdir = new String(payload.asBytes(), UTF_8);
                            createMultiGesture();
                        }
                    }else{
                        String result = new String(payload.asBytes(), UTF_8);
                        finishRound(result);
                    }

                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    if (update.getStatus() == Status.SUCCESS && direction != null && oppdir != null) {
                        //finishRound();
                    }
                }
            };
    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    Log.i("MainActivity_Multi", "onEndpointFound: endpoint found, connecting");
                    setStatusText("Connecting...");
                    connectionsClient.requestConnection(codeName, endpointId, connectionLifecycleCallback);
                }

                @Override
                public void onEndpointLost(String endpointId) {}
            };

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    Log.i("MainActivity_Multi", "onConnectionInitiated: accepting connection");
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
                    opponentName = connectionInfo.getEndpointName();
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        Log.i("MainActivity_Multi", "onConnectionResult: connection successful");

                        startSwitch = findViewById(R.id.startSwitch);
                        invertSwitch = findViewById(R.id.invertSwitch);

                        inverted = invertSwitch.isChecked();

                        if(startSwitch.isChecked()){
                            Toast toast = Toast.makeText(getApplicationContext(), "You attack", Toast.LENGTH_LONG);
                            attacking = true;
                            toast.show();
                            defenseStatement = findViewById(R.id.defenseStatement);
                            defenseStatement.setText("You are attacking.");
                        }else{
                            Toast toast = Toast.makeText(getApplicationContext(), "You defend", Toast.LENGTH_LONG);
                            attacking = false;
                            defenseStatement = findViewById(R.id.defenseStatement);
                            defenseStatement.setText("You are defending.");
                            toast.show();
                        }


                        connectionsClient.stopDiscovery();
                        connectionsClient.stopAdvertising();

                        opponentEndpointId = endpointId;
                        setOpponentName(opponentName);
                        setStatusText("status_connected");
                        setButtonState(true);

                    } else {
                        Log.i("MainActivity_Multi", "onConnectionResult: connection failed");
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    Log.i("MainActivity_Multi", "onDisconnected: disconnected from the opponent");
                    resetGame();
                }
            };

    @Override
    protected void onStart() {
        super.onStart();

        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
        }
    }

    @Override
    protected void onStop() {
        connectionsClient.stopAllEndpoints();
        resetGame();

        super.onStop();
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @CallSuper
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_CODE_REQUIRED_PERMISSIONS) {
            return;
        }

        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "error missing permissions", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        recreate();
    }

    /** Finds an opponent to play the game with using Nearby Connections. */
    public void findOpponent(View view) {
        startAdvertising();
        startDiscovery();
        setStatusText("Searching...");
        findOpponentButton.setEnabled(false);
    }

    /** Disconnects from the opponent and reset the UI. */
    public void disconnect(View view) {
        connectionsClient.disconnectFromEndpoint(opponentEndpointId);
        resetGame();
    }


    /** Starts looking for other players using Nearby Connections. */
    private void startDiscovery() {

        // Note: Discovery may fail. To keep this demo simple, we don't handle failures.
        connectionsClient.startDiscovery(
                getPackageName(), endpointDiscoveryCallback, new DiscoveryOptions(STRATEGY));
    }

    private void startAdvertising() {

        // Note: Advertising may fail. To keep this demo simple, we don't handle failures.
        connectionsClient.startAdvertising(
                codeName, getPackageName(), connectionLifecycleCallback, new AdvertisingOptions(STRATEGY));
    }

    private void resetGame() {
        opponentEndpointId = null;
        opponentName = null;
        oppdir = null;
        opponentScore = 0;
        direction = null;
        myScore = 0;

        setOpponentName("no_opponent");
        setStatusText("status_disconnected");
        updateScore(myScore, opponentScore);
        setButtonState(false);
    }

    private void sendGameChoice() {

        connectionsClient.sendPayload(
                opponentEndpointId, Payload.fromBytes(direction.getBytes(UTF_8)));

        setStatusText("placeholder");

        Log.d("MainActivity_Multi", "sent payload");

    }

    public void makeMove() {
        sendGameChoice();
    }

    private void finishRound(String result) {
        //Log.d("Round Finished", oppdir);
        v.vibrate(300); //vibrate each new "turn"
        if (result.equals("LOSE") && attacking == false) {
            // Loss!
            Toast toast = Toast.makeText(getApplicationContext(), "You were hit, you now attack", Toast.LENGTH_LONG);
            toast.show();
            opponentScore++;
            attacking = true;

        } else if(result.equals("LOSE") && attacking == true) {
            Toast toast = Toast.makeText(getApplicationContext(), "Your attack was parried, you now defend", Toast.LENGTH_LONG);
            toast.show();
            attacking = false;

            // Loss

        } else if(result.equals("WIN") && attacking == false) {
            Toast toast = Toast.makeText(getApplicationContext(), "You parried an attack, you now attack", Toast.LENGTH_LONG);
            toast.show();
            attacking = true;

        } else if(result.equals("WIN") && attacking == true){
            Toast toast = Toast.makeText(getApplicationContext(), "You landed an attack, you now defend", Toast.LENGTH_LONG);
            toast.show();
            myScore++;
            attacking = false;

        }

        if(attacking){
            defenseStatement = findViewById(R.id.defenseStatement);
            defenseStatement.setText("You are attacking.");

        }else{
            defenseStatement = findViewById(R.id.defenseStatement);
            defenseStatement.setText("You are defending.");

        }
        direction = null;
        oppdir = null;

        updateScore(myScore, opponentScore);

        // Ready for another round

    }


    private void setButtonState(boolean connected) {
        findOpponentButton.setEnabled(true);
        findOpponentButton.setVisibility(connected ? View.GONE : View.VISIBLE);
        disconnectButton.setVisibility(connected ? View.VISIBLE : View.GONE);

        //setGameChoicesEnabled(connected);
    }

    private void setStatusText(String text) {
        statusText.setText(text);
    }

    private void setOpponentName(String opponentName) {
        opponentText.setText(getString(R.string.opponent_name, opponentName));
    }

    private void updateScore(int myScore, int opponentScore) {
        scoreText.setText(getString(R.string.game_score, myScore, opponentScore));
    }


    @Override
    protected void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main_multi);

        findOpponentButton = findViewById(R.id.find_opponent);
        disconnectButton = findViewById(R.id.disconnect);

        opponentText = findViewById(R.id.opponent_name);
        statusText = findViewById(R.id.status);
        scoreText = findViewById(R.id.score);

        TextView nameView = findViewById(R.id.name);
        nameView.setText(getString(R.string.codename, codeName));



        connectionsClient = Nearby.getConnectionsClient(this);

        resetGame();

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("result"));

        sensorToggled = true;
        sensorIntent = new Intent(MainActivity_Multi.this, SensorActivity.class);
        startService(sensorIntent);

        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE); //initializes vibration

        //createGesture();

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if(!sensorToggled){
            sensorToggled = true;
        }
        return true;

    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            int result = intent.getIntExtra("result", 1);
            //Log.d("inverted",  Boolean.toString(inverted));

            if(inverted){
                switch(result){
                    case 1:
                        result = 3;
                        break;
                    case 2:
                        result = 4;
                        break;
                    case 3:
                        result = 1;
                        break;
                    case 4:
                        result = 2;
                        break;
                }
            }

            if(sensorToggled) {
                switch (result) {
                    case 1:
                        Log.d("MainActivity_Multi", "LEFT");
                        direction = "LEFT";
                        break;
                    case 2:
                        Log.d("MainActivity_Multi", "UP");
                        direction = "UP";
                        break;
                    case 3:
                        Log.d("MainActivity_Multi", "RIGHT");
                        direction = "RIGHT";
                        break;
                    case 4:
                        Log.d("MainActivity_Multi", "DOWN");
                        direction = "DOWN";
                        break;
                    case 0:
                        //fail to swipe in time
                        Log.d("MainActivity_Multi", "MISS!");
                        direction = "MISS!";
                        break;
                }

                sensorToggled = false;
                timer.cancel();
                if(attacking != null) {
                    if (attacking == false) {
                        if(oppdir != null) {
                            if (direction.equals(oppdir) || (direction.equals("LEFT") && oppdir.equals("RIGHT"))|| (direction.equals("RIGHT") && oppdir.equals("LEFT"))) {

                                connectionsClient.sendPayload(
                                        opponentEndpointId, Payload.fromBytes("LOSE".getBytes(UTF_8)));
                                finishRound("WIN");
                            } else {
                                connectionsClient.sendPayload(
                                        opponentEndpointId, Payload.fromBytes("WIN".getBytes(UTF_8)));
                                finishRound("LOSE");
                            }
                        }
                    } else {
                        connectionsClient.sendPayload(
                                opponentEndpointId, Payload.fromBytes(direction.getBytes(UTF_8)));
                    }
                }

                //createGesture();
            }
        }
    };

    public void createMultiGesture(){

        v.vibrate(100); //vibrate each new "turn"

        timer = new Timer();
        timerTaskEnd = new TimerTask(){
            @Override
            public void run(){

                runOnUiThread(new Runnable() {
                    @Override
                    public void run(){
                        finishRound("LOSE");
                    }


                });
                Log.d("MainActivity_Multi", "MISS!");
                connectionsClient.sendPayload(
                        opponentEndpointId, Payload.fromBytes("WIN".getBytes(UTF_8)));
            }

        };



        timer.schedule(timerTaskEnd, TIMING_WINDOW);
    }


    //FINAL COMMIT BLESS UP



}