package org.redukti.analysis;

import org.junit.jupiter.api.Test;
import org.redukti.examples.ExampleFinder;
import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.Analyses;
import org.redukti.rayoptics.raytr.RayGrid;
import org.redukti.spec.Prescription;
import org.redukti.spec.RayOpticsModelBuilder;
import org.redukti.spec.VigType;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiffractionMTFTest {

    private static final String SUMMICRON = "Examples/jfotoptix/leica-summicron-50mm-f2/US004123144_Example08P.txt";

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
    void otfModulusAtTheFftBinsIsTheMtf() throws Exception {
        // Off axis, where the two cutoffs differ and the tangential OTF has a phase.
        var opm = model();
        var fields = opm.optical_spec.fov.fields;
        var fld = fields[fields.length - 1];
        double wvl = opm.optical_spec.wvls.central_wvl();
        int n = 64;
        var mtf = new DiffractionMTF(opm, fld, wvl, 0.0, n, 2 * n);
        for (int k = 0; k < n - 1; k++) {
            double nu_x = k * mtf.nu_cutoff_x / (n - 1);
            double nu_y = k * mtf.nu_cutoff_y / (n - 1);
            var s = mtf.sagittal_otf(nu_x);
            var t = mtf.tangential_otf(nu_y);
            assertEquals(mtf.sagittal(nu_x), Math.hypot(s[0], s[1]), 1e-12);
            assertEquals(mtf.tangential(nu_y), Math.hypot(t[0], t[1]), 1e-12);
        }
        var dc = mtf.tangential_otf(0.0);
        assertEquals(1.0, dc[0], 1e-12);
        assertEquals(0.0, dc[1], 1e-12);
        // a real line spread: OTF(-nu) is the conjugate of OTF(nu)
        var plus = mtf.tangential_otf(40.0);
        var minus = mtf.tangential_otf(-40.0);
        assertEquals(plus[0], minus[0], 1e-12);
        assertEquals(plus[1], -minus[1], 1e-12);
        assertArrayEquals(new double[]{0.0, 0.0}, mtf.sagittal_otf(mtf.nu_cutoff_x));
    }

    @Test
    void otfBetweenTheBinsOfAnAberrationFreePupilIsTheDiffractionLimit() {
        int n = 128;
        var W = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                double x = -1 + 2.0 * i / (n - 1), y = -1 + 2.0 * j / (n - 1);
                W[i][j] = x * x + y * y <= 1 ? 0.0 : Double.NaN;
            }
        var psf = Analyses.calc_psf(W, n, 2 * n);
        var lsf_x = DiffractionMTF.line_spread(psf, true);
        var lsf_y = DiffractionMTF.line_spread(psf, false);
        for (int k = 0; k < n - 2; k += 7) {
            double nu = (k + 0.5) / (n - 1);
            double expected = DiffractionMTF.diffraction_limited(nu, 1.0);
            var x = DiffractionMTF.otf(lsf_x, nu, 1.0, n);
            var y = DiffractionMTF.otf(lsf_y, nu, 1.0, n);
            assertEquals(expected, Math.hypot(x[0], x[1]), 2e-3);
            assertEquals(expected, Math.hypot(y[0], y[1]), 2e-3);
        }
    }

    /** The committed prescription of the test lens, with its five wavelengths. */
    private static OpticalModel polychromatic_model() throws Exception {
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(ExampleFinder.geoPathToExample(SUMMICRON));
        Prescription prescription = Prescription.build_prescription(specs, true, false, false);
        return new RayOpticsModelBuilder(prescription)
                .build_optical_model(true, new double[]{0.0, 1.0}, false, VigType.SetPupil, true, 0);
    }

    @Test
    void aCommonImagePointPutsLateralColourInTheOtfPhase() throws Exception {
        var opm = polychromatic_model();
        var fld = opm.optical_spec.fov.fields[1];
        var wvls = opm.optical_spec.wvls;
        double ref = wvls.central_wvl();
        int n = 64;
        var image_pt = DiffractionMTF.image_point(opm, fld, ref, 0.0);

        // Referenced to its own chief ray, the reference wavelength is unchanged.
        var own_ref = new DiffractionMTF(opm, fld, ref, 0.0, n, 2 * n);
        var common_ref = new DiffractionMTF(opm, fld, ref, 0.0, n, 2 * n, image_pt);
        for (double nu : new double[]{10, 40, 80}) {
            assertArrayEquals(own_ref.tangential_otf(nu), common_ref.tangential_otf(nu), 1e-12);
            assertArrayEquals(own_ref.sagittal_otf(nu), common_ref.sagittal_otf(nu), 1e-12);
        }

        // Any other wavelength keeps its modulus, and its phase changes by -2 pi nu dy, where
        // dy is its lateral colour: here 5 microns in F, -0.8 in C.
        for (double wvl : wvls.wavelengths) {
            if (wvl == ref) continue;
            double dy = DiffractionMTF.image_point(opm, fld, wvl, 0.0).y - image_pt.y;
            var own = new DiffractionMTF(opm, fld, wvl, 0.0, n, 2 * n);
            var common = new DiffractionMTF(opm, fld, wvl, 0.0, n, 2 * n, image_pt);
            for (double nu : new double[]{10, 40, 80}) {
                var a = own.tangential_otf(nu);
                var b = common.tangential_otf(nu);
                assertEquals(Math.hypot(a[0], a[1]), Math.hypot(b[0], b[1]), 1e-3);
                double shift = Math.atan2(b[1], b[0]) - Math.atan2(a[1], a[0]);
                assertEquals(0.0, Math.IEEEremainder(shift + 2 * Math.PI * nu * dy, 2 * Math.PI), 1e-2);
            }
        }
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
