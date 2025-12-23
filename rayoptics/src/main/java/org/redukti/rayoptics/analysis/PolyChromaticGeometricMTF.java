package org.redukti.rayoptics.analysis;

/**
 * Combines mono chromatic MTFs for a field
 */
public class PolyChromaticGeometricMTF {

    public final Histogram h2d;
    public MTF mtf;

    public PolyChromaticGeometricMTF() {
        h2d = new Histogram(512,0.001);
    }

    public void add(SpotIntercepts intercepts, double wt) {
        h2d.accumulate(intercepts,wt);
    }

    public void compute() {
        h2d.compute();
        mtf = new MTF(h2d);
    }
}
