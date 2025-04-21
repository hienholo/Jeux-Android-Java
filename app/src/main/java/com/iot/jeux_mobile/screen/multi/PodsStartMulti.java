package com.iot.jeux_mobile.screen.multi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.iot.jeux_mobile.R;

public class PodsStartMulti extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pod_multi_player);

        SeekBar timeSeekBar = findViewById(R.id.timeSeekBar);
        TextView timeValue = findViewById(R.id.timeValue);
        Button multiplayerButton = findViewById(R.id.multiplayerButton);

        timeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int actualTime = 30 + progress * 5;
                timeValue.setText(actualTime + " secondes");
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        multiplayerButton.setOnClickListener(v -> {
            int duration = 30 + timeSeekBar.getProgress() * 5;
            BluetoothConnectionManager.getInstance().sendMessage("DURATION|" + duration);
            Intent intent = new Intent(this, PodsActivity.class);
            intent.putExtra("GAME_DURATION", duration);
            intent.putExtra("role", "client");
            startActivity(intent);
        });
    }
}