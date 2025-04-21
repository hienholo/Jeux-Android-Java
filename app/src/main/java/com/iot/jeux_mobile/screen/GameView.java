package com.iot.jeux_mobile.screen;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.Random;

public class GameView extends View {
    private ArrayList<Pod> pods = new ArrayList<>();
    private int screenWidth, screenHeight;
    private final Random random = new Random();
    private int score = 0;
    private long startTime = 0;
    private int gameDuration;

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setBackgroundColor(getResources().getColor(android.R.color.black));
    }

    public void initGame(int duration) {
        this.gameDuration = duration;
        this.startTime = System.currentTimeMillis();
        this.score = 0;
        spawnPods();
    }

    public long getStartTime() {
        return startTime;
    }

    private void spawnPods() {
        pods.clear();
        if (screenWidth == 0 || screenHeight == 0) return;

        int podCount = 5 + random.nextInt(6); // 5-10 pods
        for (int i = 0; i < podCount; i++) {
            int size = 80 + random.nextInt(60); // 80-140dp
            int x = random.nextInt(screenWidth - size);
            int y = random.nextInt(screenHeight - size);
            int colorType = random.nextInt(3); // 0=red, 1=blue, 2=green

            pods.add(new Pod(x, y, size, colorType));
        }
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;
        if (startTime > 0) { // Si le jeu est déjà lancé
            spawnPods();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Pod pod : pods) {
            pod.draw(canvas);
        }
    }

    public boolean checkTouch(float x, float y) {
        for (Pod pod : pods) {
            if (pod.contains(x, y)) {
                if (pod.isRed()) {
                    score++;
                    spawnPods();
                    return true;
                }
                return false; // Touched wrong color
            }
        }
        return true;
    }

    public boolean isGameOver() {
        return startTime == 0 || (System.currentTimeMillis() - startTime) / 1000 >= gameDuration;
    }

    public int getScore() {
        return score;
    }

    private static class Pod {
        private final int x, y, size, colorType;
        private final ShapeDrawable drawable;

        public Pod(int x, int y, int size, int colorType) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.colorType = colorType;

            drawable = new ShapeDrawable(new OvalShape());
            drawable.setBounds(x, y, x + size, y + size);

            int color;
            switch (colorType) {
                case 0:
                    color = 0xFFFF0000;
                    break;
                // red
                case 1:
                    color = 0xFF0000FF;
                    break;
                // blue
                case 2:
                    color = 0xFF00FF00;
                    break;
                // green
                default:
                    color = 0xFFFF0000;
                    break;
            }
            drawable.getPaint().setColor(color);
        }

        public void draw(Canvas canvas) {
            drawable.draw(canvas);
        }

        public boolean contains(float x, float y) {
            return x >= this.x && x <= this.x + size && y >= this.y && y <= this.y + size;
        }

        public boolean isRed() {
            return colorType == 0;
        }
    }
}