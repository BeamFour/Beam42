package org.redukti.analysis;

import org.junit.jupiter.api.Test;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.Analyses;
import org.redukti.rayoptics.raytr.RayGrid;
import org.redukti.rayoptics.specs.Field;

import static org.junit.jupiter.api.Assertions.*;

/** Compare the traced-pair integral with the FFT on the same entrance-pupil coordinate
 * scale. This deliberately does not use rim-ray frequency calibration to validate itself.
 */
class ContrastOverlapFftTest {
    private static double phasor(ContrastAnalysisResult.WavelengthResult block, int axis) {
        for (var sample : block.samples(axis)) {
            assertTrue(sample.valid(), () -> String.valueOf(sample.failure()));
        }
        return block.phasor(axis).modulus();
    }

    private static double along(double[][] mtf, double k, int axis) {
        int c = mtf.length / 2, lo = (int) Math.floor(k);
        double a = axis == 0 ? mtf[c + lo][c] : mtf[c][c + lo];
        double b = axis == 0 ? mtf[c + lo + 1][c] : mtf[c][c + lo + 1];
        return a + (k - lo) * (b - a);
    }

    @Test
    void onAxisSummicronPairsAgreeWithFftOverTheCompleteOverlap() throws Exception {
        var build = Class.forName("org.redukti.rayoptics.upstream.US004123144Example08PUpstreamTest")
                .getDeclaredMethod("build");
        build.setAccessible(true);
        var model = (OpticalModel) build.invoke(null);
        var field = model.optical_spec.fov.fields[0];
        model.optical_spec.fov.fields = new Field[]{field};
        double wavelength = model.optical_spec.wvls.central_wvl();
        int n = 256;
        var wavefront = new RayGrid(model, field, wavelength, 0.0,
                null, null, null, null, n, true, Double.NaN, 1).grid[2];
        var zero = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                zero[i][j] = Double.isFinite(wavefront[i][j]) ? 0 : Double.NaN;
        var mtf = DiffractionMTF.modulus_of_transform(Analyses.calc_psf(wavefront, n, 2 * n));
        var ideal = DiffractionMTF.modulus_of_transform(Analyses.calc_psf(zero, n, 2 * n));
        double oldError = 0, newError = 0;
        for (int frequency : new int[]{10, 30, 50}) {
            var options = new ContrastOptions(frequency).num_rings(24).num_spokes(48).check_apertures(true);
            var oldBlock = ContrastAnalysis.eval(model, options).fields.get(0).wavelengths().get(0);
            var block = ContrastAnalysis.eval(model, options.overlap_quadrature(true))
                    .fields.get(0).wavelengths().get(0);
            for (int axis : new int[]{0, 1}) {
                double width = axis == 0 ? field.vignetting_scale_x(-1) + field.vignetting_scale_x(1)
                        : field.vignetting_scale_y(-1) + field.vignetting_scale_y(1);
                double k = block.normalizedPupilShift() * (n - 1) / width;
                double expected = along(mtf, k, axis) / along(ideal, k, axis);
                double old = phasor(oldBlock, axis), actual = phasor(block, axis);
                oldError = Math.max(oldError, Math.abs(old - expected));
                newError = Math.max(newError, Math.abs(actual - expected));
                assertEquals(expected, actual, 3e-3, "frequency=" + frequency + ", axis=" + axis);
                if (axis == 0)
                    System.out.printf(java.util.Locale.ROOT,
                            "Summicron Example 8 %d cyc/mm: contracted=%.6f overlap=%.6f FFT=%.6f%n",
                            frequency, old, actual, expected);
            }
        }
        assertTrue(newError < oldError / 3, "Complete overlap should remove the contracted-region bias");
    }
}
