package org.redukti.examples;

import org.junit.jupiter.api.Test;
import org.redukti.optim.ParaxHelper;
import org.redukti.rayoptics.analysis.SpotOptions;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZeissOtusML50mmTest {

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
        int status = meritFunction.getSolver().solve();
        double finalRms = meritFunction.getRMS();

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
