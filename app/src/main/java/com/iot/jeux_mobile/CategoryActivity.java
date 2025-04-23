package com.iot.jeux_mobile;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.iot.jeux_mobile.capteur.ChoixJeuxCapteur;
import com.iot.jeux_mobile.screen.ChoixJeuxScreen;
import com.iot.jeux_mobile.capteur.JeuxCap2;
import com.iot.jeux_mobile.qcm.ChoixJeuxQcm;


public class CategoryActivity extends AppCompatActivity {

    private Button btnCapteurs, btnMouvement, btnQuestion;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        // Initialisation des boutons
        btnCapteurs = findViewById(R.id.btnCapteurs);
        btnMouvement = findViewById(R.id.btnMouvement);
        btnQuestion = findViewById(R.id.btnQuestion);

        // Navigation
        btnCapteurs.setOnClickListener(v -> {
            Intent intent = new Intent(CategoryActivity.this, ChoixJeuxCapteur.class);
            startActivity(intent);
        });

        btnMouvement.setOnClickListener(v -> {
            Intent intent = new Intent(CategoryActivity.this, ChoixJeuxScreen.class);
            startActivity(intent);
        });

        btnQuestion.setOnClickListener(v -> {
            Intent intent = new Intent(CategoryActivity.this, ChoixJeuxQcm.class);
            startActivity(intent);
        });

        // Charger le GIF en fond avec Glide
        ImageView backgroundGif = findViewById(R.id.backgroundGif);
        Glide.with(this)
                .asGif()
                .load(R.drawable.catgif) // Ton GIF dans drawable
                .into(backgroundGif);

        // Lecture audio en boucle
        mediaPlayer = MediaPlayer.create(this, R.raw.start);
        mediaPlayer.setLooping(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null) mediaPlayer.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
