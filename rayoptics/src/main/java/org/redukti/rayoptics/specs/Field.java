package org.redukti.rayoptics.specs;

public class Field {
    /**
     * x, y chief ray coords on the paraxial entrance pupil plane
     */
    public double[] aim_pt;
    double x; // x field component
    double y; // y field component
    double vux; // +x vignetting factor
    double vuy; // +y vignetting factor
    double vlx; // -x vignetting factor
    double vly; // -y vignetting factor
    double wt; //  field weight

    public void update() {
        // TODO
//        self.chief_ray = None
//        self.ref_sphere = None
    }
    //aim_pt: x, y chief ray coords on the paraxial entrance pupil plane
    //chief_ray: ray package for the ray from the field point throught the
    //center of the aperture stop, traced in the central
    //wavelength
    //ref_sphere: a tuple containing (image_pt, ref_dir, ref_sphere_radius)
}
