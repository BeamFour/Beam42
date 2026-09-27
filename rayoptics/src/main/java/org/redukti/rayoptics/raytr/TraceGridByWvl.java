// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import java.util.List;

public class TraceGridByWvl {
    public final double wvl;
    public final List<GridItem> grid;

    public TraceGridByWvl(double wvl, List<GridItem> grid) {
        this.wvl = wvl;
        this.grid = grid;
    }
}
