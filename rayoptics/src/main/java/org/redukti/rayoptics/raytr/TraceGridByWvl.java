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
