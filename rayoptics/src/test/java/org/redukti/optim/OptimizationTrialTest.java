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
import static org.redukti.optim.SetupAssertions.assertSameSetup;

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

    private static OptimizationBuilder read(String text) throws Exception {
        return OptimizationTrial.read(text, 1, true);
    }

    private static Prescription prescription(String text, boolean weighted, boolean dLineOnly) throws Exception {
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_buffer(text);
        return Prescription.build_prescription(specs, true, weighted, dLineOnly);
    }

    private static List<String> names(Var[] variables) {
        return Arrays.stream(variables).map(OptimizationTrial::describe).toList();
    }

    private static final String MTF_FIVE_FIELDS = """
            fields        0 to 1 step 0.25
            frequencies   20
            goal mtf      20 sag   50 50 50 50 50
            goal mtf      20 tan   50 50 50 50 50
            """;

    @Test
    void reusesParsedDefinitionAndRoundTripsItsSettings() throws Exception {
        String text = withTrials(SUMMICRON, "[trial 1]\n" + MTF_FIVE_FIELDS
                + "vary thicknesses 10\nweighted no\nd-line-only yes\n");
        var definition = OptimizationTrial.parse(text, 1);
        var first = definition.createBuilder(text, true);
        String canonical = definition.toTrial(first.prescription());
        assertEquals(canonical, definition.toTrial());
        assertEquals(first.toTrial(1), canonical);
        assertEquals(1, definition.number());
        assertTrue(canonical.contains("weighted"));
        assertSameSetup(read(text).build(), first.build());

        // A stage hands on its changed design, without needing to carry trial text
        // for the already-parsed definition to construct a fresh stage.
        first.prescription()._surfaces[10]._thickness += 0.25;
        String design = first.prescription().to_opt_bench_str(new StringBuilder()).toString();
        var next = definition.createBuilder(design, true);
        assertNotSame(first.prescription(), next.prescription());
        assertEquals(first.prescription()._surfaces[10]._thickness,
                next.prescription()._surfaces[10]._thickness, 1e-8);
        assertEquals(canonical, definition.toTrial(next.prescription()));
        assertArrayEquals(first.prescription()._wvls, next.prescription()._wvls);

        var reread = OptimizationTrial.parse(design + "\n" + canonical, 1);
        assertEquals(canonical, reread.toTrial(next.prescription()));
        assertSameSetup(next.build(), reread.createBuilder(design, true).build());
        // Changing one returned builder must not change the reusable definition.
        next.fields(0.0);
        next.contrastGoals(OptimizationBuilder.contrast(30, new double[]{1.0}));
        next.varyCurvatures(0);
        next.contrastBalanceGoals(new boolean[]{true});
        assertEquals(canonical, definition.toTrial(first.prescription()));
        assertEquals(canonical, definition.toTrial());
        assertEquals(canonical, definition.createBuilder(design, true).toTrial(1));
    }

    @Test
    void effectiveAnalysesAndSamplingSurviveRoundTrip() throws Exception {
        record Case(String goals, boolean spots, boolean rays, boolean mtf, boolean hexapolar) {}
        var cases = List.of(
                new Case("", false, false, false, false),
                new Case("goal spot-rms 10\n", true, false, false, false),
                new Case("goal spot-max-radius 10\n", true, false, false, true),
                new Case("goal spot-deviation 1\n", true, false, false, false),
                new Case("goal mtf 20 sag 50\ngoal mtf 20 tan 50\n", true, false, true, false),
                new Case("goal ray-aberrations yes\n", false, true, false, false),
                new Case("goal contrast 20\n", false, false, false, false),
                new Case("goal spot sampling hexapolar 32\n", false, false, false, true));
        for (var c : cases) {
            String text = withTrials(SUMMICRON, "[trial 1]\nfields 0\nfrequencies 20\n" + c.goals());
            var builder = read(text);
            var setup = builder.build();
            var analysis = setup.analysis();
            assertEquals(c.spots(), analysis._compute_spots, c.goals());
            assertEquals(c.rays(), analysis._compute_ray_aberrations, c.goals());
            assertEquals(c.mtf(), analysis._compute_mtf, c.goals());
            assertEquals(c.hexapolar()
                            ? org.redukti.rayoptics.analysis.SpotOptions.PATTERN_HEXAPOLAR
                            : org.redukti.rayoptics.analysis.SpotOptions.PATTERN_GAUSS_QUADRATURE,
                    analysis._spot_pattern, c.goals());
            String written = builder.toTrial(1);
            assertEquals(written, OptimizationTrial.parse(text, 1).toTrial());
            String normalized = written.replaceAll("\\s+", " ");
            assertEquals(c.hexapolar(), normalized.contains("sampling hexapolar"), c.goals());
            assertEquals(c.spots() && !c.hexapolar(), normalized.contains("sampling gaussian"), c.goals());
            var restored = read(withTrials(SUMMICRON, written));
            assertEquals(written, restored.toTrial(1));
            assertSameSetup(setup, restored.build());
        }
    }

    @Test
    void numbersSurfacesByPosition() throws Exception {
        String text = withTrials(SUMMICRON, "[trial 1]\n" + MTF_FIVE_FIELDS + """
                vary curvatures   all except 2 6
                vary thicknesses  10 4
                """);
        var variables = read(text).build().variables();
        // Surfaces 3, 7 and 9 are flat and 5 is the stop, so "all" leaves them out without
        // being told.
        assertEquals(List.of("surface 0 radius", "surface 1 radius", "surface 4 radius", "surface 8 radius",
                "surface 10 radius", "surface 10 thickness", "surface 4 thickness"), names(variables));
    }

    @Test
    void rejectsFractionalCurvatureConstraintsOnFlatSurfaces() throws Exception {
        for (String settings : new String[]{
                "vary curvatures 3\nconstrain curvatures\n",
                "constrain curvatures\nvary curvatures 3\n"}) {
            String text = withTrials(SUMMICRON, "[trial 1]\n" + MTF_FIVE_FIELDS + settings);
            int constraintLine = Arrays.asList(text.split("\\r?\\n")).indexOf("constrain curvatures") + 1;
            var error = assertThrows(OptimizationTrial.TrialException.class, () -> read(text));
            assertTrue(error.getMessage().contains("line " + constraintLine + ":"), error.getMessage());
            assertTrue(error.getMessage().contains("flat surface 3"), error.getMessage());
        }
        // Listing a flat surface without a fractional constraint remains supported.
        var variables = read(withTrials(SUMMICRON,
                "[trial 1]\n" + MTF_FIVE_FIELDS + "vary curvatures 3\n")).build().variables();
        assertEquals(List.of("surface 3 radius"), names(variables));
    }

    @Test
    void ignoresTheIdsInTheFile() throws Exception {
        // Position 6 is the stop, although the row with id 6 is an ordinary surface.
        assertRejected(FD300, "[trial 1]\n" + MTF_FIVE_FIELDS + "vary curvatures 6\n", "surface 6 is a stop");
        assertRejected(FD300, "[trial 1]\n" + MTF_FIVE_FIELDS + "vary thicknesses 6AS\n",
                "expected a surface number, found '6AS'");

        var builder = read(withTrials(FD300, "[trial 1]\n" + MTF_FIVE_FIELDS + """
                vary curvatures   5 7
                vary thicknesses  6 9
                """));
        assertEquals(List.of("surface 5 radius", "surface 7 radius", "surface 6 thickness", "surface 9 thickness"),
                names(builder.build().variables()));
        // Surface 5 is the row with id 6, and surface 6 the stop, whose gap is 30.10.
        assertEquals(-524.3616, builder.prescription()._surfaces[5]._radius);
        assertEquals(30.10, builder.prescription()._surfaces[6]._thickness);
    }

    @Test
    void fieldShorthandGivesTheDecimalValues() throws Exception {
        var setup = read(withTrials(SUMMICRON, """
                [trial 1]
                fields        0 to 1 step 0.1
                frequencies   20
                vary thicknesses 10
                """)).build();
        assertArrayEquals(new double[]{0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0},
                setup.analysis()._fields);
    }

    @Test
    void focalLengthGoalReplacesTheAutomaticOne() throws Exception {
        var goals = read(withTrials(SUMMICRON, """
                [trial 1]
                fields        0
                frequencies   20
                vary thicknesses 10
                goal paraxial efl 42 weight 2
                goal paraxial bfl 30
                """)).build().goals();
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
    void makesASphereAsphericWithScaledCoefficients() throws Exception {
        var builder = read(withTrials(SUMMICRON, "[trial 1]\n" + MTF_FIVE_FIELDS + """
                vary aspherics 0 K 1 2:1e9
                """));
        var variables = builder.build().variables();
        SurfaceType surface = builder.prescription()._surfaces[0];
        assertEquals(SurfaceType.ASPH_EVEN, surface._asph_type);
        assertEquals(3, surface._coeffs.length);
        assertEquals(List.of("surface 0 K", "surface 0 coefficient 1", "surface 0 coefficient 2"), names(variables));
        var a4 = (VarAsphCoeff) variables[1];
        var a6 = (VarAsphCoeff) variables[2];
        // Coefficient 1 of an even asphere is A4. Half the 28.94 diameter, to the fourth
        // power, is 43800: nearest decade 1e5.
        assertEquals(1, a4._index);
        assertEquals(1e5, a4._scaling_factor);
        assertEquals(2, a6._index);
        assertEquals(1e9, a6._scaling_factor);
    }

    /** A trial using every setting, in the form the builder writes it back. */
    private static final String EVERYTHING = """
            [trial 1]
            description           Everything a trial can say
            outdir                trials/one
            configuration         0
            fields                0 0.25 0.5 0.75 1
            frequencies           10 20
            weighted              no
            d-line-only           yes
            vignetting            set-vig frozen
            check-spot-apertures  no
            vary curvatures       all except 2 6
            vary thicknesses      0 10
            vary aspherics        existing
            vary aspherics        0 K 1:100000 2
            constrain curvatures  2
            constrain thicknesses 1
            constrain edges       0.5
            goal contrast         10 20
            goal contrast         sag 3 3 2 2 1
            goal contrast         20 tan 1 1 1 0.5 0.5
            goal contrast         balance all except 0.25 weight 0.4
            goal contrast         sampling 4 8
            goal contrast         calibrate yes
            goal contrast         exit-pupil-aiming no
            goal contrast         centering no
            goal mtf              10 sag 90 85 80 70 60
            goal mtf              10 tan 90 85 80 70 60
            goal mtf              10 tan weights 1 1 2 2 4
            goal spot-rms         10 20 30 40 50
            goal spot sampling    gaussian 6 12 0.2
            goal spot sampling    hexapolar 32
            goal ray-aberrations  yes
            goal paraxial         efl 51 weight 2
            goal paraxial         bfl 37
            """;

    @Test
    void readsAndWritesTheSameTrial() throws Exception {
        // Written differently - shorthand, other spacing, other order, defaults spelt out -
        // and read, the trial is written back in the builder's form. Goals of one kind keep
        // the order they were given in, since that is the order of the residuals.
        String loose = """
                [trial 1]
                goal paraxial efl 51 weight 2
                goal paraxial bfl 37
                description   Everything a trial can say   # a comment is not kept
                outdir  trials/one
                fields 0 to 1 step 0.25
                frequencies 10 20
                weighted no
                d-line-only yes
                vignetting set-vig frozen
                check-spot-apertures no
                vary curvatures all except 2 6
                vary thicknesses 0 10
                vary aspherics existing
                vary aspherics 0 K 1:1e5 2
                constrain curvatures 2
                constrain thicknesses
                constrain edges 0.5
                goal contrast 10 20
                goal contrast sag 3 3 2 2 1
                goal contrast 20 tan 1 1 1 0.5 0.5
                goal contrast balance all except 0.25 weight 0.4
                goal contrast sampling 4 8
                goal contrast calibrate yes
                goal contrast centering no
                goal mtf 10 sag 90 85 80 70 60
                goal mtf 10 tan 90 85 80 70 60
                goal mtf 10 tan weights 1 1 2 2 4
                goal spot-rms 10 20 30 40 50
                goal spot sampling gaussian 6 12 0.2
                goal spot sampling hexapolar 32
                goal ray-aberrations yes
                """;
        var builder = read(withTrials(SUMMICRON, loose));
        assertEquals(EVERYTHING, builder.toTrial(1));
        // Parsing and canonical writing need no prescription or solver construction.
        var definition = OptimizationTrial.parse(withTrials(SUMMICRON, loose), 1);
        assertEquals(EVERYTHING, definition.toTrial());
        assertEquals(EVERYTHING, OptimizationTrial.parse(
                withTrials(SUMMICRON, definition.toTrial()), 1).toTrial());

        // Reading what was written gives the same setup, and writes the same text again.
        var again = read(withTrials(SUMMICRON, EVERYTHING));
        assertEquals(EVERYTHING, again.toTrial(1));
        assertSameSetup(builder.build(), again.build());
    }

    @Test
    void writesASetupBuiltInCode() throws Exception {
        String lens = withTrials(SUMMICRON, "");
        var builder = OptimizationBuilder.builder(prescription(lens, true, false))
                .fields(0.0, 0.5, 1.0)
                .mtfFrequencies(10)
                .varyThicknesses(10, 4)
                .varyAsphericCoefficient(0, 1, 1e5)
                .spotDeviationGoals(new double[]{1, 2, 3}, new double[]{1, 1, 1})
                .gaussianQuadratureSampling(3, 6)
                .paraxialGoal(ParaxHelper.Back_focal_length, 37.3);
        String written = builder.toTrial(7);
        assertEquals("""
                [trial 7]
                configuration         0
                fields                0 0.5 1
                frequencies           10
                weighted              yes
                d-line-only           no
                vignetting            set-pupil
                check-spot-apertures  yes
                vary thicknesses      10 4
                vary aspherics        0 1:100000
                goal spot-deviation   x 1 2 3
                goal spot-deviation   y 1 1 1
                goal spot sampling    gaussian 3 6
                goal ray-aberrations  no
                goal paraxial         bfl 37.3
                """, written);
        var reread = OptimizationTrial.read(lens + written, 7, true);
        assertEquals(written, reread.toTrial(7));
        assertSameSetup(builder.build(), reread.build());
    }

    @Test
    void cannotWriteVariablesOrGoalsGivenAsCode() throws Exception {
        var prescription = prescription(withTrials(SUMMICRON, ""), true, false);
        var builder = OptimizationBuilder.builder(prescription)
                .fields(0.0)
                .mtfFrequencies(10)
                .additionalGoals(analysis -> new GoalParax(analysis, ParaxHelper.Back_focal_length, 37.3, 1.0));
        var e = assertThrows(IllegalStateException.class, () -> builder.toTrial(1));
        assertTrue(e.getMessage().contains("added as code"), e.getMessage());
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

    /** What LensTool2 saves: the prescription as Beam42 writes it, then the trial. */
    private static String optimized(OptimizationBuilder builder) {
        return builder.prescription().to_opt_bench_str(new StringBuilder()).append('\n')
                .append(builder.toTrial(1)).toString();
    }

    @Test
    void thePrescriptionAndTrialReadBackAsOptimized() throws Exception {
        var builder = read(withTrials(SUMMICRON, "[trial 1]\n" + MTF_FIVE_FIELDS + """
                vary curvatures   0 2
                vary thicknesses  0 10
                vary aspherics    0 K 1
                """));
        move(builder.build().variables());
        String written = optimized(builder);

        // Reads back as the optimized prescription, exactly, and the trial carried over
        // refers to the same surfaces.
        var again = read(written);
        var prescription = builder.prescription();
        var reread = again.prescription();
        for (int i = 0; i < prescription._surfaces.length; i++) {
            SurfaceType expected = prescription._surfaces[i];
            SurfaceType actual = reread._surfaces[i];
            assertEquals(expected._radius, actual._radius, "radius of surface " + i);
            assertEquals(expected._thickness, actual._thickness, "thickness of surface " + i);
            assertEquals(expected._k, actual._k, "conic of surface " + i);
            assertEquals(expected._asph_type, actual._asph_type, "asphere type of surface " + i);
            assertArrayEquals(expected._coeffs, actual._coeffs, "coefficients of surface " + i);
        }
        assertEquals(List.of("surface 0 radius", "surface 2 radius", "surface 0 thickness", "surface 10 thickness",
                "surface 0 K", "surface 0 coefficient 1"), names(again.build().variables()));
    }

    @Test
    void writesAZoomWithItsConfiguredScenariosOnly() throws Exception {
        var builder = read(withTrials(ZOOM, """
                [trial 1]
                configuration     1
                fields            0
                frequencies       10
                vary thicknesses  8 10
                goal mtf          10 sag 50
                goal mtf          10 tan 50
                """));
        for (Var variable : builder.build().variables()) {
            variable.read_from_prescription();
            variable.set_unscaled_value(variable.get_unscaled_value() + 0.5);
            variable.write_to_prescription();
        }
        String written = optimized(builder);

        // The 120mm scenario is not configured, so it is not written; the configurations
        // are renumbered from 0 in the same order.
        assertFalse(written.contains("120.07"), written);
        assertTrue(written.contains("scenarios\t0\t1\n"), written);
        var again = read(written);
        var reread = again.prescription();
        assertEquals(13.64, reread._surfaces[8]._thickness_by_scenario[1], 1e-12);
        assertEquals(8.46, reread._surfaces[8]._thickness_by_scenario[0]);
        assertEquals(20.31, reread._surfaces[10]._thickness_by_scenario[0]);
        // The trial carried over still means configuration 1 and the same surfaces.
        var moved = again.build().variables();
        assertEquals(1, ((VarThickness) moved[0])._scenario);
        assertEquals(8, ((VarThickness) moved[0])._surface_id);
    }

    private static void assertRejected(String example, String trialText, String message) throws Exception {
        String text = withTrials(example, trialText);
        var e = assertThrows(OptimizationTrial.TrialException.class, () -> read(text).build());
        assertTrue(e.getMessage().contains(message), e.getMessage());
    }

    private static void assertRejected(String trialText, String message) throws Exception {
        assertRejected(SUMMICRON, trialText, message);
    }

    @Test
    void reportsProblemsWithTheirLine() throws Exception {
        String text = withTrials(SUMMICRON, "[trial 1]\nfields 0\nbogus 1\n");
        int line = List.of(text.split("\n", -1)).indexOf("bogus 1") + 1;
        var e = assertThrows(OptimizationTrial.TrialException.class, () -> read(text));
        assertEquals("trial 1, line " + line + ": unknown keyword 'bogus'", e.getMessage());

        // A problem only the builder can see, against the prescription, still names its line.
        String aspheric = withTrials(SUMMICRON, "[trial 1]\n" + MTF_FIVE_FIELDS + "vary aspherics 0 0\n");
        int asphericLine = List.of(aspheric.split("\n", -1)).indexOf("vary aspherics 0 0") + 1;
        var a2 = assertThrows(OptimizationTrial.TrialException.class, () -> read(aspheric));
        assertEquals("trial 1, line " + asphericLine + ": coefficient 0 is not a term of an even asphere, "
                + "whose terms start at index 1, the A4 term", a2.getMessage());
    }

    @Test
    void sharedDomainRulesRejectBothEntryPointsWithSourceContext() throws Exception {
        record Invalid(String rows, String offendingRow,
                       java.util.function.Consumer<OptimizationBuilder> configure) {}
        var invalid = List.of(
                new Invalid("goal spot-rms -1\n", "goal spot-rms -1",
                        b -> b.spotRmsGoals(new double[]{-1})),
                new Invalid("goal spot-deviation -1\n", "goal spot-deviation -1",
                        b -> b.spotDeviationGoals(-1)),
                new Invalid("goal spot-rms 1 2\n", "goal spot-rms 1 2",
                        b -> b.spotRmsGoals(new double[]{1, 2})),
                new Invalid("goal mtf 20 sag 101\ngoal mtf 20 tan 50\n", "goal mtf 20 sag 101",
                        b -> b.mtfGoals(OptimizationBuilder.mtf(20, new double[]{101}, new double[]{50}))),
                new Invalid("goal mtf 30 sag 50\ngoal mtf 30 tan 50\n", "goal mtf 30 sag 50",
                        b -> b.mtfGoals(OptimizationBuilder.mtf(30, new double[]{50}, new double[]{50}))),
                new Invalid("goal contrast 20 20\n", "goal contrast 20 20",
                        b -> b.contrastGoals(OptimizationBuilder.contrast(20, new double[]{1}),
                                OptimizationBuilder.contrast(20, new double[]{1}))));
        String lens = withTrials(SUMMICRON, "");
        for (var c : invalid) {
            var builder = OptimizationBuilder.builder(prescription(lens, true, false))
                    .fields(0.0).mtfFrequencies(20);
            c.configure().accept(builder);
            assertThrows(IllegalArgumentException.class, builder::build, c.rows());
            String text = lens + "\n[trial 1]\nfields 0\nfrequencies 20\n" + c.rows();
            var error = assertThrows(OptimizationTrial.TrialException.class,
                    () -> OptimizationTrial.parse(text, 1), c.rows());
            int line = List.of(text.split("\n", -1)).indexOf(c.offendingRow()) + 1;
            assertTrue(error.getMessage().startsWith("trial 1, line " + line + ":"), error.getMessage());
        }
    }

    @Test
    void writingAnInvalidBuilderPreservesItsHexapolarSetting() throws Exception {
        String lens = withTrials(SUMMICRON, "");
        var builder = OptimizationBuilder.builder(prescription(lens, true, false))
                .fields(0.0).mtfFrequencies(20).spotDeviationGoals(1).hexapolarSampling(32);
        assertThrows(IllegalArgumentException.class, builder::build);
        String written = builder.toTrial(1);
        assertTrue(written.replaceAll("\\s+", " ").contains("goal spot sampling hexapolar 32"), written);
        assertThrows(OptimizationTrial.TrialException.class,
                () -> OptimizationTrial.parse(lens + "\n" + written, 1));
    }

    @Test
    void incompatibleSpotSamplingReportsTheConflictingLineInEitherOrder() throws Exception {
        for (String deviation : List.of("goal spot-deviation 1",
                "goal spot-deviation x 1\ngoal spot-deviation y 1")) {
            for (String conflict : List.of("goal spot sampling hexapolar 32", "goal spot-max-radius 10")) {
                for (boolean reverse : List.of(false, true)) {
                    String rows = reverse ? conflict + "\n" + deviation : deviation + "\n" + conflict;
                    String text = withTrials(SUMMICRON, "[trial 1]\nfields 0\nfrequencies 20\n" + rows);
                    int line = text.split("\n", -1).length;
                    var error = assertThrows(OptimizationTrial.TrialException.class,
                            () -> OptimizationTrial.parse(text, 1));
                    assertEquals("trial 1, line " + line
                            + ": spot deviation goals require Gaussian-quadrature spot sampling", error.getMessage());
                }
            }
        }
    }

    @Test
    void trialSamplingMatchesTheAvailableTracerPatterns() throws Exception {
        String prefix = "[trial 1]\nfields 0\nfrequencies 20\n";
        String lens = withTrials(SUMMICRON, "");
        for (int spokes : new int[]{1, 2}) {
            String text = lens + prefix + "goal contrast 20\ngoal contrast sampling 1 " + spokes;
            var error = assertThrows(OptimizationTrial.TrialException.class,
                    () -> OptimizationTrial.parse(text, 1));
            assertTrue(error.getMessage().contains("line " + text.split("\n", -1).length + ":"));
            assertThrows(IllegalArgumentException.class,
                    () -> OptimizationBuilder.builder(prescription(lens, true, false)).contrastSampling(1, spokes));
            assertThrows(IllegalArgumentException.class,
                    () -> new org.redukti.rayoptics.analysis.ContrastOptions(20).num_spokes(spokes));
        }
        assertThrows(OptimizationTrial.TrialException.class,
                () -> OptimizationTrial.parse(lens + prefix + "goal spot sampling grid 9\n", 1));
        // Independent spot and contrast settings are valid together, at the lower bound.
        String text = lens + prefix + "goal spot-rms 10\ngoal spot sampling hexapolar 2\n"
                + "goal contrast 20\ngoal contrast sampling 1 3\n";
        var builder = read(text);
        var setup = builder.build();
        setup.analysis().compute();
        assertNotNull(setup.analysis()._spots);
        assertEquals(3, setup.analysis()._contrasts[0].fields.get(0).wavelengths().get(0).samples().size());
        assertEquals(org.redukti.rayoptics.analysis.SpotOptions.PATTERN_HEXAPOLAR,
                setup.analysis()._spot_pattern);
        var restored = read(lens + builder.toTrial(1));
        assertSameSetup(builder.build(), restored.build());
        assertEquals(builder.toTrial(1), restored.toTrial(1));
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
        assertRejected("[trial 1]\n" + MTF_FIVE_FIELDS + "vary aspherics 0 A4\n",
                "expected K or a coefficient index, found 'A4'");
        assertRejected("[trial 1]\n" + MTF_FIVE_FIELDS + "vary aspherics 5 K\n", "surface 5 is a stop; it cannot be aspheric");
        assertRejected("[trial 1]\nfields 0 0.5\nfrequencies 20\ngoal contrast sag 1 1\n",
                "contrast settings need a 'goal contrast <frequencies>' line");
        assertRejected("[trial 1]\nfields 0 0.5\nfrequencies 20\nvary thicknesses 10\ngoal contrast 20\n"
                + "goal contrast balance all except 0.3\n", "there is no field 0.3 in 'fields'");
        assertRejected("[trial 1]\nfields 0 0.5\nfrequencies 20\ngoal spot-deviation 1 1\ngoal spot-deviation x 1 1\n",
                "either as one row or as x and y rows");
        assertRejected("[trial 1]\nfields 0\nfrequencies 20\ngoal paraxial focus 3\n", "unknown paraxial quantity 'focus'");
    }

    @Test
    void namesTheTrialsAFileDefines() throws Exception {
        String text = withTrials(SUMMICRON, "[trial 1]\nfields 0\nfrequencies 20\n[trial 4]\nfields 0\nfrequencies 20\n");
        var e = assertThrows(OptimizationTrial.TrialException.class, () -> OptimizationTrial.read(text, 2, true));
        assertEquals("there is no [trial 2] in this prescription; it defines trials 1, 4", e.getMessage());
        // The prescription reader is unaffected by the trials.
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_buffer(text);
        assertEquals(11, specs.get_surfaces().size());
    }
}
