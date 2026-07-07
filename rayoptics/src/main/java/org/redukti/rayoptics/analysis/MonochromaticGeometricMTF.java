package org.redukti.rayoptics.analysis;

/**
 * Input : Traced rays through the system for a given field point and wavelength. x and y intersections with the image plane.
 * <p>
 * 1. Bin these hits into a 2D intensity histogram to build the geometric PSF.
 * 2. Integrate the PSF along the perpendicular axis -> generate LSF for each axis.
 * 3. Compute the 1D Fourier transform for each LSF, then compute magnitude and normalize.
 * 4. Compute the related frequencies.
 */
public class MonochromaticGeometricMTF {

    public final double wvl;
    // 2d histogram
    public final Histogram h2d;
    public final MTF mtf;

    /**
     * Uses the default fixed grid. Prefer {@link #MonochromaticGeometricMTF(SpotIntercepts, Histogram.Config)}
     * with a field-level {@link Histogram.Config} so the window adapts to the spot
     * size and so all wavelengths of a field share one grid (required when the
     * results are combined into a {@link PolyMTF}).
     */
    public MonochromaticGeometricMTF(SpotIntercepts intercepts) {
        this(intercepts, new Histogram.Config(Histogram.DEFAULT_NUM_BINS, Histogram.DEFAULT_PIXEL_SIZE));
    }

    public MonochromaticGeometricMTF(SpotIntercepts intercepts, Histogram.Config cfg) {
        wvl = intercepts.wvl;
        h2d = new Histogram(cfg);
        h2d.accumulate(intercepts,1.0);
        h2d.compute();
        mtf = new MTF(h2d);
    }
}
