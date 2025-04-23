package com.iot.jeux_mobile;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.iot.jeux_mobile.capteur.JeuxCap1;
import com.iot.jeux_mobile.capteur.JeuxCap2;
import com.iot.jeux_mobile.qcm.CalculMental;
import com.iot.jeux_mobile.qcm.QuizActivity;
import com.iot.jeux_mobile.screen.JeuLabyrintheActivity;
import com.iot.jeux_mobile.screen.PodsActivity;
import com.iot.jeux_mobile.screen.PodsStart;
import com.iot.jeux_mobile.screen.multi.RechercheClientServer;

public class MainMenuActivity extends AppCompatActivity {

    private String gameMode; // Pour stocker le mode de jeu (Labyrinthe ou Intruit)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_option); // Remplacez par votre fichier XML

        // Récupérer la valeur passée depuis
        Intent intent = getIntent();
        if (intent != null) {
            gameMode = intent.getStringExtra("GAME_MODE"); // Assurez-vous que la clé est correcte
            if (gameMode == null) {
                gameMode = "Labyrinthe";
            }
        } else {
            gameMode = "Labyrinthe"; //Valeur par défaut
        }

        setupButtonListeners();
    }

    private void setupButtonListeners() {
        // Jouer (Mode Solo)
        Button btnPlay = findViewById(R.id.btn_jouer);
        btnPlay.setOnClickListener(v -> {
            Intent intent = new Intent();
            if ("Labyrinthe".equals(gameMode)) {
                intent = new Intent(MainMenuActivity.this, JeuLabyrintheActivity.class); // ou SoloLabyrintheActivity
            } else if ("Intruit".equals(gameMode)) {
                intent = new Intent(MainMenuActivity.this, PodsActivity.class); // ou SoloIntruitActivity
            }
         else if ("qcm".equals(gameMode)) {
            intent = new Intent(MainMenuActivity.this, QuizActivity.class); // ou SoloIntruitActivity
        }
            else if ("calcul".equals(gameMode)) {
                intent = new Intent(MainMenuActivity.this, CalculMental.class); // ou SoloIntruitActivity
            }

            else if ("war".equals(gameMode)) {
                intent = new Intent(MainMenuActivity.this, JeuxCap2.class); // ou SoloIntruitActivity
            }
            else if ("zombie".equals(gameMode)) {
                intent = new Intent(MainMenuActivity.this, JeuxCap1.class); // ou SoloIntruitActivity
            }


            else {
               // intent = new Intent(MainMenuActivity.this, SoloGameActivity.class); // prévoir un comportement par défaut
            }
            startActivity(intent);
        });

        // Jouer avec un ami
        Button btnPlayWithFriend = findViewById(R.id.btn_jouer_ami);
        btnPlayWithFriend.setOnClickListener(v -> {
            Intent intent = new Intent();
            if ("Labyrinthe".equals(gameMode)) {
                //intent = new Intent(MainMenuActivity.this, MultiplayerActivity.class); // ou MultiLabyrintheActivity
            } else if ("Intruit".equals(gameMode)) {
                intent = new Intent(MainMenuActivity.this, RechercheClientServer.class);  // ou MultiIntruitActivity
            }
            else if ("war".equals(gameMode)) {
                intent = new Intent(MainMenuActivity.this, BluetoothCreateOrJoin.class); // ou SoloIntruitActivity
            }
            else if ("zombie".equals(gameMode)) {
                intent = new Intent(MainMenuActivity.this, BluetoothCreateOrJoin.class); // ou SoloIntruitActivity
            }
            else {
               intent = new Intent(MainMenuActivity.this, RechercheClientServer.class); // prévoir un comportement par défaut
            }


            startActivity(intent);
        });

        // Entrainement
        Button btnTraining = findViewById(R.id.btn_entrainement);
        btnTraining.setOnClickListener(v -> {
            Intent intent = new Intent();
            if ("Labyrinthe".equals(gameMode)) {
             //   intent = new Intent(MainMenuActivity.this, TrainingActivity.class);  // ou TrainingLabyrintheActivity
            } else if ("Intruit".equals(gameMode)) {
                intent = new Intent(MainMenuActivity.this, PodsStart.class); // ou TrainingIntruitActivity
            } else {
                intent = new Intent(MainMenuActivity.this, PodsStart.class);  // prévoir un comportement par défaut
            }
            startActivity(intent);
        });

        // Mes Scores
        Button btnScores = findViewById(R.id.btn_scores);
        btnScores.setOnClickListener(v -> {
          //  Intent intent = new Intent(MainMenuActivity.this, ScoreboardActivity.class);
          //  startActivity(intent);
        });

        // Quitter
        Button btnQuit = findViewById(R.id.btn_quitter);
        btnQuit.setOnClickListener(v -> {
            finishAffinity(); // Ferme toutes les activités
            System.exit(0); // Sortie complète
        });
    }

    // Empêche le retour à l'écran précédent
    @Override
    public void onBackPressed() {
        // Désactive le retour arrière
        super.onBackPressed();
    }
}
