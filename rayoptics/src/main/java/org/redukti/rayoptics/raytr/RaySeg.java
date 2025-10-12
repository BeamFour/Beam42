// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector3;

import java.util.Objects;

/**
 * ray intersection and transfer data
 */
public class RaySeg {
    /**
     * the point of incidence
     */
    public Vector3 p;
    /**
     * ray direction cosine following the interface
     */
    public Vector3 d;
    /**
     * geometric distance to next point of incidence
     */
    public double dst;
    /**
     * surface normal vector at the point of incidence
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
