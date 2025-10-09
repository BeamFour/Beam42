package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector2;

public class TraceGridDef {

    public final Vector2 grid_start;
    public final Vector2 grid_stop;
    public final int num_rays;

    public TraceGridDef(Vector2 grid_start, Vector2 grid_stop, int num_rays) {
        this.grid_start = grid_start;
        this.grid_stop = grid_stop;
        this.num_rays = num_rays;
    }
}
