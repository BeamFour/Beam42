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

    /** The Zeiss Otus ML 50mm patent prescription used by most of the probes. */
    public static String inputPath() {
        return resolve("Examples/jfotoptix/cosina-otus-ml-50mm-f1.4/JP2026-105585_Example01.txt");
    }

    /**
     * The Leica R APO 75mm f/2. Used by the finding-6 probes: it is a much harder
     * starting point than the Otus and stays outside the small-phase regime where the
     * least-squares contrast merit tracks MTF.
     */
    public static String leicaInputPath() {
        return resolve("Examples/jfotoptix/leica-r-apo-75mm-f2-mandler/specs-original.txt");
    }

    private static String resolve(String relative) {
        return repositoryRoot().resolve(relative).toAbsolutePath().toString();
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
