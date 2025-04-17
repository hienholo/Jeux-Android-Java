package com.iot.jeux_mobile.capteur;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.widget.TextView;
import java.util.Random;

public class JeuxCapteur2 extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private float[] gravity;
    private float[] geomagnetic;

    private TextView directionText;
    private TextView scoreText;
    private int score = 0;

    private float targetAzimuth; // direction du trésor
    private static final int TOLERANCE = 10; // degrés d'écart permis
    private boolean found = false;

    private Vibrator vibrator;
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        directionText = new TextView(this);
        scoreText = new TextView(this);
        directionText.setTextSize(24);
        scoreText.setTextSize(24);

        setContentView(directionText);
        addContentView(scoreText, new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        generateNewTarget();
    }

    private void generateNewTarget() {
        targetAzimuth = random.nextInt(360); // entre 0 et 359
        found = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            gravity = event.values;
        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            geomagnetic = event.values;

        if (gravity != null && geomagnetic != null) {
            float[] R = new float[9];
            float[] I = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
            if (success) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                float azimuth = (float) Math.toDegrees(orientation[0]);
                if (azimuth < 0) azimuth += 360;

                directionText.setText("Orientation: " + (int) azimuth + "°");

                float diff = Math.abs(azimuth - targetAzimuth);
                if (diff > 180) diff = 360 - diff;

                if (diff < TOLERANCE && !found) {
                    score += 1;
                    found = true;
                    scoreText.setText("Trésors trouvés: " + score);
                    vibrator.vibrate(300);
                    generateNewTarget();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
