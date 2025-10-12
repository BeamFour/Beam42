// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
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
