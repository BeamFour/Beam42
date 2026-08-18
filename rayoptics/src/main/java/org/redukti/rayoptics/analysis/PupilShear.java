package org.redukti.rayoptics.analysis;

import org.redukti.mathlib.M;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.elem.transform.Transform;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.ChiefRayPkg;
import org.redukti.rayoptics.raytr.RayData;
import org.redukti.rayoptics.raytr.RayPkg;
import org.redukti.rayoptics.raytr.ReferenceSphere;
import org.redukti.rayoptics.raytr.WaveAbr;
import org.redukti.rayoptics.util.Lists;
import org.redukti.rayoptics.util.Orientation;

/**
 * Exit-pupil coordinates and realised spatial frequency for a traced ray.
 *
 * <p>Both quantities are needed by contrast optimization and neither is otherwise
 * reachable: the OTF is the autocorrelation of the exit pupil, but rays are aimed in
 * <em>entrance</em> pupil coordinates, and pupil aberration makes the mapping between the
 * two non-linear. See H. H. Hopkins, <i>Calculation of the aberrations and image
 * assessment for a general optical system</i>, Optica Acta <b>28</b>:5 (1981) 667-714,
 * &sect;5 and &sect;10.
 *
 * <p>This is a separate file rather than an addition to
 * {@link org.redukti.rayoptics.raytr.WaveAbr} because that file is a port of upstream
 * ray-optics. {@link #exit_pupil_coord} repeats five lines from
 * {@code WaveAbr.wave_abr_full_calc_finite_pup}, which computes the same vector as a
 * local named {@code p_coord} and discards it.
 */
public class PupilShear {

    /**
     * The ray's exit-pupil coordinate, measured from the exit pupil centre.
     *
     * <p>This is Hopkins' {@code (X'-x', Y'-y', Z'-z')}: the position of the equally
     * inclined chord point {@code B~'} relative to the chief ray's pupil crossing
     * {@code E'}, in the coordinate frame after the last surface. Its length at full
     * aperture reproduces {@code fod.exp_radius} to the accuracy of the paraxial
     * approximation, but unlike {@code exp_radius} it is the real, per-ray, aberrated
     * coordinate.
     *
     * <p>Hopkins normalises this by the paraxial pupil ray height {@code h'} to obtain
     * reduced coordinates (his 5.1) which stay finite when the pupil recedes. This method
     * returns the unreduced form and reports {@code null} for an infinite reference
     * sphere, because the reference sphere the coordinate is measured against does not
     * exist in that case. An afocal system needs Hopkins (5.4) instead.
     *
     * @return the exit-pupil coordinate, or null if the reference sphere is infinite or
     *         the ray did not reach the last surface
     */
    public static Vector3 exit_pupil_coord(
            RayPkg ray_pkg, ChiefRayPkg chief_ray_pkg, ReferenceSphere ref_sphere) {
        if (ray_pkg == null || ray_pkg.ray == null || ray_pkg.ray.size() < 2) return null;
        if (chief_ray_pkg == null || ref_sphere == null) return null;
        if (M.is_kinda_big(ref_sphere.ref_sphere_radius)) return null;

        var cr_ray = chief_ray_pkg.chief_ray.ray;
        if (cr_ray == null || cr_ray.size() < 2) return null;
        var cr_exp_seg = chief_ray_pkg.cr_exp_seg;

        int k = -2; // last interface in sequence
        var ray = ray_pkg.ray;

        // eq 3.13: the equally inclined chord distance at the last surface
        var ekp = WaveAbr.eic_distance(
                new RayData(Lists.get(ray, k).p, Lists.get(ray, k).d),
                new RayData(Lists.get(cr_ray, k).p, Lists.get(cr_ray, k).d));

        var after = Transform.transform_after_surface(
                cr_exp_seg.ifc, new RayData(Lists.get(ray, k).p, Lists.get(ray, k).d));

        // Walk back along the ray to the point equally inclined with the chief ray's
        // pupil crossing: the two travel distances then differ by exactly -ekp.
        var dst = ekp - cr_exp_seg.exp_dst;
        var eic_exp_pt = after.pt.minus(after.dir.times(dst));
        return eic_exp_pt.minus(cr_exp_seg.exp_pt);
    }

    /**
     * The image-space spatial frequency a pair of rays actually realises, in cycles per
     * system length unit.
     *
     * <p>Two rays converging on the image form fringes of frequency {@code nu} exactly
     * when the difference of their image-space direction cosines along the fringe
     * direction is {@code lambda*nu/n}. This holds wherever the exit pupil happens to lie,
     * which is what makes it usable without locating the pupil at all - it is the
     * direction-cosine form of Hopkins' reduced spatial frequency (his 10.28).
     *
     * <p>Directions are taken after the last surface, in image-space coordinates, so a
     * tilted or decentred final surface is handled correctly.
     *
     * @param axis {@link Orientation#X} for a sagittal shear, {@link Orientation#Y} for
     *             a tangential one
     * @return the realised frequency, or null if either ray did not reach the image
     */
    public static Double realized_frequency(
            OpticalModel opt_model, RayPkg reference, RayPkg shifted, double wvl, int axis) {
        Orientation.checked(axis);
        var d0 = image_direction(opt_model, reference);
        var d1 = image_direction(opt_model, shifted);
        if (d0 == null || d1 == null) return null;

        double wavelength = opt_model.nm_to_sys_units(wvl);
        if (!(wavelength > 0.0)) return null;
        double n_img = Math.abs(opt_model.optical_spec.parax_data.fod.n_img);

        var delta = d1.minus(d0);
        double component = axis == Orientation.X ? delta.x : delta.y;
        return n_img * Math.abs(component) / wavelength;
    }

    /** Direction cosine of the ray after the last surface, in image-space coordinates. */
    private static Vector3 image_direction(OpticalModel opt_model, RayPkg ray_pkg) {
        if (ray_pkg == null || ray_pkg.ray == null || ray_pkg.ray.size() < 2) return null;
        var ifc = Lists.get(opt_model.seq_model.ifcs, -2);
        var seg = Lists.get(ray_pkg.ray, -2);
        return Transform.transform_after_surface(ifc, new RayData(seg.p, seg.d)).dir;
    }
}
