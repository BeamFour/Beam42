package org.redukti.rayoptics.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContrastAnalysisTest {

    @Test
    void convertsImageFrequencyToNormalizedPupilShift() {
        var model = MtfTest.buildTestModel();
        double wavelength = 550.0;
        double frequency = 50.0;

        double shift = ContrastAnalysis.normalized_entry_pupil_shift(model, wavelength, frequency);

        assertEquals(0.05413662972175293, shift, 1.0e-12);
    }

    @Test
    void evaluatesWeightedWavefrontDifferencesForEveryFieldAndWavelength() {
        var model = MtfTest.buildTestModel();
        var options = new ContrastOptions(40.0).num_rings(2).num_spokes(6);

        var result = ContrastAnalysis.eval(model, options);

        assertEquals(3, result.fields.size());
        for (var field : result.fields) {
            assertEquals(3, field.wavelengths().size());
            for (var wavelength : field.wavelengths()) {
                assertEquals(12, wavelength.samples().size());
                assertEquals(1.0, wavelength.samples().stream()
                        .mapToDouble(ContrastAnalysisResult.Sample::weight).sum(), 1.0e-14);
                for (var sample : wavelength.samples()) {
                    assertTrue(Double.isFinite(sample.sagittalDifference()));
                    assertTrue(Double.isFinite(sample.tangentialDifference()));
                    assertTrue(Double.isFinite(sample.sagittalResidual()));
                    assertTrue(Double.isFinite(sample.tangentialResidual()));
                }
            }
        }
    }
}
