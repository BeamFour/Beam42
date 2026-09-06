// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.specs;

import org.redukti.rayoptics.raytr.ChiefRayPkg;
import org.redukti.rayoptics.raytr.RayPkg;
import org.redukti.rayoptics.raytr.ReferenceSphere;

import java.util.Arrays;
import java.util.Map;

/**
 * a single field point, chief ray pkg and pupil limits
 *
 *     The Field class manages several types of data:
 *
 *     - the field coordinates, unscaled and fractional
 *     - aim info for tracing through the stop surface
 *     - the vignetting factors for the pupil definition
 *     - pkgs for the chief ray and reference sphere
 *
 *     The Field can have a reference to a fov/FieldSpec (recommended!) which is used to support the fractional and value interfaces simultaneously. If
 *     no fov is given, a max_field may be specified, with the default being unit field size.
 *
 *     Attributes:
 *         vux: +x vignetting factor
 *         vuy: +y vignetting factor
 *         vlx: -x vignetting factor
 *         vly: -y vignetting factor
 *         wt: field weight
 *         aim_info: x, y chief ray coords on the paraxial entrance pupil plane,
 *                   or z_enp for wide angle fovs
 *         chief_ray: ray package for the ray from the field point throught the
 *                    center of the aperture stop, traced in the central
 *                    wavelength
 *         ref_sphere: a tuple containing (image_pt, ref_dir, ref_sphere_radius)
 *         fov: :class:`~.FieldSpec` to be used as reference or None
 */
public class Field {
    public double x; // x field component
    public double y; // y field component
    public double vux; // +x vignetting factor
    public double vuy; // +y vignetting factor
    public double vlx; // -x vignetting factor
    public double vly; // -y vignetting factor
    public double wt; //  field weight
    /**
     * Populated for non wide-angle system
     * x, y chief ray coords on the paraxial entrance pupil plane
     * When this is populated z_enp should be null.
     */
    public double[] aim_info;
    /**
     * The z center of the real pupil for `fld`, wrt 1st ifc
     * Populated for wide-angle system
     * When this is populated aim_info should be null;
     */
    public Double z_enp;
    /**
     * ray package for the ray from the field point through the
     * center of the aperture stop, traced in the central wavelength
     */
    public ChiefRayPkg chief_ray;
    /**
     * a tuple containing (image_pt, ref_dir, ref_sphere_radius)
     */
    public ReferenceSphere ref_sphere;
    public Map<String, RayPkg> pupil_rays;

    public final FieldSpec fov;

    public Field(FieldSpec fov) {
        this.fov = fov;
    }

    public void update() {
        aim_info = null;
        z_enp = null;
        chief_ray = null;
        ref_sphere = null;
    }

    public void apply_scale_factor(double scale_factor) {
        x *= scale_factor;
        y *= scale_factor;
    }

    /**
     * Scale relative pupil coordinates by this field's vignetting factors,
     * returning a new array. The argument is not modified.
     * <p>
     * This differs from upstream, deliberately. Upstream writes
     * {@code vig_pupil = pupil[:]}, which for a numpy array is a view rather
     * than a copy, so scaling {@code vig_pupil} also scales the caller's array
     * in place. Callers that read their pupil array back after tracing see the
     * vignetted value there: {@code analyses.trace_ray_fan} records the pupil
     * after calling {@code trace_safe} and so reports vignetted fan
     * coordinates, where this implementation reports the nominal ones.
     * <p>
     * The rays traced are the same either way - only what a caller observes in
     * its own array differs - but it is visible when comparing fan data against
     * upstream, where a first ray at nominal -1.0 shows up there as
     * -1 * (1 - vlx). See tools/src/main/python/README.md.
     *
     * @param pupil relative pupil coordinates, unmodified by this call
     * @return a new array with the vignetting factors applied
     */
    public double[] apply_vignetting(double[] pupil) {
        return apply_vignetting(pupil, VignettingMapping.Piecewise);
    }

    /** Map a normalized coordinate using either the upstream or translated-ellipse model. */
    public double[] apply_vignetting(double[] pupil, VignettingMapping mapping) {
        double[] vig_pupil = Arrays.copyOf(pupil, pupil.length);
        if (mapping == VignettingMapping.AffineEllipse) {
            vig_pupil[0] = vignetting_offset(vlx, vux)
                    + pupil[0] * affine_vignetting_scale(vlx, vux);
            vig_pupil[1] = vignetting_offset(vly, vuy)
                    + pupil[1] * affine_vignetting_scale(vly, vuy);
            return vig_pupil;
        }
        vig_pupil[0] *= vignetting_scale_x(pupil[0]);
        vig_pupil[1] *= vignetting_scale_y(pupil[1]);
        return vig_pupil;
    }

    public static double affine_vignetting_scale(double lower, double upper) {
        return 1.0 - 0.5 * (lower + upper);
    }

    public static double vignetting_offset(double lower, double upper) {
        return 0.5 * (lower - upper);
    }

    /**
     * Factor by which {@link #apply_vignetting} scales an x pupil coordinate.
     * The upper and lower factors differ, so the scale depends on the sign of
     * the coordinate and the map has a kink at the axis.
     */
    public double vignetting_scale_x(double x) {
        return vignetting_scale(x, vlx, vux);
    }

    /** Factor by which {@link #apply_vignetting} scales a y pupil coordinate. */
    public double vignetting_scale_y(double y) {
        return vignetting_scale(y, vly, vuy);
    }

    private static double vignetting_scale(double coordinate, double lower, double upper) {
        double factor = coordinate < 0.0 ? lower : upper;
        return factor == 0.0 ? 1.0 : 1.0 - factor;
    }

    /**
     * Resets vignetting values to 0.
     */
    public void clear_vignetting() {
        vux = vuy = vlx = vly = 0.;
    }

    @Override
    public String toString() {
        return "Field(x=" + x + ", y=" + y + ")";
    }

    public void list_str(StringBuilder sb, String fmtstr) {
        switch (fmtstr) {
            case "x"-> sb.append(String.format("x =%7.3f (%5.2f) vlx=%6.3f vux=%6.3f vly=%6.3f vuy=%6.3f",
                        xv(), xf(), vlx, vux, vly, vuy));
            case "y"-> sb.append(String.format("y =%7.3f (%5.2f) vlx=%6.3f vux=%6.3f vly=%6.3f vuy=%6.3f",
                    yv(), yf(), vlx, vux, vly, vuy));
            case ""-> sb.append(String.format("x,y=%5.2f vlx=%6.3f vux=%6.3f vly=%6.3f vuy=%6.3f",
                    yv(), vlx, vux, vly, vuy));
            default -> sb.append(String.format("xy=(%7.3f, %7.3f) (%5.2f, %5.2f) vlx=%6.3f vux=%6.3f vly=%6.3f vuy=%6.3f",
                    xv(), yv(), xf(), yf(), vlx, vux, vly, vuy));
        }
        if (aim_info != null)
            sb.append(" aim_info: [").append(aim_info[0]).append(",").append(aim_info[1]).append("]");
        if (z_enp != null)
            sb.append(" z_enp: ").append(z_enp);
        sb.append("\n");
    }

    public boolean is_relative() {
        if (fov != null)
            return fov.is_relative;
        return false;
    }

    /**
     * the maximum field value used for the fractional field calculation.
     */
    public double max_field() {
        if (fov != null)
            return fov.value;
        // omitted local max_field
        return 1.;
    }

    public double xv() {
        if (is_relative())
            return _get_x_by_fref();
        return x;
    }

    public double _get_x_by_fref() {
        return x * max_field();
    }

    public double _get_y_by_fref() {
        return y * max_field();
    }

    public double xf() {
        if (is_relative())
            return x;
        return _get_x_by_vref();
    }

    private double _get_x_by_vref() {
        return x / max_field();
    }

    private double _get_y_by_vref() {
        return y / max_field();
    }

    public double yf() {
        if (is_relative())
            return _get_y_by_fref();
        return y;
    }

    public double yv() {
        if (is_relative())
            return y;
        return _get_y_by_vref();
    }
}
