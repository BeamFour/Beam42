package org.redukti.rayoptics.raytr;

public class ChiefRayPkg {
    public RayPkg chief_ray;
    public ChiefRayExitPupilSegment cr_exp_seg;

    public ChiefRayPkg(RayPkg chief_ray, ChiefRayExitPupilSegment cr_exp_seg) {
        this.chief_ray = chief_ray;
        this.cr_exp_seg = cr_exp_seg;
    }
}
