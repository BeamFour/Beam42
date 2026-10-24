package org.redukti.analysis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolyDiffractionMTFTest {

    private static final int N = 64;

    @Test
    void oneWavelengthIsItsMonochromaticMtf() throws Exception {
        var opm = DiffractionMTFTest.polychromatic_model();
        var fld = opm.optical_spec.fov.fields[1];
        double ref = opm.optical_spec.wvls.central_wvl();
        var mono = new DiffractionMTF(opm, fld, ref, 0.0, N, 2 * N);
        // the weight only scales the sum, which is divided by it
        var poly = new PolyDiffractionMTF(opm, fld, new double[]{ref, 500.0}, new double[]{2.5, 0.0}, 0.0, N, 2 * N);
        assertEquals(1, poly.mono.size());
        for (double nu : new double[]{0, 10, 40, 80}) {
            var s = mono.sagittal_otf(nu);
            var t = mono.tangential_otf(nu);
            assertEquals(Math.hypot(s[0], s[1]), poly.sagittal(nu), 1e-12);
            assertEquals(Math.hypot(t[0], t[1]), poly.tangential(nu), 1e-12);
        }
    }

    @Test
    void lateralColourLowersThePolychromaticMtf() throws Exception {
        var opm = DiffractionMTFTest.polychromatic_model();
        var wvls = opm.optical_spec.wvls;
        var fld = opm.optical_spec.fov.fields[1];
        var poly = new PolyDiffractionMTF(opm, fld, wvls.wavelengths, wvls.spectral_wts, 0.0, N, 2 * N);
        assertEquals(wvls.wavelengths.length, poly.mono.size());
        assertEquals(1.0, poly.tangential(0.0), 1e-12);
        // |sum w OTF| / sum w never exceeds the weighted mean of the moduli; with 5 microns of
        // lateral colour in F it falls clearly below it in the tangential direction
        double sum = 0;
        for (double w : poly.weights) sum += w;
        for (double nu : new double[]{10, 20, 40, 80}) {
            double mean_s = 0, mean_t = 0;
            for (int i = 0; i < poly.mono.size(); i++) {
                var s = poly.mono.get(i).sagittal_otf(nu);
                var t = poly.mono.get(i).tangential_otf(nu);
                mean_s += poly.weights[i] * Math.hypot(s[0], s[1]) / sum;
                mean_t += poly.weights[i] * Math.hypot(t[0], t[1]) / sum;
            }
            assertTrue(poly.sagittal(nu) <= mean_s + 1e-12);
            assertTrue(poly.tangential(nu) <= mean_t + 1e-12);
            if (nu >= 20)
                assertTrue(poly.tangential(nu) < mean_t - 1e-3,
                        nu + ": " + poly.tangential(nu) + " vs " + mean_t);
        }
    }

    @Test
    void resultsByFrequencyAreInCyclesPerMillimetre() throws Exception {
        var opm = DiffractionMTFTest.polychromatic_model();
        var wvls = opm.optical_spec.wvls;
        var fields = opm.optical_spec.fov.fields;
        var mtfs = List.of(
                new PolyDiffractionMTF(opm, fields[0], wvls.wavelengths, wvls.spectral_wts, 0.0, N, 2 * N),
                new PolyDiffractionMTF(opm, fields[1], wvls.wavelengths, wvls.spectral_wts, 0.0, N, 2 * N));
        var result = MTFResultByFreq.from_diffraction(opm, mtfs, 40);
        assertEquals(40, result.freq);
        for (int fi = 0; fi < 2; fi++) {
            assertEquals(mtfs.get(fi).sagittal(40.0), result.sag_mtf_by_field[fi], 1e-15);
            assertEquals(mtfs.get(fi).tangential(40.0), result.tan_mtf_by_field[fi], 1e-15);
        }
    }

    @Test
    void rejectsWeightsThatDoNotMatchOrAreAllZero() throws Exception {
        var opm = DiffractionMTFTest.polychromatic_model();
        var fld = opm.optical_spec.fov.fields[0];
        double ref = opm.optical_spec.wvls.central_wvl();
        assertThrows(IllegalArgumentException.class,
                () -> new PolyDiffractionMTF(opm, fld, new double[]{ref}, new double[]{1.0, 1.0}, 0.0, N, 2 * N));
        assertThrows(IllegalArgumentException.class,
                () -> new PolyDiffractionMTF(opm, fld, new double[]{ref}, new double[]{0.0}, 0.0, N, 2 * N));
    }
}
