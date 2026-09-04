package org.redukti.rayoptics.analysis;

import org.redukti.rayoptics.util.Orientation;

public class MTF extends BaseMTF {


    // line spreads get padded to twice the size to improve FFT correctness
    public double[] padded_lsf_x;
    public double[] padded_lsf_y;

    public MTF(Histogram h2d) {
        super(h2d.num_bins * 2, h2d.pixel_size);
        padded_lsf_x = new double[fft_size];
        padded_lsf_y = new double[fft_size];
        compute_mtfs(h2d);
    }

    private void pad_lfs(double[] lsf, double[] padded_lsf) {
        // pad the LSF with zeroes
        // This is to ensure FFT computation is correct
        for (int i = 0; i < fft_size / 2; i++)
            padded_lsf[i + fft_size / 4] = lsf[i];
    }

    private void compute_mtf(int xy) {
        double[] lsf = xy == Orientation.SAGITTAL ? padded_lsf_x : padded_lsf_y;
        double[] fft = xy == Orientation.SAGITTAL ? fft_x : fft_y;
        // copy the reals and set imaginary numbers to 0
        for (int i = 0; i < fft_size; i++) {
            fft[2 * i] = lsf[i];    // real
            fft[2 * i + 1] = 0.0;   // imaginary
        }
        compute_fft(xy);
    }

    private void compute_mtfs(Histogram h2d) {
        pad_lfs(h2d.lsf_x,padded_lsf_x);
        pad_lfs(h2d.lsf_y,padded_lsf_y);
        compute_mtf(0);
        compute_mtf(1);
        compute_magnitude(0);
        compute_magnitude(1);
    }
}
