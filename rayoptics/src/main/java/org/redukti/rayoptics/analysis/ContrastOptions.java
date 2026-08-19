package org.redukti.rayoptics.analysis;

import org.redukti.rayoptics.raytr.TraceOptions;

/** Options for pupil-autocorrelation contrast analysis. */
public class ContrastOptions {
    final double spatialFrequency;
    int numRings = 3;
    Integer numSpokes = 6;
    TraceOptions traceOptions = new TraceOptions();
    boolean calibrateFrequency = false;
    boolean centerResiduals = false;
    boolean measureFrequency = false;
    boolean normalizeFrequency = false;

    /**
     * @param spatialFrequency image-space spatial frequency in cycles per
     *                         optical-system length unit (normally cycles/mm)
     */
    public ContrastOptions(double spatialFrequency) {
        if (!Double.isFinite(spatialFrequency) || spatialFrequency < 0.0) {
            throw new IllegalArgumentException("Spatial frequency must be finite and non-negative");
        }
        this.spatialFrequency = spatialFrequency;
        // Contrast samples already occupy the common vignetted-pupil overlap.
        // Do not additionally reject them against surface apertures by default.
        this.traceOptions.check_apertures = false;
    }

    public ContrastOptions num_rings(int value) {
        if (value < 1) throw new IllegalArgumentException("Number of rings must be at least 1");
        numRings = value;
        return this;
    }

    public ContrastOptions num_spokes(Integer value) {
        if (value != null && value < 1) throw new IllegalArgumentException("Number of spokes must be at least 1");
        numSpokes = value;
        return this;
    }

    public ContrastOptions trace_options(TraceOptions value) {
        if (value == null) throw new IllegalArgumentException("Trace options cannot be null");
        traceOptions = value;
        return this;
    }

    /**
     * Subtract the constant part of each wavefront-difference block, so the residuals
     * carry the variance the OTF modulus depends on rather than the un-centred second
     * moment. Off by default because it changes every contrast residual. See
     * {@link ContrastAnalysis#center_residuals(ContrastAnalysisResult, int)}.
     */
    public ContrastOptions center_residuals(boolean value) {
        centerResiduals = value;
        return this;
    }

    /** Whether traced contrast rays are also rejected by surface apertures. */
    public ContrastOptions check_apertures(boolean value) {
        traceOptions.check_apertures = value;
        return this;
    }

    /**
     * Correct the pupil shift so the sampled pair realises the requested spatial
     * frequency in image space.
     *
     * <p>The shift is applied in entrance-pupil coordinates but is derived from an
     * exit-pupil relation, so pupil aberration makes the realised frequency fall short -
     * measured around 8% low at full field on an f/2 lens, and worsening with field.
     * Enabling this measures the shortfall with one probe pair per field, wavelength and
     * direction, and scales the shift to compensate. It costs four extra rays per field
     * and wavelength.
     *
     * <p>Off by default: it changes the sampled frequency and therefore every contrast
     * residual, so it is opt-in until the correct exit-pupil treatment is settled.
     */
    public ContrastOptions calibrate_frequency(boolean value) {
        calibrateFrequency = value;
        return this;
    }

    /**
     * Record, for every sample, the exit-pupil coordinate of its reference ray and the
     * spatial frequency its two pairs actually realised.
     *
     * <p>Diagnostic only: it populates {@link ContrastAnalysisResult.Shear} and changes no
     * residual. Where {@link #calibrate_frequency(boolean)} infers a single scale from one
     * probe pair per field, wavelength and direction, this measures every pair directly,
     * so it shows the variation across the pupil that a single scale cannot represent.
     *
     * <p>Costs no extra rays - both quantities come from rays the samples already trace.
     */
    public ContrastOptions measure_frequency(boolean value) {
        measureFrequency = value;
        return this;
    }

    /**
     * Rescale each wavefront difference to the frequency that was requested, using that
     * sample's own realised frequency.
     *
     * <p>This is the per-sample alternative to {@link #calibrate_frequency(boolean)}.
     * Rather than adjusting the entrance-pupil shift so the pair lands on the right
     * frequency - which is exact only on average, because the entrance-to-exit pupil
     * mapping is non-linear - it accepts whatever frequency the pair realised and
     * corrects the wavefront difference for it:
     *
     * <pre>dW &lt;- dW * (nu_requested / nu_realized)</pre>
     *
     * <p>To first order in the shear this is exact, since {@code dW ~ s.dW/ds}; the
     * residual error is second order in the frequency discrepancy, which is a few percent.
     * The two mechanisms compose: calibration removes the bulk field-dependent bias and
     * leaves a smaller discrepancy for this to correct, which keeps the rescaling well
     * inside its first-order regime.
     *
     * <p>Implies {@link #measure_frequency(boolean)}. Off by default: like calibration it
     * changes every contrast residual.
     */
    public ContrastOptions normalize_frequency(boolean value) {
        normalizeFrequency = value;
        if (value) measureFrequency = true;
        return this;
    }
}
