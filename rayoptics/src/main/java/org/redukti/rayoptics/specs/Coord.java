package org.redukti.rayoptics.specs;

import org.redukti.mathlib.Vector3;

public class Coord {
    public Vector3 pt;
    public Vector3 dir;

    public Coord(Vector3 pt, Vector3 dir) {
        this.pt = pt;
        this.dir = dir;
    }
}
