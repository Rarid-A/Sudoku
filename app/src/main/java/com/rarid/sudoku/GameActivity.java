package com.rarid.sudoku;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.GridLayout;
import android.graphics.drawable.GradientDrawable;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.view.Gravity;
import android.widget.ScrollView;
import android.widget.Toast;

import com.rarid.sudoku.generator.SudokuGenerator;

public class GameActivity extends AppCompatActivity {

    private SudokuBoardView boardView;
    private String currentDifficulty;
    private int hintsLeft = 3;
    private int hintsUsed = 0;
    private final View[] hintCircles = new View[3];
    private int[][] solutionGrid = new int[9][9];
    private boolean puzzleCompleted = false;

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // --- Get references to views from XML ---
        boardView = findViewById(R.id.sudoku_board);

        hintCircles[0] = findViewById(R.id.hint_circle_1);
        hintCircles[1] = findViewById(R.id.hint_circle_2);
        hintCircles[2] = findViewById(R.id.hint_circle_3);

        Button hintButton = findViewById(R.id.hint_button);
        Button deleteButton = findViewById(R.id.delete_button);
        Button newGameButton = findViewById(R.id.new_game_button);
        Button pencilToggle = findViewById(R.id.pencil_toggle);

        // --- Game Logic Loading (unchanged, same as yours) ---
        Intent intent = getIntent();
        if (intent != null) {
            currentDifficulty = intent.getStringExtra("difficulty");
            boolean newGame = intent.getBooleanExtra("newGame", false);

            if (newGame) {
                SudokuGenerator.PuzzleWithSolution pws = SudokuGenerator
                        .generatePuzzleAndSolutionForDifficulty(currentDifficulty);
                SudokuGenerator.saveGeneratedPuzzle(this, currentDifficulty, pws.puzzle, pws.solution);
                this.deleteFile(currentDifficulty + "_progress.json");
                boardView.setGrid(pws.puzzle);
                hintsLeft = 3;
                hintsUsed = 0;
                solutionGrid = pws.solution;
            } else {
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
                    boolean validSolution = false;
                    if (data.solutionGrid != null && data.solutionGrid.length == 9) {
                        outer: for (int r = 0; r < 9; r++) {
                            for (int c = 0; c < 9; c++) {
                                if (data.solutionGrid[r][c] != 0) {
                                    validSolution = true;
                                    break outer;
                                }
                            }
                        }
                        if (validSolution) {
                            solutionGrid = data.solutionGrid;
                        }
                    }
                    if (!validSolution) {
                        solutionGrid = loadSolutionFromAssets(currentDifficulty);
                    }
                } else {
                    int[][] puzzle = loadPuzzleFromUserCopy(currentDifficulty);
                    boardView.setGrid(puzzle);
                    hintsLeft = 3;
                    hintsUsed = 0;
                    solutionGrid = loadSolutionFromAssets(currentDifficulty);
                }
            }
        }

        ensurePuzzleCopyExists(currentDifficulty);

        // --- Button Listeners ---
        hintButton.setOnClickListener(v -> giveHint());

        deleteButton.setOnClickListener(v -> {
            boardView.deleteSelected();
            if (isPuzzleComplete())
                showCompletionDialog();
        });

        newGameButton.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Start New Game?")
                    .setMessage("Are you sure? Progress will be lost.")
                    .setPositiveButton("Yes", (d, w) -> {
                        Intent newIntent = new Intent(GameActivity.this, GameActivity.class);
                        newIntent.putExtra("difficulty", currentDifficulty);
                        newIntent.putExtra("newGame", true);
                        finish();
                        startActivity(newIntent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        pencilToggle.setOnClickListener(v -> {
            boolean isOn = pencilToggle.getText().toString().endsWith("OFF");
            boardView.setPencilmarkMode(isOn);
            pencilToggle.setText("Pencilmark: " + (isOn ? "ON" : "OFF"));
        });

        // --- Number Pad Listeners ---
        GridLayout numberPad = findViewById(R.id.number_pad);
        for (int i = 1; i <= 9; i++) {
            int resId = getResources().getIdentifier("num_" + i, "id", getPackageName());
            Button numBtn = numberPad.findViewById(resId);
            final int num = i;
            numBtn.setOnClickListener(v -> {
                boardView.setNumber(num);
                if (isPuzzleComplete())
                    showCompletionDialog();
            });
        }

        updateHintCircles();
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
        ProgressManager.saveProgress(
                this,
                currentDifficulty,
                boardView.getGrid(),
                boardView.getIsClue(),
                boardView.getPencilmarks(),
                boardView.getIsError(),
                hintsLeft,
                hintsUsed,
                solutionGrid);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveProgress();
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
        boardView.setNumber(correct);
        boardView.setCellAsClue(row, col);
        hintsLeft--;
        hintsUsed++;
        updateHintCircles();
        if (isPuzzleComplete()) {
            showCompletionDialog();
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
        if (puzzleCompleted)
            return;
        puzzleCompleted = true;
        ProgressManager.saveCompleted(this, currentDifficulty);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Congratulations!")
                .setMessage("You solved the puzzle!")
                .setCancelable(false)
                .setPositiveButton("Back to Menu", (dialog, which) -> {
                    this.deleteFile(currentDifficulty + "_progress.json");
                    finish();
                })
                .setNegativeButton("New Game", (dialog, which) -> {
                    Intent newIntent = new Intent(GameActivity.this, GameActivity.class);
                    newIntent.putExtra("difficulty", currentDifficulty);
                    newIntent.putExtra("newGame", true);
                    finish();
                    startActivity(newIntent);
                })
                .show();
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
