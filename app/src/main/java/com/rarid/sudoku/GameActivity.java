package com.rarid.sudoku;

import android.content.SharedPreferences;
import android.app.Activity;
import android.content.Intent;
import androidx.annotation.Nullable;
import android.os.Bundle;
import android.view.View;
import android.graphics.drawable.GradientDrawable;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.rarid.sudoku.generator.SudokuGenerator;

public class GameActivity extends AppCompatActivity {
    private static final int SETTINGS_REQUEST = 1001;

    private SudokuBoardView boardView;
    private String currentDifficulty;
    private int hintsLeft = 3;
    private int hintsUsed = 0;
    private final View[] hintCircles = new View[3];
    private int[][] solutionGrid = new int[9][9];
    private boolean puzzleCompleted = false;
    private long startTime;
    private long pausedTime;
    private boolean isPaused = false;
    private TextView timerText;
    private android.os.Handler timerHandler;
    private Runnable timerRunnable;
    private long totalElapsedTime = 0;

    private final boolean[] numberLocked = new boolean[10]; // 1-9, ignore index 0

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // --- Get references to views from XML ---
        boardView = findViewById(R.id.sudoku_board);
        timerText = findViewById(R.id.timer_text);

        hintCircles[0] = findViewById(R.id.hint_circle_1);
        hintCircles[1] = findViewById(R.id.hint_circle_2);
        hintCircles[2] = findViewById(R.id.hint_circle_3);

        Button deleteButton = findViewById(R.id.delete_button);
        Button pencilToggle = findViewById(R.id.pencil_toggle);
        Button undoButton = findViewById(R.id.undo_button);
        ImageButton settingsBtn = findViewById(R.id.settings_button);

        // Initialize timer
        if (savedInstanceState != null) {
            startTime = savedInstanceState.getLong("startTime", System.currentTimeMillis());
            pausedTime = savedInstanceState.getLong("pausedTime", 0);
            totalElapsedTime = savedInstanceState.getLong("totalElapsedTime", 0);
            isPaused = savedInstanceState.getBoolean("isPaused", false);
        } else {
            startTime = System.currentTimeMillis();
            pausedTime = 0;
            totalElapsedTime = 0;
            isPaused = false;
        }

        timerHandler = new android.os.Handler();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPaused && !puzzleCompleted) {
                    long currentTime = System.currentTimeMillis();
                    long elapsedTime = totalElapsedTime + (currentTime - startTime);
                    if (elapsedTime < 0)
                        elapsedTime = 0;

                    int seconds = (int) (elapsedTime / 1000);
                    int minutes = seconds / 60;
                    seconds = seconds % 60;
                    timerText.setText(String.format("%02d:%02d", minutes, seconds));
                }
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);

        // --- Game Logic Loading (unchanged, same as yours) ---
        AtomicReference<Intent> intent = new AtomicReference<>(getIntent());
        if (intent.get() != null) {
            currentDifficulty = intent.get().getStringExtra("difficulty");
            boolean newGame = intent.get().getBooleanExtra("newGame", false);

            if (newGame) {
                SudokuGenerator.PuzzleWithSolution pws = SudokuGenerator
                        .generatePuzzleAndSolutionForDifficulty(currentDifficulty);
                SudokuGenerator.saveGeneratedPuzzle(this, currentDifficulty, pws.puzzle, pws.solution);
                this.deleteFile(currentDifficulty + "_progress.json");
                this.deleteFile(currentDifficulty + "_completed.json"); // <-- Add this line
                boardView.setGrid(pws.puzzle);
                boardView.clearMoveHistory(); // Clear undo history for new game
                hintsLeft = 3;
                hintsUsed = 0;
                solutionGrid = pws.solution;
            } else {
                loadProgress();
                boardView.clearMoveHistory(); // Clear undo history when loading existing game
            }
        }

        ensurePuzzleCopyExists(currentDifficulty);

        // --- Set toolbar title to show current difficulty ---
        String difficultyLabel = "Sudoku";
        if (currentDifficulty != null) {
            String cap = currentDifficulty.substring(0, 1).toUpperCase() + currentDifficulty.substring(1);
            difficultyLabel = cap + " Sudoku";
        }
        TextView toolbarTitle = findViewById(R.id.game_toolbar_title);
        toolbarTitle.setText(difficultyLabel);

        // --- Button Listeners ---
        deleteButton.setOnClickListener(v -> {
            boardView.deleteSelected();
            checkAndLockCompletedNumbers();
            if (isPuzzleComplete())
                showCompletionDialog();
        });

        pencilToggle.setOnClickListener(v -> {
            boolean isOn = pencilToggle.getText().toString().endsWith("OFF");
            boardView.setPencilmarkMode(isOn);
            pencilToggle.setText("Pencilmark: " + (isOn ? "ON" : "OFF"));
        });

        undoButton.setOnClickListener(v -> {
            if (boardView.canUndo()) {
                boardView.undo();
                if (isPuzzleComplete())
                    showCompletionDialog();
            }
        });

        settingsBtn.setOnClickListener(v -> {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            settingsIntent.putExtra("difficulty", currentDifficulty);
            startActivityForResult(settingsIntent, SETTINGS_REQUEST);
        });

        // --- Number Pad Listeners (updated for toolbar layout) ---
        for (int i = 1; i <= 9; i++) {
            int resId = getResources().getIdentifier("num_" + i, "id", getPackageName());
            Button numBtn = findViewById(resId);
            final int num = i;
            numBtn.setOnClickListener(v -> {
                boardView.setNumber(num);
                // Always update auto validation upon entering a puzzle
                checkAndLockCompletedNumbers();
                if (isPuzzleComplete())
                    showCompletionDialog();
            });
        }

        // Load settings and configure UI
        SharedPreferences prefs = getSharedPreferences("sudoku_settings", MODE_PRIVATE);
        boolean hintsEnabled = prefs.getBoolean("hints_enabled", false);
        boolean pencilmarkEnabled = prefs.getBoolean("pencilmark_enabled", true);
        
        Button hintButton = findViewById(R.id.hint_button);
        hintButton.setVisibility(hintsEnabled ? View.VISIBLE : View.GONE);
        hintButton.setEnabled(hintsEnabled);

        View hintCirclesLayout = findViewById(R.id.hint_circles_layout);
        hintCirclesLayout.setVisibility(hintsEnabled ? View.VISIBLE : View.GONE);
        
        // Configure pencilmark toggle based on settings
        if (!pencilmarkEnabled) {
            pencilToggle.setVisibility(View.GONE);
            boardView.setPencilmarkMode(false);
        } else {
            pencilToggle.setVisibility(View.VISIBLE);
        }

        hintButton.setOnClickListener(v -> {
            if (hintsEnabled) {
                giveHint();
            }
        });

        updateHintCircles();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_REQUEST) {
            SharedPreferences prefs = getSharedPreferences("sudoku_settings", MODE_PRIVATE);
            boolean hintsEnabled = prefs.getBoolean("hints_enabled", false);
            boolean pencilmarkEnabled = prefs.getBoolean("pencilmark_enabled", true);
            
            Button hintButton = findViewById(R.id.hint_button);
            hintButton.setVisibility(hintsEnabled ? View.VISIBLE : View.GONE);
            hintButton.setEnabled(hintsEnabled);

            View hintCirclesLayout = findViewById(R.id.hint_circles_layout);
            hintCirclesLayout.setVisibility(hintsEnabled ? View.VISIBLE : View.GONE);
            
            // Update pencilmark toggle visibility
            Button pencilToggle = findViewById(R.id.pencil_toggle);
            if (!pencilmarkEnabled) {
                pencilToggle.setVisibility(View.GONE);
                boardView.setPencilmarkMode(false);
            } else {
                pencilToggle.setVisibility(View.VISIBLE);
            }

            // If user pressed "New Game" in settings, start a new game
            if (resultCode == Activity.RESULT_OK) {
                Intent newIntent = new Intent(GameActivity.this, GameActivity.class);
                newIntent.putExtra("difficulty", currentDifficulty);
                newIntent.putExtra("newGame", true);
                finish();
                startActivity(newIntent);
            }
        }
    }

    private int[][] loadPuzzleFromUserCopy(String difficulty) {
        try {
            String filename = difficulty + "_user.json";
            InputStream is = openFileInput(filename);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);

            JSONObject puzzleObj = new JSONObject(json);
            JSONArray puzzleArr = puzzleObj.getJSONArray("puzzle");

            int[][] puzzle = new int[9][9];
            for (int i = 0; i < 9; i++) {
                JSONArray row = puzzleArr.getJSONArray(i);
                for (int j = 0; j < 9; j++) {
                    puzzle[i][j] = row.getInt(j);
                }
            }
            return puzzle;
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(
                    () -> Toast.makeText(this, "Failed to load puzzle: " + e.getMessage(), Toast.LENGTH_LONG).show());
            return new int[9][9];
        }
    }

    private void ensurePuzzleCopyExists(String difficulty) {
        try {
            String filename = difficulty + "_user.json";
            java.io.File file = new java.io.File(getFilesDir(), filename);
            if (!file.exists()) {
                InputStream is = getAssets().open(difficulty + ".json");
                java.io.FileOutputStream fos = openFileOutput(filename, MODE_PRIVATE);
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                is.close();
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveProgress() {
        if (puzzleCompleted)
            return;

        long currentTime = System.currentTimeMillis();
        long sessionTime = currentTime - startTime;
        long finalElapsedTime = totalElapsedTime + sessionTime;

        ProgressManager.saveProgress(
                this,
                currentDifficulty,
                boardView.getGrid(),
                boardView.getIsClue(),
                boardView.getPencilmarks(),
                boardView.getIsError(),
                hintsLeft,
                hintsUsed,
                solutionGrid,
                finalElapsedTime,
                startTime,
                pausedTime);
    }

    private void loadProgress() {
        ProgressManager.ProgressData data = ProgressManager.loadProgress(this, currentDifficulty);
        if (data != null) {
            boardView.setGridAndClues(data.grid, data.isClue);
            if (data.pencilmarks != null) {
                boardView.setPencilmarks(data.pencilmarks);
            }
            if (data.isError != null) {
                boardView.setIsError(data.isError);
            }
            hintsLeft = data.hintsLeft;
            hintsUsed = data.hintsUsed;
            solutionGrid = data.solutionGrid;
            startTime = System.currentTimeMillis(); // Reset startTime to current time
            pausedTime = 0; // Reset pausedTime
            totalElapsedTime = data.totalElapsedTime; // Keep the total elapsed time
            updateHintCircles();
        } else {
            // Only load a new puzzle if there's no saved progress
            int[][] puzzle = loadPuzzleFromUserCopy(currentDifficulty);
            if (puzzle == null || puzzle[0][0] == 0) {
                // If no puzzle exists, generate a new one
                SudokuGenerator.PuzzleWithSolution pws = SudokuGenerator
                        .generatePuzzleAndSolutionForDifficulty(currentDifficulty);
                SudokuGenerator.saveGeneratedPuzzle(this, currentDifficulty, pws.puzzle, pws.solution);
                boardView.setGrid(pws.puzzle);
                solutionGrid = pws.solution;
            } else {
                boardView.setGrid(puzzle);
                solutionGrid = loadSolutionFromAssets(currentDifficulty);
            }
            hintsLeft = 3;
            hintsUsed = 0;
            startTime = System.currentTimeMillis();
            pausedTime = 0;
            totalElapsedTime = 0;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("startTime", startTime);
        outState.putLong("pausedTime", pausedTime);
        outState.putLong("totalElapsedTime", totalElapsedTime);
        outState.putBoolean("isPaused", isPaused);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!puzzleCompleted) {
            isPaused = true;
            long currentTime = System.currentTimeMillis();
            long sessionTime = currentTime - startTime;
            totalElapsedTime += sessionTime;
            startTime = currentTime;
        }
        saveProgress();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!puzzleCompleted) {
            isPaused = false;
            startTime = System.currentTimeMillis();
        }
    }

    private int[][] loadSolutionFromAssets(String difficulty) {
        try {
            String filename = difficulty + "_user.json";
            InputStream is = openFileInput(filename);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);

            JSONObject puzzleObj = new JSONObject(json);
            JSONArray solutionArr = puzzleObj.getJSONArray("solution");

            int[][] solution = new int[9][9];
            for (int i = 0; i < 9; i++) {
                JSONArray row = solutionArr.getJSONArray(i);
                for (int j = 0; j < 9; j++) {
                    solution[i][j] = row.getInt(j);
                }
            }
            return solution;
        } catch (Exception e) {
            e.printStackTrace();
            return new int[9][9];
        }
    }

    private void giveHint() {
        if (hintsLeft <= 0) {
            Toast.makeText(this, "No hints left!", Toast.LENGTH_SHORT).show();
            return;
        }
        int row = boardView.getSelectedRow();
        int col = boardView.getSelectedCol();
        if (row < 0 || col < 0 || boardView.isSelectedCellClue()) {
            Toast.makeText(this, "Select an empty cell!", Toast.LENGTH_SHORT).show();
            return;
        }
        int correct = solutionGrid[row][col];
        if (correct == 0) {
            Toast.makeText(this, "No solution available!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean prevPencilmarkMode = boardView.isPencilmarkMode(); // Use getter
        boardView.setPencilmarkMode(false); // Always insert as normal number
        boardView.setNumber(correct);
        boardView.setCellAsClue(row, col);
        boardView.setPencilmarkMode(prevPencilmarkMode); // Restore previous mode

        hintsLeft--;
        hintsUsed++;
        updateHintCircles();
        if (isPuzzleComplete()) {
            showCompletionDialog();
        }
    }

    private void checkAndLockCompletedNumbers() {
        int[][] userGrid = boardView.getGrid();
        for (int num = 1; num <= 9; num++) {
            if (numberLocked[num])
                continue;
            int count = 0;
            boolean allCorrect = true;
            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    if (userGrid[r][c] == num) {
                        count++;
                        if (solutionGrid[r][c] != num) {
                            allCorrect = false;
                        }
                    }
                }
            }
            if (count == 9 && allCorrect) {
                numberLocked[num] = true;
                // Lock cells in boardView
                for (int r = 0; r < 9; r++) {
                    for (int c = 0; c < 9; c++) {
                        if (userGrid[r][c] == num) {
                            boardView.setCellAsClue(r, c);
                        }
                    }
                }
                // Disable number button
                int resId = getResources().getIdentifier("num_" + num, "id", getPackageName());
                Button numBtn = findViewById(resId);
                if (numBtn != null)
                    numBtn.setEnabled(false);
                
                // Clear move history when numbers are validated (can't undo validated moves)
                boardView.clearMoveHistory();
            }
        }
    }

    private boolean isPuzzleComplete() {
        int[][] userGrid = boardView.getGrid();
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (userGrid[r][c] != solutionGrid[r][c]) {
                    return false;
                }
            }
        }
        boolean[][] errors = boardView.getIsError();
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (errors[r][c])
                    return false;
        return true;
    }

    private void showCompletionDialog() {
        puzzleCompleted = true;
        if (timerHandler != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }

        long currentTime = System.currentTimeMillis();
        long sessionTime = currentTime - startTime;
        long finalTime = totalElapsedTime + sessionTime;

        // Save the completion time
        ProgressManager.saveBestTime(this, currentDifficulty, finalTime);

        // Disable all buttons to prevent multiple completions
        disableAllButtons();

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Congratulations!")
                .setMessage("You completed the puzzle!")
                .setPositiveButton("New Game", (dialog, which) -> {
                    Intent intent = new Intent(GameActivity.this, GameActivity.class);
                    intent.putExtra("difficulty", currentDifficulty);
                    intent.putExtra("newGame", true);
                    finish();
                    startActivity(intent);
                })
                .setNegativeButton("Close", (dialog, which) -> {
                    // Return to main activity
                    Intent intent = new Intent(GameActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    finish();
                    startActivity(intent);
                })
                .show();
    }

    private void disableAllButtons() {
        // Disable number buttons
        for (int i = 1; i <= 9; i++) {
            int resId = getResources().getIdentifier("num_" + i, "id", getPackageName());
            Button numBtn = findViewById(resId);
            if (numBtn != null) {
                numBtn.setEnabled(false);
            }
        }
        
        // Disable action buttons
        Button deleteButton = findViewById(R.id.delete_button);
        if (deleteButton != null) {
            deleteButton.setEnabled(false);
        }
        
        Button hintButton = findViewById(R.id.hint_button);
        if (hintButton != null) {
            hintButton.setEnabled(false);
        }
        
        Button undoButton = findViewById(R.id.undo_button);
        if (undoButton != null) {
            undoButton.setEnabled(false);
        }
        
        Button pencilToggle = findViewById(R.id.pencil_toggle);
        if (pencilToggle != null) {
            pencilToggle.setEnabled(false);
        }
        
        // Disable settings button
        ImageButton settingsBtn = findViewById(R.id.settings_button);
        if (settingsBtn != null) {
            settingsBtn.setEnabled(false);
        }
        
        // Disable board interaction
        boardView.setEnabled(false);
    }

    private void updateHintCircles() {
        for (int i = 0; i < 3; i++) {
            GradientDrawable drawable = (GradientDrawable) hintCircles[i].getBackground();
            if (i < (3 - hintsLeft)) {
                drawable.setColor(getResources().getColor(R.color.zen_hint_used));
            } else {
                drawable.setColor(getResources().getColor(R.color.zen_hint_unused));
            }
            hintCircles[i].invalidate();
        }
    }
}