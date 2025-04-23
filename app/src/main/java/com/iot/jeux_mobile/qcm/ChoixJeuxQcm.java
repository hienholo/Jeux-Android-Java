package com.iot.jeux_mobile.qcm;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.iot.jeux_mobile.MainMenuActivity;
import com.iot.jeux_mobile.R;
import com.iot.jeux_mobile.screen.ChoixJeuxScreen;

public class ChoixJeuxQcm extends AppCompatActivity {

    private Button btnCulture, btnCalcul;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choix_qcm);

        // Initialisation des boutons
        btnCulture = findViewById(R.id.btnCulture);
        btnCalcul = findViewById(R.id.btnCalcul);


        // Navigation
        btnCulture.setOnClickListener(v -> {
            Intent intent = new Intent(ChoixJeuxQcm.this, MainMenuActivity.class); // Changement ici
            intent.putExtra("GAME_MODE", "qcm"); // Ajout de cette ligne
            startActivity(intent);
        });

        btnCalcul.setOnClickListener(v -> {
            Intent intent = new Intent(ChoixJeuxQcm.this, MainMenuActivity.class); // Changement ici
            intent.putExtra("GAME_MODE", "calcul"); // Ajout de cette ligne
            startActivity(intent);
        });

        // Charger le GIF en fond avec Glide
        ImageView backgroundGif = findViewById(R.id.backgroundGif);
        Glide.with(this)
                .asGif()
                .load(R.drawable.catgif) // Ton GIF dans drawable
                .into(backgroundGif);

        // Lecture audio en boucle
        mediaPlayer = MediaPlayer.create(this, R.raw.playe);
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
