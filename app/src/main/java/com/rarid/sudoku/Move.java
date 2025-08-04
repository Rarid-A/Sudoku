package com.rarid.sudoku;

import java.util.Set;

public class Move {
    private int row;
    private int col;
    private int oldValue;
    private int newValue;
    private Set<Integer> oldPencilmarks;
    private boolean wasPencilmark;

    public Move(int row, int col, int oldValue, int newValue, Set<Integer> oldPencilmarks, boolean wasPencilmark) {
        this.row = row;
        this.col = col;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.oldPencilmarks = oldPencilmarks;
        this.wasPencilmark = wasPencilmark;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public int getOldValue() { return oldValue; }
    public int getNewValue() { return newValue; }
    public Set<Integer> getOldPencilmarks() { return oldPencilmarks; }
    public boolean wasPencilmark() { return wasPencilmark; }
} 