package org.redukti.mathlib;

public class Line3 {
    /**
     * intersection point with interface
     */
    public final Vector3 origin;
    /**
     * direction cosine exiting the interface
     */
    public final Vector3 direction;

    public Line3(Vector3 origin, Vector3 direction) {
        this.origin = origin;
        this.direction = direction;
    }
}
