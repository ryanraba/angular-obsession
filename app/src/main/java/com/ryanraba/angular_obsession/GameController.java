package com.ryanraba.angular_obsession;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;


class GameController {
    private Drawable launcher;
    private Drawable projectile;
    private Drawable[] blocks;
    private Drawable wall;
    private Resources res;

    private int[] projectile_ids = {R.drawable.ball, R.drawable.ball2, R.drawable.ball3};
    private int[] block_ids = {R.drawable.block, R.drawable.block2, R.drawable.block3};
    private int maxX;    // max X of plot
    private int maxY;    // max Y of plot
    private int centerX;

    private int launcher_wx, launcher_y;
    private int projectile_x, projectile_y;
    private int projectile_sx, projectile_sy;
    private float launcher_angle, ball_angle;
    private int bounce_count, turn_count;
    private int projectile_increment, projectile_state;

    Random rand = new Random();

    boolean projectile_fired, collision, gameover;


    ///////////////////////////
    // Constructor
    ///////////////////////////
    GameController(Resources res_i, int width, int height, int density_i)
    {
        int[] block_state = {0};

        res = res_i;
        maxX = width;
        maxY = height;
        centerX = maxX / 2;

        launcher = res.getDrawable(R.drawable.launcher_basic_24dp);
        wall = res.getDrawable(R.drawable.wall);
        blocks = new Drawable[10*5];
        for (int ii=0; ii<blocks.length; ii++)
        {
            block_state[0] = rand.nextInt(3);
            blocks[ii] = res.getDrawable(block_ids[block_state[0]]);
            blocks[ii].setState(block_state);
        }
        projectile_state = rand.nextInt(3);
        projectile = res.getDrawable(projectile_ids[projectile_state]);

        launcher_wx = maxX/25;
        launcher_y = maxY/20;
        projectile_x = maxX/10;
        projectile_y = maxX/10;
        projectile_increment = (int)(10 * ((float)density_i / 160f));

        launcher_angle = 0;
        ball_angle = 0;
        projectile_sx = projectile_x;
        projectile_sy = projectile_y;
        projectile_fired = false;
        bounce_count = 0;
        turn_count = 0;
        collision = false;
        gameover = false;
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
            }
            //else if (event.getAction() == MotionEvent.ACTION_DOWN)

            ball_angle = launcher_angle;
            return true;
        }
    };


    ///////////////////////////////////////
    public void setBlocks(Canvas canvas)
    {
        int ll,tt;
        boolean visibleblocks = false;

        wall.setBounds(0, 0, maxX, turn_count*projectile_sy);
        wall.draw(canvas);
        if (Rect.intersects(wall.getBounds(), projectile.getBounds())) collision = true;

        for (int ii=0; ii<blocks.length; ii++)
        {
            ll = (ii%10)*projectile_sx;
            tt = (ii/10)*projectile_sy + turn_count*projectile_sy;
            blocks[ii].setBounds(ll, tt, ll+projectile_sx, tt+projectile_sy);

            if (blocks[ii].isVisible() && (Rect.intersects(blocks[ii].getBounds(), projectile.getBounds())))
            {
                collision = true;
                blocks[ii].setVisible(false, false);
            }

            if (blocks[ii].isVisible() && (tt >= (maxY - launcher_y - 2*projectile_sy)))
                gameover = true;

            if (blocks[ii].isVisible())
            {
                blocks[ii].draw(canvas);
                visibleblocks = true;
            }
        }
        if (!visibleblocks) gameover = true;
    }


    ///////////////////////////////////////
    public void setProjectilePosition()
    {
        if ((projectile_y <= 0) || (bounce_count >= 10) || (collision))
        {
            projectile_fired = false;
            ball_angle = launcher_angle;
            bounce_count = 0;
            collision = false;
            turn_count = turn_count + 1;
            projectile_state = rand.nextInt(3);
            projectile = res.getDrawable(projectile_ids[projectile_state]);
        }
        else if ((projectile_x <= 0) || (projectile_x > maxX))
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


    ///////////////////////////////////////
    // runs game
    public boolean animateGame(Canvas canvas)
    {
        int pw = projectile_sx/2;

        canvas.drawColor(Color.WHITE);

        setBlocks(canvas);
        setProjectilePosition();

        canvas.save();
        canvas.rotate(launcher_angle, centerX, maxY - (launcher_y/2));

        launcher.setBounds(centerX - launcher_wx, maxY - launcher_y, centerX + launcher_wx, maxY);
        launcher.draw(canvas);

        canvas.restore();

        projectile.setBounds(projectile_x-pw, projectile_y-pw,projectile_x+pw, projectile_y+pw);
        projectile.draw(canvas);

        //System.out.println("################## got here ###################");
        return gameover;
    }

}
