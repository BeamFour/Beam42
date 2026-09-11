package org.redukti.optim;

import org.junit.jupiter.api.Test;
import org.redukti.examples.ExampleFinder;
import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.spec.Prescription;
import org.redukti.spec.SurfaceType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OptimizationTrialTest {

    /** Eleven surfaces, 0 to 10: the stop is 5, surfaces 3, 7 and 9 are flat, and 10 the last. */
    private static final String SUMMICRON = "Examples/jfotoptix/leica-summicron-50mm-f2/US004123144_Example08P.txt";
    /** Ten surfaces whose ids run 1 to 6, then the stop 6AS, then 7 to 9. */
    private static final String FD300 = "Examples/jfotoptix/canon-fd300mm-f2.8-fluorite/US003868174_Example01P.txt";
    /** A zoom of three scenarios, configured as 0 and 2. */
    private static final String ZOOM = "Examples/jfotoptix/canon-rf70-200mm-f2.8LZ/US20250155694_Example01P.txt";

    private static String withTrials(String example, String trials) throws Exception {
        return Files.readString(Path.of(ExampleFinder.geoPathToExample(example))) + "\n" + trials;
    }

    private static Prescription prescription(String text, OptimizationTrial trial) throws Exception {
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_buffer(text);
        return Prescription.build_prescription(specs, true, trial.weighted(), trial.dLineOnly());
    }

    private static final String MTF_FIVE_FIELDS = """
            fields        0 to 1 step 0.25
            frequencies   20
            goal mtf      20 sag   50 50 50 50 50
            goal mtf      20 tan   50 50 50 50 50
            """;

    @Test
    void numbersSurfacesByPosition() throws Exception {
        String text = withTrials(SUMMICRON, "[trial 1]\n" + MTF_FIVE_FIELDS + """
                vary curvatures   all except 2 6
                vary thicknesses  10 4
                """);
        var trial = OptimizationTrial.parse(text, 1);
        var setup = trial.builder(prescription(text, trial)).build();
        List<String> names = Arrays.stream(setup.variables()).map(trial::describe).toList();
        // Surfaces 3, 7 and 9 are flat and 5 is the stop, so "all" leaves them out without
        // being told.
        assertEquals(List.of("radius 0", "radius 1", "radius 4", "radius 8", "radius 10",
                "thickness 10", "thickness 4"), names);
        assertEquals(10, ((VarThickness) setup.variables()[5])._surface_id);
        assertEquals(4, ((VarThickness) setup.variables()[6])._surface_id);
    }

    @Test
    void ignoresTheIdsInTheFile() throws Exception {
        // Position 6 is the stop, although the row with id 6 is an ordinary surface.
        assertRejected(FD300, "[trial 1]\n" + MTF_FIVE_FIELDS + "vary curvatures 6\n", "surface 6 is a stop");
        assertRejected(FD300, "[trial 1]\n" + MTF_FIVE_FIELDS + "vary thicknesses 6AS\n",
                "expected a surface number, found '6AS'");

        String text = withTrials(FD300, "[trial 1]\n" + MTF_FIVE_FIELDS + """
                vary curvatures   5 7
                vary thicknesses  6 9
                """);
        var trial = OptimizationTrial.parse(text, 1);
        var prescription = prescription(text, trial);
        var variables = trial.builder(prescription).build().variables();
        assertEquals(List.of("radius 5", "radius 7", "thickness 6", "thickness 9"),
                Arrays.stream(variables).map(trial::describe).toList());
        // Surface 5 is the row with id 6, and surface 6 the stop, whose gap is 30.10.
        assertEquals(-524.3616, prescription._surfaces[5]._radius);
        assertEquals(30.10, prescription._surfaces[6]._thickness);
    }

    @Test
    void fieldShorthandGivesTheDecimalValues() throws Exception {
        String text = withTrials(SUMMICRON, """
                [trial 1]
                fields        0 to 1 step 0.1
                frequencies   20
                vary thicknesses 10
                """);
        var trial = OptimizationTrial.parse(text, 1);
        var setup = trial.builder(prescription(text, trial)).build();
        assertArrayEquals(new double[]{0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0},
                setup.analysis()._fields);
    }

    @Test
    void focalLengthGoalReplacesTheAutomaticOne() throws Exception {
        String text = withTrials(SUMMICRON, """
                [trial 1]
                fields        0
                frequencies   20
                vary thicknesses 10
                goal paraxial efl 42 weight 2
                goal paraxial bfl 30
                """);
        var trial = OptimizationTrial.parse(text, 1);
        var goals = trial.builder(prescription(text, trial)).build().goals();
        var paraxial = Arrays.stream(goals).map(GoalParax.class::cast).toList();
        assertEquals(3, paraxial.size());
        assertEquals(ParaxHelper.Effective_focal_length, paraxial.get(0)._parax_id);
        assertEquals(42.0, paraxial.get(0)._target);
        assertEquals(2.0, paraxial.get(0)._weight);
        assertEquals(ParaxHelper.Fno, paraxial.get(1)._parax_id);
        assertEquals(2.0, paraxial.get(1)._target);
        assertEquals(1.0, paraxial.get(1)._weight);
        assertEquals(ParaxHelper.Back_focal_length, paraxial.get(2)._parax_id);
        assertEquals(30.0, paraxial.get(2)._target);
        assertEquals(1.0, paraxial.get(2)._weight);
    }

    @Test
    void makesASphereAsphericWithScaledTerms() throws Exception {
        String text = withTrials(SUMMICRON, "[trial 1]\n" + MTF_FIVE_FIELDS + """
                vary aspherics 0 K A4 A6:1e9
                """);
        var trial = OptimizationTrial.parse(text, 1);
        var prescription = prescription(text, trial);
        var variables = trial.builder(prescription).build().variables();
        SurfaceType surface = prescription._surfaces[0];
        assertEquals(SurfaceType.ASPH_EVEN, surface._asph_type);
        assertEquals(3, surface._coeffs.length);
        assertInstanceOf(VarAsphK.class, variables[0]);
        var a4 = (VarAsphCoeff) variables[1];
        var a6 = (VarAsphCoeff) variables[2];
        assertEquals(1, a4._index);
        // Half the 28.94 diameter, to the fourth power, is 43800: nearest decade 1e5.
        assertEquals(Math.pow(10.0, Math.round(4 * Math.log10(28.94 / 2))), a4._scaling_factor);
        assertEquals(1e5, a4._scaling_factor);
        assertEquals(2, a6._index);
        assertEquals(1e9, a6._scaling_factor);
        assertEquals(List.of("K 0", "A4 0", "A6 0"),
                Arrays.stream(variables).map(trial::describe).toList());
    }

    /** Moves every variable, as a solve would, and writes the result to the prescription. */
    private static void move(Var[] variables) {
        for (Var variable : variables) {
            variable.read_from_prescription();
            double value = variable instanceof VarAsphCoeff ? 1.25e-5
                    : variable instanceof VarAsphK ? -0.75
                    : variable.get_unscaled_value() * 1.01;
            variable.set_unscaled_value(value);
            variable.write_to_prescription();
        }
    }

    @Test
    void writesThePrescriptionFollowedByTheTrial() throws Exception {
        String text = withTrials(SUMMICRON, "[trial 1]\n" + MTF_FIVE_FIELDS + """
                vary curvatures   0 2
                vary thicknesses  0 10
                vary aspherics    0 K A4


                [notes]
                Not part of the trial
                """);
        var trial = OptimizationTrial.parse(text, 1);
        var prescription = prescription(text, trial);
        move(trial.builder(prescription).build().variables());

        String written = trial.optimizedPrescription(prescription);

        assertTrue(written.startsWith(prescription.to_opt_bench_str(new StringBuilder()).toString()));
        assertTrue(written.endsWith("\n[trial 1]\n" + MTF_FIVE_FIELDS + """
                vary curvatures   0 2
                vary thicknesses  0 10
                vary aspherics    0 K A4
                """));
        // Reads back as the optimized prescription, exactly, and the trial carried over
        // refers to the same surfaces.
        var again = OptimizationTrial.parse(written, 1);
        var reread = prescription(written, again);
        for (int i = 0; i < prescription._surfaces.length; i++) {
            SurfaceType expected = prescription._surfaces[i];
            SurfaceType actual = reread._surfaces[i];
            assertEquals(expected._radius, actual._radius, "radius of surface " + i);
            assertEquals(expected._thickness, actual._thickness, "thickness of surface " + i);
            assertEquals(expected._k, actual._k, "conic of surface " + i);
            assertEquals(expected._asph_type, actual._asph_type, "asphere type of surface " + i);
            assertArrayEquals(expected._coeffs, actual._coeffs, "coefficients of surface " + i);
        }
        assertEquals(List.of("radius 0", "radius 2", "thickness 0", "thickness 10", "K 0", "A4 0"),
                Arrays.stream(again.builder(reread).build().variables()).map(again::describe).toList());
    }

    @Test
    void writesAZoomWithItsConfiguredScenariosOnly() throws Exception {
        String text = withTrials(ZOOM, """
                [trial 1]
                configuration     1
                fields            0
                frequencies       10
                vary thicknesses  8 10
                goal mtf          10 sag 50
                goal mtf          10 tan 50
                """);
        var trial = OptimizationTrial.parse(text, 1);
        var prescription = prescription(text, trial);
        var variables = trial.builder(prescription).build().variables();
        for (Var variable : variables) {
            variable.read_from_prescription();
            variable.set_unscaled_value(variable.get_unscaled_value() + 0.5);
            variable.write_to_prescription();
        }
        String written = trial.optimizedPrescription(prescription);

        // The 120mm scenario is not configured, so it is not written; the configurations
        // are renumbered from 0 in the same order.
        assertFalse(written.contains("120.07"), written);
        assertTrue(written.contains("scenarios\t0\t1\n"), written);
        var again = OptimizationTrial.parse(written, 1);
        var reread = prescription(written, again);
        assertEquals(13.64, reread._surfaces[8]._thickness_by_scenario[1], 1e-12);
        assertEquals(8.46, reread._surfaces[8]._thickness_by_scenario[0]);
        assertEquals(prescription._surfaces[10]._thickness_by_scenario[1],
                reread._surfaces[10]._thickness_by_scenario[1]);
        assertEquals(20.31, reread._surfaces[10]._thickness_by_scenario[0]);
        // The trial carried over still means configuration 1 and the same surfaces.
        var moved = again.builder(reread).build().variables();
        assertEquals(1, ((VarThickness) moved[0])._scenario);
        assertEquals(8, ((VarThickness) moved[0])._surface_id);
    }

    @Test
    void keepsTheTrialsTextButNotWhatFollows() throws Exception {
        String text = withTrials(SUMMICRON, """
                [trial 2]
                description  Back focus  # tuned by hand
                fields       0
                frequencies  20

                vary thicknesses 10


                [notes]
                Not part of the trial
                """);
        assertEquals("[trial 2]\ndescription  Back focus  # tuned by hand\nfields       0\n"
                + "frequencies  20\n\nvary thicknesses 10", OptimizationTrial.parse(text, 2).section());
    }

    private static void assertRejected(String example, String trialText, String message) throws Exception {
        String text = withTrials(example, trialText);
        var e = assertThrows(OptimizationTrial.TrialException.class, () -> {
            var trial = OptimizationTrial.parse(text, 1);
            trial.builder(prescription(text, trial)).build();
        });
        assertTrue(e.getMessage().contains(message), e.getMessage());
    }

    private static void assertRejected(String trialText, String message) throws Exception {
        assertRejected(SUMMICRON, trialText, message);
    }

    @Test
    void reportsProblemsWithTheirLine() throws Exception {
        String text = withTrials(SUMMICRON, "[trial 1]\nfields 0\nbogus 1\n");
        int line = List.of(text.split("\n", -1)).indexOf("bogus 1") + 1;
        var e = assertThrows(OptimizationTrial.TrialException.class,
                () -> OptimizationTrial.parse(text, 1));
        assertEquals("trial 1, line " + line + ": unknown keyword 'bogus'", e.getMessage());
    }

    @Test
    void rejectsMistakes() throws Exception {
        assertRejected("[trial 1]\nfields 0\nfields 0\n", "'fields' is given more than once");
        assertRejected("[trial 1]\nfields 0\n", "'frequencies' is required");
        assertRejected("[trial 1]\nfields 0 to 1 step 0.25\nfrequencies 20\n"
                        + "goal mtf 20 sag 50 50 50\ngoal mtf 20 tan 50 50 50 50 50\n",
                "expected 5 targets, one per field, but found 3");
        assertRejected("[trial 1]\n" + MTF_FIVE_FIELDS + "vary curvatures 11\n",
                "there is no surface 11; [lens data] has surfaces 0 to 10");
        assertRejected("[trial 1]\n" + MTF_FIVE_FIELDS + "vary curvatures 5\n", "surface 5 is a stop");
        assertRejected("[trial 1]\n" + MTF_FIVE_FIELDS + "vary thicknesses Bf\n",
                "expected a surface number, found 'Bf'");
        assertRejected("[trial 1]\n" + MTF_FIVE_FIELDS + "vary thicknesses 4 04\n", "surface 4 is listed twice");
        assertRejected("[trial 1]\n" + MTF_FIVE_FIELDS + "vary aspherics 0 K:10\n", "K takes no scale");
        assertRejected("[trial 1]\n" + MTF_FIVE_FIELDS + "vary aspherics 0 A5\n", "A5 is not a term of an even asphere");
        assertRejected("[trial 1]\nfields 0 0.5\nfrequencies 20\ngoal contrast sag 1 1\n",
                "contrast settings need a 'goal contrast <frequencies>' line");
        assertRejected("[trial 1]\nfields 0 0.5\nfrequencies 20\nvary thicknesses 10\ngoal contrast 20\n"
                + "goal contrast balance all except 0.3\n", "there is no field 0.3 in 'fields'");
        assertRejected("[trial 1]\nfields 0\nfrequencies 20\ngoal paraxial focus 3\n", "unknown paraxial quantity 'focus'");
    }

    @Test
    void namesTheTrialsAFileDefines() throws Exception {
        String text = withTrials(SUMMICRON, "[trial 1]\nfields 0\nfrequencies 20\n[trial 4]\nfields 0\nfrequencies 20\n");
        var e = assertThrows(OptimizationTrial.TrialException.class, () -> OptimizationTrial.parse(text, 2));
        assertEquals("there is no [trial 2] in this prescription; it defines trials 1, 4", e.getMessage());
        // The prescription reader is unaffected by the trials.
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_buffer(text);
        assertEquals(11, specs.get_surfaces().size());
    }
}
