// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector3;

public class RayResultWithStopCoord {
    public final Vector3 stop_coord;
    public final RayResult rr;
    public final int stop_idx;

    public RayResultWithStopCoord(Vector3 stop_coord, RayResult rr, int stop_idx) {
        this.stop_coord = stop_coord;
        this.rr = rr;
        this.stop_idx = stop_idx;
    }
}
