// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.elem.transform.Transform;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.seq.Interface;
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

    /**
     * Given the exiting interface and chief ray data, return exit pupil ray coords.
     *
     *     Args:
     *         interface: the exiting :class:'~.Interface' for the path sequence
     *         ray_seg: ray segment exiting from **interface**
     *         exp_dst_parax: z distance to the paraxial exit pupil
     *
     *     Returns:
     *         (**exp_pt**, **exp_dir**, **exp_dst**)
     *
     *         - **exp_pt** - ray intersection with exit pupil plane
     *         - **exp_dir** - direction cosine of the ray in exit pupil space
     *         - **exp_dst** - distance from interface to exit pupil pt
     *         - **interface** - exiting :class:'~.Interface' for the path sequence
     *         - **b4_pt** - ray intersection pt wrt image gap coordinates
     *         - **b4_dir** - ray direction cosine wrt image gap coordinates
     */
    public static ChiefRayExitPupilSegment transfer_to_exit_pupil(
            Interface ifc,
            RayData ray_seg,
            double exp_dst_parax) {
        RayData b4_ray = Transform.transform_after_surface(ifc, ray_seg);
        Vector3 b4_pt = b4_ray.pt;
        Vector3 b4_dir = b4_ray.dir;

        // h = b4_pt[0]**2 + b4_pt[1]**2
        // u = b4_dir[0]**2 + b4_dir[1]**2
        // handle field points in the YZ plane

        double h = b4_pt.y;     // y=1
        double u = b4_dir.y;    // y=1
        double exp_dst;
        if (Math.abs(u) < 1e-14) {
            exp_dst = exp_dst_parax;
        } else {
            // exp_dst = -np.sign(b4_dir[2])*sqrt(h/u)
            exp_dst = -h / u;
        }
        Vector3 exp_pt = b4_pt.plus(b4_dir.times(exp_dst));
        Vector3 exp_dir = b4_dir;

        return new ChiefRayExitPupilSegment(exp_pt, exp_dir, exp_dst, ifc, b4_pt, b4_dir);
    }

}
