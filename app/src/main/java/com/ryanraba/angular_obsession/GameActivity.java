package com.ryanraba.angular_obsession;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

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

    private static final boolean AUTO_HIDE = true;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            //mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };


    ////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DisplayMetrics screen = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(screen);
        gameView = new GameController(getResources(), screen.widthPixels, screen.heightPixels, screen.densityDpi);

        setContentView(R.layout.activity_game);

        mVisible = true;
        //mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.gameView);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        surface = (SurfaceView) findViewById(R.id.gameView);
        holder = surface.getHolder();

        // assign function to handle touches to graph
        surface.setOnTouchListener(gameView.launchListener);

    }
    /////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////



    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        //mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

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

        tr = new Thread() {
            public void run() {

                boolean gamefinished = false;

                while (gameUpdateRunning && !gamefinished)
                {
                    try
                    {
                        Thread.sleep(25);
                    }
                    catch(InterruptedException ex)
                    {
                        gameUpdateRunning = false;
                        Thread.currentThread().interrupt();
                    }

                    if(!holder.getSurface().isValid())
                        continue;

                    canvas = holder.lockCanvas();

                    gamefinished = gameView.animateGame(canvas);

                    holder.unlockCanvasAndPost(canvas);

                    //GameActivity.this.runOnUiThread(new Runnable() {
                    //    public void run() {
                    //        return;
                    //    }
                    //});

                }

                gameUpdateRunning = false;
                if (gamefinished) finish();

                //wakeLock.release();
            }  // end thread run
        }; // end thread body

        tr.start();  // start thread
    }
}
