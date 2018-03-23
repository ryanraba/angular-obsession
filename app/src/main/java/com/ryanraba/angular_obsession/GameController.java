package com.ryanraba.angular_obsession;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


class GameController {
    private Drawable launcher;
    private Drawable projectile;
    private List<Drawable[]> blocks;
    private List<int[][]> bstats;
    private Drawable wall;
    private ProgressBar turntracker;
    private Resources res;
    private MediaPlayer launch_player;
    private MediaPlayer hit_player, big_hit_player;
    private MediaPlayer miss_player;
    private Canvas bitmapcanvas;
    private Bitmap bitmap;


    private int[] projectile_ids = {R.drawable.ball, R.drawable.ball2, R.drawable.ball3, R.drawable.ball4};
    private int[] block_ids = {R.drawable.block, R.drawable.block2, R.drawable.block3, R.drawable.block4, R.drawable.block5};
    private boolean[] active_states;
    private int maxX;    // max X of plot
    private int maxY;    // max Y of plot
    private int centerX;
    private int BLOCK_X = 10;
    private int BLOCK_Y = 10;
    private int MINOR_TURNS = 5;
    private int BIG_HIT = 4;

    private int BTP_IDX = 2;  // index of block type
    private int PTT_IDX = 3;  // index of path to top indicator

    private int projectile_x, projectile_y, launcher_y;
    private int block_width;
    private float launcher_angle, ball_angle;
    private int bounce_count, turn_count, gamescore, gameover;
    private int projectile_increment, projectile_state;
    private boolean projectile_fired, collision;

    Random rand = new Random();

    ///////////////////////////
    // Constructor
    ///////////////////////////
    GameController(Resources res_i, TextView scorebox_i, ProgressBar tt_i, MediaPlayer lp_i, MediaPlayer hp_i, MediaPlayer bhp_i, MediaPlayer mp_i, int width, int height) {
        res = res_i;
        maxX = width;
        maxY = height;
        centerX = maxX / 2;

        launch_player = lp_i;
        hit_player = hp_i;
        big_hit_player = bhp_i;
        miss_player = mp_i;
        launch_player.setVolume((float)0.8, (float)0.8);
        hit_player.setVolume((float)0.8, (float)0.8);
        big_hit_player.setVolume((float)0.8, (float)0.8);
        miss_player.setVolume((float)0.99, (float)0.99);

        launcher = res.getDrawable(R.drawable.launcher_basic_24dp);
        wall = res.getDrawable(R.drawable.wall);
        projectile_state = rand.nextInt(4);
        projectile = res.getDrawable(projectile_ids[projectile_state]);

        launcher_y = maxY / 20;
        block_width = (int)((0.5*maxY) / BLOCK_Y);
        BLOCK_X = Math.round((float)maxX/(float)block_width);
        block_width = Math.round((float)maxX/(float)BLOCK_X);  // changes a little due to rounding
        projectile_x = (launcher_y/2+block_width/2) + centerX;
        projectile_y = maxY - launcher_y/2 - (launcher_y/2+block_width/2);
        projectile_increment = block_width/3;

        blocks = new ArrayList<>();
        bstats = new ArrayList<>();
        for (int ii = 0; ii < BLOCK_Y; ii++)
        {
            blocks.add(new Drawable[BLOCK_X]);
            bstats.add(new int[BLOCK_X][4]);
            for (int jj = 0; jj < BLOCK_X; jj++) {
                bstats.get(ii)[jj][1] = jj * block_width;  // set column position
                bstats.get(ii)[jj][PTT_IDX] = 0;
                bstats.get(ii)[jj][BTP_IDX] = rand.nextInt(4);  // set block type
                if (rand.nextInt(10) < 1) bstats.get(ii)[jj][BTP_IDX] = 4; // set to wall type
                blocks.get(ii)[jj] = res.getDrawable(block_ids[bstats.get(ii)[jj][BTP_IDX]]);
            }
        }

        // check for trapped blocks
        boolean trapped;
        for (int ii = 0; ii < BLOCK_Y; ii++)
            for (int jj = 0; jj < BLOCK_X; jj++)
            {
                trapped = true;
                if ((ii>0) && (bstats.get(ii-1)[jj][BTP_IDX] != 4)) trapped = false;
                else if ((jj>0) && (bstats.get(ii)[jj-1][BTP_IDX] != 4)) trapped = false;
                else if ((ii<BLOCK_Y-2) && (bstats.get(ii+1)[jj][BTP_IDX] != 4)) trapped = false;
                else if ((jj<BLOCK_X-2) && (bstats.get(ii)[jj+1][BTP_IDX] != 4)) trapped = false;
                if (trapped)
                {
                    bstats.get(ii)[jj][BTP_IDX] = 4;
                    blocks.get(ii)[jj] = res.getDrawable(block_ids[4]);
                }
            }


        active_states = new boolean[]{true, true, true, true};
        launcher_angle = 0;
        ball_angle = 0;
        projectile_fired = false;
        bounce_count = 0;
        turn_count = 0;
        collision = false;
        gameover = 0;
        gamescore = 0;

        turntracker = tt_i;
        turntracker.getLayoutParams().height = (int)(0.6*block_width);
        turntracker.getLayoutParams().width = (int)(2.0*block_width);
        turntracker.setProgress(0);

        scorebox_i.setText("0");
        scorebox_i.getLayoutParams().height = (int)(0.6*block_width);
        scorebox_i.getLayoutParams().width = (int)(2.0*block_width);

        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmapcanvas = new Canvas(bitmap);
        launcher.setBounds(centerX - maxX/25, maxY - launcher_y, centerX + maxX/25, maxY);
        setBlocks();
        drawBlocks();
    }


    /////////////////////////
    public View.OnTouchListener launchListener = new View.OnTouchListener()
    {
        public boolean onTouch(View v, MotionEvent event)
        {
            float px, py;

            if (projectile_fired) return true;

            //System.out.println("MAX x, y = " + maxX + ", " + maxY);
            //System.out.println("Touch at " + event.getX() + ", " + event.getY());

            // rotate launcher to follow finger
            px = event.getX() - (maxX / 2);
            py = maxY - event.getY();
            if (py <= 0)
                py = 1;
            if (px == 0)
                launcher_angle = 0;
            else
                launcher_angle = 1*((float)Math.toDegrees(Math.atan(px/py)));

            if (launcher_angle > 75) launcher_angle = 75;
            else if (launcher_angle < -75) launcher_angle = -75;
            //System.out.println("Rotate at " + rot_angle);

            if (event.getAction() == MotionEvent.ACTION_UP)
            {
                projectile_fired = true;
                launch_player.start();
            }
            //else if (event.getAction() == MotionEvent.ACTION_DOWN)

            ball_angle = launcher_angle;
            return true;
        }
    };


    /////////////////////////////////////////
    private int recursiveHitCheck(int rr, int cc, int state, boolean top)
    {
        int hitcount=0;

        if ((rr < 0) || (rr >= BLOCK_Y) || (cc < 0) || (cc >= BLOCK_X)) return 0;
        if (!blocks.get(rr)[cc].isVisible()) return 0;
        if (bstats.get(rr)[cc][BTP_IDX] != state) return 0;

        blocks.get(rr)[cc].setVisible(false, false);
        hitcount += recursiveHitCheck(rr-1, cc, state, false);
        hitcount += recursiveHitCheck(rr+1, cc, state, false);
        hitcount += recursiveHitCheck(rr, cc-1, state, false);
        hitcount += recursiveHitCheck(rr, cc+1, state, false);

        if (top && (hitcount==0))
            blocks.get(rr)[cc].setVisible(true, false);
        else
            hitcount += 1;

        return hitcount;
    }


    //////////////////////////////////////////
    private void recursivePathToTop(int rr, int cc)
    {
        // out of bounds
        if ((rr < 0) || (rr >= BLOCK_Y) || (cc < 0) || (cc >= BLOCK_X)) return;

        if ( (!blocks.get(rr)[cc].isVisible()) || (bstats.get(rr)[cc][PTT_IDX] == 1) ) return;

        bstats.get(rr)[cc][PTT_IDX] = 1;
        recursivePathToTop(rr-1, cc);
        recursivePathToTop(rr+1, cc);
        recursivePathToTop(rr, cc-1);
        recursivePathToTop(rr, cc+1);
    }



    ///////////////////////////////////////
    private int checkCollision()
    {
        Rect bb;
        int hitcount, sr, sc, mr=-1, mc=-1;
        int major_turns = turn_count / MINOR_TURNS + 1;
        float px, py, bx, by, fr, fc;
        double dd, shortest;
        boolean hitguard;

        px = projectile.getBounds().exactCenterX();
        py = projectile.getBounds().exactCenterY();

        fr = (py - block_width/2 - major_turns*block_width) / block_width;
        if (fr >= BLOCK_Y) return 0;
        fc = ((px-block_width/2)/maxX)*BLOCK_X;

        //sr = (int)Math.floor(fr);
        //sc = Math.max(Math.min(Math.round(fc),BLOCK_X-1), 0);

        // if we hit the ceiling
        //if ((sr <= 0) && (!blocks.get(0)[sc].isVisible()))
        //{
        //    mr = 0;
        //    mc = sc;
        //}
        //else
        //{

        // find closest visible block to current projectile position
        mr = -1;
        mc = -1;
        shortest = 999999999.0;
        for (int mm = (int)Math.floor(fr); mm <= (int)Math.ceil(fr); mm++)
            for (int nn = (int)Math.floor(fc); nn <= (int)Math.ceil(fc); nn++)
            {
                if ((nn < 0) || (mm >= BLOCK_Y) || (nn >= BLOCK_X)) continue;
                if ((mm >= 0) && (!blocks.get(mm)[nn].isVisible())) continue;
                bb = blocks.get(Math.max(mm,0))[nn].getBounds();
                if (mm < 0) bb.offset(0, -block_width);  // check collision with ceiling
                dd = Math.pow(px - bb.centerX(), 2) + Math.pow(py - bb.centerY(), 2);
                if (dd < shortest) {
                    mr = mm;
                    mc = nn;
                    shortest = dd;
                }
            }
        if (mc < 0) return 0;
        sr = mr;
        sc = mc;

        bb = blocks.get(Math.max(sr,0))[sc].getBounds();
        if (sr < 0) bb.offset(0, -block_width);  // pseudo ceiling block

        // since projectile is circle not square, check radius
        bx = bb.exactCenterX();
        by = bb.exactCenterY();
        hitguard = Math.sqrt(Math.pow(px-bx,2) + Math.pow(py-by,2)) > (0.85*block_width);
        if (hitguard && (sr >= 0) && (bstats.get(sr)[sc][BTP_IDX] != projectile_state)) return 0;

        // collision between projectile and a block has occurred
        // create or re-activate a block in the location of collision and then check for
        // contiguous blocks of same state to destroy

        if (sr == (BLOCK_Y - 1)) // collided with bottom row
        {
            blocks.add(new Drawable[BLOCK_X]);
            bstats.add(new int[BLOCK_X][4]);
            BLOCK_Y = BLOCK_Y + 1;
            for (int ll = 0; ll < BLOCK_X; ll++) {
                bstats.get(sr + 1)[ll][1] = ll * block_width;  // set column position
                bstats.get(sr + 1)[ll][BTP_IDX] = projectile_state;  // set block type
                bstats.get(sr + 1)[ll][PTT_IDX] = 0;
                blocks.get(sr + 1)[ll] = res.getDrawable(block_ids[bstats.get(sr + 1)[ll][BTP_IDX]]);
                blocks.get(sr + 1)[ll].setBounds(bstats.get(sr + 1)[ll][1], bb.bottom,
                        bstats.get(sr + 1)[ll][1] + block_width,
                        bb.bottom + block_width);
                blocks.get(sr + 1)[ll].setVisible(false, false);
            }
        }

        // scan around block and find something to reactivate
        // should be able to reactivate invisible block
        shortest = 999999999.0;
        for (int mm = sr - 1; mm <= sr + 1; mm++)
            for (int nn = sc - 1; nn <= sc + 1; nn++) {
                if ((Math.abs(sr-mm) + Math.abs(sc-nn)) >= 2) continue;
                if ((mm == sr) & (nn == sc)) continue;
                if ((mm < 0) || (nn < 0) || (mm >= BLOCK_Y) || (nn >= BLOCK_X)) continue;
                if (blocks.get(mm)[nn].isVisible()) continue;
                dd = Math.pow(px - blocks.get(mm)[nn].getBounds().centerX(), 2);
                dd += Math.pow(py - blocks.get(mm)[nn].getBounds().centerY(), 2);
                if (dd < shortest) {
                    mr = mm;
                    mc = nn;
                    shortest = dd;
                }
            }
        //}

        // re-activate closest block
        //System.out.println("activating " + mr +", "+mc);
        bstats.get(mr)[mc][BTP_IDX] = projectile_state;  // set block type
        blocks.get(mr)[mc] = res.getDrawable(block_ids[bstats.get(mr)[mc][BTP_IDX]]);
        blocks.get(mr)[mc].setVisible(true, false);

        collision = true;
        hitcount = recursiveHitCheck(mr, mc, projectile_state, true);

        if (hitcount == 0)
            miss_player.start();
        else // spawn recursive checks for free groups of blocks
        {
            hitcount = hitcount - 1; // dont count projectile
            for (int nn = 0; nn < BLOCK_X; nn++)
                recursivePathToTop(0, nn);
            // remove any block not connected to top wall
            for (int mm=0; mm<BLOCK_Y; mm++)
                for (int nn=0; nn<BLOCK_X; nn++)
                {
                    if (!blocks.get(mm)[nn].isVisible()) continue;
                    if (bstats.get(mm)[nn][PTT_IDX] == 0)
                    {
                        blocks.get(mm)[nn].setVisible(false, false);
                        hitcount += 1;
                    }
                }
            if (hitcount >= BIG_HIT) big_hit_player.start();
            else hit_player.start();
        }

        // return count of contiguous blocks of same state
        return hitcount;
    }


    ///////////////////////////////////////
    private void setBlocks()
    {
        int tt;
        int major_turns = turn_count / MINOR_TURNS + 1;

        wall.setBounds(0, 0, maxX, major_turns * block_width);

        for (int ii = BLOCK_Y - 1; ii >= 0; ii--) {
            tt = ii * block_width + major_turns * block_width;
            for (int jj = 0; jj < BLOCK_X; jj++) {
                blocks.get(ii)[jj].setBounds(bstats.get(ii)[jj][1], tt, bstats.get(ii)[jj][1] + block_width, tt + block_width);
                bstats.get(ii)[jj][PTT_IDX] = 0;   // reset path to top check
            }
        }
    }


    ///////////////////////////////////////
    public void setProjectilePosition()
    {
        if (projectile_fired)
        {
            projectile_x += (int)(projectile_increment * Math.sin(Math.toRadians(ball_angle)));
            projectile_y -= (int)(projectile_increment * Math.cos(Math.toRadians(ball_angle)));
        }
        else
        {
            projectile_x = (int)((launcher_y/2+block_width/2) * Math.sin(Math.toRadians(ball_angle)) + centerX);
            projectile_y = (int)(maxY - launcher_y/2 - (launcher_y/2+block_width/2) * Math.cos(Math.toRadians(ball_angle)));
        }

        if (((projectile_x-block_width/4) <= 0) || ((projectile_x+block_width/4) > maxX))
        {
            ball_angle = -ball_angle;
            bounce_count = bounce_count + 1;
        }
    }


    //////////////////////////////////////////
    private int drawBlocks()
    {
        int remainingblocks = 0;

        bitmapcanvas.drawColor(Color.WHITE);
        wall.draw(bitmapcanvas);

        for (int ii=0; ii<4; ii++) active_states[ii] = false;

        for (int ii=0; ii<BLOCK_Y; ii++)
            for (int jj=0; jj<BLOCK_X; jj++)
                if (blocks.get(ii)[jj].isVisible())
                {
                    blocks.get(ii)[jj].draw(bitmapcanvas);
                    if (bstats.get(ii)[jj][BTP_IDX] != 4)
                    {
                        active_states[bstats.get(ii)[jj][BTP_IDX]] = true;
                        remainingblocks += 1;
                    }
                    // blocks reached bottom of play area
                    if (blocks.get(ii)[jj].getBounds().bottom >= (maxY - launcher_y - 1 * block_width))
                        gameover = 1;
                }

        return remainingblocks;
    }


    ///////////////////////////////////////
    // runs game
    public int[] animateGame(Canvas canvas)
    {
        int[] rc = {-1, -1, -1};  // gameover, remaining blocks, gamescore

        int hits=0, pw = block_width/2;

        canvas.drawBitmap(bitmap, 0, 0, null);

        setProjectilePosition();

        canvas.save();
        canvas.rotate(launcher_angle, centerX, maxY - (launcher_y/2));

        launcher.draw(canvas);

        canvas.restore();

        projectile.setBounds(projectile_x-pw, projectile_y-pw,projectile_x+pw, projectile_y+pw);
        projectile.draw(canvas);

        // check if ball has collided with a block(s)
        if (projectile_fired)
            hits = checkCollision();

        // collision occurred
        if ((collision) || (bounce_count >= 10))
        {
            gamescore = gamescore + 100*hits;
            if (hits > 1) gamescore += 30 * (hits*hits);

            projectile_fired = false;
            ball_angle = launcher_angle;
            bounce_count = 0;
            collision = false;
            if (hits >= BIG_HIT) turn_count = (turn_count / MINOR_TURNS) * MINOR_TURNS;
            else turn_count += 1;
            turntracker.setProgress( (100 * (turn_count % MINOR_TURNS)) / (MINOR_TURNS-1));

            setBlocks();
            rc[1] = drawBlocks();
            if (rc[1] == 0) {
                gameover = 1;
                gamescore = gamescore + 5000;
            }
            else
            {
                for (int xx=0; xx<100; xx++) {
                    projectile_state = rand.nextInt(4);
                    if (active_states[projectile_state]) break;
                }
                projectile = res.getDrawable(projectile_ids[projectile_state]);
            }
        }

        rc[0] = gameover;
        rc[2] = gamescore;
        return rc;
    }

}
