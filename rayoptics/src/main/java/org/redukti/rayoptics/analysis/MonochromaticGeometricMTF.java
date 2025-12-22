package org.redukti.rayoptics.analysis;

import org.jtransforms.fft.DoubleFFT_1D;

/**
 * Input : Traced rays through the system for a given field point and wavelength. x and y intersections with the image plane.
 * <p>
 * 1. Bin these hits into a 2D intensity histogram to build the geometric PSF.
 * 2. Integrate the PSF along the perpendicular axis -> generate LSF for each axis.
 * 3. Compute the 1D Fourier transform for each LSF, then compute magnitude and normalize.
 * 4. Compute the related frequencies.
 */
public class MonochromaticGeometricMTF extends BaseGeometricMTF {

    public final SpotIntercepts intercepts;
    public final double hmin, hmax;
    // 2d histogram
    public double[][] h2d = new double[num_bins][num_bins];
    // line spreads
    public double[] lsf_x = new double[num_bins];
    public double[] lsf_y = new double[num_bins];
    // line spreads get padded to twice the size to improve FFT correctness
    public double[] padded_lsf_x = new double[fft_size];
    public double[] padded_lsf_y = new double[fft_size];
    // fourier transforms - complex numbers so has real and imaginary pairs
    public double[] fft_x = new double[fft_size * 2];
    public double[] fft_y = new double[fft_size * 2];
    // magnitudes - computed for the positive half
    public final double[] mag_x = new double[mtf_size];
    public final double[] mag_y = new double[mtf_size];

    public MonochromaticGeometricMTF(SpotIntercepts intercepts) {
        this.intercepts = intercepts;
        var width = pixel_size * num_bins;
        hmin = -width / 2;
        hmax = width / 2;
        build_2d_histogram();
        build_lsfs();
        compute_mtfs();
        compute_freq();
    }

    public void discard_work_arrays() {
        h2d = null;
        lsf_x = null;
        lsf_y = null;
        padded_lsf_y = null;
        padded_lsf_x = null;
        fft_x = null;
        fft_y = null;
    }

    private void build_2d_histogram() {
        for (int i = 0; i < intercepts.x.length; i++) {
            var x = intercepts.x[i];
            var y = intercepts.y[i];
            if (x < hmin || x > hmax
                    || y < hmin || y > hmax)
                continue;
            int ix = (int) Math.floor(num_bins * (x - hmin) / (hmax - hmin));
            int iy = (int) Math.floor(num_bins * (y - hmin) / (hmax - hmin));
            if (ix < 0 || ix >= num_bins || iy < 0 || iy >= num_bins)
                continue;
            // ideally we should assign a weight here
            // For now we assign 1 for each hit
            h2d[ix][iy] += 1.0;
        }
        normalize_histogram();
    }

    private void normalize_histogram() {
        double sum = 0.0;
        for (int i = 0; i < num_bins; i++)
            for (int j = 0; j < num_bins; j++)
                sum += h2d[i][j];

        for (int i = 0; i < num_bins; i++)
            for (int j = 0; j < num_bins; j++)
                h2d[i][j] /= sum;
    }

    private void build_lsf(int xy) {
        double[] lsf = xy == 0 ? lsf_x : lsf_y;
        double[] padded_lsf = xy == 0 ? padded_lsf_x : padded_lsf_y;
        if (xy == 0) {
            // integrate over y → LSF(x)
            for (int i = 0; i < num_bins; i++) {
                double s = 0;
                for (int j = 0; j < num_bins; j++) {
                    s += h2d[i][j];
                }
                lsf[i] = s;
            }
        } else {
            // integrate over x → LSF(y)
            for (int j = 0; j < num_bins; j++) {
                double s = 0;
                for (int i = 0; i < num_bins; i++) {
                    s += h2d[i][j];
                }
                lsf[j] = s;
            }
        }
        // normalize lsf
        double lsfSum = 0;
        for (double v : lsf)
            lsfSum += v;
        for (int i = 0; i < num_bins; i++)
            lsf[i] /= lsfSum;
        // pad the LSF with zeroes
        // This is to ensure FFT computation is correct
        for (int i = 0; i < num_bins; i++)
            padded_lsf[i + num_bins/2] = lsf[i];
    }

    private void build_lsfs() {
        build_lsf(0);   // x
        build_lsf(1);   // y
    }

    private void compute_mtf(int xy) {
        double[] lsf = xy == 0 ? padded_lsf_x : padded_lsf_y;
        double[] fft = xy == 0 ? fft_x : fft_y;
        // copy the reals and set imaginary numbers to 0
        for (int i = 0; i < fft_size; i++) {
            fft[2 * i] = lsf[i];    // real
            fft[2 * i + 1] = 0.0;   // imaginary
        }
        var fft2d = new DoubleFFT_1D(fft_size);
        fft2d.complexForward(fft);
    }

    private void compute_magnitude(int xy) {
        double[] fft = xy == 0 ? fft_x : fft_y;
        double[] mag = xy == 0 ? mag_x : mag_y;
        compute_magnitude(mag, fft);
    }

    private void compute_mtfs() {
        compute_mtf(0);
        compute_mtf(1);
        compute_magnitude(0);
        compute_magnitude(1);
    }
}
