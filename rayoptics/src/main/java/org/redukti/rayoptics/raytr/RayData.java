package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector3;

public class RayData {
    /**
     * intersection point with interface
     */
    public Vector3 p;
    /**
     * direction cosine exiting the interface
     */
    public Vector3 d;

    public RayData(Vector3 p, Vector3 d) {
        this.p = p;
        this.d = d;
    }
}
