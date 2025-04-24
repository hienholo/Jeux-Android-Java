package com.iot.jeux_mobile.multi;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.iot.jeux_mobile.R;

public class NameInputActivity extends AppCompatActivity {
    private EditText nameInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_name_input);

        nameInput = findViewById(R.id.name_input);
        Button submitButton = findViewById(R.id.submit_button);

        submitButton.setOnClickListener(v -> {
            String playerName = nameInput.getText().toString().trim();
            if (!playerName.isEmpty()) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("PLAYER_NAME", playerName);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Enter your name", Toast.LENGTH_SHORT).show();
            }
        });
    }
}