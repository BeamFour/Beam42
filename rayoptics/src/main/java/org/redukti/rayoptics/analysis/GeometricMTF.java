package org.redukti.rayoptics.analysis;

import org.jtransforms.fft.DoubleFFT_1D;

public class GeometricMTF {

    public final SpotIntercepts intercepts;
    public final int nbins = 512;
    public final int mtf_size = 512/2+1;
    public final double pixel_size = 0.001;
    public final double hmin, hmax;
    public final double[][] h2d = new double[nbins][nbins];
    public final double[] lxf_x = new double[nbins];
    public final double[] lsf_y = new double[nbins];
    public final double[] fft_x = new double[nbins*2];
    public final double[] fft_y = new double[nbins*2];
    public final double[] mag_x = new double[mtf_size];
    public final double[] mag_y = new double[mtf_size];
    public final double[] freq = new double[mtf_size];

    public GeometricMTF(SpotIntercepts intercepts) {
        this.intercepts = intercepts;
        var width = pixel_size * nbins;
        hmin = -width / 2;
        hmax = width / 2;
        build_histogram();
        normalize();
        build_lsfs();
        compute_ffts();
        compute_freq();
    }

    private void build_histogram() {
        for (int i = 0; i < intercepts.x.length; i++) {
            var x = intercepts.x[i];
            var y = intercepts.y[i];
            if (x < hmin || x > hmax
                || y < hmin || y > hmax)
                continue;
            int ix = (int) Math.floor(nbins*(x-hmin)/(hmax-hmin));
            int iy = (int) Math.floor(nbins*(y-hmin)/(hmax-hmin));
            if (ix < 0 || ix >= nbins || iy < 0 || iy >= nbins)
                continue;
            h2d[ix][iy]++;
        }
    }
    private void normalize() {
        double sum = 0.0;
        for (int i = 0; i < nbins; i++)
            for (int j = 0; j < nbins; j++)
                sum += h2d[i][j];

        for (int i = 0; i < nbins; i++)
            for (int j = 0; j < nbins; j++)
                h2d[i][j] /= sum;
    }
    private void build_lsf(int xy) {
        double[] lsf = xy == 0 ? lxf_x : lsf_y;
        if (xy == 0) {
            for (int i = 0; i < nbins; i++) {
                double s = 0;
                for (int j = 0; j < nbins; j++) {
                    s += h2d[i][j];
                }
                lsf[i] = s;
            }
        }
        else {
            for (int j = 0; j < nbins; j++) {
                double s = 0;
                for (int i = 0; i < nbins; i++) {
                    s += h2d[i][j];
                }
                lsf[j] = s;
            }
        }
        // normalize lsf
        double lsfSum = 0;
        for (double v : lsf)
            lsfSum += v;
        for (int i = 0; i < nbins; i++)
            lsf[i] /= lsfSum;
    }
    private void build_lsfs() {
        build_lsf(0);   // x
        build_lsf(1);   // y
    }
    private void compute_fft(int xy) {
        double[] lsf = xy == 0 ? lxf_x : lsf_y;
        double[] fft = xy == 0 ? fft_x : fft_y;
        double[] mag = xy == 0 ? mag_x : mag_y;
        for (int i=0; i<nbins; i++)
        {
            fft[2*i] = lsf[i];
            fft[2*i+1] = 0.0;
        }
        var fft2d = new DoubleFFT_1D(nbins);
        fft2d.complexForward(fft);
        // Only positive frequencies 0 … N/2
        for (int i=0; i<mag.length; i++)
            mag[i] = Math.hypot(fft[2*i],fft[2*i+1]);
        // normalize
        double dc = mag[0];
        for (int k = 0; k < mag.length; k++) {
            mag[k] = mag[k] / dc;
        }
    }
    private void compute_ffts() {
        compute_fft(0);
        compute_fft(1);
    }
    private void compute_freq() {
        for (int i = 0; i < freq.length; i++)
            freq[i] = i / (nbins * pixel_size);
    }
}
