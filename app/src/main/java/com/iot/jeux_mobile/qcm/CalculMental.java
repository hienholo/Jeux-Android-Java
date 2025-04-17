package com.iot.jeux_mobile.qcm;

import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.iot.jeux_mobile.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CalculMental extends AppCompatActivity {

    private TextView txtQuestion, txtScore, txtCompteur, txtNiveau;
    private Button btn1, btn2, btn3, btn4;

    private List<Question> questions;
    private int index = 0;
    private int scoreOk = 0, scoreKo = 0;
    private final int MAX_QUESTIONS = 10;

    private Vibrator vibrator;
    private MediaPlayer mediaPlayer;

    private String[] niveaux = {"Débutant", "Intermédiaire", "Avancé"};
    private int niveauActuel = 0;
    private HashMap<String, Integer> scoresParNiveau = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qcm_calcul);

        txtQuestion = findViewById(R.id.txtQuestion);
        txtScore = findViewById(R.id.txtScore);
        txtCompteur = findViewById(R.id.txtCompteur);
        txtNiveau = findViewById(R.id.txtNiveau); // ➤ Nouveau
        btn1 = findViewById(R.id.btn1);
        btn2 = findViewById(R.id.btn2);
        btn3 = findViewById(R.id.btn3);
        btn4 = findViewById(R.id.btn4);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        chargerNiveau();

        btn1.setOnClickListener(v -> verifierReponse(btn1.getText().toString()));
        btn2.setOnClickListener(v -> verifierReponse(btn2.getText().toString()));
        btn3.setOnClickListener(v -> verifierReponse(btn3.getText().toString()));
        btn4.setOnClickListener(v -> verifierReponse(btn4.getText().toString()));
    }

    private void chargerNiveau() {
        if (niveauActuel < niveaux.length) {
            String niveau = niveaux[niveauActuel];
            questions = chargerQuestionsDepuisAssets("qcm_calcul_mental.json", niveau);
            index = 0;
            scoreOk = 0;
            scoreKo = 0;
            majAffichageNiveau(niveau);
            afficherQuestion();
        } else {
            afficherFinDuJeu();
        }
    }

    private void majAffichageNiveau(String niveau) {
        txtNiveau.setText("NIVEAU : " + niveau.toUpperCase());

        // ➤ Couleur dynamique
        switch (niveau.toLowerCase()) {
            case "débutant":
                txtNiveau.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                break;
            case "intermédiaire":
                txtNiveau.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
                break;
            case "avancé":
                txtNiveau.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                break;
            default:
                txtNiveau.setTextColor(getResources().getColor(android.R.color.white));
        }
    }

    private void afficherQuestion() {
        if (index >= questions.size()) {
            afficherFinDeNiveau();
            return;
        }

        Question q = questions.get(index);
        txtQuestion.setText(q.question);
        txtCompteur.setText((index + 1) + "/" + MAX_QUESTIONS);
        txtScore.setText("✔ " + scoreOk + "   ❌ " + scoreKo);

        btn1.setText(q.propositions[0]);
        btn2.setText(q.propositions[1]);
        btn3.setText(q.propositions[2]);
        btn4.setText(q.propositions[3]);
    }

    private void verifierReponse(String reponseChoisie) {
        Question q = questions.get(index);

        if (reponseChoisie.equals(q.reponse)) {
            scoreOk++;
            vibrer(1);
        } else {
            scoreKo++;
            vibrer(2);
        }

        txtScore.setText("✔ " + scoreOk + "   ❌ " + scoreKo);
        index++;
        afficherQuestion();
    }

    private void vibrer(int fois) {
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = (fois == 1) ? new long[]{0, 200} : new long[]{0, 200, 100, 200};
            vibrator.vibrate(pattern, -1);
        }
    }

    private void afficherFinDeNiveau() {
        String niveau = niveaux[niveauActuel];
        scoresParNiveau.put(niveau, scoreOk);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Niveau terminé !");
        builder.setMessage("Score : " + scoreOk + "/" + MAX_QUESTIONS);
        builder.setCancelable(false);
        builder.setPositiveButton("Niveau suivant", (dialog, which) -> {
            niveauActuel++;
            chargerNiveau();
        });
        builder.show();
    }

    private void afficherFinDuJeu() {
        StringBuilder resume = new StringBuilder();
        int totalScore = 0;

        for (String niveau : niveaux) {
            int score = scoresParNiveau.getOrDefault(niveau, 0);
            resume.append(niveau).append(" : ").append(score).append("/").append(MAX_QUESTIONS).append("\n");
            totalScore += score;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Jeu terminé !");
        builder.setMessage(resume.toString());
        builder.setCancelable(false);
        builder.setPositiveButton("OK", null);
        builder.show();

        jouerSonFin();
    }

    private void jouerSonFin() {
        mediaPlayer = MediaPlayer.create(this, R.raw.victory_music);
        mediaPlayer.setOnCompletionListener(mp -> mp.release());
        mediaPlayer.start();
    }

    private List<Question> chargerQuestionsDepuisAssets(String filename, String niveau) {
        List<Question> list = new ArrayList<>();
        try {
            InputStream is = getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, "UTF-8");

            JSONArray array = new JSONArray(jsonStr);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (obj.getString("niveau").equalsIgnoreCase(niveau)) {
                    JSONArray prop = obj.getJSONArray("propositions");
                    String[] propositions = new String[4];
                    for (int j = 0; j < 4; j++) {
                        propositions[j] = prop.getString(j);
                    }
                    list.add(new Question(obj.getString("question"), obj.getString("reponse"), propositions));
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erreur de chargement", Toast.LENGTH_SHORT).show();
        }
        return list;
    }

    public static class Question {
        public String question;
        public String reponse;
        public String[] propositions;

        public Question(String question, String reponse, String[] propositions) {
            this.question = question;
            this.reponse = reponse;
            this.propositions = propositions;
        }
    }
}
