package org.redukti.rayoptics.analysis;

import org.redukti.rayoptics.raytr.TraceOptions;

/** Options for pupil-autocorrelation contrast analysis. */
public class ContrastOptions {
    final double spatialFrequency;
    int numRings = 3;
    Integer numSpokes = 6;
    TraceOptions traceOptions = new TraceOptions();
    boolean calibrateFrequency = false;
    boolean aimExitPupil = false;
    boolean centerResiduals = false;

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
     * Inverse-aim every displaced contrast ray so its separation from the reference ray
     * is the requested vector on the exit-pupil reference sphere.
     *
     * <p>This is the physically direct alternative to block calibration. It retains the
     * entrance-pupil quadrature for the reference rays, but does not approximate the
     * partner ray with a rigid entrance-pupil displacement. It is unavailable for afocal
     * systems and cannot be combined with {@link #calibrate_frequency(boolean)}.
     */
    public ContrastOptions aim_exit_pupil(boolean value) {
        aimExitPupil = value;
        return this;
    }
}
