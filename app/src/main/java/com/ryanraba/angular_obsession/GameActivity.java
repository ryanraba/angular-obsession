package com.ryanraba.angular_obsession;

import android.content.Intent;
import android.graphics.Canvas;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class GameActivity extends AppCompatActivity
{
    Thread tr = null;
    GameController gameView;
    boolean gameUpdateRunning = false;

    private SurfaceView surface;
    private SurfaceHolder holder;
    Canvas canvas;
    MediaPlayer hitplayer, bighitplayer, missplayer, launchplayer;
    private int gamescore;

    ////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_game);

        getSupportActionBar().hide();

        DisplayMetrics screen = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(screen);
        int navheight = getResources().getDimensionPixelSize(getResources().getIdentifier("navigation_bar_height", "dimen", "android"));

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .setUsage(AudioAttributes.USAGE_GAME)
                .build();

        launchplayer = MediaPlayer.create(this, R.raw.launch2, attributes, ((AudioManager)getSystemService(this.AUDIO_SERVICE)).generateAudioSessionId());
        hitplayer = MediaPlayer.create(this, R.raw.hit5, attributes, ((AudioManager)getSystemService(this.AUDIO_SERVICE)).generateAudioSessionId());
        bighitplayer = MediaPlayer.create(this, R.raw.hit7, attributes, ((AudioManager)getSystemService(this.AUDIO_SERVICE)).generateAudioSessionId());
        missplayer = MediaPlayer.create(this, R.raw.miss2, attributes, ((AudioManager)getSystemService(this.AUDIO_SERVICE)).generateAudioSessionId());

        surface = (SurfaceView) findViewById(R.id.gameView);
        holder = surface.getHolder();

        ((TextView)findViewById(R.id.winMessage)).setVisibility(View.INVISIBLE);

        gamescore = 0;

    }
    /////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////

    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            gameView = new GameController(getResources(),
                    (TextView)findViewById(R.id.scoreBox),
                    (ProgressBar)findViewById(R.id.turnTracker),
                    launchplayer,
                    hitplayer,
                    bighitplayer,
                    missplayer,
                    surface.getWidth(), surface.getHeight());

            // assign function to handle touches to graph
            surface.setOnTouchListener(gameView.launchListener);

            // start game
            gameUpdateThread();
        }
    }

    public void onPause()
    {
        launchplayer.release();
        hitplayer.release();
        bighitplayer.release();
        missplayer.release();
        gameUpdateRunning = false;
        try { if (tr != null) tr.join(); }
        catch (InterruptedException ex) {}
        super.onPause();
    }


    ///////////////////////////////////////
    ///////////////////////////////////////
    public void gameUpdateThread()
    {
        if (gameUpdateRunning) return;
        gameUpdateRunning = true;

        tr = new Thread() {
            public void run() {

                int[] rc = {0, -1, 0};  // gameover, remaining blocks, gamescore
                while (gameUpdateRunning && (rc[0] == 0))
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
                    rc = gameView.animateGame(canvas);
                    holder.unlockCanvasAndPost(canvas);

                    gamescore = rc[2];
                    GameActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                ((TextView)findViewById(R.id.scoreBox)).setText(String.valueOf(gamescore)); }});
                }

                if ((rc[0] == 1) && (rc[1] == 0))
                {
                    GameActivity.this.runOnUiThread(new Runnable() {
                        public void run() { ((TextView)findViewById(R.id.winMessage)).setVisibility(View.VISIBLE); }});
                    for (int zz=0; zz<10; zz++)
                    {
                        if (!gameUpdateRunning) break;
                        try { Thread.sleep(100); }
                        catch (Exception e) { }
                    }
                }
                gameUpdateRunning = false;
                //System.out.println("################## got here ########" + String.valueOf(gamescore));
                Intent intent = new Intent();
                intent.putExtra("gamescore", String.valueOf(gamescore));
                setResult(RESULT_OK, intent);
                finish();

                //wakeLock.release();
            }  // end thread run
        }; // end thread body

        tr.start();  // start thread
    }
}
