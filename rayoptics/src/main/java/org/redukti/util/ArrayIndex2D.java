// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.util;

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
