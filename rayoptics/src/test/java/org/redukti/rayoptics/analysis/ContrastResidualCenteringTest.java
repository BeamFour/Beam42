package org.redukti.rayoptics.analysis;

import org.junit.jupiter.api.Test;
import org.redukti.mathlib.Vector2;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Centring must remove exactly the constant part of a block and nothing else:
 * {@code sum(w.dW^2) = Var + mean^2} becomes {@code Var}.
 *
 * <p>Built from synthetic samples rather than a traced lens so the mean is known exactly
 * and the identity can be asserted rather than approximated.
 */
class ContrastResidualCenteringTest {

    // Weights sum to 1, as the quadrature normalizes them, and the shape is chosen with
    // weighted mean exactly zero so the block's mean is the injected tilt and nothing
    // else. WEIGHTS and SHAPE are checked against that premise below.
    private static final double[] WEIGHTS = {0.1, 0.2, 0.4, 0.2, 0.1};
    private static final double[] SHAPE = {-0.315, -0.115, 0.035, 0.085, 0.235};

    @Test
    void theFixtureShapeHasZeroWeightedMean() {
        double mean = 0.0;
        for (int i = 0; i < WEIGHTS.length; i++) mean += WEIGHTS[i] * SHAPE[i];
        assertEquals(0.0, mean, 1.0e-15,
                "the other tests read the block mean as the injected tilt alone");
    }

    /** A block whose tangential differences carry a known tilt on top of a known shape. */
    private static ContrastAnalysisResult blockWithTangentialTilt(double tilt) {
        double[] weights = WEIGHTS;
        double[] shape = SHAPE;
        var samples = new ArrayList<ContrastAnalysisResult.Sample>();
        for (int i = 0; i < weights.length; i++) {
            samples.add(new ContrastAnalysisResult.Sample(
                    new Vector2(0.1 * i, 0.0),
                    // Sagittal deliberately carries no tilt: on a rotationally symmetric
                    // system its mean is identically zero.
                    shape[i],
                    shape[i] + tilt,
                    weights[i], true, null));
        }
        var result = new ContrastAnalysisResult(40.0);
        var wavelengths = new ArrayList<ContrastAnalysisResult.WavelengthResult>();
        wavelengths.add(new ContrastAnalysisResult.WavelengthResult(587.5618, 0.0674, samples));
        result.fields.add(new ContrastAnalysisResult.FieldResult(null, wavelengths));
        return result;
    }

    private static double sumOfSquares(ContrastAnalysisResult result, boolean tangential) {
        var block = result.fields.get(0).wavelengths().get(0);
        double sos = 0.0;
        for (int i = 0; i < block.samples().size(); i++) {
            double r = tangential ? block.tangentialResidual(i) : block.sagittalResidual(i);
            sos += r * r;
        }
        return sos;
    }

    @Test
    void centeringRemovesExactlyTheMeanSquareTerm() {
        double tilt = 0.22;
        var uncentred = blockWithTangentialTilt(tilt);
        double before = sumOfSquares(uncentred, true);

        var centred = blockWithTangentialTilt(tilt);
        ContrastAnalysis.center_residuals(centred, 0);
        double after = sumOfSquares(centred, true);

        // The shape has weighted mean zero, so the block's mean is exactly the tilt and
        // the removed term is exactly mean^2 (weights sum to 1).
        var block = centred.fields.get(0).wavelengths().get(0);
        assertEquals(tilt, block.tangentialOffset(), 1.0e-12);
        assertEquals(before - tilt * tilt, after, 1.0e-12);
        assertNotEquals(before, after);
    }

    @Test
    void centredResidualsHaveZeroWeightedMean() {
        var centred = blockWithTangentialTilt(0.22);
        ContrastAnalysis.center_residuals(centred, 0);
        var block = centred.fields.get(0).wavelengths().get(0);

        double weightedMean = 0.0;
        for (int i = 0; i < block.samples().size(); i++) {
            var sample = block.samples().get(i);
            weightedMean += Math.sqrt(sample.weight()) * block.tangentialResidual(i);
        }
        assertEquals(0.0, weightedMean, 1.0e-12);
    }

    /** A block with no tilt must come through untouched, as sagittal blocks always do. */
    @Test
    void aBlockWithNoConstantPartIsUnchanged() {
        var result = blockWithTangentialTilt(0.0);
        double before = sumOfSquares(result, false);
        ContrastAnalysis.center_residuals(result, 0);
        assertEquals(0.0, result.fields.get(0).wavelengths().get(0).sagittalOffset(), 1.0e-12);
        assertEquals(before, sumOfSquares(result, false), 1.0e-12);
    }

    /**
     * Every wavelength gets the reference wavelength's offset, not its own: a tilt common
     * to all wavelengths is a harmless image shift, while one that differs between them is
     * lateral colour and does cost polychromatic MTF. Subtracting per-wavelength means
     * would discard that difference.
     */
    @Test
    void allWavelengthsShareTheReferenceWavelengthOffset() {
        var result = new ContrastAnalysisResult(40.0);
        var wavelengths = new ArrayList<ContrastAnalysisResult.WavelengthResult>();
        wavelengths.add(block(486.13, 0.10));
        wavelengths.add(block(587.56, 0.22));   // reference
        wavelengths.add(block(656.27, 0.31));
        result.fields.add(new ContrastAnalysisResult.FieldResult(null, wavelengths));

        ContrastAnalysis.center_residuals(result, 1);

        var centred = result.fields.get(0).wavelengths();
        for (var block : centred)
            assertEquals(0.22, block.tangentialOffset(), 1.0e-12);

        // The colour differences survive: F and C keep their offset from the reference.
        assertEquals(0.10 - 0.22, centred.get(0).tangentialResidual(0), 1.0e-12);
        assertEquals(0.31 - 0.22, centred.get(2).tangentialResidual(0), 1.0e-12);
    }

    /** One unit-weight sample whose tangential difference is exactly {@code value}. */
    private static ContrastAnalysisResult.WavelengthResult block(double wavelength, double value) {
        List<ContrastAnalysisResult.Sample> samples = List.of(
                new ContrastAnalysisResult.Sample(
                        new Vector2(0.0, 0.0), 0.0, value, 1.0, true, null));
        return new ContrastAnalysisResult.WavelengthResult(wavelength, 0.0674, samples);
    }
}
