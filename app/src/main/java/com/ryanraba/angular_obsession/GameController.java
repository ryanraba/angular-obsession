package com.ryanraba.angular_obsession;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

class GameController {
    Thread t = null;
    ImageView image;
    Bitmap bmap;
    Canvas canvas;
    int guardX;  // max X of gridline
    int guardY;  // max Y of gridline
    int maxX;    // max X of plot
    int maxY;    // max Y of plot
    int guardPx, textPx;
    int originX, originY;
    int spanX, spanY;
    float xstep, ystep;

    // fft globals
    public int nn = 2048;   // fft size power of 2
    int [] plotidxs;        // precompute uniform set of freq domain indices to plot
    float [] freqThreshDB;  // frequency domain threshold values db
    float [] freqFilterDB;  // frequency domain filter values db (display purposes only
    float hzstep = 44100/nn;
    int xtics = 6;
    int plotLength = xtics*10;

    // constants used for x-axis scale conversion equation
    // works out to be 20Hz to 20kHz
    // flog = k*log(n) + c
    float kk = (float)2.378234;
    float cc = (float)-4.22886;

    // find indices in frequency domain corrensponding to human hearing
    // 20 Hz to 20 kHz
    //int hzmin = (int)Math.ceil(60 / hzstep);
    //int hzmax = (int)Math.floor(20000 / hzstep);

    ///////////////////////////
    // Constructor
    ///////////////////////////
    GameController(ImageView image_i, int width, int height)
    {
        image = image_i;
        maxX = width;
        maxY = height;
        textPx = height/20;
        guardPx = height/10;
        guardX = width - guardPx;
        guardY = height - guardPx;
        originX = guardPx + guardPx/2;
        originY = maxY - guardPx - guardPx/2;
        spanX = maxX - 2*guardPx;
        spanY = maxY - 2*guardPx;
        xstep = (float)spanX / (float)plotLength;
        ystep = (float)spanY / (float)90; // db
        //ystep = (float)spanY / (sqrt2*(float)32768); // db

        bmap = Bitmap.createBitmap(maxX, maxY, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bmap);

        // constructor must be called from GUI thread
        initializePlot(true);

        // compute a uniform set of steps in log scale for later plotting
        // of freq domain data
        float hzval;
        plotidxs = new int[plotLength];
        for (int ii=0; ii < plotLength; ii++)
        {
            hzval = (float)Math.pow(10.0, ((ii/10.0) - cc)/kk);
            plotidxs[ii] = Math.round(hzval/hzstep);
        }

        // setup initial default threshold
        freqThreshDB = new float[nn];
        freqFilterDB = new float[nn];

    }


    /////////////////////////
    public View.OnTouchListener thresholdListener = new View.OnTouchListener()
    {
        public boolean onTouch(View v, MotionEvent event)
        {
            float xx, yy, ydB;
            int ii, jj, xindex, idxs;

            xx = event.getX() - originX;
            if (xx < 0) xx = 0;
            if (xx > spanX) xx = spanX+1;

            yy = originY - event.getY();
            if (yy > spanY) yy = spanY;
            if (yy < 0) yy = 0;

            //System.out.println("Touch at " + xx);

            // figure out the dB value of the point
            ydB = (yy / ystep) - 90;

            // figure out where xx falls in the list of plotIdxs
            // set new threshold for that point accordingly
            xindex = Math.round(((float)xx/(float)spanX)*plotLength);
            if (xindex < 0) xindex = 0;
            if (xindex >= plotLength) xindex = plotLength - 1;

            freqThreshDB[plotidxs[xindex]] = ydB;

            if (event.getAction() == MotionEvent.ACTION_UP)
            {
                // convert to linear values and interpolate between points in plot array
                for (ii = 0; ii < plotLength - 1; ii++)
                {
                    idxs = plotidxs[ii + 1] - plotidxs[ii];
                    for (jj = 0; jj < idxs; jj++)
                        freqThreshDB[plotidxs[ii] + jj] = freqThreshDB[plotidxs[ii]] + (freqThreshDB[plotidxs[ii+1]] - freqThreshDB[plotidxs[ii]]) * ((float)jj / (float)idxs);
                }
            }

            return true;
        }
    };



    ///////////////
    public void initializePlot(boolean draw)
    {
        int ii;
        Paint paint = new Paint();

        // coordinates [left, top, right, bottom]
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, (float) maxX, (float) maxY, paint);

        // add gridlines over top
        // coordinates [startx, starty, stopx, stopy]
        float xinc = (guardX - guardPx)/xtics;  // vertical line increments
        float yinc = (guardY - guardPx)/9;  // horizontal line increments
        paint.setColor(Color.GRAY);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setSubpixelText(true);
        paint.setTextSize(textPx);
        for (ii=0; ii <= 9; ii++)  // draw horizontal lines
        {
            canvas.drawLine(guardPx, yinc*ii + guardPx/2, maxX, yinc*ii + guardPx/2, paint);
            if (ii==0)
                canvas.drawText(" dB", 0, yinc * ii + guardPx / 2 + textPx / 2, paint);
            else
                canvas.drawText("-" + Integer.toString(ii * 10), 0, yinc * ii + guardPx / 2 + textPx / 2, paint);
        }

        canvas.drawText(" Hz", 0, maxY - textPx / 2, paint);
        for (ii=0; ii <= xtics; ii++)  // draw vertical lines
        { // xscale = kk log(Hz) + cc => Hz = 10^((xscale - cc)/kk)
            canvas.drawLine(xinc * ii + guardPx + guardPx / 2, 0, xinc * ii + guardPx + guardPx / 2, guardY, paint);
            if (ii < 3)
                canvas.drawText(Integer.toString(Math.round((float)Math.pow(10, (ii-cc)/kk))), xinc*ii + guardPx, maxY-textPx/2, paint);
            else
                canvas.drawText(Integer.toString(Math.round((float)Math.pow(10, (ii-cc)/kk))/1000)+"k", xinc*ii + guardPx, maxY-textPx/2, paint);
        }
        // draw (only if running from GUI thread)
        if (draw) image.setImageBitmap(bmap);
    }



    ///////////////////////////////////////
    // plots data and returns true if audio is above threshold
    public void plotAudio()
    {
        boolean rc=false;
        int ii;
        float px, py, cx, cy, tx, ty, rr=0;

        initializePlot(false);
        plotThreshold(freqThreshDB, Color.rgb(230,184,0));
        plotThreshold(freqFilterDB, Color.rgb(222, 125, 92));

        /*
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        //paint.setStrokeWidth(SettingsStn.dd.plotLineWidth / 2);

        // draw graph with points above threshold with a different color
        for (ii=1; ii < plotLength; ii++)
        {
            px = originX + (ii-1) * xstep;
            py = originY - (ystep * Math.max(1, freqBuffer[plotidxs[ii-1]] + 90));
            cx = originX + ii * xstep;
            cy = originY - (ystep * Math.max(1, freqBuffer[plotidxs[ii]] + 90));

            // both points above threshold
            if ((freqBuffer[plotidxs[ii]] > freqThreshDB[plotidxs[ii]]) && (freqBuffer[plotidxs[ii-1]] > freqThreshDB[plotidxs[ii-1]]))
            {
                paint.setColor(Color.rgb(184,230,46));
                canvas.drawLine(px, py, cx, cy, paint);
            }
            // first above, second below
            else if ((freqBuffer[plotidxs[ii]] > freqThreshDB[plotidxs[ii]]) && (freqBuffer[plotidxs[ii-1]] < freqThreshDB[plotidxs[ii-1]]))
            {
                paint.setColor(Color.rgb(184,230,46));
                rr = (freqBuffer[plotidxs[ii]] - freqThreshDB[plotidxs[ii]]) / Math.abs(freqBuffer[plotidxs[ii]] - freqBuffer[plotidxs[ii-1]]);
                tx = originX + (ii * xstep) - xstep * rr; // next x value
                ty = originY - ystep*Math.max(1, freqThreshDB[plotidxs[ii]] + 90);
                canvas.drawLine(cx, cy, tx, ty, paint);
                paint.setColor(Color.LTGRAY);
                canvas.drawLine(px, py, tx, ty, paint);
            }
            // first below, second above
            else if ((freqBuffer[plotidxs[ii]] < freqThreshDB[plotidxs[ii]]) && (freqBuffer[plotidxs[ii-1]] > freqThreshDB[plotidxs[ii-1]]))
            {
                paint.setColor(Color.rgb(184,230,46));
                rr = (freqBuffer[plotidxs[ii-1]] - freqThreshDB[plotidxs[ii-1]]) / Math.abs(freqBuffer[plotidxs[ii]] - freqBuffer[plotidxs[ii-1]]);
                tx = originX + ((ii-1) * xstep) + xstep * rr; // next x value
                ty = originY - ystep*Math.max(1, freqThreshDB[plotidxs[ii-1]] + 90);
                canvas.drawLine(px, py, tx, ty, paint);
                paint.setColor(Color.LTGRAY);
                canvas.drawLine(cx, cy, tx, ty, paint);
            }
            // both points below threshold
            else
            {
                paint.setColor(Color.LTGRAY);
                canvas.drawLine(px, py, cx, cy, paint);
            }
        }
        */
        // plot refresh happens on gui thread
        image.postInvalidate();
    }


    ///////////////////////////////////////
    // plot frequency domain threshold and values
    public void plotThreshold(float [] freqDBArray, int color)
    {
        int ii;

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        //paint.setStrokeWidth(SettingsStn.dd.plotLineWidth / 2);

        float[] drawPoints = new float[plotLength*2];

        // draw graph
        for (ii=0; ii < plotLength; ii++)
        {
            drawPoints[2*ii] = ii*xstep + originX;
            drawPoints[2*ii+1] = originY - (ystep*(freqDBArray[plotidxs[ii]]+90));
        }
        for (ii=1; ii < plotLength; ii++)
            canvas.drawLine(drawPoints[2*(ii-1)], drawPoints[2*(ii-1)+1], drawPoints[2*ii], drawPoints[2*ii+1], paint);
    }


    ////////////////////////////
    public void refreshPlot()
    {
        image.setImageBitmap(bmap);
    }



}
