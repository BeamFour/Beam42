package org.redukti.optim;

import org.junit.jupiter.api.Test;
import org.redukti.examples.ExampleFinder;
import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.spec.Prescription;
import org.redukti.spec.SurfaceType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OptimizationTrialTest {

    /** Eleven rows: the stop is 6, surfaces 4, 8 and 10 are flat, and the back focus is named Bf. */
    private static final String SUMMICRON = "Examples/jfotoptix/leica-summicron-50mm-f2/US004123144_Example08P.txt";
    /** A zoom whose second configuration is the file's scenario 2. */
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
    void resolvesSurfacesByTheirPrescriptionNames() throws Exception {
        String text = withTrials(SUMMICRON, "[trial 1]\n" + MTF_FIVE_FIELDS + """
                vary curvatures   all except 3 7
                vary thicknesses  Bf 5
                """);
        var trial = OptimizationTrial.parse(text, 1);
        var setup = trial.builder(prescription(text, trial)).build();
        List<String> names = Arrays.stream(setup.variables()).map(trial::describe).toList();
        // Surfaces 4, 8 and 10 are flat and 6 is the stop, so "all" leaves them out without
        // being told.
        assertEquals(List.of("radius 1", "radius 2", "radius 5", "radius 9", "radius 11",
                "thickness Bf", "thickness 5"), names);
        assertEquals(10, ((VarThickness) setup.variables()[5])._surface_id);
        assertEquals(4, ((VarThickness) setup.variables()[6])._surface_id);
    }

    @Test
    void fieldShorthandGivesTheDecimalValues() throws Exception {
        String text = withTrials(SUMMICRON, """
                [trial 1]
                fields        0 to 1 step 0.1
                frequencies   20
                vary thicknesses Bf
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
                vary thicknesses Bf
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
                vary aspherics 1 K A4 A6:1e9
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
        assertEquals(List.of("K 1", "A4 1", "A6 1"),
                Arrays.stream(variables).map(trial::describe).toList());
    }

    @Test
    void writesOnlyTheVariedValues() throws Exception {
        String text = withTrials(SUMMICRON, "[trial 1]\n" + MTF_FIVE_FIELDS + """
                vary curvatures   1 3
                vary thicknesses  1 Bf
                vary aspherics    1 K A4
                """);
        var trial = OptimizationTrial.parse(text, 1);
        var prescription = prescription(text, trial);
        var variables = trial.builder(prescription).build().variables();
        for (Var variable : variables) {
            variable.read_from_prescription();
            double value = variable instanceof VarAsphCoeff ? 1.25e-5
                    : variable instanceof VarAsphK ? -0.75
                    : variable.get_unscaled_value() * 1.01;
            variable.set_unscaled_value(value);
            variable.write_to_prescription();
        }
        String written = OptimizedPrescriptionWriter.write(text, prescription, variables);

        // Reads back as the optimized prescription, exactly.
        var reread = prescription(written, trial);
        for (int i = 0; i < prescription._surfaces.length; i++) {
            SurfaceType expected = prescription._surfaces[i];
            SurfaceType actual = reread._surfaces[i];
            assertEquals(expected._radius, actual._radius, "radius of surface " + i);
            assertEquals(expected._thickness, actual._thickness, "thickness of surface " + i);
            assertEquals(expected._k, actual._k, "conic of surface " + i);
            assertEquals(expected._asph_type, actual._asph_type, "asphere type of surface " + i);
            assertArrayEquals(expected._coeffs, actual._coeffs, "coefficients of surface " + i);
        }

        // Nothing else moved: set aside the new aspherical data, and three lines differ -
        // lens rows 1 and 3, and the Bf distance.
        List<String> before = List.of(text.split("\n", -1));
        List<String> after = new ArrayList<>(List.of(written.split("\n", -1)));
        int header = after.indexOf("[aspherical data]");
        assertTrue(header > 0);
        assertTrue(after.get(header + 1).startsWith("1\t"));
        after.remove(header + 1);
        after.remove(header);
        assertEquals(before.size(), after.size());
        int differing = 0;
        for (int i = 0; i < before.size(); i++)
            if (!before.get(i).equals(after.get(i)))
                differing++;
        assertEquals(3, differing);
        assertTrue(written.contains("[trial 1]"));
    }

    @Test
    void writesAZoomDistanceToItsConfigurationsColumn() throws Exception {
        String text = withTrials(ZOOM, """
                [trial 1]
                configuration     1
                fields            0
                frequencies       10
                vary thicknesses  d9 d11
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
        String written = OptimizedPrescriptionWriter.write(text, prescription, variables);
        // Configuration 1 is scenario 2 in this file, the third column of values; the
        // other configurations' columns are left as they were.
        String[] d9 = row(written, "d9");
        assertEquals("8.46", d9[1]);
        assertEquals("10.54", d9[2]);
        assertEquals(prescription._surfaces[8]._thickness_by_scenario[1], Double.parseDouble(d9[3]));
        String[] d11 = row(written, "d11");
        assertEquals("20.31", d11[1]);
        assertEquals("8.04", d11[2]);
        assertEquals(prescription._surfaces[10]._thickness_by_scenario[1], Double.parseDouble(d11[3]));
        // The file's CRLF line endings are kept.
        assertFalse(written.replace("\r\n", "").contains("\n"));
        var reread = prescription(written, trial);
        assertEquals(prescription._surfaces[8]._thickness_by_scenario[1],
                reread._surfaces[8]._thickness_by_scenario[1]);
        assertEquals(8.46, reread._surfaces[8]._thickness_by_scenario[0]);
    }

    private static String[] row(String text, String name) {
        for (String line : text.split("\\r?\\n"))
            if (line.startsWith(name + "\t"))
                return line.split("\t", -1);
        throw new AssertionError("no row " + name);
    }

    private static void assertRejected(String trialText, String message) throws Exception {
        String text = withTrials(SUMMICRON, trialText);
        var e = assertThrows(OptimizationTrial.TrialException.class, () -> {
            var trial = OptimizationTrial.parse(text, 1);
            trial.builder(prescription(text, trial)).build();
        });
        assertTrue(e.getMessage().contains(message), e.getMessage());
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
        assertRejected("[trial 1]\n" + MTF_FIVE_FIELDS + "vary curvatures 12\n", "there is no surface '12' in [lens data]");
        assertRejected("[trial 1]\n" + MTF_FIVE_FIELDS + "vary curvatures 6\n", "surface 6 is a stop");
        assertRejected("[trial 1]\n" + MTF_FIVE_FIELDS + "vary aspherics 1 K:10\n", "K takes no scale");
        assertRejected("[trial 1]\n" + MTF_FIVE_FIELDS + "vary aspherics 1 A5\n", "A5 is not a term of an even asphere");
        assertRejected("[trial 1]\nfields 0 0.5\nfrequencies 20\ngoal contrast sag 1 1\n",
                "contrast settings need a 'goal contrast <frequencies>' line");
        assertRejected("[trial 1]\nfields 0 0.5\nfrequencies 20\nvary thicknesses Bf\ngoal contrast 20\n"
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
