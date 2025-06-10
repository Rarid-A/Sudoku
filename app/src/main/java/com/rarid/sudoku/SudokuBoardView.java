package com.rarid.sudoku;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashSet;
import java.util.Set;

public class SudokuBoardView extends View {
    private Paint thickLinePaint;
    private Paint thinLinePaint;
    private Paint textPaint;
    private Paint cellBgPaint;
    private int cellSize;
    private int boardSize;
    private int[][] sudokuGrid = new int[9][9];
    private boolean[][] isClue = new boolean[9][9];
    private boolean[][] isError = new boolean[9][9];
    private int selectedRow = -1, selectedCol = -1;
    private final Set<Integer>[][] pencilmarks = new HashSet[9][9];
    private boolean pencilmarkMode = false; // If true, number input will pencilmark instead of set
    private Integer highlightedNumber = null; // null means no highlight

    public SudokuBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        thickLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thickLinePaint.setStyle(Paint.Style.STROKE);
        thickLinePaint.setColor(Color.BLACK);

        thinLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thinLinePaint.setStyle(Paint.Style.STROKE);
        thinLinePaint.setColor(Color.LTGRAY);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.CENTER);

        cellBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cellBgPaint.setColor(Color.WHITE);
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                pencilmarks[r][c] = new HashSet<>();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // Use the full available space for the board
        boardSize = Math.min(w, h);
        cellSize = boardSize / 9;

        // Scale lines and text based on size
        thickLinePaint.setStrokeWidth(boardSize / 60f);
        thinLinePaint.setStrokeWidth(boardSize / 180f);
        textPaint.setTextSize(cellSize * 0.6f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Convert 4dp to pixels
        float marginDp = 4f;
        float density = getResources().getDisplayMetrics().density;
        int marginPx = (int) (marginDp * density);

        // Adjust boardSize to fit within vertical margins
        int availableWidth = getWidth();
        int availableHeight = getHeight() - 2 * marginPx;
        int drawBoardSize = Math.min(availableWidth, availableHeight);
        int left = (getWidth() - drawBoardSize) / 2;
        int top = marginPx + (availableHeight - drawBoardSize) / 2;

        float radius = drawBoardSize / 18f;
        RectF bgRect = new RectF(left, top, left + drawBoardSize, top + drawBoardSize);
        cellBgPaint.setColor(Color.WHITE);
        canvas.drawRoundRect(bgRect, radius, radius, cellBgPaint);

        // Use drawBoardSize and recalculate cellSize for drawing
        float cell = drawBoardSize / 9f;

        // Draw cells and numbers
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                float x = left + c * cell;
                float y = top + r * cell;
                // Highlight selected cell
                if (r == selectedRow && c == selectedCol) {
                    cellBgPaint.setColor(0xFFD0E8FF); // light blue
                    canvas.drawRect(x, y, x + cell, y + cell, cellBgPaint);
                } else if (highlightedNumber != null && sudokuGrid[r][c] == highlightedNumber
                        && sudokuGrid[r][c] != 0) {
                    cellBgPaint.setColor(0xFFB3FFD9); // light green for highlight
                    canvas.drawRect(x, y, x + cell, y + cell, cellBgPaint);
                } else if ((r / 3 + c / 3) % 2 == 0) {
                    cellBgPaint.setColor(0xFFF7F7F7);
                    canvas.drawRect(x, y, x + cell, y + cell, cellBgPaint);
                }
                int val = sudokuGrid[r][c];
                if (val != 0) {
                    // Set color: clue=black, user=blue, error=red
                    if (isError[r][c]) {
                        textPaint.setColor(Color.RED);
                    } else if (isClue[r][c]) {
                        textPaint.setColor(Color.BLACK);
                    } else {
                        textPaint.setColor(Color.BLUE);
                    }
                    float textY = y + cell / 2f - (textPaint.descent() + textPaint.ascent()) / 2;
                    canvas.drawText(String.valueOf(val), x + cell / 2f, textY, textPaint);
                } else if (!pencilmarks[r][c].isEmpty()) {
                    // Draw pencilmarked numbers (pencil marks)
                    float pencilmarkTextSize = cell * 0.22f;
                    textPaint.setTextSize(pencilmarkTextSize);
                    textPaint.setColor(0xFF888888); // Gray for pencilmarks
                    int count = 0;
                    for (int n = 1; n <= 9; n++) {
                        if (pencilmarks[r][c].contains(n)) {
                            int row = (count) / 3;
                            int col = (count) % 3;
                            float ex = x + (col + 0.5f) * cell / 3f;
                            float ey = y + (row + 0.5f) * cell / 3f
                                    - (textPaint.descent() + textPaint.ascent()) / 2;
                            canvas.drawText(String.valueOf(n), ex, ey, textPaint);
                        }
                        count++;
                    }
                    textPaint.setTextSize(cell * 0.6f); // Restore text size
                }
            }
        }

        // Draw all thin (light gray) lines first
        for (int i = 0; i <= 9; i++) {
            if (i % 3 != 0) {
                float x = left + i * cell;
                canvas.drawLine(x, top, x, top + boardSize, thinLinePaint);
                float y = top + i * cell;
                canvas.drawLine(left, y, left + boardSize, y, thinLinePaint);
            }
        }
        // Draw all thick (black) lines last, so they appear on top
        float halfThick = thickLinePaint.getStrokeWidth() / 2f;
        for (int i = 0; i <= 9; i++) {
            if (i % 3 == 0) {
                float x, y;
                if (i == 0) {
                    x = left + halfThick;
                    y = top + halfThick;
                } else if (i == 9) {
                    x = left + boardSize - halfThick;
                    y = top + boardSize - halfThick;
                } else {
                    x = left + i * cell;
                    y = top + i * cell;
                }
                // Vertical
                canvas.drawLine(x, top + halfThick, x, top + boardSize - halfThick, thickLinePaint);
                // Horizontal
                canvas.drawLine(left + halfThick, y, left + boardSize - halfThick, y, thickLinePaint);
            }
        }
    }

    // Handle touch to select cell
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int left = (getWidth() - boardSize) / 2;
            int top = (getHeight() - boardSize) / 2;
            float x = event.getX() - left;
            float y = event.getY() - top;
            if (x >= 0 && y >= 0 && x < boardSize && y < boardSize) {
                int col = (int) (x / cellSize);
                int row = (int) (y / cellSize);
                selectedRow = row;
                selectedCol = col;
                int val = sudokuGrid[row][col];
                highlightedNumber = (val != 0) ? val : null; // highlight if not empty
                invalidate();
                if (onCellSelectedListener != null) {
                    onCellSelectedListener.onCellSelected(row, col);
                }
            }
        }
        return true;
    }

    // Set the initial grid and mark clues
    public void setGrid(int[][] grid) {
        this.sudokuGrid = new int[9][9];
        this.isClue = new boolean[9][9];
        this.isError = new boolean[9][9];
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                this.sudokuGrid[r][c] = grid[r][c];
                this.isClue[r][c] = grid[r][c] != 0;
                this.isError[r][c] = false;
            }
        }
        invalidate();
    }

    // Set pencilmark mode (call from activity)
    public void setPencilmarkMode(boolean enabled) {
        this.pencilmarkMode = enabled;
    }

    // Set a number or pencilmark in the selected cell
    public void setNumber(int number) {
        if (selectedRow >= 0 && selectedCol >= 0 && !isClue[selectedRow][selectedCol]) {
            if (pencilmarkMode) {
                if (pencilmarks[selectedRow][selectedCol].contains(number)) {
                    pencilmarks[selectedRow][selectedCol].remove(number);
                } else {
                    pencilmarks[selectedRow][selectedCol].add(number);
                }
                invalidate();
            } else {
                sudokuGrid[selectedRow][selectedCol] = number;
                pencilmarks[selectedRow][selectedCol].clear(); // Clear pencilmarks on input
                checkErrors();
                highlightedNumber = number; // highlight this number
                invalidate();
            }
        }
    }

    // Delete user input or pencilmarks in selected cell
    public void deleteSelected() {
        if (selectedRow >= 0 && selectedCol >= 0 && !isClue[selectedRow][selectedCol]) {
            if (pencilmarkMode) {
                pencilmarks[selectedRow][selectedCol].clear();
            } else {
                sudokuGrid[selectedRow][selectedCol] = 0;
                isError[selectedRow][selectedCol] = false;
                highlightedNumber = null; // clear highlight
                checkErrors(); // <-- Add this line!
            }
            invalidate();
        }
    }

    // Check for visible errors (row, col, box)
    public void checkErrors() {
        // Reset errors
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                isError[r][c] = false;

        // Check for duplicates
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                int val = sudokuGrid[r][c];
                if (val == 0)
                    continue;
                // Only check user input
                if (isClue[r][c])
                    continue;
                // Row
                for (int cc = 0; cc < 9; cc++) {
                    if (cc != c && sudokuGrid[r][cc] == val) {
                        isError[r][c] = true;
                        break;
                    }
                }
                // Col
                for (int rr = 0; rr < 9; rr++) {
                    if (rr != r && sudokuGrid[rr][c] == val) {
                        isError[r][c] = true;
                        break;
                    }
                }
                // Box
                int boxRow = (r / 3) * 3, boxCol = (c / 3) * 3;
                for (int dr = 0; dr < 3; dr++) {
                    for (int dc = 0; dc < 3; dc++) {
                        int rr = boxRow + dr, cc = boxCol + dc;
                        if ((rr != r || cc != c) && sudokuGrid[rr][cc] == val) {
                            isError[r][c] = true;
                            break;
                        }
                    }
                }
            }
        }
    }

    // Listener for cell selection (optional, for GameActivity)
    public interface OnCellSelectedListener {
        void onCellSelected(int row, int col);
    }

    private OnCellSelectedListener onCellSelectedListener;

    // Optionally expose selected cell for GameActivity
    public int getSelectedRow() {
        return selectedRow;
    }

    public int getSelectedCol() {
        return selectedCol;
    }

    public boolean isSelectedCellClue() {
        return selectedRow >= 0 && selectedCol >= 0 && isClue[selectedRow][selectedCol];
    }

    public int[][] getGrid() {
        int[][] copy = new int[9][9];
        for (int r = 0; r < 9; r++)
            System.arraycopy(sudokuGrid[r], 0, copy[r], 0, 9);
        return copy;
    }

    public boolean[][] getIsClue() {
        boolean[][] copy = new boolean[9][9];
        for (int r = 0; r < 9; r++)
            System.arraycopy(isClue[r], 0, copy[r], 0, 9);
        return copy;
    }

    public boolean[][] getIsError() {
        boolean[][] copy = new boolean[9][9];
        for (int r = 0; r < 9; r++)
            System.arraycopy(isError[r], 0, copy[r], 0, 9);
        return copy;
    }

    public void setIsError(boolean[][] error) {
        for (int r = 0; r < 9; r++)
            System.arraycopy(error[r], 0, isError[r], 0, 9);
        invalidate();
    }

    public Set<Integer>[][] getPencilmarks() {
        return pencilmarks;
    }

    public void setPencilmarks(Set<Integer>[][] pencilmarks) {
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                this.pencilmarks[r][c] = new HashSet<>(pencilmarks[r][c]);
        invalidate();
    }

    // Optionally, add a method to set both grid and isClue at once
    public void setGridAndClues(int[][] grid, boolean[][] clues) {
        this.sudokuGrid = new int[9][9];
        this.isClue = new boolean[9][9];
        this.isError = new boolean[9][9];
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                this.sudokuGrid[r][c] = grid[r][c];
                this.isClue[r][c] = clues[r][c];
                this.isError[r][c] = false;
            }
        }
        invalidate();
    }

    public void setCellAsClue(int row, int col) {
        if (row >= 0 && row < 9 && col >= 0 && col < 9) {
            isClue[row][col] = true;
            invalidate();
        }
    }
}