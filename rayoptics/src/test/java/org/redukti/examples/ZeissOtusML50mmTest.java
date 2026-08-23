package org.redukti.examples;

import org.junit.jupiter.api.Test;
import org.redukti.optim.OptimizationBuilder;
import org.redukti.optim.OptimizationBuilder.OptimizationSetup;
import org.redukti.optim.ParaxHelper;
import org.redukti.rayoptics.analysis.SpotOptions;
import org.redukti.rayoptics.analysis.SpotAnalysis;
import org.redukti.spec.Prescription;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.redukti.optim.OptimizationBuilder.contrast;

class ZeissOtusML50mmTest {

    /** The direct spot/MTF configuration the gaussian-quadrature expectations assume. */
    private static OptimizationSetup frozenDirectSetup(Prescription prescription) {
        return OptimizationBuilder.builder(prescription)
                .fields(0.0, 0.3, 0.7, 1.0)
                .mtfFrequencies(10, 20, 40)
                .varyCurvatures(
                        0, 1, 2, 3, 4, 5, 6, 8, 9,
                        11, 12, 13, 14, 16, 17, 18, 19, 20, 21, 22, 23)
                .varyThicknesses(25)
                .varyExistingAspherics()
                .weighted(false)
                .dLineOnly(false)
                .rayAberrationGoals()
                .mtfGoals(
                        OptimizationBuilder.mtf(10,
                                new double[]{93, 93, 94, 93},
                                new double[]{93, 93, 90, 82}),
                        OptimizationBuilder.mtf(20,
                                new double[]{85, 85, 85, 80},
                                new double[]{85, 85, 78, 62}),
                        OptimizationBuilder.mtf(40,
                                new double[]{65, 65, 64, 58},
                                new double[]{65, 62, 45, 38}))
                .build();
    }

    /**
     * The contrast configuration this test's expected values were generated under,
     * declared here rather than taken from {@link ZeissOtusML50mm}.
     *
     * <p>The example class is a place to try things - frequencies, sampling, the
     * exit-pupil frequency calibration - and every one of those changes the merit
     * function and therefore every number asserted below. Sharing its setup meant an
     * experiment silently became a test failure. This test locks the implementation's
     * behaviour on a fixed configuration, so it has to own that configuration.
     *
     * <p>Change it only deliberately, and regenerate the expected values when you do.
     */
    private static OptimizationSetup frozenContrastSetup(Prescription prescription) {
        double[] fieldWeights = {1.0, 1.0, 1.0, 1.0};
        return OptimizationBuilder.builder(prescription)
                .fields(0.0, 0.3, 0.7, 1.0)
                .mtfFrequencies(10, 20, 40)
                .varyCurvatures(
                        0, 1, 2, 3, 4, 5, 6, 8, 9,
                        11, 12, 13, 14, 16, 17, 18, 19, 20, 21, 22, 23)
                .varyThicknesses(25)
                .varyExistingAspherics()
                .weighted(false)
                .dLineOnly(false)
                // Ray fans are opt-in now; they were unconditional when these values
                // were generated, so keep them to preserve the merit.
                .rayAberrationGoals()
                .contrastSampling(6, 12)
                .contrastGoals(
                        contrast(10, fieldWeights),
                        contrast(20, fieldWeights),
                        contrast(40, fieldWeights))
                .build();
    }

    @Test
    void optimizesPatentPrescriptionUsingContrast() throws Exception {
        Path input = repositoryRoot().resolve(
                "Examples/jfotoptix/cosina-otus-ml-50mm-f1.4/JP2026-105585_Example01.txt");
        boolean weighted = false;
        boolean dLineOnly = false;
        var prescription = ZeissOtusML50mm.getPrescription(
                input.toAbsolutePath().toString(), weighted, dLineOnly);
        var setup = frozenContrastSetup(prescription);

        setup.analysis().compute();
        var meritFunction = setup.meritFunction(false);
        double initialRms = meritFunction.getRMS();
        long invalidContrastGoals = java.util.Arrays.stream(setup.goals())
                .filter(org.redukti.optim.GoalContrast.class::isInstance)
                .filter(goal -> goal.value() >= org.redukti.mathlib.LMLSolver.BIGVAL)
                .count();
        assertEquals(0, invalidContrastGoals, "Initial contrast sampling contains failed rays");
        long started = System.nanoTime();
        int status = meritFunction.getSolver().solve();
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
        double finalRms = meritFunction.getRMS();

        assertTrue(status > 0, "Optimizer failed with status " + status);
        assertTrue(finalRms < initialRms,
                () -> "Expected contrast RMS to improve from " + initialRms + " but got " + finalRms);

        System.out.println(prescription);

        // Contrast residuals are a merit surrogate; final performance is
        // deliberately measured with the normal direct spot/MTF analysis.
        var analysis = setup.analysis();
        analysis.required_analyses(true, true, true);
        analysis.compute();
        double[] spotRms = java.util.Arrays.stream(analysis._spots)
                .mapToDouble(spot -> spot.get_mean_radius()).toArray();
        double[] sagittal40 = analysis._mtfs[2].sag_mtf_by_field;
        double[] tangential40 = analysis._mtfs[2].tan_mtf_by_field;
        double[] hexapolarSpotRms = SpotAnalysis.eval(
                        analysis._opt_model, new SpotOptions().use_hexapolar().num_rays(64))
                .spot_results.stream().mapToDouble(spot -> spot.get_mean_radius()).toArray();
        assertEquals(0.0064283619, finalRms, 1.0e-6);
        assertArrayEquals(new double[]{
                        2.39415192, 3.47609731, 3.90356934, 4.18874019},
                spotRms, 1.0e-6);
        // LensTool2 uses SpotOptions' 64-ray Hexapolar default. Keep this
        // second absolute regression so its report can be compared directly.
        assertArrayEquals(new double[]{
                        2.41935659, 3.72385694, 4.01817087, 4.23702001},
                hexapolarSpotRms, 1.0e-6);
        assertArrayEquals(new double[]{
                        0.91172266, 0.86866441, 0.79832238, 0.80818103},
                sagittal40, 1.0e-6);
        assertArrayEquals(new double[]{
                        0.91172266, 0.82379914, 0.80210041, 0.75217362},
                tangential40, 1.0e-6);

        // Retain a direct A/B assertion as well as the absolute values above.
        // The comparison arrays are the gaussian-quadrature test's expected
        // values; keep them in step when those are regenerated.
        assertAllLessThan(spotRms,
                new double[]{5.77765598, 7.31854410, 7.71931778, 8.90432789},
                "spot RMS");
        assertAllGreaterThan(sagittal40,
                new double[]{0.59327340, 0.59652846, 0.52547396, 0.61176714},
                "40 cycle/mm sagittal MTF");
        assertAllGreaterThan(tangential40,
                new double[]{0.59327340, 0.53765177, 0.45867084, 0.31407528},
                "40 cycle/mm tangential MTF");
        System.out.println("Contrast Otus: elapsedMs=" + elapsedMillis
                + " initialRms=" + initialRms + " finalRms=" + finalRms);
    }

    @Test
    void optimizesPatentPrescriptionUsingGaussianQuadrature() throws Exception {
        Path input = repositoryRoot().resolve(
                "Examples/jfotoptix/cosina-otus-ml-50mm-f1.4/JP2026-105585_Example01.txt");
        assertTrue(Files.isRegularFile(input), "Missing test prescription: " + input);

        boolean weighted = false;
        boolean dLineOnly = false;
        var prescription = ZeissOtusML50mm.getPrescription(
                input.toAbsolutePath().toString(), weighted, dLineOnly);
        var setup = frozenDirectSetup(prescription);

        assertEquals(SpotOptions.PATTERN_GAUSS_QUADRATURE, setup.analysis()._spot_pattern);

        setup.analysis().compute();
        var meritFunction = setup.meritFunction(false);
        double initialRms = meritFunction.getRMS();
        long started = System.nanoTime();
        int status = meritFunction.getSolver().solve();
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
        double finalRms = meritFunction.getRMS();
        System.out.println("Direct Otus: elapsedMs=" + elapsedMillis
                + " initialRms=" + initialRms + " finalRms=" + finalRms);

        assertTrue(status > 0, "Optimizer failed with status " + status);
        assertTrue(finalRms < initialRms,
                () -> "Expected RMS merit to improve from " + initialRms + " but got " + finalRms);
        assertEquals(0.0159792774, finalRms, 1.0e-6);

        var analysis = setup.analysis();
        assertEquals(50.17930093, analysis._pfo[ParaxHelper.Effective_focal_length], 1.0e-6);
        assertEquals(1.43833782, analysis._pfo[ParaxHelper.Fno], 1.0e-6);
        assertArrayEquals(new double[]{
                        5.77765598, 7.31854410, 7.71931778, 8.90432789},
                java.util.Arrays.stream(analysis._spots)
                        .mapToDouble(spot -> spot.get_mean_radius()).toArray(),
                1.0e-6);
        assertArrayEquals(new double[]{
                        0.59327340, 0.59652846, 0.52547396, 0.61176714},
                analysis._mtfs[2].sag_mtf_by_field, 1.0e-6);
        assertArrayEquals(new double[]{
                        0.59327340, 0.53765177, 0.45867084, 0.31407528},
                analysis._mtfs[2].tan_mtf_by_field, 1.0e-6);
    }

    private static void assertAllGreaterThan(double[] actual, double[] comparison, String label) {
        assertEquals(comparison.length, actual.length);
        for (int i = 0; i < actual.length; i++) {
            int index = i;
            assertTrue(actual[i] > comparison[i],
                    () -> label + " field " + index + ": expected " + actual[index]
                            + " to exceed direct result " + comparison[index]);
        }
    }

    private static void assertAllLessThan(double[] actual, double[] comparison, String label) {
        assertEquals(comparison.length, actual.length);
        for (int i = 0; i < actual.length; i++) {
            int index = i;
            assertTrue(actual[i] < comparison[i],
                    () -> label + " field " + index + ": expected " + actual[index]
                            + " to be below direct result " + comparison[index]);
        }
    }

    private static Path repositoryRoot() {
        Path root = Path.of(System.getProperty(
                "maven.multiModuleProjectDirectory", System.getProperty("user.dir")))
                .toAbsolutePath().normalize();
        if (Files.isDirectory(root.resolve("Examples")))
            return root;
        if (Files.isDirectory(root.resolve("../Examples")))
            return root.resolve("..").normalize();
        throw new IllegalStateException("Cannot locate repository root from " + root);
    }
}
