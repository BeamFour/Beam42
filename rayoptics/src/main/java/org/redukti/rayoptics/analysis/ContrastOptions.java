package org.redukti.rayoptics.analysis;

import org.redukti.rayoptics.raytr.TraceOptions;

/** Options for pupil-autocorrelation contrast analysis. */
public class ContrastOptions {
    final double spatialFrequency;
    int numRings = 3;
    Integer numSpokes = 6;
    TraceOptions traceOptions = new TraceOptions();

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
}
