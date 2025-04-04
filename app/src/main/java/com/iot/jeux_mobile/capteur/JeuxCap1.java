package com.iot.jeux_mobile.capteur;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;

public class JeuxCap1 extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private GameView gameView;
    private Button resetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialisation des capteurs
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Création de l'interface
        gameView = new GameView(this);
        resetButton = new Button(this);
        resetButton.setText("Réinitialiser");

        // Gestion du bouton pour réinitialiser
        resetButton.setOnClickListener(v -> gameView.resetBall());

        // Ajout des éléments au layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(gameView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        layout.addView(resetButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        setContentView(layout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gameView.updateBall(event.values[0], event.values[1]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public class GameView extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder holder;
        private Paint paintBall, paintHole;
        private float ballX, ballY, ballRadius = 50;
        private float screenWidth, screenHeight;
        private float[][] holes;
        private static final int HOLE_COUNT = 5;
        private float holeRadius = 60;
        private Random random;

        public GameView(Context context) {
            super(context);
            holder = getHolder();
            holder.addCallback(this);
            random = new Random();
            paintBall = new Paint();
            paintBall.setColor(Color.BLUE);
            paintHole = new Paint();
            paintHole.setColor(Color.BLACK);
            holes = new float[HOLE_COUNT][2];
        }

        public void updateBall(float x, float y) {
            ballX -= x * 3;
            ballY += y * 3;

            // Garder la bille dans l'écran
            if (ballX < ballRadius) ballX = ballRadius;
            if (ballX > screenWidth - ballRadius) ballX = screenWidth - ballRadius;
            if (ballY < ballRadius) ballY = ballRadius;
            if (ballY > screenHeight - ballRadius) ballY = screenHeight - ballRadius;

            // Vérifier la collision avec les trous
            for (float[] hole : holes) {
                if (Math.sqrt(Math.pow(ballX - hole[0], 2) + Math.pow(ballY - hole[1], 2)) < (holeRadius - 10)) {
                    resetBall();
                    break;
                }
            }

            draw();
        }

        public void resetBall() {
            ballX = screenWidth / 2;
            ballY = screenHeight / 2;
            draw();
        }

        private void draw() {
            if (holder.getSurface().isValid()) {
                Canvas canvas = holder.lockCanvas();
                if (canvas != null) {
                    canvas.drawColor(Color.WHITE);

                    // Dessiner les trous
                    for (float[] hole : holes) {
                        canvas.drawCircle(hole[0], hole[1], holeRadius, paintHole);
                    }

                    // Dessiner la bille
                    canvas.drawCircle(ballX, ballY, ballRadius, paintBall);

                    holder.unlockCanvasAndPost(canvas);
                }
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            screenWidth = getWidth();
            screenHeight = getHeight();
            resetBall();

            // Générer des trous aléatoirement (hors de la zone de départ)
            for (int i = 0; i < HOLE_COUNT; i++) {
                float holeX, holeY;
                do {
                    holeX = random.nextFloat() * (screenWidth - 2 * holeRadius) + holeRadius;
                    holeY = random.nextFloat() * (screenHeight - 2 * holeRadius) + holeRadius;
                } while (Math.sqrt(Math.pow(holeX - ballX, 2) + Math.pow(holeY - ballY, 2)) < 200); // Éviter la zone de départ
                holes[i][0] = holeX;
                holes[i][1] = holeY;
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
        }
    }
}
