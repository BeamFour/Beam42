package org.redukti.rayoptics.analysis;

/**
 * Combines mono chromatic MTFs for a field
 */
public class PolyChromaticGeometricMTF {

    public final Histogram h2d;
    public MTF mtf;

    public PolyChromaticGeometricMTF() {
        this(new Histogram.Config(Histogram.DEFAULT_NUM_BINS, Histogram.DEFAULT_PIXEL_SIZE));
    }

    public PolyChromaticGeometricMTF(Histogram.Config cfg) {
        h2d = new Histogram(cfg);
    }

    public void add(SpotIntercepts intercepts, double wt) {
        h2d.accumulate(intercepts,wt);
    }

    public void compute() {
        h2d.compute();
        mtf = new MTF(h2d);
    }
}
