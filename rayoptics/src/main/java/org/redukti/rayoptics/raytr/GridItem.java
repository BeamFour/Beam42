// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector2;

public class GridItem {
    public final Vector2 pupil;
    public final Double result;
    public final RayPkg ray_pkg;
    public final double weight;

    public GridItem(Vector2 pupil, RayPkg ray_pkg) {
        this(pupil, ray_pkg, null, 1.0);
    }
    public GridItem(Vector2 pupil, RayPkg ray_pkg, double result) {
        this(pupil, ray_pkg, result, 1.0);
    }
    private GridItem(Vector2 pupil, RayPkg ray_pkg, Double result, double weight) {
        this.pupil = pupil;
        this.result = result;
        this.ray_pkg = ray_pkg;
        this.weight = weight;
    }
    public GridItem withWeight(double weight) {
        return new GridItem(pupil, ray_pkg, result, weight);
    }
    @Override
    public String toString() {
        return pupil.toString();
    }
}
