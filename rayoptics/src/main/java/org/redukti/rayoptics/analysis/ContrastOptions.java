package org.redukti.rayoptics.analysis;

import org.redukti.rayoptics.raytr.TraceOptions;

/** Options for pupil-autocorrelation contrast analysis. */
public class ContrastOptions {
    final double spatialFrequency;
    int numRings = 3;
    Integer numSpokes = 6;
    TraceOptions traceOptions = new TraceOptions();
    boolean calibrateFrequency = false;

    /**
     * @param spatialFrequency image-space spatial frequency in cycles per
     *                         optical-system length unit (normally cycles/mm)
     */
    public ContrastOptions(double spatialFrequency) {
        if (!Double.isFinite(spatialFrequency) || spatialFrequency < 0.0) {
            throw new IllegalArgumentException("Spatial frequency must be finite and non-negative");
        }
        this.spatialFrequency = spatialFrequency;
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
}
