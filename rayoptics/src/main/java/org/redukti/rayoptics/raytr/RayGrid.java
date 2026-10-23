// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.specs.Field;

/**
 * Container for a square grid of rays.
 *
 *     Attributes:
 *         opt_model: :class:`~.OpticalModel` instance
 *         f: index into :class:`~.FieldSpec` or a :class:`~.Field` instance
 *         wl: wavelength (nm) to trace the fan, or central wavelength if None
 *         foc: focus shift to apply to the results
 *         image_pt_2d: base image point. if None, the chief ray is used
 *         image_delta: image offset to apply to image_pt_2d
 *         num_rays: number of samples along the side of the grid
 */
public class RayGrid {
    public final OpticalModel opt_model;
    public final Field fld;
    public final double wvl;
    public final double foc;
    public final Vector2 image_pt_2d;
    public final Vector2 image_delta;
    public final int num_rays;
    public final double value_if_none;
    public final double oversize;
    /** output_filter, rayerr_filter and check_apertures (clip_rays) */
    public final TraceOptions rt_kwargs;

    public Analyses.WavefrontGridPkg grid_pkg;
    /** [3][num_rays][num_rays]: pupil x, pupil y and OPD in waves */
    public double[][][] grid;

    public RayGrid(OpticalModel opt_model, int f, Double wl, Double foc, Vector2 image_pt_2d,
                   Vector2 image_delta, String output_filter, String rayerr_filter,
                   int num_rays, boolean clip_rays, double value_if_none, double oversize) {
        this(opt_model, opt_model.optical_spec.fov.fields[f], wl, foc, image_pt_2d, image_delta,
                output_filter, rayerr_filter, num_rays, clip_rays, value_if_none, oversize);
    }

    public RayGrid(OpticalModel opt_model, Field f, Double wl, Double foc, Vector2 image_pt_2d,
                   Vector2 image_delta, String output_filter, String rayerr_filter,
                   int num_rays, boolean clip_rays, double value_if_none, double oversize) {
        this.opt_model = opt_model;
        var osp = opt_model.optical_spec;
        this.fld = f;
        this.wvl = wl == null ? osp.wvls.central_wvl() : wl;

        this.foc = foc == null ? osp.defocus().focus_shift : foc;
        this.image_pt_2d = image_pt_2d;
        this.image_delta = image_delta;

        this.num_rays = num_rays;
        this.value_if_none = value_if_none;

        this.oversize = oversize;
        this.rt_kwargs = new TraceOptions();
        this.rt_kwargs.output_filter = output_filter;
        this.rt_kwargs.rayerr_filter = rayerr_filter;
        this.rt_kwargs.check_apertures = clip_rays;

        update_data(true);
    }

    /** The upstream defaults: field 0, central wavelength, nominal focus, 21 rays, clipped. */
    public RayGrid(OpticalModel opt_model) {
        this(opt_model, 0, null, null, null, null, null, null, 21, true, Double.NaN, 1.);
    }

    /**
     * @param rebuild true for upstream build='rebuild': retrace the grid before refocusing
     */
    public RayGrid update_data(boolean rebuild) {
        if (rebuild) {
            grid_pkg = Analyses.trace_wavefront(
                    opt_model, fld, wvl, foc,
                    image_pt_2d, image_delta,
                    num_rays, oversize,
                    rt_kwargs);
        }

        var opd = Analyses.focus_wavefront(opt_model, grid_pkg,
                fld, wvl, foc,
                image_pt_2d, image_delta,
                value_if_none);

        // np.rollaxis(opd, 2)
        var rolled = new double[3][opd.length][];
        for (int k = 0; k < 3; k++) {
            for (int i = 0; i < opd.length; i++) {
                rolled[k][i] = new double[opd[i].length];
                for (int j = 0; j < opd[i].length; j++)
                    rolled[k][i][j] = opd[i][j][k];
            }
        }
        grid = rolled;

        return this;
    }

    /**
     * Calculate the input and output grid spacings.
     *
     * @param maxdim The total width of the sampling grid
     * @return delta_x: The linear grid spacing on the entrance pupil,
     *         delta_xp: The linear grid spacing on the image plane
     */
    public double[] calc_psf_scaling(int maxdim) {
        return Analyses.calc_psf_scaling(opt_model, fld, wvl, num_rays, maxdim);
    }
}
