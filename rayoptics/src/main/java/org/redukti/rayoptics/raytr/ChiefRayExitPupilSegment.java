// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.seq.Interface;

/**
 * cr_exp_seg: chief ray exit pupil segment (pt, dir, dist)
 */
public final class ChiefRayExitPupilSegment {
    /**
     * Chief ray intersection with exit pupil plane
     */
    public Vector3 exp_pt;
    /**
     * direction cosine of the chief ray in exit pupil space
     */
    public Vector3 exp_dir;
    /**
     * distance from interface to the exit pupil point
     */
    public double exp_dst;
    public Interface ifc;
    public Vector3 b4_pt;
    public Vector3 b4_dir;

    public ChiefRayExitPupilSegment(Vector3 exp_pt, Vector3 exp_dir, double exp_dst, Interface ifc, Vector3 b4_pt, Vector3 b4_dir) {
        this.exp_pt = exp_pt;
        this.exp_dir = exp_dir;
        this.exp_dst = exp_dst;
        this.ifc = ifc;
        this.b4_pt = b4_pt;
        this.b4_dir = b4_dir;
    }
}
