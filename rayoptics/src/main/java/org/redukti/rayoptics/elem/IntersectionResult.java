package org.redukti.rayoptics.elem;

import org.redukti.rayoptics.math.Vector3;

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
