package com.duol.apk;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class EndlessActivity extends Activity {

    private static int INIT_WINDOW = 1000; //TODO make this randomized eventually
    private static int START_WINDOW = 2000;
    private static int END_WINDOW = 2000;

    private boolean sensorToggled = false;
    private Intent sensorIntent;

    private ConstraintLayout layout;

    //private TextView defenseDirection;
    private TextView defenseStatement;
    private TextView scoreText;
    private TextView livesText;

    private Animation animShake;
    private Animation animPop;
    private ImageView defenseImage;

    private ProgressBar timeBar;

    private Handler roundHandler;

    private Vibrator v;

    private int lives;
    private int score;

    private int gameState = 0;
    //0 - wait  1 - play  2 - resolve

    private long stateTime = 0;

    private int playState = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sensorIntent = new Intent();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("result"));

        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE); //initializes vibration

        sensorIntent = new Intent(EndlessActivity.this, SensorActivity.class);
        startService(sensorIntent);

        roundHandler = new Handler();

        roundHandler.post(initRound(INIT_WINDOW));

        animShake = AnimationUtils.loadAnimation(this, R.anim.shake);
        animPop = AnimationUtils.loadAnimation(this, R.anim.pop);

        initScreen();

        lives = 3;
        score = 0;

    }

    @Override
    protected void onStop() {
        super.onStop();
        pauseGame();
        Log.d("EndlessActivity", "GONE TO DRAWER");
    }

    private void initScreen(){
        setContentView(R.layout.game_screen);
        layout = findViewById(R.id.layout);
        //defenseDirection = findViewById(R.id.defenseDirection);
        defenseStatement = findViewById(R.id.defenseStatement);

        defenseImage = findViewById(R.id.defenseImage);

        timeBar = findViewById(R.id.timeBar);

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
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            int result = intent.getIntExtra("result", 1);
            int sendResult = 0;

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


                //figures out what to send to endRound
                if(playState == result){
                    sendResult = 1;
                    //Toast.makeText(getApplicationContext(), ("YOU PARRIED!"), Toast.LENGTH_SHORT).show();

                }else{
                    sendResult = 2;
                    //Toast.makeText(getApplicationContext(), ("YOU WERE HIT!"), Toast.LENGTH_SHORT).show();
                }

                sensorToggled = false;

                //end round early after receiving input
                roundHandler.removeCallbacksAndMessages(null);
                roundHandler.post(endRound(END_WINDOW, sendResult));
            }
        }
    };
    //waiting...
    private Runnable initRound(final long duration){
        Runnable initRunnable = new Runnable(){
            @Override
            public void run(){
                layout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.initBackgroundColor));
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
                    //defenseDirection.setText(" ");
                    defenseStatement.setText("Ready...");
                    defenseImage.setImageResource(R.drawable.ic_ellipsis);
                    Log.d("EndlessActivity", "STARTING NEW ROUND...");
                    roundHandler.postDelayed(startRound(START_WINDOW), duration);
                }
                startState();
            }

        };
        return initRunnable;
    }

    //waits for input
    //myHandler.removeCallbacks(myRunnable); use this to cancel early

    private Runnable startRound(final long duration){
        Runnable startRunnable = new Runnable(){
            @Override
            public void run(){
                layout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.startBackgroundColor));
                gameState = 1;
                if(duration == START_WINDOW) {
                    playState = (int) (Math.random() * 4 + 1);
                    v.vibrate(100); //vibrate each new "turn"
                }

                defenseStatement.setText("DUOL!");
                switch(playState) {
                    case 1:
                        Log.d("EndlessActivity", "SWIPE LEFT!");
                        //defenseDirection.setText("←");
                        defenseImage.setImageResource(R.drawable.ic_arrow_left);
                        break;
                    case 2:
                        Log.d("EndlessActivity", "SWIPE UP!");
                        //defenseDirection.setText("↑");
                        defenseImage.setImageResource(R.drawable.ic_arrow_up);
                        break;
                    case 3:
                        Log.d("EndlessActivity", "SWIPE RIGHT!");
                        //defenseDirection.setText("→");
                        defenseImage.setImageResource(R.drawable.ic_arrow_right);
                        break;
                    case 4:
                        Log.d("EndlessActivity", "SWIPE DOWN!");
                        //defenseDirection.setText("↓");
                        defenseImage.setImageResource(R.drawable.ic_arrow_down);
                        break;
                    default:
                        Log.d("EndlessActivity", "HOW DOES THIS EVEN HAPPEN");
                        break;
                }

                sensorToggled = true;

                roundHandler.postDelayed(endRound(END_WINDOW, 0), duration);
                startState();
            }

        };
        return startRunnable;
    }


    //shows result
    private Runnable endRound(final long duration, final int result){
        //0 - nothing, 1 - hit, 2 - miss
        Runnable endRunnable = new Runnable(){
            @Override
            public void run(){
                gameState = 2;

                //displays results to player
                if(result == 0) {
                    Log.d("MainActivity", "ROUND END, NO ACTION");
                    layout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.endBackgroundColorNone));
                    defenseStatement.setText("Ending round...");
                    //defenseDirection.setText("...");
                    defenseImage.setImageResource(R.drawable.ic_ellipsis);
                }else if(result == 1) {
                    Log.d("MainActivity", "ROUND END, HIT!");
                    layout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.endBackgroundColorPass));
                    defenseStatement.setText("Parried!");
                    //defenseDirection.setText("✓");
                    defenseImage.setImageResource(R.drawable.ic_check);
                    defenseImage.startAnimation(animPop);
                    score++;
                    //Toast.makeText(getApplicationContext(), ("YOU PARRIED!"), Toast.LENGTH_SHORT).show();

                }else if(result == 2){
                    Log.d("MainActivity", "ROUND END, WRONG MOTION!");
                    layout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.endBackgroundColorFail));
                    defenseStatement.setText("Missed!");
                    //defenseDirection.setText("✖");
                    defenseImage.setImageResource(R.drawable.ic_times);
                    defenseImage.startAnimation(animShake);
                    lives--;
                    //Toast.makeText(getApplicationContext(), ("YOU WERE HIT!"), Toast.LENGTH_SHORT).show();
                }


                playState = -1;
                sensorToggled = false;

                roundHandler.postDelayed(initRound(INIT_WINDOW), duration);
                startState();
            }

        };
        return endRunnable;
    }


    private void startMain(){
        Intent i = new Intent(EndlessActivity.this, MainActivity.class);
        startActivity(i);
        //close this activity
        finish();
    }

    private void pauseGame(){
        roundHandler.removeCallbacksAndMessages(null);
        sensorToggled = false;
        Runnable nextRunnable = null;
        long remainingTime = 0;
        long elapsedTime = System.currentTimeMillis() - stateTime;
        switch(gameState){
            case 0:
                remainingTime = INIT_WINDOW - elapsedTime;
                nextRunnable = initRound(remainingTime);
                break;
            case 1:
                remainingTime = START_WINDOW - elapsedTime;
                nextRunnable = startRound(remainingTime);
                break;
            case 2:
                remainingTime = END_WINDOW - elapsedTime;
                nextRunnable = endRound(remainingTime, 0);
                break;
        }
        setContentView(R.layout.endless_pause);
        Button returnButton = findViewById(R.id.returnButton);
        Log.d("EndlessActivity", "GAME PAUSED");

        final Runnable finalNextRunnable = nextRunnable;
        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initScreen();
                Log.d("EndlessActivity", "GAME UNPAUSED");
                roundHandler.postDelayed(finalNextRunnable, 0);
            }
        });

        Button endButton = findViewById(R.id.endButton);
        endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMain();
            }
        });
    }

    //to be run after the start of each state
    private void startState(){
        scoreText.setText(Integer.toString(score));
        livesText.setText(Integer.toString(lives));
        stateTime = System.currentTimeMillis();
    }

}
