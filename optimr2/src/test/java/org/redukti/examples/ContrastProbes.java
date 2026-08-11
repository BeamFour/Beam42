package org.redukti.examples;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shared support for the ContrastProbe* programs.
 *
 * <p>These are not JUnit tests. They are standalone {@code main} programs written to
 * gather the evidence in REVIEW.md for the contrast-optimization implementation. They
 * live in the test source root so they compile against the module's classpath and can
 * reach the package-private helpers on {@link ZeissOtusML50mm}.
 */
public final class ContrastProbes {

    private ContrastProbes() {
    }

    /** The Zeiss Otus ML 50mm patent prescription used by every probe. */
    public static String inputPath() {
        return repositoryRoot()
                .resolve("Examples/jfotoptix/cosina-otus-ml-50mm-f1.4/JP2026-105585_Example01.txt")
                .toAbsolutePath().toString();
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
