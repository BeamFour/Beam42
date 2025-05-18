package org.redukti.rayoptics.elem.transform;

import org.redukti.mathlib.Matrix3;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.math.Transform3;
import org.redukti.rayoptics.raytr.RayData;
import org.redukti.rayoptics.seq.Interface;
import org.redukti.rayoptics.util.Pair;

public class Transform {

    /**
     * generate transform rotation and translation from
     *         s1 coords to s2 coords
     *
     * @param s1
     * @param zdist
     * @param s2
     * @return
     */
    public static Transform3 forward_transform(Interface s1, double zdist, Interface s2) {
        // calculate origin of s2 wrt to s1
        Vector3 t_orig = new Vector3(0., 0., zdist);
        Matrix3 r_after_s1 = null,
                r_before_s2 = null;
        if (s1.decenter != null) {
            // get transformation info after s1
            Pair<Matrix3, Vector3> after = s1.decenter.tform_after_surf();
            r_after_s1 = after.first;
            Vector3 t_after_s1 = after.second;
            t_orig = t_orig.add(t_after_s1);
        }
        if (s2.decenter != null) {
            // get transformation info before s2
            Pair<Matrix3, Vector3> before = s2.decenter.tform_before_surf();
            r_before_s2 = before.first;
            Vector3 t_before_s2 = before.second;
            t_orig = t_orig.add(t_before_s2);
        }
        Matrix3 r_cascade = Matrix3.IDENTITY;
        if (r_after_s1 != null) {
            // rotate the origin of s2 around s1 "after" transformation
            t_orig = r_after_s1.multiply(t_orig);
            r_cascade = r_after_s1;
            if (r_before_s2 != null) {
                r_cascade = r_after_s1.multiply(r_before_s2);
            }
        }
        else if (r_before_s2 != null) {
            r_cascade = r_before_s2;
        }
        return new Transform3(r_cascade, t_orig);
    }

    /**
     * generate transform rotation and translation from
     *         s2 coords to s1 coords
     *
     * @param s1
     * @param zdist
     * @param s2
     * @return
     */
    public static Transform3 reverse_transform(Interface s1, double zdist, Interface s2) {
        // calculate origin of s2 wrt to s1
        Vector3 t_orig = new Vector3(0., 0., zdist);
        Matrix3 r_after_s1 = null,
                r_before_s2 = null;
        if (s1.decenter != null) {
            // get transformation info after s1
            Pair<Matrix3, Vector3> after = s1.decenter.tform_after_surf();
            r_after_s1 = after.first;
            Vector3 t_after_s1 = after.second;
            t_orig = t_orig.add(t_after_s1);
        }
        if (s2.decenter != null) {
            // get transformation info before s2
            Pair<Matrix3, Vector3> before = s2.decenter.tform_before_surf();
            r_before_s2 = before.first;
            Vector3 t_before_s2 = before.second;
            t_orig = t_orig.add(t_before_s2);
        }
        // going in reverse direction so negate translation
        t_orig = t_orig.negate();
        Matrix3 r_cascade = Matrix3.IDENTITY;
        if (r_before_s2 != null) {
            // rotate the origin of s1 around s2 "before" transformation
            r_cascade = r_before_s2.transpose();
            t_orig = r_cascade.multiply(t_orig); // TODO check what dot() does
            if (r_after_s1 != null) {
                r_cascade = r_cascade.multiply(r_after_s1.transpose()); // TODO check what dot does
            }
        }
        else if (r_after_s1 != null) {
            r_cascade = r_after_s1.transpose();
        }
        return new Transform3(r_cascade, t_orig);
    }

    /**
     * Transform ray_seg from interface to following seg.
     *
     *     Args:
     *         interface: the :class:'~seq.interface.Interface' for the path sequence
     *         ray_seg: ray segment exiting from **interface**
     *
     *     Returns:
     *         (**b4_pt**, **b4_dir**)
     *
     *         - **b4_pt** - ray intersection pt wrt following seg
     *         - **b4_dir** - ray direction cosine wrt following seg
     * @param ifc
     * @param ray_seg
     */
    public static RayData transform_after_surface(Interface ifc, RayData ray_seg) {
        Vector3 b4_pt;
        Vector3 b4_dir;
        if (ifc.decenter != null) {
            // get transformation info after surf
            Pair<Matrix3, Vector3> xform = ifc.decenter.tform_after_surf();
            Matrix3 r = xform.first;
            Vector3 t = xform.second;
            if (r == null) {
                b4_pt = ray_seg.p.minus(t);
                b4_dir = ray_seg.d;
            }
            else {
                Matrix3 rt = r.transpose();
                b4_pt = rt.multiply(ray_seg.p.minus((t)));
                b4_dir = rt.multiply(ray_seg.d);
            }
        }
        else {
            b4_pt = ray_seg.p;
            b4_dir = ray_seg.d;
        }
        return new RayData(b4_pt, b4_dir);
    }
}
