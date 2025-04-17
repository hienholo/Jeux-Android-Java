package com.iot.jeux_mobile.qcm;

import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.iot.jeux_mobile.R;
import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private TextView questionText, scoreText, timerText;
    private Button option1, option2, option3, option4;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private long timeLeft = 60000; // 1 minute
    private CountDownTimer timer;
    private List<Question> questions = new ArrayList<>();
    private MediaPlayer levelCompleteSound;
    private int currentLevel = 1;
    private static final int QUESTIONS_PER_LEVEL = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qcm_culuture);

        // Initialize views
        questionText = findViewById(R.id.questionText);
        scoreText = findViewById(R.id.scoreText);
        timerText = findViewById(R.id.timerText);
        option1 = findViewById(R.id.option1);
        option2 = findViewById(R.id.option2);
        option3 = findViewById(R.id.option3);
        option4 = findViewById(R.id.option4);

        // Initialize sound
        levelCompleteSound = MediaPlayer.create(this, R.raw.level_complete);

        // Load questions for current level
        loadQuestionsForLevel(currentLevel);

        // Start the game
        startGame();
    }

    private void loadQuestionsForLevel(int level) {
        try {
            questions.clear();
            AssetManager assetManager = getAssets();
            InputStream is = assetManager.open("qcm_culture.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);

            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String difficulty = obj.getString("niveau");

                if ((level == 1 && difficulty.equals("Facile")) ||
                        (level == 2 && difficulty.equals("Intermédiaire")) ||
                        (level == 3 && difficulty.equals("Avancé"))) {

                    questions.add(new Question(
                            obj.getString("question"),
                            obj.getString("reponse"),
                            obj.getJSONArray("propositions")
                    ));
                }
            }

            Collections.shuffle(questions);
            if (questions.size() > QUESTIONS_PER_LEVEL) {
                questions = questions.subList(0, QUESTIONS_PER_LEVEL);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur de chargement des questions", Toast.LENGTH_SHORT).show();
        }
    }

    private void startGame() {
        score = 0;
        currentQuestionIndex = 0;
        timeLeft = 60000;

        updateScore();
        startTimer();
        showNextQuestion();
    }

    private void showNextQuestion() {
        if (currentQuestionIndex < questions.size()) {
            Question currentQuestion = questions.get(currentQuestionIndex);
            questionText.setText(currentQuestion.getQuestion());

            List<String> options = currentQuestion.getAllOptions();
            Collections.shuffle(options);

            option1.setText(options.get(0));
            option2.setText(options.get(1));
            option3.setText(options.get(2));
            option4.setText(options.get(3));

            option1.setEnabled(true);
            option2.setEnabled(true);
            option3.setEnabled(true);
            option4.setEnabled(true);
            option1.setBackgroundColor(Color.WHITE);
            option2.setBackgroundColor(Color.WHITE);
            option3.setBackgroundColor(Color.WHITE);
            option4.setBackgroundColor(Color.WHITE);
        } else {
            endGame();
        }
    }

    private void startTimer() {
        if (timer != null) {
            timer.cancel();
        }

        timer = new CountDownTimer(timeLeft, 1000) {
            public void onTick(long millisUntilFinished) {
                timeLeft = millisUntilFinished;
                updateTimer();
            }
            public void onFinish() {
                endGame();
            }
        }.start();
    }

    private void updateTimer() {
        int seconds = (int) (timeLeft / 1000) % 60;
        int minutes = (int) (timeLeft / (1000 * 60));
        timerText.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private void updateScore() {
        scoreText.setText(score + "/" + questions.size());
    }

    public void onOptionSelected(View view) {
        Button selectedButton = (Button) view;
        String selectedAnswer = selectedButton.getText().toString();
        Question currentQuestion = questions.get(currentQuestionIndex);

        option1.setEnabled(false);
        option2.setEnabled(false);
        option3.setEnabled(false);
        option4.setEnabled(false);

        if (currentQuestion.isCorrectAnswer(selectedAnswer)) {
            score++;
            selectedButton.setBackgroundColor(Color.GREEN);
            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show();
        } else {
            selectedButton.setBackgroundColor(Color.RED);
            highlightCorrectAnswer(currentQuestion.getCorrectAnswer());
            Toast.makeText(this, "Incorrect!", Toast.LENGTH_SHORT).show();
        }

        updateScore();
        currentQuestionIndex++;

        new android.os.Handler().postDelayed(
                this::showNextQuestion,
                1500
        );
    }

    private void highlightCorrectAnswer(String correctAnswer) {
        if (option1.getText().equals(correctAnswer)) {
            option1.setBackgroundColor(Color.GREEN);
        } else if (option2.getText().equals(correctAnswer)) {
            option2.setBackgroundColor(Color.GREEN);
        } else if (option3.getText().equals(correctAnswer)) {
            option3.setBackgroundColor(Color.GREEN);
        } else {
            option4.setBackgroundColor(Color.GREEN);
        }
    }

    private void endGame() {
        if (timer != null) {
            timer.cancel();
        }

        if (levelCompleteSound != null) {
            levelCompleteSound.start();
        }

        showLevelCompleteDialog();
    }

    private void showLevelCompleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_level_complete, null);
        builder.setView(dialogView);

        TextView scoreText = dialogView.findViewById(R.id.scoreText);
        scoreText.setText("Votre Score: " + score + "/" + QUESTIONS_PER_LEVEL);

        Button nextLevelBtn = dialogView.findViewById(R.id.nextLevelButton);
        Button quitBtn = dialogView.findViewById(R.id.quitButton);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        nextLevelBtn.setOnClickListener(v -> {
            dialog.dismiss();
            startNextLevel();
        });

        quitBtn.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }

    private void startNextLevel() {
        currentLevel++;
        if (currentLevel > 3) {
            Toast.makeText(this, "Félicitations! Vous avez terminé tous les niveaux!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadQuestionsForLevel(currentLevel);
        startGame();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (levelCompleteSound != null) {
            levelCompleteSound.release();
        }
        if (timer != null) {
            timer.cancel();
        }
    }

    private static class Question {
        private String question;
        private String correctAnswer;
        private JSONArray propositions;

        public Question(String question, String correctAnswer, JSONArray propositions) {
            this.question = question;
            this.correctAnswer = correctAnswer;
            this.propositions = propositions;
        }

        public String getQuestion() { return question; }
        public String getCorrectAnswer() { return correctAnswer; }

        public List<String> getAllOptions() {
            List<String> options = new ArrayList<>();
            try {
                for (int i = 0; i < propositions.length(); i++) {
                    options.add(propositions.getString(i));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return options;
        }

        public boolean isCorrectAnswer(String answer) {
            return correctAnswer.equals(answer);
        }
    }
}