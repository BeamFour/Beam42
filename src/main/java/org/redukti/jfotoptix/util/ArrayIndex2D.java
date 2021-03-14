package org.redukti.jfotoptix.util;

public final class ArrayIndex2D {
    final int rowSize;
    final int colSize;

    public ArrayIndex2D(int rowSize, int colSize) {
        this.rowSize = rowSize;
        this.colSize = colSize;
    }

    public final int i(int row, int col)
    {
        return colSize * row + col;
    }
}
