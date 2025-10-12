// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.elem.surface;

import org.redukti.mathlib.Vector3;

public class IntersectionResult {
    /**
     * distance to intersection point
     */
    public final double distance;
    /**
     * intersection point p
     */
    public final Vector3 intersection_point;

    public IntersectionResult(double x, Vector3 v) {
        this.distance = x;
        this.intersection_point = v;
    }
}
