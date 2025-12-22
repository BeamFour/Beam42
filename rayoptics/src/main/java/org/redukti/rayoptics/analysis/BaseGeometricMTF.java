package org.redukti.rayoptics.analysis;

public class BaseGeometricMTF {
    /**
     * Defines the size of the grid used as histogram
     */
    public final int num_bins = 512;
    /**
     * The FFT is calculated on LSF padded to twice the size
     * of the measurements - this is apparently required for FFT correctness.
     */
    public final int fft_size = num_bins * 2;
    /**
     * The MTF uses only the non-negative bins
     */
    public final int mtf_size = fft_size / 2 + 1;
    public final double pixel_size = 0.001; // also dx, TODO this should be derived from dimension units? We assume lens dimension is in mm

    // frequencies
    public final double[] freq = new double[mtf_size];

    public static void compute_magnitude(double[] mag, double[] fft) {
        // Only positive frequencies 0 … N/2
        for (int i = 0; i < mag.length; i++)
            mag[i] = Math.hypot(fft[2 * i], fft[2 * i + 1]);
        // normalize
        double dc = mag[0];
        for (int k = 0; k < mag.length; k++) {
            mag[k] = mag[k] / dc;
        }
    }

    public void compute_freq() {
        for (int i = 0; i < freq.length; i++)
            freq[i] = i / (fft_size * pixel_size);
    }

}
