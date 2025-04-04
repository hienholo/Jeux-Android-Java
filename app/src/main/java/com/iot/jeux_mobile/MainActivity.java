package com.iot.jeux_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private Button btnPlay, btnOptions, btnCredits, btnExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnPlay = findViewById(R.id.btnPlay);
        btnOptions = findViewById(R.id.btnOptions);
      //  btnCredits = findViewById(R.id.btnCredits);
        btnExit = findViewById(R.id.btnExit);

        btnPlay.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CategoryActivity.class);
            startActivity(intent);
        });

        btnExit.setOnClickListener(v -> finish());
    }
}
