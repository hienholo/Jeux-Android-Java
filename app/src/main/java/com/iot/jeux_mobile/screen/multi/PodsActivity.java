package com.iot.jeux_mobile.screen.multi;

import android.app.AlertDialog;
import android.content.Intent;
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
import android.widget.ProgressBar;

public class PodsActivity extends AppCompatActivity {
    private TextView timerText, scoreText, playerNamesText;
    private int score = 0;
    private int opponentScore = -1;
    private CountDownTimer gameTimer;
    private final List<Button> pods = new ArrayList<>();
    private int gameDuration;
    private boolean isHost;
    private boolean localScoreSent = false;
    private boolean gameStarted = false;
    private boolean resultsShown = false;
    private boolean endReceived = false;
    private boolean durationReceived = false;

    private LinearLayout waitingLayout;
    private ProgressBar waitingSpinner;

    private List<String> currentColors = new ArrayList<>();
    private String playerName;
    private String opponentName = "Opponent";

    private final String[] COLORS = {"red", "blue", "green", "yellow"};
    private final int[] COLOR_RES = {
            R.drawable.pod_red,
            R.drawable.pod_blue,
            R.drawable.pod_green,
            R.drawable.pod_yellow
    };

    private MediaPlayer successSound, errorSound, victorySound, gameOverSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pods_activity);

        playerName = getIntent().getStringExtra("PLAYER_NAME");
        opponentName = getIntent().getStringExtra("OPPONENT_NAME");
        if(opponentName == null) opponentName = "Opponent";

        isHost = getIntent().getStringExtra("role").equals("host");
        gameDuration = getIntent().getIntExtra("GAME_DURATION", -1); // valeur par défaut -1
        waitingLayout = findViewById(R.id.waiting_layout);

        initUIComponents();
        initSounds();

        if (isHost) {
            waitingLayout.setVisibility(View.GONE);
            startGame();
        } else {
            startGame();
            setupBluetoothListener();
        }
    }

    private void setupBluetoothListener() {
        BluetoothConnectionManager.getInstance().listen(message -> {
            if(message.startsWith("DURATION|")) {
                gameDuration = Integer.parseInt(message.substring(9));
                durationReceived = true;
                runOnUiThread(() -> {
                    waitingLayout.setVisibility(View.GONE);
                    startGame();
                });
            }
            else if(message.startsWith("NAME|")) {
                opponentName = message.substring(5);
                runOnUiThread(() ->
                        playerNamesText.setText(playerName + " vs " + opponentName));
            }
            else if(message.startsWith("COLORS|")) {
                String[] parts = message.split("\\|");
                currentColors = Arrays.asList(parts[1].split(","));
                updatePodColors();
            }
            else if(message.startsWith("SCORE|")) {
                opponentScore = Integer.parseInt(message.substring(6));
                Log.d("ScoreDebug", "Score reçu : " + opponentScore);
                checkAndShowResults();
            }
            else if(message.equals("END")) {
                endReceived = true;
                checkAndShowResults();
            }
        });
    }

    private void initUIComponents() {
        timerText = findViewById(R.id.timerText);
        scoreText = findViewById(R.id.scoreText);
        playerNamesText = findViewById(R.id.player_names);
        playerNamesText.setText(playerName + " vs " + opponentName);

        pods.add(findViewById(R.id.pod1));
        pods.add(findViewById(R.id.pod2));
        pods.add(findViewById(R.id.pod3));
        pods.add(findViewById(R.id.pod4));

        for(Button pod : pods) {
            pod.setOnClickListener(this::onPodClicked);
        }
    }

    private void initSounds() {
        successSound = MediaPlayer.create(this, R.raw.success);
        errorSound = MediaPlayer.create(this, R.raw.error);
        victorySound = MediaPlayer.create(this, R.raw.level_complete);
        gameOverSound = MediaPlayer.create(this, R.raw.game_over);
    }

    private void startGame() {
        gameStarted = true;
        generateAndSendColors();

        gameTimer = new CountDownTimer(gameDuration * 1000L, 1000) {
            public void onTick(long millisUntilFinished) {
                updateTimer(millisUntilFinished);
                if((millisUntilFinished / 1000) % 2 == 0 && isHost) {
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

        if(isHost) {
            String colorsMessage = "COLORS|" + String.join(",", colors);
            BluetoothConnectionManager.getInstance().sendMessage(colorsMessage);
        }
    }

    private void updatePodColors() {
        runOnUiThread(() -> {
            for(int i=0; i<pods.size(); i++) {
                Button pod = pods.get(i);
                pod.setTag(currentColors.get(i));
                pod.setBackgroundResource(COLOR_RES[getColorIndex(currentColors.get(i))]);
            }
        });
    }

    private int getColorIndex(String color) {
        for(int i=0; i<COLORS.length; i++) {
            if(COLORS[i].equals(color)) return i;
        }
        return 0;
    }

    private void onPodClicked(View v) {
        if(!gameStarted) {
            Log.e("PodsActivity", "Game not started!");
            return;
        }

        Button pod = (Button) v;
        if("red".equals(pod.getTag())) {
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
        if(completed) victorySound.start();

        if(!localScoreSent) {
            sendScore();
            localScoreSent = true;
        }

        if(isHost) {
            BluetoothConnectionManager.getInstance().sendMessage("END");
        }

        try {
            TimeUnit.SECONDS.sleep(1);
            checkAndShowResults();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendScore() {
        BluetoothConnectionManager.getInstance().sendMessage("SCORE|" + score);
    }

    private void checkAndShowResults() {
        if ((localScoreSent || isHost) && opponentScore != -1 && !resultsShown) {
            if (isHost || endReceived) {
                resultsShown = true;
                showFinalResults(score, opponentScore);
            }
        }
    }

    private void showFinalResults(int yourScore, int opponentScore) {
        runOnUiThread(() -> {
            try {
                if (isFinishing() || isDestroyed()) return;

                Intent intent = new Intent(PodsActivity.this, ResultsActivity.class);
                intent.putExtra("PLAYER_NAME", playerName);
                intent.putExtra("YOUR_SCORE", yourScore);
                intent.putExtra("OPPONENT_NAME", opponentName);
                intent.putExtra("OPPONENT_SCORE", opponentScore);
                intent.putExtra("YOUR_ROLE", isHost ? "host" : "client"); // Passer le rôle pour le rejouer

                startActivity(intent);
                finish(); // Fermer l'activité de jeu après avoir affiché les résultats

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}