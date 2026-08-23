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
                // A single goal frequency, like the contrast test - but at 40,
                // not 20. This is NOT the saving the contrast test got: MTF
                // frequencies are nearly free once the per-field FFT has run
                // (see computeMTFs), so dropping goals does not cut the cost of
                // an evaluation. It only reshapes the merit, 8 residuals instead
                // of 24, and the less constrained problem actually takes longer
                // to converge - 97 s against 61 s for the three goals.
                //
                // It is kept because the lens is better on all twelve measured
                // numbers below. The frequency matters, though; measured:
                //
                //   goals        solve  spot RMS full fld  sag@40 full  tan@40 full
                //   10/20/40      61 s       8.90              .612         .314
                //   20 only      108 s       9.07              .444         .082
                //   40 only       97 s       6.88              .696         .382
                //
                // With the goal at 20, finalRms *improves* to 0.0046 while the
                // lens degrades: nothing constrains 40 cyc/mm any more and the
                // full-field tangential MTF falls to 0.082, next to a contrast
                // reversal. Goal at the frequency you care about, or measure the
                // one you did not constrain. 10 and 20 are still measured here,
                // for the assertions and the contrast test's comparison.
                .mtfGoals(
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
     *
     * <p>Note that {@code mtfFrequencies} is <em>not</em> part of the merit here: with
     * only contrast and ray-aberration goals, {@code configureRequiredAnalyses} turns the
     * spot and MTF analyses off for the solve. Those frequencies serve the post-solve
     * measurement below, where the cost of an extra frequency is a table lookup.
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
                .contrastSampling(4, 8)
                // One frequency, not three. Each entry costs a full
                // ContrastAnalysis.eval per merit evaluation - 4 fields x 3
                // wavelengths x rings x spokes samples x 3 rays - and that trace
                // dominates the solve. Measured here, solve time only:
                //
                //   3 freqs, 6x12   381 s
                //   1 freq,  6x12   189 s
                //   1 freq,  4x8     73 s
                //
                // All three reach the same lens; the spot RMS and MTF asserted
                // below move by ~1-4% between them, and 4x8 is marginally the
                // best of the three at fields 1-3. REVIEW.md section 4 explains
                // why one frequency suffices: at 3% of cutoff the residual is a
                // scaled OPD gradient, and cos(r20, r40) = 0.9993, so the extra
                // frequencies add ray cost and no new direction.
                //
                // On 4x8 - below the 6x12 that REVIEW.md section 2 established
                // as converged at 40 cyc/mm. Re-measured at 20 cyc/mm on the
                // OPTIMIZED design, where section 2's grid-fitting shows up,
                // sigma(dW) against a converged 20x40 is +2 to +15%, i.e. 4x8
                // reads the wavefront error slightly HIGH. The 3x6 pathology
                // that motivated section 2 was the opposite sign, -10 to -45%:
                // a merit flattered by its own quadrature. Overestimating is not
                // that exploit, and the independent spot/MTF numbers below
                // confirm the lens did not degrade. Field 0.7 sagittal is the
                // one place 4x8 reads low (-9%); watch it if this is retuned.
                .contrastGoals(contrast(20, fieldWeights))
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
        assertEquals(0.0071142877, finalRms, 1.0e-6);
        assertArrayEquals(new double[]{
                        2.40301416, 3.42632001, 3.82472092, 3.96369239},
                spotRms, 1.0e-6);
        // LensTool2 uses SpotOptions' 64-ray Hexapolar default. Keep this
        // second absolute regression so its report can be compared directly.
        assertArrayEquals(new double[]{
                        2.43550353, 3.57432255, 3.90360756, 4.00575676},
                hexapolarSpotRms, 1.0e-6);
        assertArrayEquals(new double[]{
                        0.90924673, 0.86955926, 0.79496499, 0.79446263},
                sagittal40, 1.0e-6);
        assertArrayEquals(new double[]{
                        0.90924673, 0.80797417, 0.80535821, 0.78698894},
                tangential40, 1.0e-6);

        // Retain a direct A/B assertion as well as the absolute values above.
        // The comparison arrays are the gaussian-quadrature test's expected
        // values; keep them in step when those are regenerated.
        assertAllLessThan(spotRms,
                new double[]{5.42166951, 6.74053288, 6.92564049, 6.87614585},
                "spot RMS");
        assertAllGreaterThan(sagittal40,
                new double[]{0.64949397, 0.69084539, 0.54741863, 0.69640215},
                "40 cycle/mm sagittal MTF");
        assertAllGreaterThan(tangential40,
                new double[]{0.64949397, 0.57967420, 0.51503656, 0.38198031},
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
        assertEquals(0.0118734292, finalRms, 1.0e-6);

        var analysis = setup.analysis();
        assertEquals(50.15110204, analysis._pfo[ParaxHelper.Effective_focal_length], 1.0e-6);
        assertEquals(1.43889571, analysis._pfo[ParaxHelper.Fno], 1.0e-6);
        assertArrayEquals(new double[]{
                        5.42166951, 6.74053288, 6.92564049, 6.87614585},
                java.util.Arrays.stream(analysis._spots)
                        .mapToDouble(spot -> spot.get_mean_radius()).toArray(),
                1.0e-6);
        assertArrayEquals(new double[]{
                        0.64949397, 0.69084539, 0.54741863, 0.69640215},
                analysis._mtfs[2].sag_mtf_by_field, 1.0e-6);
        assertArrayEquals(new double[]{
                        0.64949397, 0.57967420, 0.51503656, 0.38198031},
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
