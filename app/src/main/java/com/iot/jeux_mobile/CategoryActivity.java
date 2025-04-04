package com.iot.jeux_mobile;


import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.iot.jeux_mobile.capteur.JeuxCap1;

public class CategoryActivity extends AppCompatActivity {
    private Button btnCapteurs, btnMouvement, btnQuestion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        btnCapteurs = findViewById(R.id.btnCapteurs);
        btnMouvement = findViewById(R.id.btnMouvement);
        btnQuestion = findViewById(R.id.btnQuestion);

        btnCapteurs.setOnClickListener(v -> {
            Intent intent = new Intent(CategoryActivity.this, JeuxCap1.class);
            startActivity(intent);
        });

//        btnMouvement.setOnClickListener(v -> {
//            Intent intent = new Intent(CategoryActivity.this, GameMouvementActivity.class);
//            startActivity(intent);
//        });
//
//        btnQuestion.setOnClickListener(v -> {
//            Intent intent = new Intent(CategoryActivity.this, GameQuestionActivity.class);
//            startActivity(intent);
//        });
    }
}
