package org.redukti.rayoptics.raytr;

public class VigResult {
    /**
     * vignetting factor
     */
    public final double vig;
    /**
     * the index of the limiting interface
     */
    public final Integer clip_indx;
    /**
     * the vignetting-limited ray
     */
    public final RayPkg ray_pkg;

    public VigResult(double vig, Integer clip_indx, RayPkg ray_pkg) {
        this.vig = vig;
        this.clip_indx = clip_indx;
        this.ray_pkg = ray_pkg;
    }
}
