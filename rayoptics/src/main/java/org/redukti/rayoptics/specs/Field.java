package org.redukti.rayoptics.specs;

import org.redukti.rayoptics.raytr.ChiefRayPkg;
import org.redukti.rayoptics.raytr.RefSpherePkg;

import java.util.Arrays;

public class Field {
    /**
     * x, y chief ray coords on the paraxial entrance pupil plane
     */
    public double[] aim_pt;
    public double x; // x field component
    public double y; // y field component
    public double vux; // +x vignetting factor
    public double vuy; // +y vignetting factor
    public double vlx; // -x vignetting factor
    public double vly; // -y vignetting factor
    public double wt; //  field weight
    /**
     * ray package for the ray from the field point through the
     * center of the aperture stop, traced in the central wavelength
     */
    public ChiefRayPkg chief_ray;
    /**
     * a tuple containing (image_pt, ref_dir, ref_sphere_radius)
     */
    public RefSpherePkg ref_sphere;

    public void update() {
        chief_ray = null;
        ref_sphere = null;
    }


    public double[] apply_vignetting(double[] pupil) {
        double[] vig_pupil = Arrays.copyOf(pupil, pupil.length);
        if (pupil[0] < 0.0) {
            if (vlx != 0.0) {
                vig_pupil[0] *= (1.0 - vlx);
            }
        }
        else {
            if (vux != 0.0) {
                vig_pupil[0] *= (1.0 - vux);
            }
        }
        if (pupil[1] < 0.0) {
            if (vly != 0.0) {
                vig_pupil[1] *= (1.0 - vly);
            }
        }
        else {
            if (vuy != 0.0) {
                vig_pupil[1] *= (1.0 - vuy);
            }
        }
        return vig_pupil;
    }
}
