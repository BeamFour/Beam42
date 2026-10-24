// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.analysis;

import org.redukti.rayoptics.optical.OpticalModel;

import java.util.List;

public class MTFResultByFreq {
    public int freq;
    // data for above freq across fields
    public double[] sag_mtf_by_field;
    public double[] tan_mtf_by_field;

    public MTFResultByFreq(List<PolyMTF> mtfs_by_field, int freq) {
        this.freq = freq;
        sag_mtf_by_field = new double[mtfs_by_field.size()];
        tan_mtf_by_field = new double[mtfs_by_field.size()];
        for (var fi = 0; fi < mtfs_by_field.size(); fi++) {
            var mtf = mtfs_by_field.get(fi);
            sag_mtf_by_field[fi] = interpolate(mtf.freq, mtf.mag_x, freq);
            tan_mtf_by_field[fi] = interpolate(mtf.freq, mtf.mag_y, freq);
        }
    }

    private MTFResultByFreq(int freq, int num_fields) {
        this.freq = freq;
        sag_mtf_by_field = new double[num_fields];
        tan_mtf_by_field = new double[num_fields];
    }

    /**
     * Diffraction MTF at {@code freq} cycles/mm across fields. The MTFs are in cycles per
     * system length unit, converted here with the model's units.
     */
    public static MTFResultByFreq from_diffraction(OpticalModel opm, List<PolyDiffractionMTF> mtfs_by_field, int freq) {
        var result = new MTFResultByFreq(freq, mtfs_by_field.size());
        double nu = freq / opm.nm_to_sys_units(1.0e6);
        for (var fi = 0; fi < mtfs_by_field.size(); fi++) {
            var mtf = mtfs_by_field.get(fi);
            result.sag_mtf_by_field[fi] = mtf.sagittal(nu);
            result.tan_mtf_by_field[fi] = mtf.tangential(nu);
        }
        return result;
    }

    /**
     * Linearly interpolate an MTF magnitude at the requested spatial frequency.
     * The frequency axis is monotonically increasing and uniformly spaced at
     * 1/(fft_size*pixel_size), so the requested frequency almost never lands
     * exactly on a bin; interpolating avoids the ~0.5-1 cycle/mm error of picking
     * the nearest bin. Frequencies outside the sampled range are clamped to the
     * endpoints.
     */
    static double interpolate(double[] freq, double[] mag, double f) {
        int n = freq.length;
        if (n == 0)
            throw new IllegalArgumentException("empty MTF");
        if (f <= freq[0])
            return mag[0];
        if (f >= freq[n - 1])
            return mag[n - 1];
        // binary search for the bracketing interval [lo, lo+1] with freq[lo] <= f < freq[lo+1]
        int lo = 0, hi = n - 1;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (freq[mid] <= f)
                lo = mid;
            else
                hi = mid;
        }
        double t = (f - freq[lo]) / (freq[lo + 1] - freq[lo]);
        return mag[lo] + t * (mag[lo + 1] - mag[lo]);
    }
}
