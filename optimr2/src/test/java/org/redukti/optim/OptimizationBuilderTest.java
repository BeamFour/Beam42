package org.redukti.optim;

import org.junit.jupiter.api.Test;
import org.redukti.rayoptics.seq.Glass;
import org.redukti.spec.Prescription;
import org.redukti.spec.SurfaceType;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OptimizationBuilderTest {

    private static Prescription prescription() {
        return new Prescription(50.0, 1.4, 40.0, 43.28,
                new double[]{Glass.d, Glass.F, Glass.C},
                new double[]{1.0, 0.5, 0.25})
                .surf(50.0, 5.0, 30.0, 1.5, 50.0)
                .asph(SurfaceType.ASPH_EVEN, -0.5,
                        new double[]{0.0, 2.5e-6, 0.0, -4.0e-10})
                .surf(-50.0, 20.0, 30.0)
                .build();
    }

    @Test
    void buildsVariablesAndCurveOrientedMtfGoals() {
        var setup = OptimizationBuilder.builder(prescription())
                .fields(0.0, 1.0)
                .mtfFrequencies(10, 20)
                .curvatureSurfaces(0, 1)
                .thicknessSurfaces(1)
                .includeExistingAspherics(true)
                .mtfGoals(OptimizationBuilder.mtf(10,
                        new double[]{90, 80}, new double[]{85, 65},
                        new double[]{2.0, 0.5}))
                .build();

        Var[] variables = setup.variables();
        assertEquals(6, variables.length);
        assertInstanceOf(VarRadius.class, variables[0]);
        assertInstanceOf(VarThickness.class, variables[2]);
        assertInstanceOf(VarAsphK.class, variables[3]);
        assertInstanceOf(VarAsphCoeff.class, variables[4]);
        assertEquals(1.0e6, ((VarAsphCoeff) variables[4])._scaling_factor);

        Goal[] goals = setup.goals();
        assertEquals(126, goals.length);
        assertMtf(goals[0], 10, 1, OptimizationBuilder.SAGITTAL, 0.90, 2.0);
        assertMtf(goals[1], 10, 1, OptimizationBuilder.TANGENTIAL, 0.85, 2.0);
        assertMtf(goals[2], 10, 2, OptimizationBuilder.SAGITTAL, 0.80, 0.5);
        assertMtf(goals[3], 10, 2, OptimizationBuilder.TANGENTIAL, 0.65, 0.5);

        GoalParax focalLength = (GoalParax) goals[4];
        GoalParax fNumber = (GoalParax) goals[5];
        assertEquals(ParaxHelper.Effective_focal_length, focalLength._parax_id);
        assertEquals(50.0, focalLength._target);
        assertEquals(ParaxHelper.Fno, fNumber._parax_id);
        assertEquals(1.4, fNumber._target);

        double[] rayWeights = Arrays.stream(goals)
                .filter(GoalRayAberration.class::isInstance)
                .mapToDouble(goal -> goal._weight)
                .distinct()
                .sorted()
                .toArray();
        assertArrayEquals(new double[]{0.25, 0.5, 1.0}, rayWeights);
    }

    @Test
    void supportsDLineOnlyAndUnweightedGoals() {
        var setup = OptimizationBuilder.builder(prescription())
                .fields(0.0, 1.0)
                .mtfFrequencies(10)
                .weighted(false)
                .dLineOnly(true)
                .build();

        assertEquals(42, setup.goals().length); // two paraxial + 2 fields * 2 fans * 10 samples
        Arrays.stream(setup.goals())
                .filter(GoalRayAberration.class::isInstance)
                .map(GoalRayAberration.class::cast)
                .forEach(goal -> {
                    assertEquals(Glass.d, goal._wvl);
                    assertEquals(1.0, goal._weight);
                });
    }

    @Test
    void validatesMtfArrayLengthsAndMeasuredFrequencies() {
        assertThrows(IllegalArgumentException.class, () ->
                OptimizationBuilder.builder(prescription())
                        .fields(0.0, 1.0)
                        .mtfFrequencies(10)
                        .mtfGoals(OptimizationBuilder.mtf(20,
                                new double[]{90, 80}, new double[]{90, 80}))
                        .build());

        assertThrows(IllegalArgumentException.class, () ->
                OptimizationBuilder.builder(prescription())
                        .fields(0.0, 1.0)
                        .mtfFrequencies(10)
                        .mtfGoals(OptimizationBuilder.mtf(10,
                                new double[]{90}, new double[]{90, 80}))
                        .build());
    }

    private static void assertMtf(Goal goal, int frequency, int field, int orientation,
                                  double target, double weight) {
        GeoMTF mtf = assertInstanceOf(GeoMTF.class, goal);
        assertEquals(frequency, mtf._freq);
        assertEquals(field, mtf._field);
        assertEquals(orientation, mtf._xy);
        assertEquals(target, mtf._target);
        assertEquals(weight, mtf._weight);
    }
}
