package org.redukti.rayoptics.analysis;

import org.redukti.rayoptics.specs.Field;

import java.util.ArrayList;
import java.util.List;

/**
 * Combines mono chromatic MTFs for a field
 */
public class PolyChromaticGeometricMTF extends BaseGeometricMTF {
    public final Field fld;
    public final List<MonochromaticGeometricMTF> mtfs_by_wavelength = new ArrayList<>();

    // fourier transforms
    public double[] fft_x = new double[fft_size * 2];
    public double[] fft_y = new double[fft_size * 2];
    // magnitudes
    public final double[] mag_x = new double[mtf_size];
    public final double[] mag_y = new double[mtf_size];

    public PolyChromaticGeometricMTF(Field fld) {
        this.fld = fld;
    }
    public void add(MonochromaticGeometricMTF mtf) {
        mtfs_by_wavelength.add(mtf);
    }

    double get_weight_for_wvl(double wvl) {
        var fov = fld.fov;
        var osp = fov.optical_spec;
        var wvls = osp.wvls;
        for (int i = 0; i < wvls.wavelengths.length; i++) {
            if (wvls.wavelengths[i] == wvl) {
                double wt = wvls.spectral_wts[i];
                return wt == 0 ? 1.0 : wt;
            }
        }
        throw new IllegalArgumentException("No definition found for wavelength " + wvl);
    }

    /**
     * The FFTs for wavelength are summed up in a weighted sum
     */
    private void compute_combined_mtf(int xy) {
        for (var mono_mtf : mtfs_by_wavelength) {
            double wt = get_weight_for_wvl(mono_mtf.intercepts.wvl);
            double[] fft = xy == 0 ? fft_x : fft_y;
            double[] mono_fft = xy == 0 ? mono_mtf.fft_x : mono_mtf.fft_y;
            for (int i = 0; i < fft_size; i++) {
                fft[2 * i] += wt * mono_fft[2 * i];
                fft[2 * i + 1] += wt * mono_fft[2 * i + 1];
            }
        }
    }

    private void compute_magnitude(int xy) {
        double[] fft = xy == 0 ? fft_x : fft_y;
        double[] mag = xy == 0 ? mag_x : mag_y;
        compute_magnitude(mag, fft);
    }

    public void compute_mtfs() {
        compute_combined_mtf(0);
        compute_combined_mtf(1);
        compute_magnitude(0);
        compute_magnitude(1);
        compute_freq();
    }
}
