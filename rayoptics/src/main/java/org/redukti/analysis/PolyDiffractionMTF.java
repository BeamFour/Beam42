// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.analysis;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.specs.Field;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Polychromatic diffraction MTF of one field: the modulus of the weighted sum of the
 * monochromatic OTFs, divided by the sum of the weights.
 *
 * <p>Every wavelength is referenced to the image point of the reference wavelength's chief
 * ray, so lateral colour appears as a phase difference between the OTFs and lowers the
 * sum. The OTFs are evaluated at the requested frequency, since the FFT bins of each
 * wavelength fall at different frequencies. A wavelength contributes nothing beyond its
 * own cutoff.
 */
public class PolyDiffractionMTF {

    public final Field fld;
    /** the point every wavelength is referenced to */
    public final Vector2 image_pt_2d;
    /** one per wavelength with a non-zero weight, in the order given */
    public final List<DiffractionMTF> mono;
    public final double[] weights;

    /**
     * @param wvls    wavelengths, nm; those with zero weight are skipped
     * @param weights one per wavelength
     * @param n_pupil rays across the pupil; even and at least 4
     * @param maxdim  FFT size; at least {@code 2 * n_pupil}
     */
    public PolyDiffractionMTF(OpticalModel opm, Field fld, double[] wvls, double[] weights,
                              double foc, int n_pupil, int maxdim) {
        if (wvls.length != weights.length)
            throw new IllegalArgumentException("one weight is needed per wavelength");
        this.fld = fld;
        image_pt_2d = DiffractionMTF.image_point(opm, fld, opm.optical_spec.wvls.central_wvl(), foc);
        var mono = new ArrayList<DiffractionMTF>();
        var wts = new ArrayList<Double>();
        for (int i = 0; i < wvls.length; i++) {
            if (weights[i] == 0.0) continue;
            mono.add(new DiffractionMTF(opm, fld, wvls[i], foc, n_pupil, maxdim, image_pt_2d));
            wts.add(weights[i]);
        }
        if (mono.isEmpty())
            throw new IllegalArgumentException("at least one wavelength must have a non-zero weight");
        this.mono = Collections.unmodifiableList(mono);
        this.weights = wts.stream().mapToDouble(Double::doubleValue).toArray();
    }

    /** Sagittal MTF at frequency {@code nu} in cycles per system length unit. */
    public double sagittal(double nu) {
        return along(nu, true);
    }

    /** Tangential MTF at frequency {@code nu} in cycles per system length unit. */
    public double tangential(double nu) {
        return along(nu, false);
    }

    private double along(double nu, boolean x) {
        double re = 0, im = 0, sum = 0;
        for (int i = 0; i < mono.size(); i++) {
            var otf = x ? mono.get(i).sagittal_otf(nu) : mono.get(i).tangential_otf(nu);
            re += weights[i] * otf[0];
            im += weights[i] * otf[1];
            sum += weights[i];
        }
        return Math.hypot(re, im) / sum;
    }
}
