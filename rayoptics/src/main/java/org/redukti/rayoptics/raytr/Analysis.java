package org.redukti.rayoptics.raytr;

import org.redukti.rayoptics.math.Vector2;
import org.redukti.rayoptics.math.Vector3;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.util.Lists;

public class Analysis {

    /**
     * Get the chief ray package at **fld**, computing it if necessary.
     *
     * @param opt_model
     * @param fld :class:`~.Field` point for wave aberration calculation
     * @param wvl wavelength of ray (nm)
     * @param foc defocus amount
     * @return tuple of chief_ray, cr_exp_seg
     */
    public static ChiefRayPkg get_chief_ray_pkg(OpticalModel opt_model, Field fld, double wvl, double foc) {
        if (fld.chief_ray == null) {
            Trace.aim_chief_ray(opt_model, fld, wvl);
            ChiefRayPkg chief_ray_pkg = Trace.trace_chief_ray(opt_model, fld, wvl, foc);
            fld.chief_ray = chief_ray_pkg;
        }
        else if (fld.chief_ray.chief_ray.wvl != wvl) {
            ChiefRayPkg  chief_ray_pkg = Trace.trace_chief_ray(opt_model, fld, wvl, foc);
            fld.chief_ray = chief_ray_pkg;
        }
        return fld.chief_ray;
    }

    /**
     * Compute the reference sphere for a defocussed image point at **fld
     *
     * @param opt_model
     * @param fld :class:`~.Field` point for wave aberration calculation
     * @param wvl wavelength of ray (nm)
     * @param foc defocus amount
     * @param chief_ray_pkg input tuple of chief_ray, cr_exp_seg
     * @param image_pt_2d x, y image point in (defocussed) image plane, if None, use
     *                      the chief ray coordinate.
     * @return ref_sphere: tuple of image_pt, ref_dir, ref_sphere_radius
     */
    public RefSphere setup_exit_pupil_coords(OpticalModel opt_model, Field fld, double wvl, double foc,
                                             ChiefRayPkg chief_ray_pkg, Vector2 image_pt_2d) {
        RayPkg cr = chief_ray_pkg.chief_ray;
        ChiefRayExitPupilSegment cr_exp_seg = chief_ray_pkg.cr_exp_seg;
        // cr_exp_pt: E upper bar prime: pupil center for pencils from Q
        // cr_exp_pt, cr_b4_dir, cr_dst
        // cr_exp_pt = cr_exp_seg[mc.p]

        Vector3 image_pt;
        if (image_pt_2d == null) {
            // get distance along cr corresponding to a z shift of the defocus
            double dist = foc / Lists.get(cr.ray, -1).d.z;
            image_pt = Lists.get(cr.ray,-1).p.plus(Lists.get(cr.ray, -1).d.times(dist));
        }
        else {
            image_pt = new Vector3(image_pt_2d.x, image_pt_2d.y, foc);
        }

        // get the image point wrt the final surface
        double image_thi = Lists.get(opt_model.seq_model.gaps, -1).thi;
        Vector3 img_pt = new Vector3(image_pt.x, image_pt.y, image_pt.z + image_thi);
        
        // R' radius of reference sphere for O'
        Vector3 ref_sphere_vec = img_pt.minus(cr_exp_seg.exp_pt);
        double ref_sphere_radius = ref_sphere_vec.length();
        Vector3 ref_dir = ref_sphere_vec.normalize();

        return new RefSphere(image_pt, ref_dir, ref_sphere_radius);
    }
}
