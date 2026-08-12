package org.redukti.examples;

import org.junit.jupiter.api.Test;
import org.redukti.optim.ParaxHelper;
import org.redukti.rayoptics.analysis.SpotOptions;
import org.redukti.rayoptics.analysis.SpotAnalysis;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZeissOtusML50mmTest {

    @Test
    void optimizesPatentPrescriptionUsingContrast() throws Exception {
        Path input = repositoryRoot().resolve(
                "Examples/jfotoptix/cosina-otus-ml-50mm-f1.4/JP2026-105585_Example01.txt");
        boolean weighted = false;
        boolean dLineOnly = false;
        var prescription = ZeissOtusML50mm.getPrescription(
                input.toAbsolutePath().toString(), weighted, dLineOnly);
        var setup = ZeissOtusML50mm.createContrastSetup(prescription, weighted, dLineOnly);

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
        assertEquals(0.0064364267, finalRms, 1.0e-6);
        assertArrayEquals(new double[]{
                        2.39790133, 3.48696497, 3.91691417, 4.18073223},
                spotRms, 1.0e-6);
        // LensTool2 uses SpotOptions' 64-ray Hexapolar default. Keep this
        // second absolute regression so its report can be compared directly.
        assertArrayEquals(new double[]{
                        2.42367915, 3.73286600, 4.03288050, 4.22870275},
                hexapolarSpotRms, 1.0e-6);
        assertArrayEquals(new double[]{
                        0.91055107, 0.86919063, 0.79900446, 0.80860358},
                sagittal40, 1.0e-6);
        assertArrayEquals(new double[]{
                        0.91055107, 0.82205323, 0.80111212, 0.75423138},
                tangential40, 1.0e-6);

        // Retain a direct A/B assertion as well as the absolute values above.
        assertAllLessThan(spotRms,
                new double[]{5.83978998, 7.20583613, 7.53832391, 8.69058359},
                "spot RMS");
        assertAllGreaterThan(sagittal40,
                new double[]{0.59088210, 0.59802511, 0.52815747, 0.62313845},
                "40 cycle/mm sagittal MTF");
        assertAllGreaterThan(tangential40,
                new double[]{0.59088210, 0.54005008, 0.47372891, 0.32960330},
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
        var setup = ZeissOtusML50mm.createSetup(prescription, weighted, dLineOnly);

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
        assertEquals(0.0160522, finalRms, 1.0e-6);

        var analysis = setup.analysis();
        assertEquals(50.1770744, analysis._pfo[ParaxHelper.Effective_focal_length], 1.0e-6);
        assertEquals(1.43832395, analysis._pfo[ParaxHelper.Fno], 1.0e-6);
        assertArrayEquals(new double[]{
                        5.83978998, 7.20583613, 7.53832391, 8.69058359},
                java.util.Arrays.stream(analysis._spots)
                        .mapToDouble(spot -> spot.get_mean_radius()).toArray(),
                1.0e-6);
        assertArrayEquals(new double[]{
                        0.59088210, 0.59802511, 0.52815747, 0.62313845},
                analysis._mtfs[2].sag_mtf_by_field, 1.0e-6);
        assertArrayEquals(new double[]{
                        0.59088210, 0.54005008, 0.47372891, 0.32960330},
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
