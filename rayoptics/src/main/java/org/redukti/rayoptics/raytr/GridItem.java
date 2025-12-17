// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector2;

public class GridItem {
    public final Vector2 pupil;
    public final Double result;
    public final RayPkg ray_pkg;

    public GridItem(Vector2 pupil, RayPkg ray_pkg) {
        this.pupil = pupil;
        this.result = null;
        this.ray_pkg = ray_pkg;
    }
    public GridItem(Vector2 pupil, RayPkg ray_pkg, double result) {
        this.pupil = pupil;
        this.result = result;
        this.ray_pkg = ray_pkg;
    }
    @Override
    public String toString() {
        return pupil.toString();
    }
}
