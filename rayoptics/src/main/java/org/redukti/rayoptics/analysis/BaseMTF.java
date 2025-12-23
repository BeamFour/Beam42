package org.redukti.rayoptics.analysis;

import org.redukti.mathlib.fftpack.ComplexDoubleFFT;

public class BaseMTF {
    public final double pixel_size;
    /**
     * The FFT is calculated on LSF padded to twice the size
     * of the measurements - this is apparently required for FFT correctness.
     */
    public final int fft_size;
    /**
     * The MTF uses only the non-negative bins
     */
    public final int mtf_size;
    // frequencies
    public final double[] freq;
    // fourier transforms - complex numbers so has real and imaginary pairs
    public double[] fft_x;
    public double[] fft_y;
    // magnitudes - computed for the positive half
    public final double[] mag_x;
    public final double[] mag_y;

    protected BaseMTF(int fft_size, double pixel_size) {
        this.fft_size = fft_size;
        this.pixel_size = pixel_size;
        mtf_size = fft_size / 2 + 1;
        freq = new double[mtf_size];
        // fourier transforms - complex numbers so has real and imaginary pairs
        fft_x = new double[fft_size * 2];
        fft_y = new double[fft_size * 2];
        // magnitudes - computed for the positive half
        mag_x = new double[mtf_size];
        mag_y = new double[mtf_size];
        compute_freq();
    }
    protected void compute_freq() {
        for (int i = 0; i < freq.length; i++)
            freq[i] = i / (fft_size * pixel_size);
    }
    protected void compute_fft(int xy) {
        double[] fft = xy == 0 ? fft_x : fft_y;
//        var fft2d = new DoubleFFT_1D(fft_size);
//        fft2d.complexForward(fft);
        var fft2d = new ComplexDoubleFFT(fft_size);
        fft2d.ft(fft);
    }
    protected static void compute_magnitude(double[] mag, double[] fft) {
        // Only positive frequencies 0 … N/2
        for (int i = 0; i < mag.length; i++)
            mag[i] = Math.hypot(fft[2 * i], fft[2 * i + 1]);
        // normalize
        double dc = mag[0];
        for (int k = 0; k < mag.length; k++) {
            mag[k] = mag[k] / dc;
        }
    }
    protected void compute_magnitude(int xy) {
        double[] fft = xy == 0 ? fft_x : fft_y;
        double[] mag = xy == 0 ? mag_x : mag_y;
        compute_magnitude(mag, fft);
    }
}
