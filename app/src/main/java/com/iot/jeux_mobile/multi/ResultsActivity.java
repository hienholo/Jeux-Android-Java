package com.iot.jeux_mobile.multi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.iot.jeux_mobile.R;

public class ResultsActivity extends AppCompatActivity {

    private TextView player1NameTextView;
    private TextView player1ScoreTextView;
    private TextView player2NameTextView;
    private TextView player2ScoreTextView;
    private Button playAgainButton;
    private Button exitButton;

    private String player1Name;
    private int player1Score;
    private String player2Name;
    private int player2Score;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        player1NameTextView = findViewById(R.id.player1NameTextView);
        player1ScoreTextView = findViewById(R.id.player1ScoreTextView);
        player2NameTextView = findViewById(R.id.player2NameTextView);
        player2ScoreTextView = findViewById(R.id.player2ScoreTextView);
        playAgainButton = findViewById(R.id.playAgainButton);
        exitButton = findViewById(R.id.exitButton);

        // Récupérer les données passées depuis PodsActivity
        player1Name = getIntent().getStringExtra("PLAYER_NAME");
        player1Score = getIntent().getIntExtra("YOUR_SCORE", 0);
        player2Name = getIntent().getStringExtra("OPPONENT_NAME");
        player2Score = getIntent().getIntExtra("OPPONENT_SCORE", 0);

        // Afficher les données dans les TextView
        player1NameTextView.setText(player1Name);
        player1ScoreTextView.setText("Score: " + player1Score);
        player2NameTextView.setText(player2Name);
        player2ScoreTextView.setText("Score: " + player2Score);

        playAgainButton.setOnClickListener(v -> {
            Intent intent = new Intent(ResultsActivity.this, PodsActivity.class);
            intent.putExtra("PLAYER_NAME", player1Name);
            intent.putExtra("role", getIntent().getStringExtra("YOUR_ROLE")); // Renvoyer le rôle
            startActivity(intent);
            finish();
        });

        exitButton.setOnClickListener(v -> finishAffinity()); // Ferme toutes les activités liées à l'application
    }
}