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
    public final boolean valid;

    public GridItem(Vector2 pupil, RayPkg ray_pkg) {
        this(pupil, ray_pkg, null, 1.0, true);
    }
    public GridItem(Vector2 pupil, RayPkg ray_pkg, double result) {
        this(pupil, ray_pkg, result, 1.0, true);
    }
    private GridItem(Vector2 pupil, RayPkg ray_pkg, Double result, double weight, boolean valid) {
        this.pupil = pupil;
        this.result = result;
        this.ray_pkg = ray_pkg;
        this.weight = weight;
        this.valid = valid;
    }
    public static GridItem failed(Vector2 pupil) {
        return new GridItem(pupil, null, null, 1.0, false);
    }
    public GridItem withWeight(double weight) {
        return new GridItem(pupil, ray_pkg, result, weight, valid);
    }
    @Override
    public String toString() {
        return pupil.toString();
    }
}
