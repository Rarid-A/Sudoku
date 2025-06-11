package com.rarid.sudoku;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private TextView hintTextView;
    private Switch hintSwitch;
    private Button newGameButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        hintSwitch = findViewById(R.id.switch_hint);
        newGameButton = findViewById(R.id.button_new_game);
        ImageView closeBtn = findViewById(R.id.iv_close_settings);
        hintTextView = findViewById(R.id.tv_hint_label);

        // Load preferences
        SharedPreferences prefs = getSharedPreferences("sudoku_settings", MODE_PRIVATE);
        boolean hintsEnabled = prefs.getBoolean("hints_enabled", false);
        hintSwitch.setChecked(hintsEnabled);

        hintSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("hints_enabled", isChecked).apply();
        });

        newGameButton.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });

        closeBtn.setOnClickListener(v -> finish());

        // Set hint text color based on theme
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            hintTextView.setTextColor(Color.WHITE);
        } else {
            hintTextView.setTextColor(Color.BLACK);
        }

    }
}
