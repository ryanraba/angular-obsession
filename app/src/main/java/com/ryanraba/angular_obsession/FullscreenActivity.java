package com.ryanraba.angular_obsession;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

public class FullscreenActivity extends AppCompatActivity {
    ArrayAdapter<String> hs_adapter;
    File hs_fid;
    String hs_filename = "high_scores.txt";
    int hs_height = 100;
    boolean gamestarted = false;
    MediaPlayer finishplayer;

    ////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        hs_adapter = new ArrayAdapter<String>(this, R.layout.text_list_item) {
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                if (textView.getText().toString().startsWith("!!")) {
                    textView.setText(textView.getText().toString().replace("!!", ""));
                    textView.setBackground(getDrawable(R.drawable.scoreline2));
                } else
                    textView.setBackground(getDrawable(R.drawable.scoreline));
                textView.getLayoutParams().height = hs_height/11;
                return textView;
            }
        };

        ((ListView) findViewById(R.id.fsView)).setAdapter(hs_adapter);
        gamestarted = false;
    }
    //////////////////////////////////////////////////////////////

    //////////////////////////////////////////////////////////////
    private void rebuildHSList() {
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
        } catch (Exception e) { }
    }


    //////////////////////////////////////////////////////////////
    private void writeOutHSList() {
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
        } catch (Exception e) {
        }
    }


    //////////////////////////////////////////////////////////////
    public void startGame(View view) {
        Intent intent = new Intent(this, GameActivity.class);
        writeOutHSList();
        gamestarted = true;
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .setUsage(AudioAttributes.USAGE_GAME)
                .build();

        finishplayer = MediaPlayer.create(this, R.raw.hitxl, attributes, ((AudioManager)getSystemService(this.AUDIO_SERVICE)).generateAudioSessionId());
        finishplayer.setVolume((float)0.99, (float)0.99);
        finishplayer.start();

        rebuildHSList();
        if (data != null) {
            String timeStamp = new SimpleDateFormat("MM/dd/yyyy").format(new Date());
            String scoreLine = "!!" + timeStamp + "         " + data.getStringExtra("gamescore");
            if (hs_adapter.getCount() >= 10) hs_adapter.remove(hs_adapter.getItem(9));
            hs_adapter.add(scoreLine);
            hs_adapter.sort(sortHSList);
        }
    }
    //////////////////////////////////////////////////////////////


    //////////////////////////////////////////////////////////////
    public void onResume() {
        super.onResume();
    }

    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !gamestarted) {
            hs_height = ((ListView) findViewById(R.id.fsView)).getHeight();
            rebuildHSList();
        }
    }

    //////////////////////////////////////////////////////////////
    public void onStop() {
        if (finishplayer != null) finishplayer.release();
        writeOutHSList();
        super.onStop();
    }


}

