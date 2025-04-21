package com.iot.jeux_mobile.screen.multi;

import static kotlinx.coroutines.DelayKt.delay;

import android.app.AlertDialog;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.iot.jeux_mobile.R;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PodsActivity extends AppCompatActivity {
    private TextView timerText, scoreText;
    private int score = 0;
    private CountDownTimer gameTimer;
    private final List<Button> pods = new ArrayList<>();
    private int gameDuration;
    private MediaPlayer successSound, errorSound, victorySound, gameOverSound;
    private boolean isHost;
    private boolean scoreSent = false;
    private boolean gameStarted = false;
    private LinearLayout waitingLayout;
    private List<String> currentColors = new ArrayList<>();

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
        setContentView(R.layout.pods_activity);

        waitingLayout = findViewById(R.id.waiting_layout);
        String role = getIntent().getStringExtra("role");
        isHost = "host".equals(role);
        gameDuration = getIntent().getIntExtra("GAME_DURATION", 60);
        gameStarted = isHost;

        initSounds();
        initViews();

        if (isHost) {
            waitingLayout.setVisibility(View.GONE);
            startGame();
        } else {

            startGame();
            setupBluetoothListener();
        }
    }

    private void initSounds() {
        successSound = MediaPlayer.create(this, R.raw.success);
        errorSound = MediaPlayer.create(this, R.raw.error);
        victorySound = MediaPlayer.create(this, R.raw.level_complete);
        gameOverSound = MediaPlayer.create(this, R.raw.game_over);
    }

    private void initViews() {
        timerText = findViewById(R.id.timerText);
        scoreText = findViewById(R.id.scoreText);

        pods.add(findViewById(R.id.pod1));
        pods.add(findViewById(R.id.pod2));
        pods.add(findViewById(R.id.pod3));
        pods.add(findViewById(R.id.pod4));

        for (Button pod : pods) {
            pod.setOnClickListener(this::onPodClicked);
        }
    }

    private void setupBluetoothListener() {
        BluetoothConnectionManager.getInstance().listen(message -> {
            if (message.startsWith("COLORS|")) {
                String[] parts = message.split("\\|");
                currentColors = Arrays.asList(parts[1].split(","));
                updatePodColors();
            } else if (message.startsWith("SCORE|")) {
                int opponentScore = Integer.parseInt(message.substring(6));
                showFinalResults(score, opponentScore);
            }
        });
    }

    private void startGame() {
        gameStarted = true;
        generateAndSendColors();

        gameTimer = new CountDownTimer(gameDuration * 1000L, 1000) {
            public void onTick(long millisUntilFinished) {
                updateTimer(millisUntilFinished);
                if ((millisUntilFinished / 1000) % 2 == 0 && isHost) {
                    generateAndSendColors();
                }
            }

            public void onFinish() {
                endGame(true);
            }
        }.start();
    }

    private void updateTimer(long millisUntilFinished) {
        runOnUiThread(() -> timerText.setText(String.format("%02d:%02d",
                millisUntilFinished / 60000,
                (millisUntilFinished % 60000) / 1000)));
    }

    private void generateAndSendColors() {
        List<String> colors = new ArrayList<>(Arrays.asList(COLORS));
        Collections.shuffle(colors);
        currentColors = colors;
        updatePodColors();

        if (isHost) {
            String colorsMessage = "COLORS|" + String.join(",", colors);
            BluetoothConnectionManager.getInstance().sendMessage(colorsMessage);
        }
    }

    private void updatePodColors() {
        runOnUiThread(() -> {
            for (int i = 0; i < pods.size(); i++) {
                Button pod = pods.get(i);
                pod.setTag(currentColors.get(i));
                pod.setBackgroundResource(COLOR_RES[getColorIndex(currentColors.get(i))]);
            }
        });
    }

    private int getColorIndex(String color) {
        for (int i = 0; i < COLORS.length; i++) {
            if (COLORS[i].equals(color)) return i;
        }
        return 0;
    }

    private void onPodClicked(View v) {
        if (!gameStarted) {
            Log.e("PodsActivity", "Game not started!");
            return;
        }

        Button pod = (Button) v;
        if ("red".equals(pod.getTag())) {
            handleCorrectClick();
        } else {
            handleWrongClick();
        }
    }

    private void handleCorrectClick() {
        successSound.start();
        score++;
        runOnUiThread(() -> scoreText.setText("Score: " + score));
    }

    private void handleWrongClick() {
        errorSound.start();
        endGame(false);
    }

    private void endGame(boolean completed) {
        if (completed) victorySound.start();
        else gameOverSound.start();

        if (!scoreSent) sendScore();
        showFinalScore();
    }

    private void sendScore() {
        BluetoothConnectionManager.getInstance().sendMessage("SCORE|" + score);
        scoreSent = true;
    }

    private void showFinalScore() {
        runOnUiThread(() -> {
            if (gameTimer != null) gameTimer.cancel();
            new AlertDialog.Builder(this)
                    .setTitle("Game Over")
                    .setMessage("Your score: " + score)
                    .setPositiveButton("OK", (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
        });
    }

    private void showFinalResults(int yourScore, int opponentScore) {
        runOnUiThread(() -> {
            String result;
            if (yourScore > opponentScore) {
                result = "You Win!\n";
                victorySound.start();
            } else if (yourScore < opponentScore) {
                result = "You Lose\n";
                gameOverSound.start();
            } else {
                result = "Draw!\n";
            }
            result += "Your score: " + yourScore + "\nOpponent: " + opponentScore;

            new AlertDialog.Builder(this)
                    .setTitle("Final Results")
                    .setMessage(result)
                    .setPositiveButton("OK", (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseResources();
    }

    private void releaseResources() {
        if (gameTimer != null) gameTimer.cancel();
        successSound.release();
        errorSound.release();
        victorySound.release();
        gameOverSound.release();
        BluetoothConnectionManager.getInstance().close();
    }
}