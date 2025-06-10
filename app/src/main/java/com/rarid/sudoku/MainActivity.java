package com.rarid.sudoku;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private boolean hasSavedProgress(String difficulty) {
        java.io.File file = new java.io.File(getFilesDir(), difficulty + "_progress.json");
        return file.exists();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView title = findViewById(R.id.title);
        Button easyButton = findViewById(R.id.easy_button);
        Button mediumButton = findViewById(R.id.medium_button);
        Button hardButton = findViewById(R.id.hard_button);

        title.setText("Sudoku");

        easyButton.setOnClickListener(v -> launchGame("easy"));

        mediumButton.setOnClickListener(v -> launchGame("medium"));

        hardButton.setOnClickListener(v -> launchGame("hard"));
    }

    // Add this helper method:
    private void launchGame(String difficulty) {
        Intent intent = new Intent(MainActivity.this, GameActivity.class);
        intent.putExtra("difficulty", difficulty);
        // Only set newGame=true if there is NO saved progress
        if (!hasSavedProgress(difficulty)) {
            intent.putExtra("newGame", true);
        }
        startActivity(intent);
    }
}