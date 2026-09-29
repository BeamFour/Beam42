// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
// See LICENSE-GPL-3.0.txt
package org.redukti.cppport;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shared bits of the dump programs that generate the C++ port's expected values.
 *
 * <p>The prescriptions the C++ tests assert against live in that checkout, so the dumps
 * read them from there rather than from this repository's {@code Examples}, where a file
 * may be mid-edit.</p>
 */
final class CppPort {
    private CppPort() {
    }

    /** The C++ checkout: {@code args[at]} if given, else {@code ../rayoptics-cpp}. */
    static Path checkout(String[] args, int at) {
        Path path = Path.of(args.length > at ? args[at] : "../rayoptics-cpp");
        if (!Files.isDirectory(path.resolve("Examples")))
            throw new IllegalArgumentException(
                    "not a rayoptics-cpp checkout: " + path.toAbsolutePath()
                            + " (pass it as an argument)");
        return path;
    }

    /** That checkout's Examples directory, with a trailing separator for concatenation. */
    static String examples(String[] args, int at) {
        return checkout(args, at).resolve("Examples").toString().replace('\\', '/') + "/";
    }
}
