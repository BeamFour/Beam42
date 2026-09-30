// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.optim.*;
import org.redukti.spec.Prescription;
import org.redukti.util.Args;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class AsphericOptimizationTest {
    private static final String LENS = """
            [descriptive data]
            title	Mixed asphere regression
            [constants]
            AsphericalOddCount	1
            [variable distances]
            Focal Length	50.85
            F-Number	5
            Angle of View	10
            [lens data]
            1	50	5	1.5	12	50
            2	-50	48		12
            [aspherical data]
            1	50	0.1	1e-6	2e-7	3e-9	4e-11	5e-13	6e-15
            """;

    private static Prescription prescription(String text) throws Exception {
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_buffer(text);
        return Prescription.build_prescription(specs, false);
    }

    private static int[] indices(Var[] variables) {
        return Arrays.stream(variables).filter(VarAsphCoeff.class::isInstance)
                .mapToInt(v -> ((VarAsphCoeff) v)._index).toArray();
    }

    @Test void existingMeansNonzeroNormalizedTermsAtBuildTime() throws Exception {
        var p = prescription(LENS);
        var builder = OptimizationBuilder.builder(p).fields(0).mtfFrequencies(20)
                .varyExistingAspherics().rayAberrationGoals();
        var variables = builder.build().variables();
        assertEquals(7, variables.length);
        assertInstanceOf(VarAsphK.class, variables[0]);
        assertArrayEquals(new int[]{2, 3, 5, 7, 9, 11}, indices(variables));

        // A supplied zero and an omitted power are both excluded, as is K=0.
        p._surfaces[0]._coeffs[5] = 0;
        p._surfaces[0]._k = 0;
        var rebuilt = builder.build().variables();
        assertArrayEquals(new int[]{2, 3, 7, 9, 11}, indices(rebuilt));
        assertEquals(5, rebuilt.length);
        assertArrayEquals(new int[]{2, 3, 5, 7, 9, 11}, indices(variables));
    }

    @Test void coefficientVariableChangesA6AndAnalysisRebuildsTheCorrectSag() throws Exception {
        var p = prescription(LENS);
        var setup = OptimizationBuilder.builder(p).fields(0).mtfFrequencies(20).varyAsphericCoefficient(0, 5)
                .rayAberrationGoals().build();
        var variable = (VarAsphCoeff) setup.variables()[0];
        assertEquals(1e9, variable.get_scaling_factor());
        assertEquals(3, variable.read_from_prescription(), 1e-14);
        setup.analysis().compute();
        double before = setup.analysis()._opt_model.seq_model.ifcs.get(1).profile.sag(0, 2);
        double[] expected = p._surfaces[0]._coeffs.clone();
        variable.set_scaled_value(4);
        variable.write_to_prescription();
        expected[5] = 4e-9;
        assertArrayEquals(expected, p._surfaces[0]._coeffs, 1e-24);
        setup.analysis().compute();
        double after = setup.analysis()._opt_model.seq_model.ifcs.get(1).profile.sag(0, 2);
        assertEquals(1e-9 * Math.pow(2, 6), after - before, 1e-16);
    }

    @Test void explicitTermsOverrideExistingAndCanIntroduceAnOmittedOddPower() throws Exception {
        String text = LENS + "\n[trial 1]\nfields 0\nfrequencies 20\nvary aspherics existing\nvary aspherics 0 4\n"
                + "goal spot-rms 1\n";
        var builder = OptimizationTrial.read(text, 1, false);
        var setup = builder.build();
        assertEquals(1, setup.variables().length);
        var variable = (VarAsphCoeff) setup.variables()[0];
        assertEquals(4, variable._index); // A5, not the fifth input column
        assertEquals(1e4, variable.get_scaling_factor()); // round(log10(6^5))
        variable.set_scaled_value(0.01);
        variable.write_to_prescription();
        String saved = builder.prescription().to_opt_bench_str(new StringBuilder())
                .append('\n').append(builder.toTrial(1)).toString();
        var restored = OptimizationTrial.read(saved, 1, false);
        assertEquals(2, restored.prescription()._aspherical_odd_count);
        assertArrayEquals(builder.prescription()._surfaces[0]._coeffs,
                restored.prescription()._surfaces[0]._coeffs);
        assertArrayEquals(new int[]{4}, indices(restored.build().variables()));
    }

    @Test void lensToolTrialsAndPipelinesPreserveCoefficientPowers(@TempDir Path dir) throws Exception {
        String text = LENS + """

                [trial 1]
                fields 0
                frequencies 20
                vary aspherics 0 5
                goal spot-rms 1
                goal spot sampling hexapolar 2
                solver max-evaluations 4
                [pipeline 2]
                trials 1 1
                """;
        double[] initial = prescription(LENS)._surfaces[0]._coeffs;
        for (int number : new int[]{1, 2}) {
            Path input = dir.resolve("mixed" + number + ".txt");
            Files.writeString(input, text);
            var args = Args.parseArguments(new String[]{"--specfile", input.toString(),
                    "--optimize", Integer.toString(number)});
            String output = LensTool2.runOptimizationTrial(text, args);
            assertEquals(output, Files.readString(Path.of(args.specfile)));
            var restored = prescription(output);
            assertEquals(1, restored._aspherical_odd_count);
            double[] actual = restored._surfaces[0]._coeffs;
            assertEquals(initial.length, actual.length);
            assertTrue(Double.isFinite(actual[5]));
            assertNotEquals(initial[5], actual[5]);
            for (int i = 0; i < initial.length; i++)
                if (i != 5) assertEquals(initial[i], actual[i], "coefficient index " + i);
            assertArrayEquals(new int[]{5}, indices(OptimizationTrial.read(output, 1, false).build().variables()));
            assertTrue(LensTool2.startREADME(restored).toString().contains(" P12 |"));
        }
    }
}
