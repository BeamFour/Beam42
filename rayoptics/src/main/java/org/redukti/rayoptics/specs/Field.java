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
}
