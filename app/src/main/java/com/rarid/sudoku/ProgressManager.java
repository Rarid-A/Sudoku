package com.rarid.sudoku;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class ProgressManager {

    public static void saveProgress(Context context, String difficulty, int[][] grid,
            boolean[][] isClue,
            Set<Integer>[][] pencilmarks,
            boolean[][] isError,
            int hintsLeft,
            int hintsUsed,
            int[][] solutionGrid) { // <-- add solutionGrid parameter
        try {
            JSONObject obj = new JSONObject();

            // Save grid
            JSONArray gridArr = new JSONArray();
            for (int[] row : grid) {
                JSONArray rowArr = new JSONArray();
                for (int val : row)
                    rowArr.put(val);
                gridArr.put(rowArr);
            }
            obj.put("sudokuGrid", gridArr);

            // Save clues
            JSONArray clueArr = new JSONArray();
            for (boolean[] row : isClue) {
                JSONArray rowArr = new JSONArray();
                for (boolean val : row)
                    rowArr.put(val);
                clueArr.put(rowArr);
            }
            obj.put("isClue", clueArr);

            // Save pencilmarks
            JSONArray pencilmarksArr = new JSONArray();
            for (int r = 0; r < 9; r++) {
                JSONArray rowArr = new JSONArray();
                for (int c = 0; c < 9; c++) {
                    JSONArray cellArr = new JSONArray();
                    for (int n : pencilmarks[r][c]) {
                        cellArr.put(n);
                    }
                    rowArr.put(cellArr);
                }
                pencilmarksArr.put(rowArr);
            }
            obj.put("pencilmarks", pencilmarksArr);

            // Save isError
            JSONArray errorArr = new JSONArray();
            for (boolean[] row : isError) {
                JSONArray rowArr = new JSONArray();
                for (boolean val : row)
                    rowArr.put(val);
                errorArr.put(rowArr);
            }
            obj.put("isError", errorArr);

            // Save hintsLeft and hintsUsed
            obj.put("hintsLeft", hintsLeft);
            obj.put("hintsUsed", hintsUsed);

            // Save solutionGrid
            JSONArray solutionArr = new JSONArray();
            for (int[] row : solutionGrid) {
                JSONArray rowArr = new JSONArray();
                for (int val : row)
                    rowArr.put(val);
                solutionArr.put(rowArr);
            }
            obj.put("solutionGrid", solutionArr);

            String filename = difficulty + "_progress.json";
            java.io.FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(obj.toString().getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ProgressData loadProgress(Context context, String difficulty) {
        try {
            String filename = difficulty + "_progress.json";
            java.io.FileInputStream fis = context.openFileInput(filename);
            StringBuilder sb = new StringBuilder();
            int ch;
            while ((ch = fis.read()) != -1)
                sb.append((char) ch);
            fis.close();

            JSONObject obj = new JSONObject(sb.toString());
            int[][] grid = new int[9][9];
            boolean[][] isClue = new boolean[9][9];
            boolean[][] isError = new boolean[9][9];
            Set<Integer>[][] pencilmarks = new HashSet[9][9];
            for (int r = 0; r < 9; r++)
                for (int c = 0; c < 9; c++)
                    pencilmarks[r][c] = new HashSet<>();

            // Load grid
            JSONArray gridArr = obj.getJSONArray("sudokuGrid");
            for (int r = 0; r < 9; r++) {
                JSONArray rowArr = gridArr.getJSONArray(r);
                for (int c = 0; c < 9; c++) {
                    grid[r][c] = rowArr.getInt(c);
                }
            }

            // Load clues
            JSONArray clueArr = obj.getJSONArray("isClue");
            for (int r = 0; r < 9; r++) {
                JSONArray rowArr = clueArr.getJSONArray(r);
                for (int c = 0; c < 9; c++) {
                    isClue[r][c] = rowArr.getBoolean(c);
                }
            }

            // Load pencilmarks
            if (obj.has("pencilmarks")) {
                JSONArray pencilmarksArr = obj.getJSONArray("pencilmarks");
                for (int r = 0; r < 9; r++) {
                    JSONArray rowArr = pencilmarksArr.getJSONArray(r);
                    for (int c = 0; c < 9; c++) {
                        JSONArray cellArr = rowArr.getJSONArray(c);
                        for (int i = 0; i < cellArr.length(); i++) {
                            pencilmarks[r][c].add(cellArr.getInt(i));
                        }
                    }
                }
            }

            // Load isError
            if (obj.has("isError")) {
                JSONArray errorArr = obj.getJSONArray("isError");
                for (int r = 0; r < 9; r++) {
                    JSONArray rowArr = errorArr.getJSONArray(r);
                    for (int c = 0; c < 9; c++) {
                        isError[r][c] = rowArr.getBoolean(c);
                    }
                }
            }

            // Load hintsLeft and hintsUsed
            int hintsLeft = obj.has("hintsLeft") ? obj.getInt("hintsLeft") : 3;
            int hintsUsed = obj.has("hintsUsed") ? obj.getInt("hintsUsed") : (3 - hintsLeft);

            int[][] solutionGrid = new int[9][9];
            if (obj.has("solutionGrid")) {
                JSONArray solutionArr = obj.getJSONArray("solutionGrid");
                for (int r = 0; r < 9; r++) {
                    JSONArray rowArr = solutionArr.getJSONArray(r);
                    for (int c = 0; c < 9; c++) {
                        solutionGrid[r][c] = rowArr.getInt(c);
                    }
                }
            }

            return new ProgressData(grid, isClue, pencilmarks, isError, hintsLeft, hintsUsed, solutionGrid);
        } catch (Exception e) {
            return null; // No progress or error
        }
    }

    // Save completion status as a boolean per difficulty
    public static void saveCompleted(Context context, String difficulty) {
        try {
            String filename = difficulty + "_completed.json";
            JSONObject obj = new JSONObject();
            obj.put("completed", true);
            java.io.FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(obj.toString().getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class ProgressData {
        public int[][] grid;
        public boolean[][] isClue;
        public Set<Integer>[][] pencilmarks;
        public boolean[][] isError;
        public int hintsLeft;
        public int hintsUsed;
        public int[][] solutionGrid; // <-- add this

        public ProgressData(int[][] grid, boolean[][] isClue, Set<Integer>[][] pencilmarks, boolean[][] isError,
                int hintsLeft, int hintsUsed, int[][] solutionGrid) {
            this.grid = grid;
            this.isClue = isClue;
            this.pencilmarks = pencilmarks;
            this.isError = isError;
            this.hintsLeft = hintsLeft;
            this.hintsUsed = hintsUsed;
            this.solutionGrid = solutionGrid;
        }
    }
}
