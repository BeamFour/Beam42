package org.redukti.examples;

import org.junit.jupiter.api.Test;
import org.redukti.rayoptics.analysis.SpotOptions;

import java.nio.file.Files;
import java.nio.file.Path;

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
