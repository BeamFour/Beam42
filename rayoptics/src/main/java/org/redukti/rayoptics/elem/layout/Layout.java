package org.redukti.rayoptics.elem.layout;

import org.redukti.mathlib.Matrix3;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.elem.transform.Transform;
import org.redukti.rayoptics.math.Transform3;
import org.redukti.rayoptics.raytr.RayPkg;
import org.redukti.rayoptics.raytr.RaySeg;
import org.redukti.rayoptics.seq.Interface;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.util.Lists;

import java.util.List;

public class Layout {

    /**
     * compute transformation for rays "start_offset" from 1st surface
     *
     *     Args:
     *         seq_model: the sequential model
     *         start_offset: z distance rays should start wrt first surface.
     *                       positive if to left of first surface
     *     Returns:
     *         transformation rotation and translation::
     *             (rot, t)
     * @param seq_model
     * @param start_offset
     * @return
     */
    public static Transform3 setup_shift_of_ray_bundle(SequentialModel seq_model, double start_offset) {
        Interface s1 = Lists.get(seq_model.ifcs, 1);
        Interface s0 = Lists.get(seq_model.ifcs, 0);
        return Transform.reverse_transform(s0, start_offset, s1);
    }

    /**
     * modify ray_bundle so that rays begin "start_offset" from 1st surface
     *
     *     Args:
     *         ray_bundle: list of rays in a bundle, i.e. all for one field.
     *                     ray_bundle[cr_indx] is the chief/central ray
     *         start_offset: z distance rays should start wrt first surface.
     *                       positive if to left of first surface
     *         rot: transformation rotation
     *         t: transformation translation
     *         cr_indx: index of the central ray in the bundle
     * @param start_bundle
     * @param ray_bundle
     * @param trfm
     * @param cr_indx
     */
    public static void shift_start_of_ray_bundle(List<RaySeg> start_bundle, List<RayPkg> ray_bundle, Transform3 trfm, int cr_indx) {

        Matrix3 rot = trfm.rot_mat;
        Vector3 t = trfm.vec;

        // For the chief ray, use the input offset.
        RayPkg cr = ray_bundle.get(cr_indx);
        List<RaySeg> ray = cr.ray;
        Vector3 pt1_t = rot.multiply(Lists.get(ray, 1).p).minus(t);
        Vector3 dir0 = rot.multiply(Lists.get(ray, 0).d);
        double dst = -pt1_t.z / dir0.z;
        Vector3 pt0 = pt1_t.plus(dir0.times(dst));
        RaySeg ray0 = new RaySeg(pt0, dir0, dst, Lists.get(ray, 0).nrml);
        start_bundle.set(cr_indx, ray0);

        for (int ri = 0; ri < ray_bundle.size(); ri++) {
            RayPkg ray_pkg = ray_bundle.get(ri);
            ray = ray_pkg.ray;
            Vector3 b4_pt = rot.multiply(Lists.get(ray, 1).p).minus(t);
            Vector3 b4_dir = rot.multiply(Lists.get(ray, 0).d);
            if (ri != cr_indx) {
                // Calculate distance along ray to plane perpendicular to
                // the chief ray.
                dst = -(b4_pt.minus(pt0)).dot(dir0)/b4_dir.dot(dir0);
                Vector3 pt = b4_pt.plus(b4_dir.times(dst));
                ray0 = new RaySeg(pt, b4_dir, dst, Lists.get(ray,0).nrml);
                start_bundle.set(ri, ray0);
            }
        }
    }
}
