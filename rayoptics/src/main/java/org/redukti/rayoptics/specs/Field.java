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
     */
    public double[] aim_info;
    /**
     * Populated for wide-angle system
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

    public double[] apply_vignetting(double[] pupil) {
        double[] vig_pupil = Arrays.copyOf(pupil, pupil.length);
        if (pupil[0] < 0.0) {
            if (vlx != 0.0) {
                vig_pupil[0] *= (1.0 - vlx);
            }
        }
        else {
            if (vux != 0.0) {
                vig_pupil[0] *= (1.0 - vux);
            }
        }
        if (pupil[1] < 0.0) {
            if (vly != 0.0) {
                vig_pupil[1] *= (1.0 - vly);
            }
        }
        else {
            if (vuy != 0.0) {
                vig_pupil[1] *= (1.0 - vuy);
            }
        }
        return vig_pupil;
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
            case "x"-> sb.append(String.format("x =%7.3f (%5.2f) vlx=%6.3f vux=%6.3f vly=%6.3f vuy=%6.3f\n",
                        xv(), xf(), vlx, vux, vly, vuy));
            case "y"-> sb.append(String.format("y =%7.3f (%5.2f) vlx=%6.3f vux=%6.3f vly=%6.3f vuy=%6.3f\n",
                    yv(), yf(), vlx, vux, vly, vuy));
            case ""-> sb.append(String.format("x,y=%5.2f vlx=%6.3f vux=%6.3f vly=%6.3f vuy=%6.3f\n",
                    yv(), vlx, vux, vly, vuy));
            default -> sb.append(String.format("xy=(%7.3f, %7.3f) (%5.2f, %5.2f) vlx=%6.3f vux=%6.3f vly=%6.3f vuy=%6.3f\n",
                    xv(), yv(), xf(), yf(), vlx, vux, vly, vuy));
        }
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
