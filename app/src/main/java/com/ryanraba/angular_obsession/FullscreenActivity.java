package com.ryanraba.angular_obsession;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.Spanned;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity
{
    ArrayAdapter<String> hs_adapter;
    File hs_fid;
    String hs_filename = "high_scores.txt";

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
            mControlsView.setVisibility(View.VISIBLE);
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

    ////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        mVisible = true;
        //mControlsView = findViewById(R.id.fsLinearLayout1);
        mContentView = findViewById(R.id.hslabel);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.start_button).setOnTouchListener(mDelayHideTouchListener);

        hs_adapter = new ArrayAdapter<String>(this, R.layout.text_list_item) {
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                if (textView.getText().toString().startsWith("!!")) {
                    textView.setText(textView.getText().toString().replace("!!",""));
                    textView.setBackground(getDrawable(R.drawable.scoreline2));
                }
                else
                    textView.setBackground(getDrawable(R.drawable.scoreline));
                return textView;
            }
        };

        ((ListView)findViewById(R.id.fsView)).setAdapter(hs_adapter);

        rebuildHSList();
    }
    //////////////////////////////////////////////////////////////


    //////////////////////////////////////////////////////////////
    private void rebuildHSList()
    {
        hs_adapter.clear();
        hs_fid = new File(getCacheDir(), hs_filename);
        try {
            BufferedReader buffer = new BufferedReader(new FileReader(hs_fid));
            String line = buffer.readLine();
            while (line != null) {
                hs_adapter.add(line);
                line = buffer.readLine();
            }
            buffer.close();
        }
        catch (Exception e) {  }
    }


    //////////////////////////////////////////////////////////////
    private void writeOutHSList()
    {
        String line;
        try {
            BufferedWriter buffer = new BufferedWriter(new FileWriter(hs_fid));
            for (int ii = 0; ii < hs_adapter.getCount(); ii++) {
                line = hs_adapter.getItem(ii).toString();
                if (line.startsWith("!!")) line = line.replace("!!", "");
                buffer.write(line, 0, line.length());
                buffer.newLine();
            }
            buffer.close();
        }
        catch (Exception e) { }
    }



    //////////////////////////////////////////////////////////////
    public void startGame(View view) {
        Intent intent = new Intent(this, GameActivity.class);
        writeOutHSList();
        startActivityForResult(intent, 1);
    }
    //////////////////////////////////////////////////////////////


    Comparator<String> sortHSList = new Comparator<String>() {
        public int compare(String object1, String object2) {
            int v1, v2;
            v1 = Integer.valueOf(object1.toString().split("         ")[1]);
            v2 = Integer.valueOf(object2.toString().split("         ")[1]);
            return v2 - v1;
        }
    };

    //////////////////////////////////////////////////////////////
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        rebuildHSList();
        if(data != null)
        {
            String timeStamp = new SimpleDateFormat("MM/dd/yyyy").format(new Date());
            String scoreLine = "!!" + timeStamp + "         " + data.getStringExtra("gamescore");
            if (hs_adapter.getCount() >= 10) hs_adapter.remove(hs_adapter.getItem(9));
            hs_adapter.add(scoreLine);
            hs_adapter.sort(sortHSList);
        }
    }
    //////////////////////////////////////////////////////////////



    //////////////////////////////////////////////////////////////
    public void onStop()
    {
        super.onStop();
        writeOutHSList();
    }




    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) hide();
        else show();
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
}
