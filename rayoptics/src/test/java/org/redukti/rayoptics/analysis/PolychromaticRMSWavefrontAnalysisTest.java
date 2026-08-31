package org.redukti.rayoptics.analysis;

import org.junit.jupiter.api.Test;
import org.redukti.rayoptics.raytr.Trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolychromaticRMSWavefrontAnalysisTest {

    @Test
    void returnsOneSpectrallyAndSpatiallyWeightedScalarPerField() {
        var model = MtfTest.buildTestModel();
        model.optical_spec.wvls.spectral_wts = new double[]{1.0, 2.0, 0.5};
        var result = PolychromaticRMSWavefrontAnalysis.eval(model,
                new PolychromaticRMSWavefrontOptions().num_rings(2).num_spokes(6));

        assertEquals(model.optical_spec.fov.fields.length, result.fields().size());
        for (var field : result.fields()) {
            assertEquals(model.optical_spec.wvls.wavelengths.length,
                    field.wavelengths().size());
            double expectedWeight = 0.0;
            double expectedSquares = 0.0;
            int expectedSamples = 0;
            for (var wavelength : field.wavelengths()) {
                expectedWeight += wavelength.spectralWeight() * wavelength.pupilWeight();
                expectedSquares += wavelength.spectralWeight() * wavelength.weightedSquareSum();
                expectedSamples += wavelength.validSamples();
                assertTrue(wavelength.pupilWeight() > 0.0);
                assertTrue(Double.isFinite(wavelength.rmsWaves()));
            }
            assertEquals(expectedWeight, field.totalWeight(), 1.0e-14);
            assertEquals(Math.sqrt(expectedSquares / expectedWeight), field.rmsWaves(), 1.0e-14);
            assertEquals(expectedSamples, field.validSamples());
        }
    }

    @Test
    void aSingleNonzeroSpectralWeightEqualsThatMonochromaticRms() {
        var model = MtfTest.buildTestModel();
        model.optical_spec.wvls.spectral_wts = new double[]{0.0, 3.5, 0.0};
        var result = PolychromaticRMSWavefrontAnalysis.eval(model,
                new PolychromaticRMSWavefrontOptions().num_rings(2).num_spokes(6));

        for (var field : result.fields()) {
            assertEquals(field.wavelengths().get(1).rmsWaves(), field.rmsWaves(), 1.0e-14);
        }
    }

    @Test
    void everyWavelengthUsesThePrimaryChiefRayImagePoint() {
        var model = MtfTest.buildTestModel();
        var field = model.optical_spec.fov.fields[2];
        double focus = model.optical_spec.defocus().get_focus();
        var primary = Trace.setup_pupil_coords(
                model, field, model.optical_spec.wvls.central_wvl(), focus, null, null);

        PolychromaticRMSWavefrontAnalysis.eval(model,
                new PolychromaticRMSWavefrontOptions().num_rings(1).num_spokes(3));

        assertEquals(primary.ref_sphere.image_pt.x, field.ref_sphere.image_pt.x, 1.0e-12);
        assertEquals(primary.ref_sphere.image_pt.y, field.ref_sphere.image_pt.y, 1.0e-12);
        assertEquals(model.optical_spec.wvls.wavelengths[2],
                field.chief_ray.chief_ray.wvl, 0.0);
    }
}
