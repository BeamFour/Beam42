package org.redukti.rayoptics.analysis;

/**
 * Combines monochromatic MTFs for a field
 */
public class PolyMTF extends BaseMTF {

    public PolyMTF(int fft_size, double pixel_size) {
        super(fft_size,pixel_size);
    }
    private void add(MTF mono_mtf, int xy, double wt) {
        double[] fft = xy == 0 ? fft_x : fft_y;
        double[] mono_fft = xy == 0 ? mono_mtf.fft_x : mono_mtf.fft_y;
        for (int i = 0; i < fft_size; i++) {
            fft[2 * i] += wt * mono_fft[2 * i];
            fft[2 * i + 1] += wt * mono_fft[2 * i + 1];
        }
    }
    public void add(MTF mono_mtf, double wt) {
        add(mono_mtf,0,wt);
        add(mono_mtf,1,wt);
    }
    public void compute() {
        compute_magnitude(0);
        compute_magnitude(1);
    }
}
