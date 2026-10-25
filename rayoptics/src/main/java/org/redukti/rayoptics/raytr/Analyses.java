// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.specs.Field;

import java.util.ArrayList;
import java.util.List;

import static org.redukti.mathlib.FFTUtil.fft2;
import static org.redukti.mathlib.FFTUtil.fftshift;

/**
 * Aberration calculations for (fld, wvl, foc), including focus and image shift.
 *
 * <p>Port of the rectilinear grid and PSF functions of upstream raytr/analyses.py.
 * The ray grid is traced once by {@link #trace_wavefront}, and {@link #focus_wavefront}
 * evaluates its OPD for a given focus and image point. {@link #calc_psf} computes the
 * monochromatic PSF of the resulting wavefront.
 *
 * <p>Not ported: update_psf_data, which reads a maxdim attribute that upstream's RayGrid
 * does not have; the plotting code that supplies it passes maxdim to calc_psf directly.
 */
public class Analyses {

    /** Ray traced and focus-independent OPD data for a grid, as returned by {@link #trace_wavefront}. */
    public record WavefrontGridPkg(List<List<GridItem>> grid, List<List<PreOpdPkg>> upd_grid) {
    }

    /**
     * Trace a grid of rays at fld and wvl and return ray_pkgs in 2d list.
     *
     * <p>Upstream defaults apply_vignetting to False unless the caller passes it; every
     * upstream caller relies on the default, and TraceOptions cannot express "unset", so it
     * is always off here. The grid itself is laid over the vignetted pupil.
     */
    public static List<List<GridItem>> trace_ray_grid(OpticalModel opt_model, TraceGridDef grid_rng, Field fld, double wvl, double foc,
                                                      boolean append_if_none, TraceOptions trace_options) {
        trace_options = trace_options.copy();
        trace_options.apply_vignetting = false;
        var start = grid_rng.grid_start;
        var stop = grid_rng.grid_stop;
        var num = grid_rng.num_rays;
        var step = (stop.minus(start)).divide(num - 1);
        List<List<GridItem>> grid = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            List<GridItem> grid_row = new ArrayList<>();

            for (int j = 0; j < num; j++) {
                var pupil = start;
                var ray_result = Trace.trace_safe(opt_model, pupil, fld, wvl, trace_options);
                var ray_pkg = ray_result.pkg;
                if (ray_pkg != null) {
                    grid_row.add(new GridItem(pupil, ray_pkg));
                }
                else {  // ray outside pupil or failed
                    if (append_if_none)
                        grid_row.add(new GridItem(pupil, null));
                }

                start = new Vector2(start.x, start.y + step.y);
            }

            grid.add(grid_row);
            start = new Vector2(start.x + step.x, grid_rng.grid_start.y);
        }

        return grid;
    }

    /**
     * Trace a grid of rays and evaluate the OPD across the wavefront.
     *
     * <p>Upstream defaults check_apertures to True; here it is taken from
     * {@code trace_options} as given.
     *
     * @return [num_rays][num_rays][3] of pupil x, pupil y and OPD in waves
     */
    public static double[][][] eval_wavefront(OpticalModel opt_model, Field fld, double wvl, double foc,
                                              Vector2 image_pt_2d, Vector2 image_delta, int num_rays,
                                              double value_if_none, double oversize, TraceOptions trace_options) {
        var fod = opt_model.optical_spec.parax_data.fod;
        var rs = Trace.setup_pupil_coords(opt_model, fld, wvl, foc, image_pt_2d, image_delta);
        var ref_sphere = rs.ref_sphere;
        var cr_pkg = rs.chief_ray_pkg;
        fld.chief_ray = cr_pkg;
        fld.ref_sphere = ref_sphere;

        var vig_bbox = fld.vignetting_bbox(opt_model.optical_spec.pupil, oversize);
        var vig_grid_def = new TraceGridDef(new Vector2(vig_bbox[0][0], vig_bbox[0][1]),
                new Vector2(vig_bbox[1][0], vig_bbox[1][1]), num_rays);

        var grid = trace_ray_grid(opt_model, vig_grid_def, fld, wvl, foc, true, trace_options);

        double convert_to_opd = 1 / opt_model.nm_to_sys_units(wvl);

        var opd_grid = new double[grid.size()][][];
        for (int i = 0; i < grid.size(); i++) {
            var row = grid.get(i);
            opd_grid[i] = new double[row.size()][];
            for (int j = 0; j < row.size(); j++) {
                var gij = row.get(j);
                if (gij.ray_pkg != null) {
                    double opdelta = WaveAbr.wave_abr_full_calc(fod, fld, wvl, foc, gij.ray_pkg,
                            cr_pkg, ref_sphere);
                    double opd = convert_to_opd * opdelta;
                    opd_grid[i][j] = new double[]{gij.pupil.x, gij.pupil.y, opd};
                }
                else {
                    opd_grid[i][j] = new double[]{gij.pupil.x, gij.pupil.y, value_if_none};
                }
            }
        }

        return opd_grid;
    }

    /**
     * Trace a grid of rays and pre-calculate data needed for rapid refocus.
     *
     * <p>Upstream defaults check_apertures to True; here it is taken from
     * {@code trace_options} as given.
     */
    public static WavefrontGridPkg trace_wavefront(OpticalModel opt_model, Field fld, double wvl, double foc,
                                                   Vector2 image_pt_2d, Vector2 image_delta, int num_rays,
                                                   double oversize, TraceOptions trace_options) {
        var fod = opt_model.optical_spec.parax_data.fod;
        var rs = Trace.setup_pupil_coords(opt_model, fld, wvl, foc, image_pt_2d, image_delta);
        var ref_sphere = rs.ref_sphere;
        var cr_pkg = rs.chief_ray_pkg;
        fld.chief_ray = cr_pkg;
        fld.ref_sphere = ref_sphere;

        var vig_bbox = fld.vignetting_bbox(opt_model.optical_spec.pupil, oversize);
        var vig_grid_def = new TraceGridDef(new Vector2(vig_bbox[0][0], vig_bbox[0][1]),
                new Vector2(vig_bbox[1][0], vig_bbox[1][1]), num_rays);

        var grid = trace_ray_grid(opt_model, vig_grid_def, fld, wvl, foc, true, trace_options);

        List<List<PreOpdPkg>> upd_grid = new ArrayList<>();
        for (var i : grid) {
            List<PreOpdPkg> row = new ArrayList<>();
            for (var gij : i) {
                if (gij.ray_pkg != null) {
                    var pre_opd_pkg = WaveAbr.wave_abr_pre_calc(fod, fld, wvl, foc,
                            gij.ray_pkg, cr_pkg, ref_sphere);
                    row.add(pre_opd_pkg);
                }
                else {
                    row.add(null);
                }
            }
            upd_grid.add(row);
        }

        return new WavefrontGridPkg(grid, upd_grid);
    }

    /**
     * Given pre-traced rays and a ref. sphere, return the ray's OPD.
     *
     * @return [num_rays][num_rays][3] of pupil x, pupil y and OPD in waves
     */
    public static double[][][] focus_wavefront(OpticalModel opt_model, WavefrontGridPkg grid_pkg, Field fld, double wvl, double foc,
                                               Vector2 image_pt_2d, Vector2 image_delta, double value_if_none) {
        var fod = opt_model.optical_spec.parax_data.fod;
        var grid = grid_pkg.grid();
        var upd_grid = grid_pkg.upd_grid();
        var rs = Trace.setup_pupil_coords(opt_model, fld, wvl, foc, image_pt_2d, image_delta);
        var ref_sphere = rs.ref_sphere;
        var cr_pkg = rs.chief_ray_pkg;
        double convert_to_opd = 1 / opt_model.nm_to_sys_units(wvl);

        var refocused_grid = new double[grid.size()][][];
        for (int i = 0; i < grid.size(); i++) {
            var ig = grid.get(i);
            var iu = upd_grid.get(i);
            refocused_grid[i] = new double[ig.size()][];
            for (int j = 0; j < ig.size(); j++) {
                var gij = ig.get(j);
                var uij = iu.get(j);
                if (gij.ray_pkg != null) {
                    double opdelta = WaveAbr.wave_abr_calc(fod, fld, wvl, foc,
                            gij.ray_pkg, cr_pkg, uij, ref_sphere);
                    double opd = convert_to_opd * opdelta;
                    refocused_grid[i][j] = new double[]{gij.pupil.x, gij.pupil.y, opd};
                }
                else {
                    refocused_grid[i][j] = new double[]{gij.pupil.x, gij.pupil.y, value_if_none};
                }
            }
        }

        return refocused_grid;
    }

    // --- PSF calculation

    /**
     * Given 2 of 3 parameters, calculate the third.
     *
     * @param n       The total width of the sampling grid
     * @param n_pupil The sampling across the pupil
     * @param n_airy  The sampling across the central peak of the Airy disk
     * @return (n, n_pupil, n_airy)
     */
    public static int[] psf_sampling(Integer n, Integer n_pupil, Integer n_airy) {
        // Python's round() rounds half to even, as Math.rint does
        if (n == null) {
            n = (int) Math.rint((n_pupil * n_airy) / 2.44);
        }
        else if (n_pupil == null) {
            n_pupil = (int) Math.rint(2.44 * n / n_airy);
        }
        else if (n_airy == null) {
            n_airy = (int) Math.rint(2.44 * n / n_pupil);
        }
        else {
            // upstream: npa.index(None) raises ValueError
            throw new IllegalArgumentException("one of n, n_pupil and n_airy must be null");
        }
        return new int[]{n, n_pupil, n_airy};
    }

    /**
     * Calculate the input and output grid spacings.
     *
     * @param ndim   The sampling across the wavefront
     * @param maxdim The total width of the sampling grid
     * @return delta_x: The linear grid spacing on the entrance pupil,
     *         delta_xp: The linear grid spacing on the image plane
     */
    public static double[] calc_psf_scaling(OpticalModel opt_model, Field fld, double wvl,
                                            int ndim, int maxdim) {
        var fod = opt_model.optical_spec.parax_data.fod;
        double wl = opt_model.nm_to_sys_units(wvl);

        double fill_factor = (double) ndim / maxdim;
        double max_D = 2 * fod.enp_radius / fill_factor;
        double delta_x = max_D / maxdim;
        double C = wl / fod.exp_radius;

        double delta_theta = (fill_factor * C) / 2;
        double ref_sphere_radius = fld.ref_sphere.ref_sphere_radius;
        double delta_xp = delta_theta * ref_sphere_radius;

        return new double[]{delta_x, delta_xp};
    }

    /**
     * Calculate the point spread function of wavefront W.
     * Unlike upstream, the pupil mask is kept separate from phase so zero OPD
     * remains illuminated. Non-finite samples and padding are excluded.
     *
     * @param wavefront ndim x ndim array of wavefront errors. No data
     *                  condition is indicated by nan
     * @param ndim      The sampling across the wavefront
     * @param maxdim    The total width of the sampling grid
     * @return AP, the PSF of the input wavefront
     * @throws IllegalArgumentException if the PSF has no finite positive peak
     */
    public static double[][] calc_psf(double[][] wavefront, int ndim, int maxdim) {
        int maxdim_by_2 = maxdim / 2;
        var W = new double[maxdim][maxdim];
        var pupil = new boolean[maxdim][maxdim];
        int nd2 = ndim / 2;
        int lo = maxdim_by_2 - (nd2 - 1);
        int hi = maxdim_by_2 + (nd2 + 1);
        // numpy raises here when the slice and the wavefront differ in shape, which
        // happens for every odd ndim
        if (lo < 0 || hi > maxdim || hi - lo != wavefront.length)
            throw new IllegalArgumentException("could not broadcast input array from shape ("
                    + wavefront.length + "," + wavefront.length + ") into shape ("
                    + (Math.min(hi, maxdim) - Math.max(lo, 0)) + "," + (Math.min(hi, maxdim) - Math.max(lo, 0)) + ")");
        for (int i = lo; i < hi; i++) {
            if (wavefront[i - lo].length != hi - lo)
                throw new IllegalArgumentException("wavefront must be square");
            for (int j = lo; j < hi; j++) {
                W[i][j] = nan_to_num(wavefront[i - lo][j - lo]);
                pupil[i][j] = Double.isFinite(wavefront[i - lo][j - lo]);
            }
        }

        // Upstream
        // phase = np.exp(1j*2*np.pi*W), as interleaved real and imaginary parts by row
        // We use Euler's formula: exp(i*2*pi*W) = cos(2*pi*W) + i*sin(2*pi*W).
        // Store each complex value as adjacent real and imaginary components.
        // Masked samples remain 0+0i; valid samples with zero OPD become 1+0i.
        var phase = new double[maxdim][2 * maxdim];
        for (int i = 0; i < maxdim; i++) {
            for (int j = 0; j < maxdim; j++) {
                if (!pupil[i][j]) continue;
                double angle = 2 * Math.PI * W[i][j];
                phase[i][2 * j] = Math.cos(angle);
                phase[i][2 * j + 1] = Math.sin(angle);
            }
        }

        // Upstream
        // AP = abs(fftshift(fft2(fftshift(phase))))**2
        // AP_max = np.nanmax(AP)
        var F = fftshift(fft2(fftshift(phase)));
        var AP = new double[maxdim][maxdim];
        double AP_max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < maxdim; i++) {
            for (int j = 0; j < maxdim; j++) {
                double a = Math.hypot(F[i][2 * j], F[i][2 * j + 1]);
                AP[i][j] = a * a;
                if (!Double.isNaN(AP[i][j]))
                    AP_max = Math.max(AP_max, AP[i][j]);
            }
        }
        if (!(AP_max > 0) || !Double.isFinite(AP_max))
            throw new IllegalArgumentException("wavefront must produce a finite, nonzero PSF");
        // Upstream
        // AP = AP/AP_max
        for (int i = 0; i < maxdim; i++)
            for (int j = 0; j < maxdim; j++)
                AP[i][j] = AP[i][j] / AP_max;
        return AP;
    }

    /** numpy.nan_to_num for a real value. */
    private static double nan_to_num(double x) {
        if (Double.isNaN(x)) return 0.0;
        if (x == Double.POSITIVE_INFINITY) return Double.MAX_VALUE;
        if (x == Double.NEGATIVE_INFINITY) return -Double.MAX_VALUE;
        return x;
    }
}
