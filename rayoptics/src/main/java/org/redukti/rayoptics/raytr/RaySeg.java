package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector3;

import java.util.Objects;

/**
 * ray intersection and transfer data
 */
public class RaySeg {
    /**
     * intersection point with interface
     */
    public Vector3 p;
    /**
     * direction cosine exiting the interface
     */
    public Vector3 d;
    /**
     * distance from intersection point to next interface
     */
    public double dst;
    /**
     * surface normal at intersection point
     */
    public Vector3 nrml;

    // TODO phase


    public RaySeg(Vector3 p, Vector3 d, double dst, Vector3 nrml) {
        this.p = p;
        this.d = d;
        this.dst = dst;
        this.nrml = nrml;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(p="
                + p.toString() + ", d=" + d.toString()
                + ", dst=" + Objects.toString(dst)
                + ", nrml=" + nrml.toString()
                + ")";
    }
}
