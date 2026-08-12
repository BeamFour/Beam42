package org.redukti.rayoptics.analysis;

import org.junit.jupiter.api.Test;
import org.redukti.mathlib.Vector2;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContrastOtfTest {

    /** One field, one wavelength, samples carrying the given sagittal differences. */
    private static ContrastAnalysisResult resultOf(double... sagittalDifferences) {
        return resultOf(new double[][]{sagittalDifferences});
    }

    private static ContrastAnalysisResult resultOf(double[][] byWavelength) {
        var result = new ContrastAnalysisResult(40.0);
        var wavelengths = new java.util.ArrayList<ContrastAnalysisResult.WavelengthResult>();
        for (double[] differences : byWavelength) {
            var samples = new java.util.ArrayList<ContrastAnalysisResult.Sample>();
            for (double d : differences)
                samples.add(new ContrastAnalysisResult.Sample(
                        Vector2.vector2_0, d, 0.0, 1.0 / differences.length, true));
            wavelengths.add(new ContrastAnalysisResult.WavelengthResult(587.6, 0.05, samples));
        }
        result.fields.add(new ContrastAnalysisResult.FieldResult(null, wavelengths));
        return result;
    }

    @Test
    void aFlatWavefrontDifferenceGivesFullModulation() {
        var result = resultOf(0.0, 0.0, 0.0, 0.0);
        assertEquals(1.0, result.otf_modulus(0, ContrastAnalysisResult.SAGITTAL, null), 1.0e-12);
    }

    /**
     * A constant phase difference is a pure image displacement: it moves the phase
     * transfer function but must not reduce the modulus. This is what distinguishes the
     * OTF modulus from the un-centred least-squares residual, which does charge for it.
     */
    @Test
    void aConstantPhaseOffsetDoesNotReduceTheModulus() {
        for (double offset : new double[]{0.1, 0.25, 0.5, 1.0, 3.7}) {
            var result = resultOf(offset, offset, offset, offset);
            assertEquals(1.0, result.otf_modulus(0, ContrastAnalysisResult.SAGITTAL, null), 1.0e-12,
                    () -> "offset " + offset + " should not reduce the modulus");
        }
    }

    /** Phase differences spread over a full cycle cancel completely. */
    @Test
    void phasesSpreadOverAFullCycleCancel() {
        var result = resultOf(0.0, 0.25, 0.5, 0.75);
        assertEquals(0.0, result.otf_modulus(0, ContrastAnalysisResult.SAGITTAL, null), 1.0e-12);
    }

    /** Small phases: the modulus approaches the 1 - 2(pi sigma)^2 limit the residuals assume. */
    @Test
    void smallPhasesAgreeWithTheLeastSquaresApproximation() {
        double d = 0.01;
        var result = resultOf(-d, -d, d, d);
        double sigma = d;
        assertEquals(1.0 - 2.0 * Math.PI * Math.PI * sigma * sigma,
                result.otf_modulus(0, ContrastAnalysisResult.SAGITTAL, null), 1.0e-6);
    }

    /**
     * Large phases: the same approximation is not merely inaccurate, it is meaningless.
     * Here the OTF has passed through a contrast reversal - the real part is negative,
     * so the modulus rises again as the phase grows. That non-monotonicity is exactly
     * what the least-squares residual cannot represent.
     */
    @Test
    void largePhasesDivergeFromTheLeastSquaresApproximation() {
        double d = 0.4;
        var result = resultOf(-d, -d, d, d);
        double leastSquares = 1.0 - 2.0 * Math.PI * Math.PI * d * d;
        double modulus = result.otf_modulus(0, ContrastAnalysisResult.SAGITTAL, null);
        assertTrue(leastSquares < 0.0, "the approximation has gone negative here");
        assertTrue(modulus >= 0.0 && modulus <= 1.0, "the modulus stays physical: " + modulus);
        assertEquals(Math.abs(Math.cos(2 * Math.PI * d)), modulus, 1.0e-12);

        // Non-monotone in the spread: a wider phase spread reports a higher modulus.
        double narrower = resultOf(-0.25, -0.25, 0.25, 0.25)
                .otf_modulus(0, ContrastAnalysisResult.SAGITTAL, null);
        assertTrue(modulus > narrower,
                () -> "expected the wider spread to read higher, got " + modulus
                        + " against " + narrower);
    }

    @Test
    void wavelengthsCombineAsAComplexSumNotAsAverageOfModuli() {
        // Two wavelengths in exact antiphase: a polychromatic OTF cancels, whereas
        // averaging monochromatic moduli would report full contrast.
        var result = resultOf(new double[][]{{0.0, 0.0}, {0.5, 0.5}});
        assertEquals(0.0, result.otf_modulus(0, ContrastAnalysisResult.SAGITTAL, null), 1.0e-12);
    }

    @Test
    void wavelengthWeightsAreApplied() {
        var result = resultOf(new double[][]{{0.0, 0.0}, {0.5, 0.5}});
        // Suppressing the second wavelength restores full contrast.
        assertEquals(1.0,
                result.otf_modulus(0, ContrastAnalysisResult.SAGITTAL, new double[]{1.0, 0.0}),
                1.0e-12);
    }

    @Test
    void aFailedRayMakesTheModulusUnavailable() {
        var result = new ContrastAnalysisResult(40.0);
        var samples = List.of(
                new ContrastAnalysisResult.Sample(Vector2.vector2_0, 0.0, 0.0, 0.5, true),
                new ContrastAnalysisResult.Sample(Vector2.vector2_0, 0.0, 0.0, 0.5, false));
        result.fields.add(new ContrastAnalysisResult.FieldResult(null,
                List.of(new ContrastAnalysisResult.WavelengthResult(587.6, 0.05, samples))));
        assertTrue(Double.isNaN(result.otf_modulus(0, ContrastAnalysisResult.SAGITTAL, null)));
    }

    @Test
    void validatesArguments() {
        var result = resultOf(0.0, 0.0);
        assertThrows(IllegalArgumentException.class, () -> result.otf_modulus(0, 7, null));
        assertThrows(IllegalArgumentException.class, () -> result.otf_modulus(1, 0, null));
        assertThrows(IllegalArgumentException.class,
                () -> result.otf_modulus(0, 0, new double[]{1.0, 1.0}));
    }
}
