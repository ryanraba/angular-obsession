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
    private TextView scorebox;
    private ProgressBar turntracker;
    private Resources res;
    private MediaPlayer launch_player;
    private MediaPlayer hit_player;
    private MediaPlayer miss_player;
    private Canvas bitmapcanvas;
    private Bitmap bitmap;


    private int[] projectile_ids = {R.drawable.ball, R.drawable.ball2, R.drawable.ball3, R.drawable.ball4};
    private int[] block_ids = {R.drawable.block, R.drawable.block2, R.drawable.block3, R.drawable.block4};
    private int maxX;    // max X of plot
    private int maxY;    // max Y of plot
    private int centerX;
    private int BLOCK_X = 16;
    private int BLOCK_Y = 10;
    private int MINOR_TURNS = 5;
    private double SCALE_COEF = 0.5;

    private int BTP_IDX = 2;  // index of block type
    private int PTT_IDX = 3;  // index of path to top indicator

    private int launcher_wx, launcher_y;
    private int projectile_x, projectile_y;
    private int projectile_sx, projectile_sy;
    private float launcher_angle, ball_angle;
    private int bounce_count, turn_count, gamescore;
    private int projectile_increment, projectile_state;

    Random rand = new Random();

    boolean projectile_fired, collision, gameover;


    ///////////////////////////
    // Constructor
    ///////////////////////////
    GameController(Resources res_i, TextView scorebox_i, ProgressBar tt_i, MediaPlayer lp_i, MediaPlayer hp_i, MediaPlayer mp_i, int width, int height, int density_i) {
        res = res_i;
        maxX = width;
        maxY = height;
        centerX = maxX / 2;

        launch_player = lp_i;
        hit_player = hp_i;
        miss_player = mp_i;
        launch_player.setVolume((float)0.8, (float)0.8);
        hit_player.setVolume((float)0.8, (float)0.8);
        miss_player.setVolume((float)0.99, (float)0.99);

        launcher = res.getDrawable(R.drawable.launcher_basic_24dp);
        wall = res.getDrawable(R.drawable.wall);
        projectile_state = rand.nextInt(4);
        projectile = res.getDrawable(projectile_ids[projectile_state]);

        launcher_wx = maxX / 25;
        launcher_y = maxY / 20;
        float screen_ratio = ((float)(maxY - launcher_y)) / maxX;
        BLOCK_X = Math.round((float)BLOCK_X / screen_ratio);
        System.out.println("########## screen ratio / B_X = " + screen_ratio + " / " + BLOCK_X + " ##############");
        projectile_sx = maxX / BLOCK_X;
        projectile_sy = maxX / BLOCK_X;
        projectile_x = (launcher_y/2+projectile_sx/2) + centerX;
        projectile_y = maxY - launcher_y/2 - (launcher_y/2+projectile_sy/2);
        projectile_increment = (int) (10 * ((float) density_i / 160f));

        blocks = new ArrayList<>(); //Drawable[BLOCK_Y][BLOCK_X];
        bstats = new ArrayList<>(); //int[BLOCK_Y][BLOCK_X][4];  // row, col, state, pathtotop
        for (int ii = 0; ii < BLOCK_Y; ii++)
        {
            blocks.add(new Drawable[BLOCK_X]);
            bstats.add(new int[BLOCK_X][4]);
            for (int jj = 0; jj < BLOCK_X; jj++) {
                bstats.get(ii)[jj][1] = jj * projectile_sx;  // set column position
                bstats.get(ii)[jj][BTP_IDX] = rand.nextInt(4);  // set block type
                bstats.get(ii)[jj][PTT_IDX] = 0;
                blocks.get(ii)[jj] = res.getDrawable(block_ids[bstats.get(ii)[jj][BTP_IDX]]);
            }
        }

        launcher_angle = 0;
        ball_angle = 0;
        projectile_fired = false;
        bounce_count = 0;
        turn_count = 0;
        collision = false;
        gameover = false;
        gamescore = 0;

        scorebox = scorebox_i;
        scorebox.setText("0");
        scorebox.getLayoutParams().height = (int)(0.6*projectile_sy);
        scorebox.getLayoutParams().width = (int)(2.0*projectile_sy);

        turntracker = tt_i;
        turntracker.getLayoutParams().height = (int)(0.6*projectile_sy);
        turntracker.getLayoutParams().width = (int)(2.0*projectile_sy);
        turntracker.setProgress(0);

        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmapcanvas = new Canvas(bitmap);
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
        Drawable bb;
        int hitcount, sr, sc;
        int major_turns = turn_count / MINOR_TURNS + 1;
        float px, py, bx, by;
        boolean hitguard;

        px = projectile.getBounds().exactCenterX();
        py = projectile.getBounds().exactCenterY();
        sr = (int)((py - major_turns * projectile_sy) / projectile_sy);
        sc = (int)((px/maxX)*BLOCK_X);

        for (int ii=sr+1; ii >= sr-1; ii--)
            for (int jj=sc-1; jj <= sc+1; jj++)
            {
                if ((ii < 0) || (jj < 0) || (ii >= BLOCK_Y) || (jj >= BLOCK_X)) continue;
                bb = blocks.get(ii)[jj];
                if (!bb.isVisible()) continue;

                // score a hit
                // intersect sets block bounds to point of intersection
                if (bb.getBounds().intersect(projectile.getBounds()))
                {
                    // since projectile is circle not square, check radius
                    bx = bb.getBounds().exactCenterX();
                    by = bb.getBounds().exactCenterY();
                    hitguard = Math.sqrt(Math.pow(px-bx,2) + Math.pow(py-by,2)) > (SCALE_COEF*projectile_sy);
                    if (hitguard && (bstats.get(ii)[jj][BTP_IDX] != projectile_state)) continue;

                    // collision between projectile and a block has occurred
                    // create or re-activate a block in the location of collision and then check for
                    // contiguous blocks of same state to destroy

                    if (ii == (BLOCK_Y-1)) // collided with bottom row
                    {
                        blocks.add(new Drawable[BLOCK_X]);
                        bstats.add(new int[BLOCK_X][4]);
                        BLOCK_Y = BLOCK_Y + 1;
                        for (int ll = 0; ll < BLOCK_X; ll++) {
                            bstats.get(ii+1)[ll][1] = ll * projectile_sx;  // set column position
                            bstats.get(ii+1)[ll][BTP_IDX] = projectile_state;  // set block type
                            bstats.get(ii+1)[ll][PTT_IDX] = 0;
                            blocks.get(ii+1)[ll] = res.getDrawable(block_ids[bstats.get(ii+1)[ll][BTP_IDX]]);
                            blocks.get(ii+1)[ll].setBounds(bstats.get(ii+1)[ll][1], bb.getBounds().bottom,
                                    bstats.get(ii+1)[ll][1]+projectile_sx,
                                    bb.getBounds().bottom+projectile_sy);
                            blocks.get(ii+1)[ll].setVisible(false, false);
                        }
                        //System.out.println("################## got here ############## " + blocks.size());
                    }

                    // should be able to reactivate invisible block
                    int mr=ii, mc=jj;
                    double dd, shortest = 999999999.0;
                    boolean pathtotop;  // make sure reactivated block has a path to top
                    // scan around block and find something to reactivate
                    for (int mm=ii-1; mm<=ii+1; mm++)
                        for (int nn=jj-1; nn<=jj+1; nn++)
                        {
                            //if ((Math.abs(mm) + Math.abs(nn)) >= 2) continue;
                            if ((mm < 0) || (nn < 0) || (nn >= BLOCK_X)) continue;
                            if (blocks.get(mm)[nn].isVisible()) continue;
                            pathtotop = false;
                            if ((mm-1 > 0) && blocks.get(mm-1)[nn].isVisible()) pathtotop = true;
                            else if ((mm+1 < BLOCK_Y) && blocks.get(mm+1)[nn].isVisible()) pathtotop = true;
                            else if ((nn-1 > 0) && blocks.get(mm)[nn-1].isVisible()) pathtotop = true;
                            else if ((nn+1 < BLOCK_X) && blocks.get(mm)[nn+1].isVisible()) pathtotop = true;
                            if (!pathtotop) continue;
                            dd = Math.pow(px - blocks.get(mm)[nn].getBounds().centerX(), 2);
                            dd += Math.pow(py - blocks.get(mm)[nn].getBounds().centerY(), 2);
                            if (dd < shortest)
                            {
                                mr = mm;
                                mc = nn;
                                shortest = dd;
                            }
                        }

                    // re-activate closest block
                    bstats.get(mr)[mc][BTP_IDX] = projectile_state;  // set block type
                    blocks.get(mr)[mc] = res.getDrawable(block_ids[bstats.get(mr)[mc][BTP_IDX]]);
                    blocks.get(mr)[mc].setVisible(true, false);

                    collision = true;
                    hitcount = recursiveHitCheck(mr, mc, projectile_state, true);

                    if (hitcount == 0)
                        miss_player.start();
                    else // spawn recursive checks for free groups of blocks
                    {
                        hit_player.start();
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
                    }

                    // return count of contiguous blocks of same state
                    return hitcount;
                }
            }
        return 0;
    }


    ///////////////////////////////////////
    private void setBlocks()
    {
        int tt;
        int major_turns = turn_count / MINOR_TURNS + 1;

        wall.setBounds(0, 0, maxX, major_turns * projectile_sy);

        for (int ii = BLOCK_Y - 1; ii >= 0; ii--) {
            tt = ii * projectile_sy + major_turns * projectile_sy;
            for (int jj = 0; jj < BLOCK_X; jj++) {
                blocks.get(ii)[jj].setBounds(bstats.get(ii)[jj][1], tt, bstats.get(ii)[jj][1] + projectile_sx, tt + projectile_sy);
                bstats.get(ii)[jj][PTT_IDX] = 0;   // reset path to top check
            }
        }
    }


    ///////////////////////////////////////
    public void setProjectilePosition()
    {
        if ((projectile_x <= 0) || (projectile_x > maxX))
        {
            ball_angle = -ball_angle;
            bounce_count = bounce_count + 1;
        }

        if (projectile_fired)
        {
            projectile_x += (int)(projectile_increment * Math.sin(Math.toRadians(ball_angle)));
            projectile_y -= (int)(projectile_increment * Math.cos(Math.toRadians(ball_angle)));
        }
        else
        {
            projectile_x = (int)((launcher_y/2+projectile_sx/2) * Math.sin(Math.toRadians(ball_angle)) + centerX);
            projectile_y = (int)(maxY - launcher_y/2 - (launcher_y/2+projectile_sy/2) * Math.cos(Math.toRadians(ball_angle)));
        }
    }


    //////////////////////////////////////////
    private void drawBlocks()
    {
        boolean visibleblocks = false;

        bitmapcanvas.drawColor(Color.WHITE);
        wall.draw(bitmapcanvas);

        for (int ii=0; ii<BLOCK_Y; ii++)
            for (int jj=0; jj<BLOCK_X; jj++)
                if (blocks.get(ii)[jj].isVisible())
                {
                    blocks.get(ii)[jj].draw(bitmapcanvas);
                    visibleblocks = true;
                    // blocks reached bottom of play area
                    if (blocks.get(ii)[jj].getBounds().bottom >= (maxY - launcher_y - 1 * projectile_sy))
                        gameover = true;
                }

        if (!visibleblocks) gameover = true;
    }


    ///////////////////////////////////////
    // runs game
    public int animateGame(Canvas canvas)
    {
        int hits=0, pw = projectile_sx/2;

        canvas.drawBitmap(bitmap, 0, 0, null);

        setProjectilePosition();

        canvas.save();
        canvas.rotate(launcher_angle, centerX, maxY - (launcher_y/2));

        launcher.setBounds(centerX - launcher_wx, maxY - launcher_y, centerX + launcher_wx, maxY);
        launcher.draw(canvas);

        canvas.restore();

        projectile.setBounds(projectile_x-pw, projectile_y-pw,projectile_x+pw, projectile_y+pw);
        projectile.draw(canvas);


        // check if ball has collided with a block(s)
        if (projectile_fired)
            hits = checkCollision();

        // collision occurred
        if ((projectile_y <= wall.getBounds().bottom) || (bounce_count >= 10) || (collision))
        {
            gamescore = gamescore + 100*hits;
            if (hits > 1) gamescore += 10 * (hits*hits);

            projectile_fired = false;
            ball_angle = launcher_angle;
            bounce_count = 0;
            collision = false;
            turn_count = turn_count + 1;
            turntracker.setProgress( (100 * (turn_count % MINOR_TURNS)) / (MINOR_TURNS-1));
            projectile_state = rand.nextInt(4);
            projectile = res.getDrawable(projectile_ids[projectile_state]);
            setBlocks();
            drawBlocks();
        }

        //System.out.println("################## got here ###################");
        if (gameover) gamescore = -1;
        return gamescore;
    }

}
