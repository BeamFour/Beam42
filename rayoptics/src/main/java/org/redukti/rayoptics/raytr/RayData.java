package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector3;

public class RayData {
    /**
     * intersection point with interface
     */
    public Vector3 pt;
    /**
     * direction cosine exiting the interface
     */
    public Vector3 dir;

    public RayData(Vector3 pt, Vector3 dir) {
        this.pt = pt;
        this.dir = dir;
    }
}
