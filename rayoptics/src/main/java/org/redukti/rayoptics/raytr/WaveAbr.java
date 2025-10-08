package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.util.Lists;

public class WaveAbr {

    /**
     * Compute the reference sphere for a defocussed image point at **fld**.
     *
     *         The local transform from the final interface to the image interface is
     *         included to facilitate infinite refernce sphere calculations.
     *
     *     Args:
     *         opt_model: :class:`~.OpticalModel` instance
     *         fld: :class:`~.Field` point for wave aberration calculation
     *         wvl: wavelength of ray (nm)
     *         foc: defocus amount
     *         chief_ray_pkg: input tuple of chief_ray, cr_exp_seg
     *         image_pt_2d: x, y image point in (defocussed) image plane, if None, use
     *                      the chief ray coordinate.
     *         image_delta: x, y displacements from image_pt_2d in (defocussed)
     *                      image plane, if not None.
     *
     *     Returns:
     *         ref_sphere: tuple of image_pt, ref_dir, ref_sphere_radius, lcl_tfrm_last
     */
    public static ReferenceSphere calculate_reference_sphere(
            OpticalModel opt_model,
            Field fld,
            double wvl,
            double foc,
            ChiefRayPkg chief_ray_pkg,
            Vector2 image_pt_2d,
            Vector2 image_delta) {

        var cr = chief_ray_pkg.chief_ray;
        var cr_exp_seg = chief_ray_pkg.cr_exp_seg;

        Vector3 image_pt;
        if (image_pt_2d == null) {
            // get distance along cr corresponding to a z shift of the defocus
            var dist = foc / Lists.get(cr.ray,-1).d.z;
            image_pt = Lists.get(cr.ray,-1).p.plus(Lists.get(cr.ray,-1).d.times(dist));
        }
        else {
            image_pt = new Vector3(image_pt_2d.x, image_pt_2d.y, foc);
        }
        if (image_delta != null)
            image_pt = new Vector3(image_pt.x+image_delta.x, image_pt.y+image_delta.y, image_pt.z);

        // get the image point wrt the final surface
        var seq_model = opt_model.seq_model;
        var lcl_tfrm_last = Lists.get(seq_model.lcl_tfrms,-2);
        var image_thi = Lists.get(seq_model.gaps,-1).thi;
        var img_pt = new Vector3(image_pt.x, image_pt.y, image_pt.z+image_thi);

        // R' radius of reference sphere for O'
        var ref_sphere_vec = img_pt.minus(cr_exp_seg.exp_pt); //p=exp_pt
        var ref_sphere_radius = ref_sphere_vec.length();
        var ref_dir = ref_sphere_vec.normalize();

        return new ReferenceSphere(image_pt, ref_dir, ref_sphere_radius, lcl_tfrm_last);
    }

}
