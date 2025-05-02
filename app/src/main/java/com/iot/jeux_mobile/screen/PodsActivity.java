package com.iot.jeux_mobile.screen;

import android.app.AlertDialog;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.iot.jeux_mobile.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PodsActivity extends AppCompatActivity {
    private TextView timerText, scoreText;
    private int score = 0;
    private CountDownTimer gameTimer;
    private final List<Button> pods = new ArrayList<>();
    private int gameDuration;
    private final Random random = new Random();
    private MediaPlayer successSound, errorSound, victorySound,gameOver;
    private boolean gameFinishedNormally = false;

    private final String[] COLORS = {"red", "blue", "green", "yellow"};
    private final int[] COLOR_RES = {
            R.drawable.pod_red,
            R.drawable.pod_blue,
            R.drawable.pod_green,
            R.drawable.pod_yellow
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pods_activity_solo);

        // Initialisation des sons
        successSound = MediaPlayer.create(this, R.raw.success);
        errorSound = MediaPlayer.create(this, R.raw.error);
        victorySound = MediaPlayer.create(this, R.raw.level_complete);
        gameOver = MediaPlayer.create(this, R.raw.game_over);

        gameDuration = getIntent().getIntExtra("GAME_DURATION", 60);
        timerText = findViewById(R.id.timerText);
        scoreText = findViewById(R.id.scoreText);

        pods.add(findViewById(R.id.pod1));
        pods.add(findViewById(R.id.pod2));
        pods.add(findViewById(R.id.pod3));
        pods.add(findViewById(R.id.pod4));

        for (Button pod : pods) {
            pod.setOnClickListener(this::onPodClicked);
        }

        startGame();
    }

    private void startGame() {
        gameTimer = new CountDownTimer(gameDuration * 1000L, 1000) {
            public void onTick(long millisUntilFinished) {
                timerText.setText(String.format("%02d:%02d",
                        millisUntilFinished / 60000,
                        (millisUntilFinished % 60000) / 1000));

                if (millisUntilFinished / 1000 % 2 == 0) {
                    randomizeColors();
                }
            }

            public void onFinish() {
                gameFinishedNormally = true;
                victorySound.start();
                showFinalScore("Partie terminée", "Votre score: " + score);
            }
        }.start();

        randomizeColors();
    }

    private void randomizeColors() {
        List<String> colors = new ArrayList<>();
        colors.add("red"); // Un seul rouge

        List<String> otherColors = new ArrayList<>();
        otherColors.add("blue");
        otherColors.add("green");
        otherColors.add("yellow");
        Collections.shuffle(otherColors);

        colors.add(otherColors.get(0));
        colors.add(otherColors.get(1));
        colors.add(otherColors.get(2));

        Collections.shuffle(colors);

        for (int i = 0; i < pods.size(); i++) {
            Button pod = pods.get(i);
            String color = colors.get(i);
            pod.setTag(color);

            for (int j = 0; j < COLORS.length; j++) {
                if (COLORS[j].equals(color)) {
                    pod.setBackgroundResource(COLOR_RES[j]);
                    break;
                }
            }
        }
    }

    private void onPodClicked(View v) {
        Button pod = (Button) v;
        if ("red".equals(pod.getTag())) {
            successSound.start();
            score++;
            scoreText.setText("Score: " + score);
            randomizeColors();
        } else {
            gameOver.start();
            showFinalScore("Game Over", "Vous avez cliqué sur une mauvaise couleur ! Score final: " + score);
        }
    }

    private void showFinalScore(String title, String message) {
        if (gameTimer != null) {
            gameTimer.cancel();
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        successSound.release();
        errorSound.release();
        victorySound.release();
        super.onDestroy();
    }
}