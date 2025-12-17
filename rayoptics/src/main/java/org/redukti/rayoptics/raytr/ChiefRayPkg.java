// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

public class ChiefRayPkg {
    public final RayPkg chief_ray;
    public final ChiefRayExitPupilSegment cr_exp_seg;

    public ChiefRayPkg(RayPkg chief_ray, ChiefRayExitPupilSegment cr_exp_seg) {
        this.chief_ray = chief_ray;
        this.cr_exp_seg = cr_exp_seg;
    }
}
