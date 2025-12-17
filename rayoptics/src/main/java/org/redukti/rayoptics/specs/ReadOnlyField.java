package org.redukti.rayoptics.specs;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.raytr.ChiefRayPkg;
import org.redukti.rayoptics.raytr.ReferenceSphere;

/**
 * A readonly snapshot of a Field
 */
public class ReadOnlyField {
    public final double x; // x field component
    public final double y; // y field component
    public final double vux; // +x vignetting factor
    public final double vuy; // +y vignetting factor
    public final double vlx; // -x vignetting factor
    public final double vly; // -y vignetting factor
    public final double wt; //  field weight
    /**
     * Populated for non wide-angle system
     * x, y chief ray coords on the paraxial entrance pupil plane
     */
    public final Vector2 aim_info;
    /**
     * Populated for wide-angle system
     */
    public final Double z_enp;
    /**
     * ray package for the ray from the field point through the
     * center of the aperture stop, traced in the central wavelength
     */
    public final ChiefRayPkg chief_ray;
    /**
     * a tuple containing (image_pt, ref_dir, ref_sphere_radius)
     */
    public final ReferenceSphere ref_sphere;

    public final FieldSpec fov;

    public ReadOnlyField(Field fld) {
        this.x = fld.x;
        this.y = fld.y;
        this.vux = fld.vux;
        this.vuy = fld.vuy;
        this.vlx = fld.vlx;
        this.vly = fld.vly;
        this.wt = fld.wt;
        this.aim_info = fld.aim_info != null ? new Vector2(fld.aim_info[0],fld.aim_info[1]) : null;
        this.z_enp = fld.z_enp;
        this.chief_ray = fld.chief_ray;
        this.ref_sphere = fld.ref_sphere;
        this.fov = fld.fov;
    }
}
