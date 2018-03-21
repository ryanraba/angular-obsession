package com.ryanraba.angular_obsession;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Canvas;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class GameActivity extends AppCompatActivity
{
    Thread tr = null;
    GameController gameView;
    boolean gameUpdateRunning = false;

    private SurfaceView surface;
    private SurfaceHolder holder;
    Canvas canvas;
    MediaPlayer hitplayer, missplayer, launchplayer;
    int gamescore = 0;

    private View mContentView;

    ////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_game);

        mContentView = findViewById(R.id.gameView);
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        DisplayMetrics screen = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(screen);MediaPlayer.create(this, R.raw.launch2);

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .setUsage(AudioAttributes.USAGE_GAME)
                .build();

        launchplayer = MediaPlayer.create(this, R.raw.launch2, attributes, ((AudioManager)getSystemService(this.AUDIO_SERVICE)).generateAudioSessionId());
        hitplayer = MediaPlayer.create(this, R.raw.hit5, attributes, ((AudioManager)getSystemService(this.AUDIO_SERVICE)).generateAudioSessionId());
        missplayer = MediaPlayer.create(this, R.raw.miss2, attributes, ((AudioManager)getSystemService(this.AUDIO_SERVICE)).generateAudioSessionId());

        gameView = new GameController(getResources(),
                (TextView)findViewById(R.id.scoreBox),
                (ProgressBar)findViewById(R.id.turnTracker),
                launchplayer,
                hitplayer,
                missplayer,
        screen.widthPixels, screen.heightPixels, screen.densityDpi);

        surface = (SurfaceView) findViewById(R.id.gameView);
        holder = surface.getHolder();

        // assign function to handle touches to graph
        surface.setOnTouchListener(gameView.launchListener);

    }
    /////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////


    public void onResume()
    {
        super.onResume();
        if (gameUpdateRunning == false)
            gameUpdateThread();
    }

    public void onPause()
    {
        super.onPause();
        gameUpdateRunning = false;
        try { if (tr != null) tr.join(); }
        catch (InterruptedException ex) {}
    }

    public void onStop()
    {
        super.onStop();
        launchplayer.release();
        hitplayer.release();
        missplayer.release();
        gameUpdateRunning = false;
        try { if (tr != null) tr.join(); }
        catch (InterruptedException ex) {}
    }

    public void onDestroy()
    {
        super.onDestroy();
        gameUpdateRunning = false;
        try { if (tr != null) tr.join(); }
        catch (InterruptedException ex) {}
    }



    ///////////////////////////////////////
    ///////////////////////////////////////
    public void gameUpdateThread()
    {
        if (gameUpdateRunning) return;
        gameUpdateRunning = true;
        gamescore = 0;

        tr = new Thread() {
            public void run() {

                int lastscore = -1;

                while (gameUpdateRunning && (gamescore >= 0))
                {
                    try
                    {
                        Thread.sleep(10);
                    }
                    catch(InterruptedException ex)
                    {
                        gameUpdateRunning = false;
                        Thread.currentThread().interrupt();
                    }

                    if(!holder.getSurface().isValid())
                        continue;

                    canvas = holder.lockCanvas();

                    gamescore = gameView.animateGame(canvas);

                    holder.unlockCanvasAndPost(canvas);

                    if ((gamescore != lastscore) && (gamescore >= 0))
                        GameActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                ((TextView)findViewById(R.id.scoreBox)).setText(String.valueOf(gamescore));
                            }
                        });

                    if (gamescore >= 0) lastscore = gamescore;
                }

                gameUpdateRunning = false;
                //System.out.println("################## got here ########" + String.valueOf(gamescore));
                Intent intent = new Intent();
                intent.putExtra("gamescore", String.valueOf(lastscore));
                setResult(RESULT_OK, intent);
                finish();

                //wakeLock.release();
            }  // end thread run
        }; // end thread body

        tr.start();  // start thread
    }
}
