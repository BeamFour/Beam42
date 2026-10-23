package org.redukti.analysis;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ContrastPhasorTest {
    private static ContrastAnalysisResult.Sample sample(double difference, double weight, boolean valid) {
        return new ContrastAnalysisResult.Sample(null, difference, difference, weight, valid, null);
    }

    @Test
    void computesWeightedComplexAverageOverValidSamples() {
        var samples = List.of(sample(0, 0.1, true), sample(0.25, 0.15, true),
                sample(0.5, 0.25, true), sample(0.1, 100, false));
        var result = new ContrastAnalysisResult.WavelengthResult(550, 0.1, samples).phasor(0);
        assertEquals(-0.3, result.real(), 1e-14);
        assertEquals(0.3, result.imaginary(), 1e-14);
        assertEquals(Math.sqrt(0.18), result.modulus(), 1e-14);
        assertEquals(0.5, result.validWeight(), 1e-14);
    }

    @Test
    void constantPhaseDifferenceHasUnitModulusAndCenteringDoesNotChangeRawPhasor() {
        var block = new ContrastAnalysisResult.WavelengthResult(550, 0.1,
                List.of(sample(0.125, 0.3, true), sample(0.125, 0.7, true)));
        var phasor = block.phasor(0);
        assertEquals(Math.sqrt(0.5), phasor.real(), 1e-14);
        assertEquals(Math.sqrt(0.5), phasor.imaginary(), 1e-14);
        assertEquals(1, phasor.modulus(), 1e-14);
        assertEquals(phasor, block.withOffsets(0.125, 0.125).phasor(0));
        assertEquals(0, block.withOffsets(0.125, 0.125).sagittalResidual(0), 1e-14);
    }

    @Test
    void usesIndependentOrientationSamplesAndValidity() {
        var sagittal = List.of(sample(0, 1, true), sample(0.5, 1, true));
        var tangential = List.of(sample(0.25, 0.4, true), sample(0.5, 0.6, false));
        var block = new ContrastAnalysisResult.WavelengthResult(550, 0.1, sagittal, 0, 0, tangential);
        assertEquals(0, block.phasor(0).modulus(), 1e-14);
        assertEquals(1, block.phasor(1).modulus(), 1e-14);
        assertEquals(1, block.phasor(1).imaginary(), 1e-14);
        assertEquals(0.4, block.phasor(1).validWeight(), 1e-14);
    }

    @Test
    void equalRmsDoesNotImplyEqualPhasorModulus() {
        var a = new ContrastAnalysisResult.WavelengthResult(550, 0.1,
                List.of(sample(-0.25, 0.5, true), sample(0.25, 0.5, true)));
        var b = new ContrastAnalysisResult.WavelengthResult(550, 0.1,
                List.of(sample(-0.5, 0.125, true), sample(0, 0.75, true), sample(0.5, 0.125, true)));
        assertEquals(ContrastReport.rms(a, 0), ContrastReport.rms(b, 0), 1e-14);
        assertEquals(0, a.phasor(0).modulus(), 1e-14);
        assertEquals(0.5, b.phasor(0).modulus(), 1e-14);
    }

    @Test
    void missingFinitePositiveWeightProducesUndefinedPhasor() {
        var block = new ContrastAnalysisResult.WavelengthResult(550, 0.1,
                List.of(sample(0, 1, false), sample(Double.NaN, 1, true), sample(0, 0, true)));
        assertTrue(Double.isNaN(block.phasor(0).modulus()));
        assertEquals(0, block.phasor(0).validWeight());
        assertThrows(IllegalArgumentException.class, () -> block.phasor(2));
    }
}
