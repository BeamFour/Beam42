package org.redukti.rayoptics.analysis;

import org.redukti.rayoptics.raytr.TraceOptions;

/** Options for chief-ray-referenced polychromatic RMS wavefront analysis. */
public class PolychromaticRMSWavefrontOptions {
    int numRings = 14;
    Integer numSpokes = 20;
    double innerPupilRadius = 0.0;
    TraceOptions traceOptions = new TraceOptions();

    public PolychromaticRMSWavefrontOptions() {
        // Gaussian quadrature integrates the vignetting-factor pupil. Deleting rays
        // clipped by surface apertures invalidates its circular/elliptical-pupil weights.
        traceOptions.check_apertures = false;
    }

    public PolychromaticRMSWavefrontOptions num_rings(int value) {
        if (value < 1) throw new IllegalArgumentException("Number of rings must be at least 1");
        numRings = value;
        return this;
    }

    public PolychromaticRMSWavefrontOptions num_spokes(Integer value) {
        if (value != null && value < 3)
            throw new IllegalArgumentException("Number of spokes must be at least 3");
        numSpokes = value;
        return this;
    }

    public PolychromaticRMSWavefrontOptions inner_pupil_radius(double value) {
        if (!Double.isFinite(value) || value < 0.0 || value >= 1.0)
            throw new IllegalArgumentException("Inner pupil radius must be finite and in [0, 1)");
        innerPupilRadius = value;
        return this;
    }

    public PolychromaticRMSWavefrontOptions trace_options(TraceOptions value) {
        if (value == null) throw new IllegalArgumentException("Trace options cannot be null");
        traceOptions = value.copy();
        return this;
    }

    public PolychromaticRMSWavefrontOptions check_apertures(boolean value) {
        traceOptions.check_apertures = value;
        return this;
    }
}
