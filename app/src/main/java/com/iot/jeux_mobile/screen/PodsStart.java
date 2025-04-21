package com.iot.jeux_mobile.screen;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.iot.jeux_mobile.R;

public class PodsStart extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pods_start);

        SeekBar timeSeekBar = findViewById(R.id.timeSeekBar);
        TextView timeValue = findViewById(R.id.timeValue);
        Button startButton = findViewById(R.id.startButton);

        timeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                timeValue.setText(progress + " seconds");
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        startButton.setOnClickListener(v -> {
            int duration = timeSeekBar.getProgress();
            Intent intent = new Intent(PodsStart.this, PodsActivity.class);
            intent.putExtra("GAME_DURATION", duration);
            startActivity(intent);
        });
    }
}