// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.specs;

import org.redukti.mathlib.Vector3;

public class Coord {
    public Vector3 pt;
    public Vector3 dir;

    public Coord(Vector3 pt, Vector3 dir) {
        this.pt = pt;
        this.dir = dir;
    }

    @Override
    public String toString() {
        return "Coord(" + "pt=" + pt + ", dir=" + dir + ')';
    }
}
