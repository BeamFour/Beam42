package org.redukti.rayoptics.analysis;

import org.junit.jupiter.api.Test;
import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.raytr.GridItem;
import org.redukti.rayoptics.raytr.Trace;
import org.redukti.rayoptics.raytr.TraceOptions;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
                expectedSquares += wavelength.spectralWeight() *
                        wavelength.centeredWeightedSquareSum();
                expectedSamples += wavelength.validSamples();
                assertTrue(wavelength.pupilWeight() > 0.0);
                assertTrue(Double.isFinite(wavelength.rmsWaves()));
                assertTrue(wavelength.rmsWaves() <= wavelength.unreferencedRmsWaves());
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
    void everyWavelengthUsesThePrimaryImagePointAndLeavesCentralState() {
        var model = MtfTest.buildTestModel();
        var field = model.optical_spec.fov.fields[2];
        double focus = model.optical_spec.defocus().get_focus();
        var primary = Trace.setup_pupil_coords(
                model, field, model.optical_spec.wvls.central_wvl(), focus, null, null);

        PolychromaticRMSWavefrontAnalysis.eval(model,
                new PolychromaticRMSWavefrontOptions().num_rings(1).num_spokes(3));

        assertEquals(primary.ref_sphere.image_pt.x, field.ref_sphere.image_pt.x, 1.0e-12);
        assertEquals(primary.ref_sphere.image_pt.y, field.ref_sphere.image_pt.y, 1.0e-12);
        assertEquals(model.optical_spec.wvls.central_wvl(),
                field.chief_ray.chief_ray.wvl, 0.0);
    }

    @Test
    void subtractingTheWeightedMeanMakesTheRmsInvariantToPiston() {
        var original = List.of(
                new GridItem(Vector2.vector2_0, null, 1.0).withWeight(0.75),
                new GridItem(Vector2.vector2_0, null, 3.0).withWeight(0.25));
        var shifted = List.of(
                new GridItem(Vector2.vector2_0, null, 11.0).withWeight(0.75),
                new GridItem(Vector2.vector2_0, null, 13.0).withWeight(0.25));

        var a = PolychromaticRMSWavefrontAnalysis.summarize(550.0, 1.0, original);
        var b = PolychromaticRMSWavefrontAnalysis.summarize(550.0, 1.0, shifted);

        assertEquals(1.5, a.meanWaves(), 1.0e-15);
        assertEquals(11.5, b.meanWaves(), 1.0e-15);
        assertEquals(Math.sqrt(0.75), a.rmsWaves(), 1.0e-15);
        assertEquals(a.rmsWaves(), b.rmsWaves(), 1.0e-14);
        assertTrue(b.unreferencedRmsWaves() > a.unreferencedRmsWaves());
    }

    @Test
    void resultDoesNotDependOnPreviouslyCachedWavelength() {
        var freshModel = MtfTest.buildTestModel();
        var seededModel = MtfTest.buildTestModel();
        double focus = seededModel.optical_spec.defocus().get_focus();
        double seedWavelength = seededModel.optical_spec.wvls.wavelengths[2];
        for (var field : seededModel.optical_spec.fov.fields) {
            var seeded = Trace.setup_pupil_coords(
                    seededModel, field, seedWavelength, focus, null, null);
            field.chief_ray = seeded.chief_ray_pkg;
            field.ref_sphere = seeded.ref_sphere;
        }
        var options = new PolychromaticRMSWavefrontOptions().num_rings(2).num_spokes(6);

        var fresh = PolychromaticRMSWavefrontAnalysis.eval(freshModel, options);
        var preseeded = PolychromaticRMSWavefrontAnalysis.eval(seededModel, options);

        for (int i = 0; i < fresh.fields().size(); i++) {
            assertEquals(fresh.fields().get(i).rmsWaves(),
                    preseeded.fields().get(i).rmsWaves(), 1.0e-12);
        }
    }

    @Test
    void validatesConfigurationBeforeTracingAndCopiesTraceOptions() {
        var defaults = new PolychromaticRMSWavefrontOptions();
        assertFalse(defaults.traceOptions.check_apertures);
        assertThrows(IllegalArgumentException.class, () -> defaults.num_spokes(2));

        var callerOptions = new TraceOptions();
        callerOptions.check_apertures = true;
        var options = new PolychromaticRMSWavefrontOptions().trace_options(callerOptions);
        options.check_apertures(false);
        assertTrue(callerOptions.check_apertures);

        var noWeights = MtfTest.buildTestModel();
        noWeights.optical_spec.wvls.spectral_wts = new double[]{0.0, 0.0, 0.0};
        assertThrows(IllegalArgumentException.class,
                () -> PolychromaticRMSWavefrontAnalysis.eval(noWeights, defaults));

        var mismatched = MtfTest.buildTestModel();
        mismatched.optical_spec.wvls.spectral_wts = new double[]{1.0};
        assertThrows(IllegalArgumentException.class,
                () -> PolychromaticRMSWavefrontAnalysis.eval(mismatched, defaults));
    }
}
