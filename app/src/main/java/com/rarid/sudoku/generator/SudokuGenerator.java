package com.rarid.sudoku.generator;

import android.content.Context;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

public class SudokuGenerator {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter difficulty (easy, medium, hard):");
        String difficulty = scanner.nextLine().trim().toLowerCase();
        while (!difficulty.equals("easy") && !difficulty.equals("medium") && !difficulty.equals("hard")) {
            System.out.println("Invalid difficulty. Please enter easy, medium, or hard:");
            difficulty = scanner.nextLine().trim().toLowerCase();
        }

        System.out.println("How many sudokus do you want to generate?");
        int count;
        while (true) {
            try {
                count = Integer.parseInt(scanner.nextLine().trim());
                if (count > 0)
                    break;
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Please enter a valid positive number:");
        }

        List<SudokuPuzzle> puzzles = new ArrayList<>();
        Set<String> puzzleSet = new HashSet<>(); // To track unique puzzles

        int generated = 0;
        while (generated < count) {
            int[][] solution = generateFullSolution();
            int[][] puzzle = generatePuzzle(solution, difficulty);

            String puzzleKey = Arrays.deepToString(puzzle);
            if (!puzzleSet.contains(puzzleKey)) {
                puzzles.add(new SudokuPuzzle(puzzle, solution));
                puzzleSet.add(puzzleKey);
                generated++;
            }
        }

        String fileName = difficulty + ".json";
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(fileName)) {
            gson.toJson(puzzles, writer);
            System.out.println("Sudokus generated and saved to " + fileName);
        } catch (IOException e) {
            System.out.println("Error writing to file: " + e.getMessage());
        }
    }

    // Represents a Sudoku puzzle and its solution
    static class SudokuPuzzle {
        int[][] puzzle;
        int[][] solution;

        SudokuPuzzle(int[][] puzzle, int[][] solution) {
            this.puzzle = puzzle;
            this.solution = solution;
        }
    }

    // Generates a fully filled valid Sudoku grid
    private static int[][] generateFullSolution() {
        int[][] board = new int[9][9];
        fillBoard(board, 0, 0);
        return board;
    }

    private static boolean fillBoard(int[][] board, int row, int col) {
        if (row == 9)
            return true;
        if (col == 9)
            return fillBoard(board, row + 1, 0);

        List<Integer> nums = new ArrayList<>();
        for (int i = 1; i <= 9; i++)
            nums.add(i);
        Collections.shuffle(nums);

        for (int num : nums) {
            if (isSafe(board, row, col, num)) {
                board[row][col] = num;
                if (fillBoard(board, row, col + 1))
                    return true;
                board[row][col] = 0;
            }
        }
        return false;
    }

    private static boolean isSafe(int[][] board, int row, int col, int num) {
        for (int i = 0; i < 9; i++) {
            if (board[row][i] == num || board[i][col] == num)
                return false;
        }
        int boxRow = row - row % 3, boxCol = col - col % 3;
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                if (board[boxRow + i][boxCol + j] == num)
                    return false;
        return true;
    }

    // Removes numbers from a full solution to create a puzzle of the given
    // difficulty
    private static int[][] generatePuzzle(int[][] solution, String difficulty) {
        int[][] puzzle = new int[9][9];
        for (int i = 0; i < 9; i++)
            puzzle[i] = Arrays.copyOf(solution[i], 9);

        int clues;
        switch (difficulty) {
            case "easy":
                clues = 36 + new Random().nextInt(6);
                break; // 36-41 clues
            case "medium":
                clues = 32 + new Random().nextInt(5);
                break; // 32-36 clues
            case "hard":
                clues = 26 + new Random().nextInt(6);
                break; // 26-31 clues
            default:
                clues = 36;
        }

        List<int[]> cells = new ArrayList<>();
        for (int i = 0; i < 9; i++)
            for (int j = 0; j < 9; j++)
                cells.add(new int[] { i, j });
        Collections.shuffle(cells);

        int removed = 81 - clues;
        for (int k = 0; k < removed; k++) {
            int[] cell = cells.get(k);
            int backup = puzzle[cell[0]][cell[1]];
            puzzle[cell[0]][cell[1]] = 0;
            if (!hasUniqueSolution(puzzle)) {
                puzzle[cell[0]][cell[1]] = backup; // revert if not unique
            }
        }
        return puzzle;
    }

    // Checks if a puzzle has a unique solution
    private static boolean hasUniqueSolution(int[][] puzzle) {
        return countSolutions(puzzle, 0, 0, 0) == 1;
    }

    private static int countSolutions(int[][] board, int row, int col, int count) {
        if (row == 9)
            return count + 1;
        if (col == 9)
            return countSolutions(board, row + 1, 0, count);

        if (board[row][col] != 0)
            return countSolutions(board, row, col + 1, count);

        for (int num = 1; num <= 9; num++) {
            if (isSafe(board, row, col, num)) {
                board[row][col] = num;
                count = countSolutions(board, row, col + 1, count);
                if (count > 1) {
                    board[row][col] = 0;
                    return count; // early exit
                }
                board[row][col] = 0;
            }
        }
        return count;
    }

    public static class PuzzleWithSolution {
        public int[][] puzzle;
        public int[][] solution;
        public PuzzleWithSolution(int[][] puzzle, int[][] solution) {
            this.puzzle = puzzle;
            this.solution = solution;
        }
    }

    public static PuzzleWithSolution generatePuzzleAndSolutionForDifficulty(String difficulty) {
        int[][] solution = generateFullSolution();
        int[][] puzzle = generatePuzzle(solution, difficulty);
        return new PuzzleWithSolution(puzzle, solution);
    }

    public static int[][] solvePuzzle(int[][] puzzle) {
        int[][] copy = new int[9][9];
        for (int i = 0; i < 9; i++)
            copy[i] = Arrays.copyOf(puzzle[i], 9);
        fillBoard(copy, 0, 0);
        return copy;
    }

    // Save the generated puzzle and solution to user files
    public static void saveGeneratedPuzzle(Context context, String difficulty, int[][] puzzle, int[][] solution) {
        try {
            // Save puzzle and solution in the same file
            String filename = difficulty + "_user.json";
            JSONObject puzzleObj = new JSONObject();

            // Save puzzle
            JSONArray puzzleArr = new JSONArray();
            for (int[] row : puzzle) {
                JSONArray rowArr = new JSONArray();
                for (int val : row)
                    rowArr.put(val);
                puzzleArr.put(rowArr);
            }
            puzzleObj.put("puzzle", puzzleArr);

            // Save solution
            JSONArray solutionArr = new JSONArray();
            for (int[] row : solution) {
                JSONArray rowArr = new JSONArray();
                for (int val : row)
                    rowArr.put(val);
                solutionArr.put(rowArr);
            }
            puzzleObj.put("solution", solutionArr);

            java.io.FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(puzzleObj.toString().getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}