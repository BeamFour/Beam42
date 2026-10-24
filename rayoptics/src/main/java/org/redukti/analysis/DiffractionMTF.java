// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.analysis;

import org.redukti.mathlib.FFTUtil;
import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.Analyses;
import org.redukti.rayoptics.raytr.RayGrid;
import org.redukti.rayoptics.raytr.Trace;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.util.Lists;

/**
 * Monochromatic diffraction MTF of one field, from the Fourier transform of the PSF.
 *
 * <p>The wavefront is sampled on a {@link RayGrid} of {@code n_pupil} rays across the
 * vignetted pupil, its PSF computed by {@link Analyses#calc_psf} on a {@code maxdim} grid,
 * and the MTF is the modulus of the Fourier transform of the PSF, normalized to 1 at zero
 * frequency. Sagittal is the frequency along x, tangential along y.
 *
 * <p>Frequency scale. The grid spans the pupil in {@code n_pupil - 1} intervals in each
 * direction, so a shift of {@code k} intervals is {@code k / (n_pupil - 1)} of the pupil
 * diameter and corresponds to that fraction of the cutoff frequency. The cutoff is taken
 * per direction from the real rim rays: {@code 2 n NA / lambda}, where {@code NA} is half
 * the difference of the image-space direction cosines of the two rim rays in that
 * direction. Off axis and for anamorphic systems the two cutoffs differ. The grid is
 * uniform in the entrance pupil, so pupil aberration that distorts the exit pupil
 * non-uniformly is not accounted for.
 *
 * <p>The FFT gives the MTF only at multiples of {@code nu_cutoff / (n_pupil - 1)}.
 * {@link #sagittal_otf} and {@link #tangential_otf} evaluate the complex OTF at any
 * frequency from the line spread, with the phase relative to the reference image point.
 *
 * <p>The approach, and the analytic reference {@link #diffraction_limited}, follow
 * M. J. Hayford's "Diffraction MTF" notebook for ray-optics issue #182, in
 * https://github.com/mjhoptics/ray-optics-notebooks. This is an independent implementation.
 */
public class DiffractionMTF {

    public final Field fld;
    public final double wvl;
    public final double foc;
    public final int n_pupil;
    public final int maxdim;
    /** reference image point of the wavefront and the OTF phase; null for this wavelength's chief ray */
    public final Vector2 image_pt_2d;
    /** cycles per system length unit */
    public final double nu_cutoff_x;
    public final double nu_cutoff_y;
    /** maxdim x maxdim, first index along x, zero frequency at [maxdim/2][maxdim/2] */
    public final double[][] mtf;
    /** line spreads along x and y, unit sum, the reference image point at maxdim/2 */
    private final double[] lsf_x;
    private final double[] lsf_y;

    /**
     * @param n_pupil rays across the pupil; even and at least 4
     * @param maxdim  FFT size; at least {@code 2 * n_pupil} so the MTF does not wrap
     */
    public DiffractionMTF(OpticalModel opm, Field fld, double wvl, double foc, int n_pupil, int maxdim) {
        this(opm, fld, wvl, foc, n_pupil, maxdim, null);
    }

    /**
     * @param n_pupil     rays across the pupil; even and at least 4
     * @param maxdim      FFT size; at least {@code 2 * n_pupil} so the MTF does not wrap
     * @param image_pt_2d image point the wavefront is referenced to, x and y in the image
     *                    plane; null for this wavelength's chief ray. Pass the same point,
     *                    e.g. {@link #image_point} of the reference wavelength, for every
     *                    wavelength of a field so that their OTF phases include lateral colour.
     */
    public DiffractionMTF(OpticalModel opm, Field fld, double wvl, double foc, int n_pupil, int maxdim,
                          Vector2 image_pt_2d) {
        if (n_pupil < 4 || n_pupil % 2 != 0)
            throw new IllegalArgumentException("n_pupil must be even and at least 4");
        if (maxdim < 2 * n_pupil)
            throw new IllegalArgumentException("maxdim must be at least 2 * n_pupil");
        this.fld = fld;
        this.wvl = wvl;
        this.foc = foc;
        this.n_pupil = n_pupil;
        this.maxdim = maxdim;
        this.image_pt_2d = image_pt_2d;

        double[] cutoffs = cutoff_frequencies(opm, fld, wvl);
        nu_cutoff_x = cutoffs[0];
        nu_cutoff_y = cutoffs[1];

        var grid = new RayGrid(opm, fld, wvl, foc, image_pt_2d, null, null, null,
                n_pupil, true, Double.NaN, 1.0);
        var psf = Analyses.calc_psf(grid.grid[2], n_pupil, maxdim);
        mtf = modulus_of_transform(psf);
        lsf_x = line_spread(psf, true);
        lsf_y = line_spread(psf, false);
    }

    /**
     * Cutoff frequency along x and y, {@code 2 n NA / lambda}, from the rim rays.
     */
    public static double[] cutoff_frequencies(OpticalModel opm, Field fld, double wvl) {
        var rim_rays = Trace.trace_boundary_rays_at_field(opm, fld, wvl, new TraceOptions());
        // default pupil rays: centre, +x, -x, +y, -y
        var pupil_rays = opm.optical_spec.pupil.pupil_rays;
        int xp = -1, xm = -1, yp = -1, ym = -1;
        for (int i = 0; i < pupil_rays.length; i++) {
            var p = pupil_rays[i];
            if (p[0] == 1.0 && p[1] == 0.0) xp = i;
            else if (p[0] == -1.0 && p[1] == 0.0) xm = i;
            else if (p[0] == 0.0 && p[1] == 1.0) yp = i;
            else if (p[0] == 0.0 && p[1] == -1.0) ym = i;
        }
        if (xp < 0 || xm < 0 || yp < 0 || ym < 0)
            throw new IllegalArgumentException("pupil rays must include the four rim rays at +-1 in x and y");
        for (int i : new int[]{xp, xm, yp, ym})
            if (rim_rays.get(i) == null)
                throw new IllegalArgumentException("a rim ray failed to trace");
        double na_x = 0.5 * Math.abs(image_direction(rim_rays.get(xp)).x - image_direction(rim_rays.get(xm)).x);
        double na_y = 0.5 * Math.abs(image_direction(rim_rays.get(yp)).y - image_direction(rim_rays.get(ym)).y);
        double n_img = Math.abs(opm.optical_spec.parax_data.fod.n_img);
        double wl = opm.nm_to_sys_units(wvl);
        return new double[]{2 * n_img * na_x / wl, 2 * n_img * na_y / wl};
    }

    /**
     * Where the chief ray of {@code wvl} meets the image plane shifted by {@code foc}: the
     * point {@link RayGrid} references the wavefront to when given no image point.
     */
    public static Vector2 image_point(OpticalModel opm, Field fld, double wvl, double foc) {
        var last = Lists.get(Trace.get_chief_ray_pkg(opm, fld, wvl, foc).chief_ray.ray, -1);
        var p = last.p.plus(last.d.times(foc / last.d.z));
        return new Vector2(p.x, p.y);
    }

    private static org.redukti.mathlib.Vector3 image_direction(org.redukti.rayoptics.raytr.RayPkg pkg) {
        return Lists.get(pkg.ray, -2).d;
    }

    /** MTF of an aberration-free circular pupil; 0 at and beyond the cutoff. */
    public static double diffraction_limited(double nu, double nu_cutoff) {
        double cos_phi = Math.abs(nu) / nu_cutoff;
        if (cos_phi >= 1.0) return 0.0;
        double phi = Math.acos(cos_phi);
        return (2 / Math.PI) * (phi - Math.sin(phi) * cos_phi);
    }

    /** Sagittal MTF at frequency {@code nu} in cycles per system length unit. */
    public double sagittal(double nu) {
        return along(nu, nu_cutoff_x, true);
    }

    /** Tangential MTF at frequency {@code nu} in cycles per system length unit. */
    public double tangential(double nu) {
        return along(nu, nu_cutoff_y, false);
    }

    /** MTF of the aberration-free pupil with the same cutoff, along x. */
    public double sagittal_diffraction_limited(double nu) {
        return diffraction_limited(nu, nu_cutoff_x);
    }

    /** MTF of the aberration-free pupil with the same cutoff, along y. */
    public double tangential_diffraction_limited(double nu) {
        return diffraction_limited(nu, nu_cutoff_y);
    }

    /**
     * Sagittal OTF at frequency {@code nu} in cycles per system length unit, as {re, im}.
     * Evaluated at {@code nu} exactly; see {@link #otf}.
     */
    public double[] sagittal_otf(double nu) {
        return otf(lsf_x, nu, nu_cutoff_x, n_pupil);
    }

    /**
     * Tangential OTF at frequency {@code nu} in cycles per system length unit, as {re, im}.
     * Evaluated at {@code nu} exactly; see {@link #otf}.
     */
    public double[] tangential_otf(double nu) {
        return otf(lsf_y, nu, nu_cutoff_y, n_pupil);
    }

    /**
     * OTF at frequency {@code nu}, as {re, im}: the Fourier transform of the line spread,
     * {@code sum lsf[i] exp(-2 pi i nu x_i)}, with {@code x_i} measured from the reference
     * image point at {@code lsf.length / 2}. The phase is relative to that point.
     *
     * <p>The PSF of {@link Analyses#calc_psf} is mirrored with respect to the image plane:
     * index {@code i} lies at {@code x_i = -(i - lsf.length / 2) dx}. With the sign taken
     * here, a PSF displaced by {@code d} in the image has its phase changed by
     * {@code -2 pi nu d}.
     *
     * <p>The FFT of the PSF has bins {@code nu_cutoff / (n_pupil - 1)} apart, so the PSF
     * samples are {@code (n_pupil - 1) / (maxdim nu_cutoff)} apart. At a bin frequency the
     * modulus equals the FFT value; between bins this is the band-limited interpolation of
     * the bins. 0 at and beyond the cutoff.
     *
     * @param lsf unit-sum line spread of maxdim samples
     */
    static double[] otf(double[] lsf, double nu, double nu_cutoff, int n_pupil) {
        if (Math.abs(nu) >= nu_cutoff) return new double[]{0.0, 0.0};
        int maxdim = lsf.length;
        int centre = maxdim / 2;
        double dx = (n_pupil - 1) / (maxdim * nu_cutoff);
        double re = 0, im = 0;
        for (int i = 0; i < maxdim; i++) {
            double angle = 2 * Math.PI * nu * (i - centre) * dx;
            re += lsf[i] * Math.cos(angle);
            im += lsf[i] * Math.sin(angle);
        }
        return new double[]{re, im};
    }

    /** The PSF integrated across y (along x) or across x (along y), normalized to unit sum. */
    static double[] line_spread(double[][] psf, boolean x) {
        int n = psf.length;
        var lsf = new double[n];
        double sum = 0;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                lsf[x ? i : j] += psf[i][j];
                sum += psf[i][j];
            }
        for (int i = 0; i < n; i++)
            lsf[i] /= sum;
        return lsf;
    }

    /** Linear interpolation along the central row or column, which holds k = 0 at maxdim/2. */
    private double along(double nu, double nu_cutoff, boolean x) {
        double k = Math.abs(nu) * (n_pupil - 1) / nu_cutoff;
        if (k >= n_pupil - 1) return 0.0;
        int centre = maxdim / 2;
        int lo = (int) Math.floor(k);
        double t = k - lo;
        double a = x ? mtf[centre + lo][centre] : mtf[centre][centre + lo];
        double b = x ? mtf[centre + lo + 1][centre] : mtf[centre][centre + lo + 1];
        return a + t * (b - a);
    }

    /**
     * |FFT(psf)| with zero frequency at the centre, normalized to 1 there. The PSF is real,
     * so the modulus is symmetric and the sign convention of the transform does not matter.
     */
    static double[][] modulus_of_transform(double[][] psf) {
        int n = psf.length;
        int shift = n / 2;
        // the PSF as interleaved complex rows
        var x = new double[n][2 * n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                x[i][2 * j] = psf[i][j];
        var F = FFTUtil.fftshift(FFTUtil.fft2(FFTUtil.fftshift(x)));
        // take the modulus, normalize to the zero frequency
        var mtf = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                mtf[i][j] = Math.hypot(F[i][2 * j], F[i][2 * j + 1]);
        double dc = mtf[shift][shift];
        if (!(dc > 0) || !Double.isFinite(dc))
            throw new IllegalArgumentException("PSF must have finite, positive total intensity");
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                mtf[i][j] /= dc;
        return mtf;
    }
}
