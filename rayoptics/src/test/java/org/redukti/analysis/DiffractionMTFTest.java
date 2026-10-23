package org.redukti.analysis;

import org.junit.jupiter.api.Test;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.Analyses;
import org.redukti.rayoptics.raytr.RayGrid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiffractionMTFTest {

    private static OpticalModel model() throws Exception {
        var build = Class.forName("org.redukti.rayoptics.upstream.US004123144Example08PUpstreamTest")
                .getDeclaredMethod("build");
        build.setAccessible(true);
        return (OpticalModel) build.invoke(null);
    }

    @Test
    void diffractionLimitedMtfOfACircularPupil() {
        assertEquals(1.0, DiffractionMTF.diffraction_limited(0.0, 100.0), 1e-15);
        // phi = acos(0.5): (2/pi)(pi/3 - sqrt(3)/4)
        assertEquals((2 / Math.PI) * (Math.PI / 3 - Math.sqrt(3) / 4),
                DiffractionMTF.diffraction_limited(50.0, 100.0), 1e-15);
        assertEquals(0.0, DiffractionMTF.diffraction_limited(100.0, 100.0));
        assertEquals(0.0, DiffractionMTF.diffraction_limited(150.0, 100.0));
    }

    @Test
    void transformOfAnAberrationFreePupilIsTheDiffractionLimit() {
        int n = 128;
        var W = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                double x = -1 + 2.0 * i / (n - 1), y = -1 + 2.0 * j / (n - 1);
                W[i][j] = x * x + y * y <= 1 ? 0.0 : Double.NaN;
            }
        var mtf = DiffractionMTF.modulus_of_transform(Analyses.calc_psf(W, n, 2 * n));
        int c = n;
        for (int k = 0; k < n; k++) {
            double expected = DiffractionMTF.diffraction_limited((double) k / (n - 1), 1.0);
            assertEquals(expected, mtf[c + k][c], 2e-3);
            assertEquals(expected, mtf[c][c + k], 2e-3);
        }
    }

    @Test
    void mtfIsTheOverlapIntegralOfThePupilFunction() throws Exception {
        // On axis the FFT must agree with a direct sum of exp(i 2 pi dW) over the overlap of
        // the pupil and its copy shifted by k grid intervals, normalized by the overlap size.
        var opm = model();
        var fld = opm.optical_spec.fov.fields[0];
        double wvl = opm.optical_spec.wvls.central_wvl();
        int n = 128;
        var mtf = new DiffractionMTF(opm, fld, wvl, 0.0, n, 2 * n);
        var W = new RayGrid(opm, fld, wvl, 0.0, null, null, null, null, n, true, Double.NaN, 1.0).grid[2];
        for (int k : new int[]{5, 20, 40}) {
            double re = 0, im = 0, count = 0;
            for (int i = 0; i + k < n; i++)
                for (int j = 0; j < n; j++) {
                    if (Double.isNaN(W[i][j]) || Double.isNaN(W[i + k][j])) continue;
                    double phase = 2 * Math.PI * (W[i + k][j] - W[i][j]);
                    re += Math.cos(phase);
                    im += Math.sin(phase);
                    count++;
                }
            double nu = k * mtf.nu_cutoff_x / (n - 1);
            assertEquals(Math.hypot(re, im) / count,
                    mtf.sagittal(nu) / mtf.sagittal_diffraction_limited(nu), 5e-3);
        }
    }

    @Test
    void onAxisTheCutoffIsParaxialAndTheMeridiansAgree() throws Exception {
        var opm = model();
        var fld = opm.optical_spec.fov.fields[0];
        double wvl = opm.optical_spec.wvls.central_wvl();
        var mtf = new DiffractionMTF(opm, fld, wvl, 0.0, 64, 128);
        var fod = opm.optical_spec.parax_data.fod;
        double paraxial = 1 / (opm.nm_to_sys_units(wvl) * Math.abs(fod.fno));
        assertEquals(paraxial, mtf.nu_cutoff_x, paraxial * 5e-3);
        assertEquals(mtf.nu_cutoff_x, mtf.nu_cutoff_y, mtf.nu_cutoff_x * 1e-12);
        assertEquals(1.0, mtf.sagittal(0.0), 1e-12);
        for (double nu : new double[]{10, 30, 50})
            assertEquals(mtf.sagittal(nu), mtf.tangential(nu), 1e-6);
        assertEquals(0.0, mtf.sagittal(mtf.nu_cutoff_x));
        assertTrue(mtf.sagittal(30) < mtf.sagittal_diffraction_limited(30));
    }

    @Test
    void rejectsSamplingThatCannotHoldTheMtf() throws Exception {
        var opm = model();
        var fld = opm.optical_spec.fov.fields[0];
        double wvl = opm.optical_spec.wvls.central_wvl();
        assertThrows(IllegalArgumentException.class, () -> new DiffractionMTF(opm, fld, wvl, 0.0, 2, 4));
        assertThrows(IllegalArgumentException.class, () -> new DiffractionMTF(opm, fld, wvl, 0.0, 63, 128));
        assertThrows(IllegalArgumentException.class, () -> new DiffractionMTF(opm, fld, wvl, 0.0, 64, 100));
    }

    @Test
    void rejectsPsfWithoutFinitePositiveIntensity() {
        assertThrows(IllegalArgumentException.class,
                () -> DiffractionMTF.modulus_of_transform(new double[8][8]));
        var psf = new double[8][8];
        psf[4][4] = Double.NaN;
        assertThrows(IllegalArgumentException.class,
                () -> DiffractionMTF.modulus_of_transform(psf));
    }
}
