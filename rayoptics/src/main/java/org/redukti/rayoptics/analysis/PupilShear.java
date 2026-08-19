package org.redukti.rayoptics.analysis;

import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.elem.transform.Transform;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.ChiefRayPkg;
import org.redukti.rayoptics.raytr.ExitPupilAiming;
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
 * two non-linear. See H. H. Hopkins,
 * <a href="https://doi.org/10.1364/AO.473823"><i>Calculation of the aberrations and
 * image assessment for a general optical system</i></a>, &sect;5 and &sect;10.
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
        return ExitPupilAiming.chord_coord(ray_pkg, chief_ray_pkg, ref_sphere);
    }

    /**
     * The image-space spatial frequency a pair of rays actually realises, in cycles per
     * system length unit.
     *
     * <p>Two rays converging on the image form fringes of frequency {@code nu} when the
     * difference of their image-space direction cosines along the fringe direction is
     * {@code lambda*nu/n}, and that holds wherever the exit pupil lies.
     *
     * <p><b>This is not Hopkins' OTF frequency.</b> His (10.26) shears the pupil function
     * in reduced exit-pupil coordinates, so the OTF variable is a separation on the
     * exit-pupil reference sphere. A ray's image-space direction is the wavefront normal,
     * so a direction-cosine difference carries that separation <em>plus</em> the
     * difference in wavefront slope between the two pupil points - transverse ray
     * aberration, which is what optimization changes. The two coincide only for an
     * unaberrated system. Measured divergence runs from 0.1 percent on axis to 2.7 percent
     * at full field tangential on the Otus 50/1.4; see REVIEW.md finding 9. Use
     * {@link #exit_pupil_sphere_coord} as the coordinate foundation when the OTF pupil
     * separation is what is wanted.
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

    /**
     * The ray's coordinate on the exit-pupil reference sphere, which is where Hopkins
     * defines the pupil function.
     *
     * <p>{@link #exit_pupil_coord} returns the equally inclined chord point {@code B~'},
     * which lies close to the reference sphere but not on it. Hopkins closes the gap with
     * {@code e'} (his 4.6) and converts in (5.7): {@code B' = B~' + e'.d}. The correction
     * is small in absolute terms - under 4e-3 mm on the JP2018-189733 model - but it is
     * not negligible against the pupil separation of a contrast pair, which is a few
     * tenths of a millimetre, so it is applied here rather than ignored.
     *
     * <p>Note this uses the discriminant of Hopkins (4.6), {@code F^2 - J/R'}, rather than
     * the {@code F^2 + J/R'} carried by {@code WaveAbr.wave_abr_full_calc_finite_pup} and
     * by upstream ray-optics. The two agree to first order in {@code J}; the paper's form
     * places the point on the sphere exactly, which is what this method needs.
     *
     * @return the coordinate relative to the exit pupil centre, or null if unavailable
     */
    public static Vector3 exit_pupil_sphere_coord(
            RayPkg ray_pkg, ChiefRayPkg chief_ray_pkg, ReferenceSphere ref_sphere) {
        return ExitPupilAiming.sphere_coord(ray_pkg, chief_ray_pkg, ref_sphere);
    }

    /** Direction cosine of the ray after the last surface, in image-space coordinates. */
    private static Vector3 image_direction(OpticalModel opt_model, RayPkg ray_pkg) {
        if (ray_pkg == null || ray_pkg.ray == null || ray_pkg.ray.size() < 2) return null;
        var ifc = Lists.get(opt_model.seq_model.ifcs, -2);
        var seg = Lists.get(ray_pkg.ray, -2);
        return Transform.transform_after_surface(ifc, new RayData(seg.p, seg.d)).dir;
    }
}
