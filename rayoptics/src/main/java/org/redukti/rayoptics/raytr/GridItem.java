// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector2;

public class GridItem {
    public Vector2 pupil;
    public Double result = null;
    public RayPkg ray_pkg = null;

    public GridItem(Vector2 pupil, RayPkg ray_pkg) {
        this.pupil = pupil;
        this.ray_pkg = ray_pkg;
    }
    public GridItem(Vector2 pupil, RayPkg ray_pkg, double result) {
        this.pupil = pupil;
        this.result = result;
        this.ray_pkg = ray_pkg;
    }
    public GridItem(Vector2 pupil, double result) {
        this.pupil = pupil;
        this.result = result;
    }

    @Override
    public String toString() {
        return pupil.toString();
    }
}
