package org.redukti.examples;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenericOptTest {

    @Test
    void reportsInitialContrastTirWithContext() throws Exception {
        Path input = repositoryRoot().resolve(
                "Examples/jfotoptix/nikkor-58mm-f1.4g/JP2013-019993_Example01.txt");
        var prescription = GenericContrastOpt.getPrescription(input.toString(), false, false);
        var setup = GenericContrastOpt.createContrastSetup(prescription, false, false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> setup.meritFunction(false).getSolver().solve());

        String message = exception.getMessage();
        assertTrue(message.contains("contrast samples contain failed rays"), message);
        assertTrue(message.contains("TraceTIRException at surface 15"), message);
        assertTrue(message.contains("field=1.0"), message);
    }

    private static Path repositoryRoot() {
        Path root = Path.of(System.getProperty(
                "maven.multiModuleProjectDirectory", System.getProperty("user.dir")))
                .toAbsolutePath().normalize();
        if (Files.isDirectory(root.resolve("Examples"))) return root;
        return root.resolve("..").normalize();
    }
}
