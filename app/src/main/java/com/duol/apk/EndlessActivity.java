package com.duol.apk;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class EndlessActivity extends Activity {

    public static int INIT_WINDOW = 1000; //TODO make this randomized eventually
    public static int START_WINDOW = 2000;
    public static int END_WINDOW = 2000;

    public boolean sensorToggled = false;
    public Intent sensorIntent;

    public TextView defenseDirection;
    public TextView defenseStatement;
    public TextView scoreText;
    public TextView livesText;

    private Handler roundHandler;

    Vibrator v;

    public int lives;
    public int score;

    public int gameState = 0;
    //0 - wait  1 - play  2 - resolve
    public long stateTime = 0;

    public int playState = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_screen);
        sensorIntent = new Intent();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("result"));

        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE); //initializes vibration

        sensorIntent = new Intent(EndlessActivity.this, SensorActivity.class);
        startService(sensorIntent);

        roundHandler = new Handler();

        roundHandler.post(initRound);

        defenseDirection = findViewById(R.id.defenseDirection);
        defenseStatement = findViewById(R.id.defenseStatement);

        scoreText = findViewById(R.id.score);
        livesText = findViewById(R.id.lives);

        Button pauseButton = findViewById(R.id.pauseButton);

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContentView(R.layout.endless_pause);
                pauseGame();
            }
        });

        lives = 3;
        score = 0;
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            int result = intent.getIntExtra("result", 1);

            //Log.d("receiver", "Got message: " + Integer.toString(result));
            if(sensorToggled) {
                switch (result) {
                    case 1:
                        Log.d("EndlessActivity", "LEFT");
                        break;
                    case 2:
                        Log.d("EndlessActivity", "UP");
                        break;
                    case 3:
                        Log.d("EndlessActivity", "RIGHT");
                        break;
                    case 4:
                        Log.d("EndlessActivity", "DOWN");
                        break;
                    case 0:
                        Log.d("EndlessActivity", "RETURN");
                        break;
                }


                //displays results to player
                if(playState == result){
                    Log.d("MainActivity", "HIT!");
                    defenseStatement.setText("Parried!");
                    defenseDirection.setText("✓");
                    score++;
                    //Toast.makeText(getApplicationContext(), ("YOU PARRIED!"), Toast.LENGTH_SHORT).show();

                }else{
                    Log.d("MainActivity", "WRONG MOTION!");
                    defenseStatement.setText("Missed!");
                    defenseDirection.setText("✖");
                    lives--;
                    //Toast.makeText(getApplicationContext(), ("YOU WERE HIT!"), Toast.LENGTH_SHORT).show();
                }

                sensorToggled = false;

                //end round early after receiving input
                roundHandler.removeCallbacks(endRound);
                roundHandler.post(endRound);
            }
        }
    };

    //waiting...
    private Runnable initRound = new Runnable(){
        @Override
        public void run(){
            gameState = 0;
            if(lives <= 0){
                roundHandler.removeCallbacks(null);
                setContentView(R.layout.endless_resolve);
                Button returnButton = findViewById(R.id.returnButton);
                returnButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        startMain();
                    }
                });
                TextView newScoreText = findViewById(R.id.score);
                newScoreText.setText(Integer.toString(score));
            }else{
                defenseDirection.setText(" ");
                defenseStatement.setText("Waiting...");
                Log.d("EndlessActivity", "STARTING NEW ROUND...");
                roundHandler.postDelayed(startRound, INIT_WINDOW);
                endState();
            }
        }

    };

    //waits for input
    //myHandler.removeCallbacks(myRunnable); use this to cancel early
    private Runnable startRound = new Runnable(){
        @Override
        public void run(){
            gameState = 1;
            playState = (int)(Math.random()*4+1);

            defenseStatement.setText("SWIPE!");
            switch(playState) {
                case 1:
                    Log.d("EndlessActivity", "SWIPE LEFT!");
                    defenseDirection.setText("←");
                    break;
                case 2:
                    Log.d("EndlessActivity", "SWIPE UP!");
                    defenseDirection.setText("↑");
                    break;
                case 3:
                    Log.d("EndlessActivity", "SWIPE RIGHT!");
                    defenseDirection.setText("→");
                    break;
                case 4:
                    Log.d("EndlessActivity", "SWIPE DOWN!");
                    defenseDirection.setText("↓");
                    break;
                default:
                    Log.d("EndlessActivity", "HOW DOES THIS EVEN HAPPEN");
                    break;
            }

            v.vibrate(100); //vibrate each new "turn"
            sensorToggled = true;

            roundHandler.postDelayed(endRound, START_WINDOW);
            endState();
        }

    };

    //shows result
    private Runnable endRound = new Runnable(){
        @Override
        public void run(){
            gameState = 2;
            Log.d("EndlessActivity", "ENDING ROUND");

            playState = -1;
            sensorToggled = false;

            roundHandler.postDelayed(initRound, END_WINDOW);
            endState();
        }

    };

    public void startMain(){
        Intent i = new Intent(EndlessActivity.this, MainActivity.class);
        startActivity(i);
        //close this activity
        finish();
    }

    private void pauseGame(){
        roundHandler.removeCallbacks(null);
        sensorToggled = false;
        Runnable nextRunnable = null;
        long remainingTime = 0;
        long elapsedTime = System.nanoTime() - stateTime;
        switch(gameState){
            case 0:
                nextRunnable = startRound;
                remainingTime = INIT_WINDOW - elapsedTime;
                break;
            case 1:
                nextRunnable = endRound;
                remainingTime = START_WINDOW - elapsedTime;
                sensorToggled = true; //put it back to original state
                break;
            case 2:
                remainingTime = END_WINDOW - elapsedTime;
                nextRunnable = initRound;
                break;
        }
        setContentView(R.layout.endless_pause);
        Button returnButton = findViewById(R.id.returnButton);

        final long finalRemainingTime = remainingTime;
        final Runnable finalNextRunnable = nextRunnable;
        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContentView(R.layout.game_screen);
                roundHandler.postDelayed(finalNextRunnable, finalRemainingTime);
            }
        });
    }


    //to be run after the end of each state
    private void endState(){
        scoreText.setText(Integer.toString(score));
        livesText.setText(Integer.toString(lives));
        stateTime = System.nanoTime();
    }

}
