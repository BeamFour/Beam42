package org.redukti.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.redukti.examples.ExampleFinder;
import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.spec.Prescription;
import org.redukti.util.Args;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Running a [trial n] from LensTool2, and the --optimize argument that asks for it. */
class OptimizationTrialRunTest {

    private static final String OTUS = "Examples/jfotoptix/cosina-otus-ml-50mm-f1.4/JP2026-105585_Example01.txt";

    @Test
    void optimizeTakesAnOptionalTrialNumber() {
        var trial = Args.parseArguments(new String[]{"--specfile", "lens.txt", "--optimize", "2"});
        assertEquals(2, trial.optimize_trial);
        assertFalse(trial.optimize);

        var routine = Args.parseArguments(new String[]{"--specfile", "lens.txt", "--optimize", "--mtf", "10,20"});
        assertTrue(routine.optimize);
        assertNull(routine.optimize_trial);
        assertArrayEquals(new int[]{10, 20}, routine.mtf_freqs);

        var last = Args.parseArguments(new String[]{"--specfile", "lens.txt", "--optimize"});
        assertTrue(last.optimize);

        var e = assertThrows(IllegalArgumentException.class,
                () -> Args.parseArguments(new String[]{"--optimize", "two"}));
        assertTrue(e.getMessage().contains("[trial n]"), e.getMessage());
    }

    private static final String TRIALS = """

            [trial 3]
            description   Back focus for contrast
            fields        0
            frequencies   20
            vary thicknesses  Bf
            goal contrast     20
            goal contrast     sampling 3 6

            [trial 4]
            description   The same, reported separately
            outdir        trials/back focus
            fields        0
            frequencies   20
            vary thicknesses  Bf
            goal contrast     20
            goal contrast     sampling 3 6
            """;

    private static Path otus(Path dir) throws Exception {
        Path spec = dir.resolve("otus.txt");
        Files.writeString(spec, Files.readString(Path.of(ExampleFinder.geoPathToExample(OTUS))) + TRIALS);
        return spec;
    }

    @Test
    void writesToTheTrialsOwnDirectory(@TempDir Path dir) throws Exception {
        Path spec = otus(dir);
        Args arguments = Args.parseArguments(new String[]{"--specfile", spec.toString(), "--optimize", "4"});
        LensTool2.runOptimizationTrial(Files.readString(spec), arguments);
        Path output = dir.resolve("trials").resolve("back focus").resolve("otus-trial4.txt");
        assertTrue(Files.exists(output));
        assertEquals(output.toAbsolutePath(), Path.of(arguments.specfile).toAbsolutePath());
        assertNull(arguments.outdir);
    }

    @Test
    void commandLineOutdirTakesPrecedence(@TempDir Path dir) throws Exception {
        Path spec = otus(dir);
        Path elsewhere = dir.resolve("elsewhere");
        Args arguments = Args.parseArguments(new String[]{"--specfile", spec.toString(),
                "--outdir", elsewhere.toString(), "--optimize", "4"});
        LensTool2.runOptimizationTrial(Files.readString(spec), arguments);
        assertTrue(Files.exists(elsewhere.resolve("otus-trial4.txt")));
        assertFalse(Files.exists(dir.resolve("trials")));
    }

    @Test
    void runsTheTrialAndReportsOnTheResult(@TempDir Path dir) throws Exception {
        Path spec = otus(dir);
        Args arguments = Args.parseArguments(new String[]{"--specfile", spec.toString(), "--optimize", "3"});

        String optimized = LensTool2.runOptimizationTrial(Files.readString(spec), arguments);

        // Without an outdir the optimized prescription is written next to the input, and
        // the rest of the run is pointed at it.
        Path output = dir.resolve("otus-trial3.txt");
        assertEquals(output.toAbsolutePath(), Path.of(arguments.specfile).toAbsolutePath());
        assertEquals(optimized, Files.readString(output));
        assertTrue(optimized.contains("[trial 3]"));

        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_buffer(optimized);
        var prescription = Prescription.build_prescription(specs, true, false, false);
        assertNotEquals(18.665, prescription._surfaces[25]._thickness);
        // Only the infinity-focus column of Bf is this configuration's.
        assertTrue(optimized.lines().anyMatch(line -> line.startsWith("Bf\t") && line.endsWith("\t25.55")));
    }
}
