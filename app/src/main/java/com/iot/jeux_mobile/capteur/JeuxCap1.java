package com.iot.jeux_mobile.capteur;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Random;

public class JeuxCap1 extends AppCompatActivity implements SensorEventListener {

    private GameView gameView;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private TextView scoreText;
    private Button resetButton;
    private int score = 0;
    private boolean gameOver = false;
    private Handler spawnHandler = new Handler();
    private static final int SPAWN_INTERVAL = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        gameView = new GameView(this);
        scoreText = new TextView(this);
        scoreText.setText("Score: 0");
        scoreText.setTextSize(24);

        resetButton = new Button(this);
        resetButton.setText("Nouvelle partie");
        resetButton.setOnClickListener(v -> resetGame());

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(scoreText);
        layout.addView(gameView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        layout.addView(resetButton);

        setContentView(layout);
    }

    private void resetGame() {
        score = 0;
        gameOver = false;
        scoreText.setText("Score: 0");
        gameView.reset();
        startHoleSpawning();
    }

    private void startHoleSpawning() {
        spawnHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!gameOver) {
                    gameView.addNewHole();
                    gameView.addNewHole();
                    gameView.addNewHole();
                    spawnHandler.postDelayed(this, SPAWN_INTERVAL);
                }
            }
        }, SPAWN_INTERVAL);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        if (!gameOver) {
            startHoleSpawning();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        spawnHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!gameOver && event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if (Math.abs(event.values[0]) > 0.2f || Math.abs(event.values[1]) > 0.2f) {
                gameView.updateBall(-event.values[0], event.values[1]);
                score += new Random().nextInt(2) + 2; // 2 ou 3
                runOnUiThread(() -> scoreText.setText("Score: " + score));
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public void gameOver() {
        gameOver = true;
        spawnHandler.removeCallbacksAndMessages(null);
        runOnUiThread(() -> showGameOverDialog());
    }

    private void showGameOverDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Jeu Terminé")
                .setMessage("Votre score: " + score)
                .setPositiveButton("Rejouer", (dialog, which) -> resetGame())
                .setCancelable(false)
                .show();
    }

    class GameView extends SurfaceView implements SurfaceHolder.Callback {

        private final SurfaceHolder holder;
        private final Paint ballPaint = new Paint();
        private final Paint holePaint = new Paint();
        private final Random random = new Random();

        private float ballX, ballY;
        private float ballRadius = 30f;
        private float holeRadius = 50f;
        private ArrayList<Hole> holes = new ArrayList<>();
        private float width, height;
        private final Handler movementHandler = new Handler();
        private final int MOVEMENT_INTERVAL = 30;

        public GameView(Context context) {
            super(context);
            holder = getHolder();
            holder.addCallback(this);

            ballPaint.setColor(Color.BLUE);
            holePaint.setColor(Color.BLACK);
        }

        class Hole {
            float x, y, dx, dy;

            Hole(float x, float y, float dx, float dy) {
                this.x = x;
                this.y = y;
                this.dx = dx;
                this.dy = dy;
            }

            void updatePosition() {
                x += dx;
                y += dy;
                if (x < holeRadius || x > width - holeRadius) dx = -dx;
                if (y < holeRadius || y > height - holeRadius) dy = -dy;
            }
        }

        public void updateBall(float dx, float dy) {
            if (gameOver) return;

            ballX += dx * 5;
            ballY += dy * 5;

            ballX = Math.max(ballRadius, Math.min(width - ballRadius, ballX));
            ballY = Math.max(ballRadius, Math.min(height - ballRadius, ballY));

            for (Hole hole : holes) {
                float distance = (float) Math.sqrt(Math.pow(ballX - hole.x, 2) + Math.pow(ballY - hole.y, 2));
                if (distance < holeRadius - 10) {
                    ((JeuxCap1) getContext()).gameOver();
                    break;
                }
            }

            draw();
        }

        public void addNewHole() {
            float x = random.nextFloat() * (width - 2 * holeRadius) + holeRadius;
            float y = random.nextFloat() * (height - 2 * holeRadius) + holeRadius;
            float dx = (random.nextFloat() - 0.5f) * 6;
            float dy = (random.nextFloat() - 0.5f) * 6;
            holes.add(new Hole(x, y, dx, dy));
            draw();
        }

        public void reset() {
            ballX = width / 2;
            ballY = height / 2;
            holes.clear();
            addNewHole();
            startMovingHoles();
            draw();
        }

        private void startMovingHoles() {
            movementHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!gameOver) {
                        for (Hole hole : holes) {
                            hole.updatePosition();
                        }
                        draw();
                        movementHandler.postDelayed(this, MOVEMENT_INTERVAL);
                    }
                }
            }, MOVEMENT_INTERVAL);
        }

        private void draw() {
            if (!holder.getSurface().isValid()) return;

            Canvas canvas = holder.lockCanvas();
            canvas.drawColor(Color.WHITE);

            for (Hole hole : holes) {
                canvas.drawCircle(hole.x, hole.y, holeRadius, holePaint);
            }

            canvas.drawCircle(ballX, ballY, ballRadius, ballPaint);
            holder.unlockCanvasAndPost(canvas);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            width = getWidth();
            height = getHeight();
            reset();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {}
    }
}
