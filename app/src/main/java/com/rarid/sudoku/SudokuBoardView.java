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
import java.util.ArrayList;
import java.util.List;

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
    
    // Undo functionality
    private List<Move> moveHistory = new ArrayList<>();
    private static final int MAX_UNDO_MOVES = 10;

    // Screen burn prevention variables
    private float currentOffsetX = 0;
    private float currentOffsetY = 0;
    private float currentOpacity = 1.0f;
    private long lastAnimationTime = 0;
    private static final long ANIMATION_INTERVAL = 5 * 60 * 1000; // 5 minutes
    private static final float MAX_OFFSET = 2.0f; // Maximum pixel offset
    private static final float OPACITY_VARIATION = 0.02f; // Maximum opacity variation

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

        // Update screen burn prevention
        updateScreenBurnPrevention();

        int availableWidth = getWidth();
        int availableHeight = getHeight();
        float margin = boardSize / 14f;

        int drawBoardSize = availableWidth;
        int left = 0;
        int top = (availableHeight - drawBoardSize) / 2;
        if (availableHeight < availableWidth) {
            drawBoardSize = availableHeight;
            left = (availableWidth - drawBoardSize) / 2;
            top = 0;
        }

        // Apply screen burn prevention offsets
        float cellAreaLeft = left + margin + currentOffsetX;
        float cellAreaTop = top + margin + currentOffsetY;
        float cellAreaSize = drawBoardSize - 2 * margin;
        float cell = cellAreaSize / 9f;

        // Apply opacity variation
        float radius = cellAreaSize / 18f;
        RectF bgRect = new RectF(cellAreaLeft, cellAreaTop, cellAreaLeft + cellAreaSize, cellAreaTop + cellAreaSize);
        cellBgPaint.setColor(Color.WHITE);
        cellBgPaint.setAlpha((int) (255 * currentOpacity));
        canvas.drawRoundRect(bgRect, radius, radius, cellBgPaint);

        // Draw cells and numbers
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                float x = cellAreaLeft + c * cell;
                float y = cellAreaTop + r * cell;
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
                float x = cellAreaLeft + i * cell;
                canvas.drawLine(x, cellAreaTop, x, cellAreaTop + cellAreaSize, thinLinePaint);
                float y = cellAreaTop + i * cell;
                canvas.drawLine(cellAreaLeft, y, cellAreaLeft + cellAreaSize, y, thinLinePaint);
            }
        }
        // Draw all thick (black) lines last, so they appear on top
        float halfThick = thickLinePaint.getStrokeWidth() / 2f;
        for (int i = 0; i <= 9; i++) {
            if (i % 3 == 0) {
                float x, y;
                if (i == 0) {
                    x = cellAreaLeft - halfThick; // Draw slightly outside for border
                    y = cellAreaTop - halfThick; // Draw slightly outside for border
                } else if (i == 9) {
                    x = cellAreaLeft + cellAreaSize + halfThick; // Draw slightly outside for border
                    y = cellAreaTop + cellAreaSize + halfThick; // Draw slightly outside for border
                } else {
                    x = cellAreaLeft + i * cell;
                    y = cellAreaTop + i * cell;
                }
                // Vertical: Extend lines to cover the full outer border
                canvas.drawLine(x, cellAreaTop - halfThick, x, cellAreaTop + cellAreaSize + halfThick, thickLinePaint);
                // Horizontal: Extend lines to cover the full outer border
                canvas.drawLine(cellAreaLeft - halfThick, y, cellAreaLeft + cellAreaSize + halfThick, y,
                        thickLinePaint);
            }
        }
    }

    private void updateScreenBurnPrevention() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAnimationTime >= ANIMATION_INTERVAL) {
            // Generate new random offsets
            currentOffsetX = (float) (Math.random() * MAX_OFFSET * 2 - MAX_OFFSET);
            currentOffsetY = (float) (Math.random() * MAX_OFFSET * 2 - MAX_OFFSET);
            currentOpacity = 1.0f - (float) (Math.random() * OPACITY_VARIATION);
            lastAnimationTime = currentTime;
        }
    }

    // Handle touch to select cell
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float touchX = event.getX();
            float touchY = event.getY();

            int availableWidth = getWidth();
            int availableHeight = getHeight();
            float margin = boardSize / 14f;

            int drawBoardSize = availableWidth;
            int boardLeft = 0;
            int boardTop = (availableHeight - drawBoardSize) / 2;
            if (availableHeight < availableWidth) {
                drawBoardSize = availableHeight;
                boardLeft = (availableWidth - drawBoardSize) / 2;
                boardTop = 0;
            }

            float cellAreaLeft = boardLeft + margin;
            float cellAreaTop = boardTop + margin;
            float cellAreaSize = drawBoardSize - 2 * margin;
            float currentCellSize = cellAreaSize / 9f;

            if (touchX >= cellAreaLeft && touchX < cellAreaLeft + cellAreaSize && touchY >= cellAreaTop
                    && touchY < cellAreaTop + cellAreaSize) {
                int col = (int) ((touchX - cellAreaLeft) / currentCellSize);
                int row = (int) ((touchY - cellAreaTop) / currentCellSize);
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
            int oldValue = sudokuGrid[selectedRow][selectedCol];
            Set<Integer> oldPencilmarks = new HashSet<>(pencilmarks[selectedRow][selectedCol]);
            
            if (pencilmarkMode) {
                if (pencilmarks[selectedRow][selectedCol].contains(number)) {
                    pencilmarks[selectedRow][selectedCol].remove(number);
                } else {
                    pencilmarks[selectedRow][selectedCol].add(number);
                }
                // Record move for undo
                addMoveToHistory(selectedRow, selectedCol, oldValue, 0, oldPencilmarks, true);
                invalidate();
            } else {
                sudokuGrid[selectedRow][selectedCol] = number;
                pencilmarks[selectedRow][selectedCol].clear(); // Clear pencilmarks on input
                // Record move for undo
                addMoveToHistory(selectedRow, selectedCol, oldValue, number, oldPencilmarks, false);
                checkErrors();
                highlightedNumber = number; // highlight this number
                invalidate();
            }
        }
    }

    // Delete user input or pencilmarks in selected cell
    public void deleteSelected() {
        if (selectedRow >= 0 && selectedCol >= 0 && !isClue[selectedRow][selectedCol]) {
            int oldValue = sudokuGrid[selectedRow][selectedCol];
            Set<Integer> oldPencilmarks = new HashSet<>(pencilmarks[selectedRow][selectedCol]);
            
            if (pencilmarkMode) {
                pencilmarks[selectedRow][selectedCol].clear();
                // Record move for undo
                addMoveToHistory(selectedRow, selectedCol, 0, 0, oldPencilmarks, true);
            } else {
                sudokuGrid[selectedRow][selectedCol] = 0;
                isError[selectedRow][selectedCol] = false;
                highlightedNumber = null; // clear highlight
                // Record move for undo
                addMoveToHistory(selectedRow, selectedCol, oldValue, 0, oldPencilmarks, false);
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

    public boolean isPencilmarkMode() {
        return pencilmarkMode;
    }
    
    // Undo functionality methods
    private void addMoveToHistory(int row, int col, int oldValue, int newValue, Set<Integer> oldPencilmarks, boolean wasPencilmark) {
        Move move = new Move(row, col, oldValue, newValue, new HashSet<>(oldPencilmarks), wasPencilmark);
        moveHistory.add(move);
        
        // Limit history size
        if (moveHistory.size() > MAX_UNDO_MOVES) {
            moveHistory.remove(0);
        }
    }
    
    public boolean canUndo() {
        return !moveHistory.isEmpty();
    }
    
    public void undo() {
        if (!moveHistory.isEmpty()) {
            Move lastMove = moveHistory.remove(moveHistory.size() - 1);
            
            // Restore the previous state
            if (lastMove.wasPencilmark()) {
                // Restore pencilmarks
                pencilmarks[lastMove.getRow()][lastMove.getCol()].clear();
                pencilmarks[lastMove.getRow()][lastMove.getCol()].addAll(lastMove.getOldPencilmarks());
            } else {
                // Restore number
                sudokuGrid[lastMove.getRow()][lastMove.getCol()] = lastMove.getOldValue();
                pencilmarks[lastMove.getRow()][lastMove.getCol()].clear();
                pencilmarks[lastMove.getRow()][lastMove.getCol()].addAll(lastMove.getOldPencilmarks());
                
                // Clear error state
                isError[lastMove.getRow()][lastMove.getCol()] = false;
                
                // Update highlight
                if (lastMove.getOldValue() > 0) {
                    highlightedNumber = lastMove.getOldValue();
                } else {
                    highlightedNumber = null;
                }
                
                // Recheck errors
                checkErrors();
            }
            
            invalidate();
        }
    }
    
    public void clearMoveHistory() {
        moveHistory.clear();
    }
}