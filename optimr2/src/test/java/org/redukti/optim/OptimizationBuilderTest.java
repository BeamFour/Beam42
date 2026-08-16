package org.redukti.optim;

import org.redukti.rayoptics.util.Orientation;
import org.junit.jupiter.api.Test;
import org.redukti.rayoptics.analysis.SpotOptions;
import org.redukti.rayoptics.seq.Glass;
import org.redukti.spec.Prescription;
import org.redukti.spec.VigType;
import org.redukti.spec.SurfaceType;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;

class OptimizationBuilderTest {

    private static Prescription prescription() {
        return new Prescription(50.0, 1.4, 40.0, 43.28,
                new double[]{Glass.d, Glass.F, Glass.C},
                new double[]{1.0, 0.5, 0.25})
                .surf(50.0, 5.0, 30.0, 1.5, 50.0)
                .asph(SurfaceType.ASPH_EVEN, -0.5,
                        new double[]{0.0, 2.5e-6, 0.0, -4.0e-10})
                .surf(-50.0, 20.0, 30.0)
                .stop(1.0, 20.0)
                .surf(0.0, 5.0, 30.0)
                .build();
    }

    @Test
    void buildsVariablesAndCurveOrientedMtfGoals() {
        var setup = OptimizationBuilder.builder(prescription())
                .fields(0.0, 1.0)
                .mtfFrequencies(10, 20)
                .varyCurvatures(0, 1)
                .varyThicknesses(1)
                .varyExistingAspherics()
                .rayAberrationGoals()
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
        assertMtf(goals[0], 10, 1, Orientation.SAGITTAL, 0.90, 2.0);
        assertMtf(goals[1], 10, 1, Orientation.TANGENTIAL, 0.85, 2.0);
        assertMtf(goals[2], 10, 2, Orientation.SAGITTAL, 0.80, 0.5);
        assertMtf(goals[3], 10, 2, Orientation.TANGENTIAL, 0.65, 0.5);

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
                .rayAberrationGoals()
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
    void rayAberrationGoalsAreOptionalAndDisabledByDefault() {
        var setup = OptimizationBuilder.builder(prescription())
                .fields(0.0, 1.0)
                .mtfFrequencies(10)
                .build();

        assertEquals(2, setup.goals().length, "only the paraxial anchors are automatic");
        assertFalse(Arrays.stream(setup.goals()).anyMatch(GoalRayAberration.class::isInstance));
        assertFalse(setup.analysis()._compute_ray_aberrations);
    }

    @Test
    void rejectsUnderdeterminedMeritInsteadOfAddingHiddenRayGoals() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                OptimizationBuilder.builder(prescription())
                        .fields(0.0)
                        .mtfFrequencies(10)
                        .varyCurvatures(0, 1)
                        .varyThicknesses(0, 1)
                        .build());

        assertTrue(exception.getMessage().contains("rayAberrationGoals()"));
    }

    @Test
    void buildsSpotGoalsWithOptionalFieldWeights() {
        var setup = OptimizationBuilder.builder(prescription())
                .fields(0.0, 1.0)
                .mtfFrequencies(10)
                .spotRmsGoals(new double[]{8.0, 20.0}, new double[]{3.0, 1.0})
                .spotMaxRadiusGoals(new double[]{15.0, 50.0})
                .build();

        Goal[] goals = setup.goals();
        GoalSpotRMS rms0 = assertInstanceOf(GoalSpotRMS.class, goals[0]);
        GoalSpotRMS rms1 = assertInstanceOf(GoalSpotRMS.class, goals[1]);
        GoalSpotMaxRadius max0 = assertInstanceOf(GoalSpotMaxRadius.class, goals[2]);
        GoalSpotMaxRadius max1 = assertInstanceOf(GoalSpotMaxRadius.class, goals[3]);
        assertEquals(8.0, rms0._target);
        assertEquals(3.0, rms0._weight);
        assertEquals(20.0, rms1._target);
        assertEquals(1.0, rms1._weight);
        assertEquals(15.0, max0._target);
        assertEquals(1.0, max0._weight);
        assertEquals(50.0, max1._target);
        assertEquals(1.0, max1._weight);
    }

    @Test
    void selectsAllCurvedNonStopSurfaces() {
        Var[] variables = OptimizationBuilder.builder(prescription())
                .fields(0.0)
                .mtfFrequencies(10)
                .varyAllCurvatures()
                .build()
                .variables();

        assertEquals(2, variables.length);
        assertEquals(0, ((VarRadius) variables[0])._surface_id);
        assertEquals(1, ((VarRadius) variables[1])._surface_id);
    }

    @Test
    void appendsUserSuppliedVariablesAndAnalysisBoundGoals() {
        Prescription prescription = prescription();
        Var customVariable = new VarThickness(prescription, 0);
        var setup = OptimizationBuilder.builder(prescription)
                .fields(0.0)
                .mtfFrequencies(10)
                .dLineOnly(true)
                .additionalVariables(customVariable)
                .additionalGoals(analysis -> new GoalSpotRMS(analysis, 1, 7.5, 4.0))
                .build();

        assertEquals(1, setup.variables().length);
        assertEquals(customVariable, setup.variables()[0]);
        Goal customGoal = setup.goals()[setup.goals().length - 1];
        GoalSpotRMS spotGoal = assertInstanceOf(GoalSpotRMS.class, customGoal);
        assertEquals(setup.analysis(), spotGoal._analysis);
        assertEquals(7.5, spotGoal._target);
        assertEquals(4.0, spotGoal._weight);
    }

    /**
     * SetPupil live is what every committed regression value was generated under. Both
     * halves of this matter: a changed default silently moves every golden number, and a
     * default that froze would quietly stop tracking the design.
     */
    @Test
    void vignettingDefaultsToLiveSetPupilAndIsConfigurable() {
        assertEquals(VigType.SetPupil, OptimizationBuilder.builder(prescription())
                .fields(0.0).mtfFrequencies(10).build().analysis()._vig_type);
        assertFalse(OptimizationBuilder.builder(prescription())
                .fields(0.0).mtfFrequencies(10).build().analysis()._freeze_vignetting);

        Analysis configured = OptimizationBuilder.builder(prescription())
                .fields(0.0)
                .mtfFrequencies(10)
                .vignetting(VigType.Paraxial)
                .freezeVignetting()
                .build()
                .analysis();
        assertEquals(VigType.Paraxial, configured._vig_type);
        assertTrue(configured._freeze_vignetting);
    }

    @Test
    void spotApertureCheckingDefaultsOnAndIsConfigurable() {
        Analysis defaults = OptimizationBuilder.builder(prescription())
                .fields(0.0)
                .mtfFrequencies(10)
                .build()
                .analysis();
        assertTrue(defaults._check_spot_apertures);

        Analysis fixedPupil = OptimizationBuilder.builder(prescription())
                .fields(0.0)
                .mtfFrequencies(10)
                .freezeVignetting()
                .checkSpotApertures(false)
                .build()
                .analysis();
        assertFalse(fixedPupil._check_spot_apertures);
    }

    /**
     * The point of freezing: the factors must not follow the design. Nothing else in the
     * merit would notice if they did - the residuals would simply be evaluated on a pupil
     * that quietly moved, which is the drift this option exists to remove.
     */
    @Test
    void frozenVignettingSurvivesAPrescriptionChange() {
        Prescription prescription = prescription();
        Analysis analysis = OptimizationBuilder.builder(prescription)
                .fields(0.0, 1.0)
                .mtfFrequencies(10)
                .freezeVignetting()
                .build()
                .analysis();

        analysis.compute();
        double[][] captured = analysis.frozen_vignetting();
        assertEquals(2, captured.length);
        double[] modelFactors = vignettingOf(analysis, 1);
        assertArrayEquals(captured[1], modelFactors, 0.0);

        // Move a surface far enough that live factors would certainly follow.
        prescription._surfaces[0]._radius *= 0.5;
        analysis.compute();
        assertArrayEquals(captured[1], vignettingOf(analysis, 1), 0.0,
                "frozen vignetting changed when the prescription did");
        assertArrayEquals(captured[1], analysis.frozen_vignetting()[1], 0.0);

        // Unfreezing drops the capture, so the next compute measures the design again.
        analysis.freezing_vignetting(false);
        assertNull(analysis.frozen_vignetting());
    }

    private static double[] vignettingOf(Analysis analysis, int fieldIndex) {
        var field = analysis._opt_model.optical_spec.fov.fields[fieldIndex];
        return new double[]{field.vux, field.vlx, field.vuy, field.vly};
    }

    /**
     * The case {@link ConstraintThickness} cannot see: axial thickness untouched, but the
     * surfaces bent until they cross away from the axis. If this ever stops failing for
     * the thickness constraint, the edge constraint has lost its reason to exist.
     */
    @Test
    void edgeConstraintSeesCurvatureDrivenCrossingThatThicknessConstraintCannot() {
        Prescription prescription = prescription();
        var setup = OptimizationBuilder.builder(prescription)
                .fields(0.0)
                .mtfFrequencies(10)
                .varyThicknesses(0)
                .applyThicknessConstraints()
                .applyEdgeThicknessConstraints()
                .rayAberrationGoals()
                .build();

        ConstraintThickness axial = (ConstraintThickness) Arrays.stream(setup.goals())
                .filter(ConstraintThickness.class::isInstance).findFirst().orElseThrow();
        ConstraintEdgeThickness edge = (ConstraintEdgeThickness) Arrays.stream(setup.goals())
                .filter(ConstraintEdgeThickness.class::isInstance).findFirst().orElseThrow();

        // Surface 0 is r=50 with a 30mm diameter, so the gap is measured at h=15.
        assertEquals(15.0, edge._height, 1.0e-12);
        assertEquals(0.0, axial.fractional_deviation(), 1.0e-12);
        assertEquals(0.0, edge.fractional_deviation(), 1.0e-12);

        // Bend surface 0 hard toward surface 1 without touching any thickness.
        prescription._surfaces[0]._radius = 16.0;

        assertEquals(0.0, axial.fractional_deviation(), 1.0e-12,
                "axial thickness is unchanged, which is exactly why it cannot see this");
        assertTrue(edge.value() < 0.0,
                () -> "expected the edge gap to have gone negative, got " + edge.value());
        assertTrue(edge.fractional_deviation() < -1.0);
    }

    @Test
    void edgeConstraintsSkipGapsThatCannotBeAnchored() {
        Prescription prescription = prescription();
        // The last surface has no following surface to form a gap with.
        int last = prescription._surfaces.length - 1;
        assertFalse(ConstraintEdgeThickness.is_constrainable(
                new Analysis(prescription, new double[]{0.0}, new int[]{10}), last));

        var setup = OptimizationBuilder.builder(prescription)
                .fields(0.0)
                .mtfFrequencies(10)
                .varyThicknesses(last)
                .applyEdgeThicknessConstraints()
                .rayAberrationGoals()
                .build();

        assertFalse(Arrays.stream(setup.goals())
                .anyMatch(ConstraintEdgeThickness.class::isInstance));
    }

    @Test
    void contrastBalanceGoalsAreBuiltPerEnabledFieldAndFrequency() {
        var setup = OptimizationBuilder.builder(prescription())
                .fields(0.0, 0.5, 1.0)
                .mtfFrequencies(10, 20)
                .contrastSampling(2, 4)
                .contrastGoals(
                        OptimizationBuilder.contrast(10, new double[]{1, 1, 1}),
                        OptimizationBuilder.contrast(20, new double[]{1, 1, 1}))
                // Outer field left unconstrained, as leniency there is usual.
                .contrastBalanceGoals(new boolean[]{true, true, false}, 4.0)
                .build();

        GoalContrastBalance[] balance = Arrays.stream(setup.goals())
                .filter(GoalContrastBalance.class::isInstance)
                .map(GoalContrastBalance.class::cast)
                .toArray(GoalContrastBalance[]::new);

        // Two enabled fields times two frequencies, and nothing for the disabled field.
        assertEquals(4, balance.length);
        assertArrayEquals(new int[]{0, 1, 0, 1},
                Arrays.stream(balance).mapToInt(goal -> goal._field).toArray());
        assertArrayEquals(new int[]{10, 10, 20, 20},
                Arrays.stream(balance).mapToInt(goal -> goal._frequency).toArray());
        for (var goal : balance) {
            assertEquals(0.0, goal._target);
            assertEquals(4.0, goal._weight);
        }
    }

    @Test
    void contrastBalanceRejectsAFlagPerFieldMismatch() {
        IllegalArgumentException tooFew = assertThrows(IllegalArgumentException.class, () ->
                OptimizationBuilder.builder(prescription())
                        .fields(0.0, 0.5, 1.0)
                        .mtfFrequencies(10)
                        .contrastSampling(2, 4)
                        .contrastGoals(OptimizationBuilder.contrast(10, new double[]{1, 1, 1}))
                        .contrastBalanceGoals(new boolean[]{true, true})
                        .build());
        assertTrue(tooFew.getMessage().contains("one flag per field"));

        assertThrows(IllegalArgumentException.class, () ->
                OptimizationBuilder.builder(prescription())
                        .fields(0.0)
                        .mtfFrequencies(10)
                        .contrastBalanceGoals(new boolean[]{true})
                        .build(),
                "balance without contrast goals has nothing to balance");
    }

    /**
     * The point of the goal: zero when the two meridians contribute equally, and signed by
     * which one is worse. It must not care about the overall aberration level, only the
     * split - that is what the contrast merit already handles.
     */
    @Test
    void contrastBalanceReadsZeroWhenMeridiansAreEqualAndSignedOtherwise() {
        var setup = OptimizationBuilder.builder(prescription())
                .fields(0.0, 1.0)
                .mtfFrequencies(10)
                // Unweighted so the goal's wavelength pooling and the sum below coincide;
                // weighted pooling is covered separately.
                .weighted(false)
                .contrastSampling(2, 4)
                .contrastGoals(OptimizationBuilder.contrast(10, new double[]{1, 1}))
                .contrastBalanceGoals(new boolean[]{true, true})
                .build();
        setup.analysis().compute();

        GoalContrastBalance onAxis = Arrays.stream(setup.goals())
                .filter(GoalContrastBalance.class::isInstance)
                .map(GoalContrastBalance.class::cast)
                .filter(goal -> goal._field == 0)
                .findFirst().orElseThrow();

        // On axis the two meridians are identical by rotational symmetry, so a balance
        // goal there is satisfied however aberrated the lens is.
        assertEquals(0.0, onAxis.value(), 1.0e-12);

        double sagittal = blockSumOfSquares(setup, Orientation.SAGITTAL, 1);
        double tangential = blockSumOfSquares(setup, Orientation.TANGENTIAL, 1);
        GoalContrastBalance offAxis = Arrays.stream(setup.goals())
                .filter(GoalContrastBalance.class::isInstance)
                .map(GoalContrastBalance.class::cast)
                .filter(goal -> goal._field == 1)
                .findFirst().orElseThrow();
        assertEquals(sagittal - tangential, offAxis.value(), 1.0e-9,
                "balance must equal the difference the two meridians contribute to the merit");
    }

    /**
     * The goal pools per-wavelength blocks using the same wavelength weights the merit
     * applies to the contrast residuals, so an unweighted setup is the case where this
     * plain sum matches.
     */
    @Test
    void contrastBalancePoolsWavelengthsWithTheConfiguredWeights() {
        var setup = OptimizationBuilder.builder(prescription())
                .fields(0.0, 1.0)
                .mtfFrequencies(10)
                .weighted(true)   // prescription weights are 1.0, 0.5, 0.25
                .contrastSampling(2, 4)
                .contrastGoals(OptimizationBuilder.contrast(10, new double[]{1, 1}))
                .contrastBalanceGoals(new boolean[]{false, true})
                .build();
        setup.analysis().compute();

        var balance = Arrays.stream(setup.goals())
                .filter(GoalContrastBalance.class::isInstance)
                .map(GoalContrastBalance.class::cast)
                .findFirst().orElseThrow();

        double[] weights = {1.0, 0.5, 0.25};
        double expected = 0.0;
        for (int w = 0; w < weights.length; w++) {
            double sagittal = wavelengthSumOfSquares(setup, Orientation.SAGITTAL, 1, w);
            double tangential = wavelengthSumOfSquares(setup, Orientation.TANGENTIAL, 1, w);
            expected += weights[w] * (sagittal - tangential);
        }
        assertEquals(expected, balance.value(), 1.0e-9);
    }

    @Test
    void contrastBalanceIncludesSagittalAndTangentialFieldWeights() {
        var setup = OptimizationBuilder.builder(prescription())
                .fields(0.0, 1.0)
                .mtfFrequencies(10)
                .weighted(false)
                .contrastSampling(2, 4)
                .contrastGoals(OptimizationBuilder.contrast(10,
                        new double[]{1.0, 2.5}, new double[]{1.0, 0.4}))
                .contrastBalanceGoals(new boolean[]{false, true})
                .build();
        setup.analysis().compute();

        var balance = Arrays.stream(setup.goals())
                .filter(GoalContrastBalance.class::isInstance)
                .map(GoalContrastBalance.class::cast)
                .findFirst().orElseThrow();

        double sagittal = blockSumOfSquares(setup, Orientation.SAGITTAL, 1);
        double tangential = blockSumOfSquares(setup, Orientation.TANGENTIAL, 1);
        assertEquals(2.5 * sagittal - 0.4 * tangential, balance.value(), 1.0e-9,
                "balance must include the contrast merit's field/orientation weights");
    }

    private static double wavelengthSumOfSquares(
            OptimizationBuilder.OptimizationSetup setup, int orientation, int field, int wavelength) {
        return Arrays.stream(setup.goals())
                .filter(GoalContrast.class::isInstance)
                .map(GoalContrast.class::cast)
                .filter(goal -> goal._field == field && goal._orientation == orientation
                        && goal._wavelength_index == wavelength)
                .mapToDouble(goal -> goal.value() * goal.value())
                .sum();
    }

    /** Sum of squares of the merit's own contrast residuals for one field and orientation. */
    private static double blockSumOfSquares(
            OptimizationBuilder.OptimizationSetup setup, int orientation, int field) {
        return Arrays.stream(setup.goals())
                .filter(GoalContrast.class::isInstance)
                .map(GoalContrast.class::cast)
                .filter(goal -> goal._field == field && goal._orientation == orientation)
                .mapToDouble(goal -> goal.value() * goal.value())
                .sum();
    }

    @Test
    void usesGaussianQuadratureByDefault() {
        Analysis analysis = OptimizationBuilder.builder(prescription())
                .fields(0.0)
                .mtfFrequencies(10)
                .build()
                .analysis();

        assertEquals(SpotOptions.PATTERN_GAUSS_QUADRATURE, analysis._spot_pattern);
        assertEquals(14, analysis._num_rings);
        assertEquals(20, analysis._num_spokes);

        Analysis configured = OptimizationBuilder.builder(prescription())
                .fields(0.0)
                .mtfFrequencies(10)
                .gaussianQuadratureSampling(3, 8)
                .build()
                .analysis();
        assertEquals(3, configured._num_rings);
        assertEquals(8, configured._num_spokes);
    }

    @Test
    void permitsExplicitHexapolarSpotSampling() {
        Analysis analysis = OptimizationBuilder.builder(prescription())
                .fields(0.0)
                .mtfFrequencies(10)
                .hexapolarSampling(32)
                .build()
                .analysis();

        assertEquals(SpotOptions.PATTERN_HEXAPOLAR, analysis._spot_pattern);
        assertEquals(32, analysis._num_rays);
    }

    @Test
    void maximumSpotRadiusGoalForcesHexapolarSampling() {
        Analysis analysis = OptimizationBuilder.builder(prescription())
                .fields(0.0)
                .mtfFrequencies(10)
                .spotMaxRadiusGoals(new double[]{20.0})
                .build()
                .analysis();

        assertEquals(SpotOptions.PATTERN_HEXAPOLAR, analysis._spot_pattern);
        assertEquals(64, analysis._num_rays);
    }

    @Test
    void additionalMaximumSpotRadiusGoalForcesHexapolarSampling() {
        Analysis analysis = OptimizationBuilder.builder(prescription())
                .fields(0.0)
                .mtfFrequencies(10)
                .additionalGoals(a -> new GoalSpotMaxRadius(a, 1, 20.0, 1.0))
                .build()
                .analysis();

        assertEquals(SpotOptions.PATTERN_HEXAPOLAR, analysis._spot_pattern);
    }

    @Test
    void rejectsInvalidHexapolarSampleCount() {
        assertThrows(IllegalArgumentException.class, () ->
                OptimizationBuilder.builder(prescription()).hexapolarSampling(0));
    }

    @Test
    void buildsStablePerSampleContrastGoalsAndSkipsUnusedMtfAnalysis() {
        var setup = OptimizationBuilder.builder(prescription())
                .fields(0.0, 1.0)
                .mtfFrequencies(10, 20)
                .contrastSampling(2, 4)
                .contrastGoals(OptimizationBuilder.contrast(20,
                        new double[]{2.0, 0.5}, new double[]{3.0, 0.25}))
                .build();

        GoalContrast[] contrastGoals = Arrays.stream(setup.goals())
                .filter(GoalContrast.class::isInstance)
                .map(GoalContrast.class::cast)
                .toArray(GoalContrast[]::new);
        assertEquals(2 * 3 * 2 * 4 * 2, contrastGoals.length);
        assertEquals(0, contrastGoals[0]._contrast_index);
        assertEquals(20, contrastGoals[0]._frequency);
        assertEquals(0, contrastGoals[0]._field);
        assertEquals(0, contrastGoals[0]._wavelength_index);
        assertEquals(0, contrastGoals[0]._sample_index);
        assertEquals(Orientation.SAGITTAL, contrastGoals[0]._orientation);
        assertEquals(2.0, contrastGoals[0]._weight);
        assertEquals(Orientation.TANGENTIAL, contrastGoals[1]._orientation);
        assertEquals(3.0, contrastGoals[1]._weight);

        Analysis analysis = setup.analysis();
        assertArrayEquals(new int[]{20}, analysis._contrast_freqs);
        assertEquals(2, analysis._contrast_num_rings);
        assertEquals(4, analysis._contrast_num_spokes);
        assertFalse(analysis._compute_spots);
        assertFalse(analysis._compute_mtf);
        assertFalse(analysis._compute_ray_aberrations);
    }

    @Test
    void validatesContrastConfiguration() {
        assertThrows(IllegalArgumentException.class, () ->
                OptimizationBuilder.builder(prescription()).contrastSampling(0, 6));
        assertThrows(IllegalArgumentException.class, () ->
                OptimizationBuilder.builder(prescription())
                        .fields(0.0, 1.0)
                        .mtfFrequencies(20)
                        .contrastGoals(OptimizationBuilder.contrast(20, new double[]{1.0}))
                        .build());
        assertThrows(IllegalArgumentException.class, () ->
                OptimizationBuilder.builder(prescription())
                        .fields(0.0)
                        .mtfFrequencies(20)
                        .contrastGoals(
                                OptimizationBuilder.contrast(20, new double[]{1.0}),
                                OptimizationBuilder.contrast(20, new double[]{1.0}))
                        .build());
    }

    @Test
    void computesContrastGoalsWithoutComputingSpotMtf() {
        var setup = OptimizationBuilder.builder(prescription())
                .fields(0.0)
                .mtfFrequencies(20)
                .dLineOnly(true)
                .contrastSampling(2, 4)
                .contrastGoals(
                        OptimizationBuilder.contrast(10, new double[]{1.0}),
                        OptimizationBuilder.contrast(20, new double[]{1.0}))
                .build();

        setup.analysis().compute();

        assertEquals(null, setup.analysis()._spots);
        assertEquals(null, setup.analysis()._mtfs);
        assertEquals(2, setup.analysis()._contrasts.length);
        GoalContrast[] goals = Arrays.stream(setup.goals())
                .filter(GoalContrast.class::isInstance)
                .map(GoalContrast.class::cast)
                .toArray(GoalContrast[]::new);
        assertTrue(Arrays.stream(goals).anyMatch(goal -> goal._contrast_index == 0 && goal._frequency == 10));
        assertTrue(Arrays.stream(goals).anyMatch(goal -> goal._contrast_index == 1 && goal._frequency == 20));
        Arrays.stream(goals).forEach(goal -> assertTrue(Double.isFinite(goal.value())));
    }

    @Test
    void buildsPerRaySpotGoalsWhoseSumMatchesRmsSpotRadius() {
        var setup = OptimizationBuilder.builder(prescription())
                .fields(0.0)
                .mtfFrequencies(20)
                .weighted(false)
                .gaussianQuadratureSampling(2, 4)
                .spotDeviationGoals(new double[]{1.0}, new double[]{1.0})
                .build();

        assertEquals(SpotOptions.PATTERN_GAUSS_QUADRATURE, setup.analysis()._spot_pattern);
        assertEquals(2, setup.analysis()._num_rings);
        assertEquals(4, setup.analysis()._num_spokes);
        assertTrue(setup.analysis()._append_failed_spot_rays);
        setup.analysis().compute();

        GoalSpotDeviation[] goals = Arrays.stream(setup.goals())
                .filter(GoalSpotDeviation.class::isInstance)
                .map(GoalSpotDeviation.class::cast)
                .toArray(GoalSpotDeviation[]::new);
        assertEquals(3 * 2 * 4 * 2, goals.length);
        assertEquals(Orientation.X, goals[0]._orientation);
        assertEquals(Orientation.Y, goals[1]._orientation);
        assertEquals(1.0, goals[0]._weight);
        assertEquals(1.0, goals[1]._weight);
        // Fields are one based on the way in, as for every other field-addressed goal,
        // and stored zero based. An off-by-one here would silently attach the goal to
        // the wrong field rather than fail, so pin it.
        assertEquals(0, goals[0]._field);
        assertThrows(IllegalArgumentException.class,
                () -> new GoalSpotDeviation(setup.analysis(), 0, 0, 0, Orientation.X, 1.0));

        double sumOfSquares = Arrays.stream(goals)
                .mapToDouble(goal -> goal.value() * goal.value())
                .sum();
        double rmsFromGoals = Math.sqrt(sumOfSquares / 3.0);
        assertEquals(setup.analysis()._spots[0].get_mean_radius(), rmsFromGoals, 1.0e-9);
    }

    @Test
    void perRaySpotGoalsImproveSpotMeritDuringOptimization() {
        var setup = OptimizationBuilder.builder(prescription())
                .fields(0.0)
                .mtfFrequencies(20)
                .varyCurvatures(0)
                .weighted(false)
                .gaussianQuadratureSampling(2, 4)
                .spotDeviationGoals(1.0)
                .build();
        setup.analysis().compute();
        var merit = setup.meritFunction(false);
        double initial = merit.getRMS();

        int status = merit.getSolver().solve();

        assertTrue(status > 0, "lmder status=" + status);
        assertTrue(merit.getRMS() < initial);
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

        assertThrows(IllegalArgumentException.class, () ->
                OptimizationBuilder.builder(prescription())
                        .fields(0.0, 1.0)
                        .mtfFrequencies(10)
                        .spotRmsGoals(new double[]{10.0})
                        .build());

        assertThrows(IllegalArgumentException.class, () ->
                OptimizationBuilder.builder(prescription())
                        .fields(0.0, 1.0)
                        .mtfFrequencies(10)
                        .spotDeviationGoals(1.0)
                        .build());

        assertThrows(IllegalArgumentException.class, () ->
                OptimizationBuilder.builder(prescription())
                        .fields(0.0)
                        .mtfFrequencies(10)
                        .spotDeviationGoals(1.0)
                        .hexapolarSampling()
                        .build());

        Prescription prescription = prescription();
        assertThrows(IllegalArgumentException.class, () ->
                OptimizationBuilder.builder(prescription)
                        .additionalVariables(new VarThickness(prescription(), 0)));
    }

    private static void assertMtf(Goal goal, int frequency, int field, int orientation,
                                  double target, double weight) {
        GoalGeoMTF mtf = assertInstanceOf(GoalGeoMTF.class, goal);
        assertEquals(frequency, mtf._freq);
        assertEquals(field, mtf._field);
        assertEquals(orientation, mtf._orientation);
        assertEquals(target, mtf._target);
        assertEquals(weight, mtf._weight);
    }
}
