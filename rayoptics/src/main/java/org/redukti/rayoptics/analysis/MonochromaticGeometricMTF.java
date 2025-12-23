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

    public MonochromaticGeometricMTF(SpotIntercepts intercepts) {
        wvl = intercepts.wvl;
        h2d = new Histogram(512,0.001);
        h2d.accumulate(intercepts,1.0);
        h2d.compute();
        mtf = new MTF(h2d);
    }
}
