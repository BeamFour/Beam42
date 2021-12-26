package org.redukti.rayoptics.raytr;

import org.redukti.rayoptics.math.Vector3;

public class Ray {
    /**
     * intersection point with interface
     */
    public Vector3 p;
    /**
     * direction cosine exiting the interface
     */
    public Vector3 d;

    public Ray(Vector3 p, Vector3 d) {
        this.p = p;
        this.d = d;
    }
}
