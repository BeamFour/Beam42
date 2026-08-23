package org.redukti.examples;

import java.nio.file.Files;
import java.nio.file.Path;

public class ExampleFinder {

    public static Path repositoryRoot() {
        Path root = Path.of(System.getProperty(
                        "maven.multiModuleProjectDirectory", System.getProperty("user.dir")))
                .toAbsolutePath().normalize();
        if (Files.isDirectory(root.resolve("Examples")))
            return root;
        if (Files.isDirectory(root.resolve("../Examples")))
            return root.resolve("..").normalize();
        throw new IllegalStateException("Cannot locate repository root from " + root);
    }

    public static String geoPathToExample(String example) {
        return repositoryRoot().resolve(
                example).toAbsolutePath().toString();
    }
}
