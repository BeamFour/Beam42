package org.redukti.rayoptics.analysis;

import org.junit.jupiter.api.Test;
import org.redukti.rayoptics.raytr.Trace;
import org.redukti.rayoptics.util.Orientation;

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

    @Test
    void frequencyCalibrationUsesTheSameCentralReferenceSetupAsContrastTracing() {
        var model = MtfTest.buildTestModel();
        var field = model.optical_spec.fov.fields[2];
        var wavelengths = model.optical_spec.wvls.wavelengths;
        double focus = model.optical_spec.defocus().get_focus();

        // Seed a different wavelength to ensure calibration does not merely inherit the
        // wavelength-specific chief ray and reference sphere left by an earlier analysis.
        var stale = Trace.setup_pupil_coords(model, field, wavelengths[2], focus, null, null);
        field.chief_ray = stale.chief_ray_pkg;
        field.ref_sphere = stale.ref_sphere;

        double wavelength = wavelengths[1];
        var central = Trace.setup_pupil_coords(
                model, field, model.seq_model.central_wavelength(), focus, null, null);
        var expected = Trace.setup_pupil_coords(model, field, wavelength, focus,
                central.ref_sphere.image_pt.project_xy(), null);

        var options = new ContrastOptions(40.0).calibrate_frequency(true);
        double shift = ContrastAnalysis.normalized_entry_pupil_shift(model, wavelength, 40.0);
        double scale = ContrastAnalysis.exit_pupil_frequency_calibration(
                model, field, wavelength, shift, Orientation.X, options);

        assertTrue(Double.isFinite(scale));
        assertEquals(wavelength, field.chief_ray.chief_ray.wvl, 0.0);
        assertEquals(expected.ref_sphere.image_pt.x, field.ref_sphere.image_pt.x, 1.0e-12);
        assertEquals(expected.ref_sphere.image_pt.y, field.ref_sphere.image_pt.y, 1.0e-12);
        assertEquals(expected.ref_sphere.image_pt.z, field.ref_sphere.image_pt.z, 1.0e-12);
    }

    @Test
    void frequencyCalibrationHonoursContrastApertureChecking() {
        var model = MtfTest.buildTestModel();
        var field = model.optical_spec.fov.fields[1];
        double wavelength = model.optical_spec.wvls.wavelengths[1];
        double shift = ContrastAnalysis.normalized_entry_pupil_shift(model, wavelength, 40.0);

        // Make the first real surface reject both calibration probes when physical
        // aperture checking is requested. With checking disabled the same rays trace.
        model.seq_model.ifcs.get(1).max_aperture = 1.0e-6;
        double checked = ContrastAnalysis.exit_pupil_frequency_calibration(
                model, field, wavelength, shift, Orientation.X,
                new ContrastOptions(40.0).calibrate_frequency(true).check_apertures(true));
        double unchecked = ContrastAnalysis.exit_pupil_frequency_calibration(
                model, field, wavelength, shift, Orientation.X,
                new ContrastOptions(40.0).calibrate_frequency(true).check_apertures(false));

        assertEquals(1.0, checked, 0.0,
                "a clipped calibration probe should fall back to no correction");
        assertTrue(Math.abs(unchecked - 1.0) > 1.0e-6,
                "disabling aperture checks should allow the calibration probes to trace");
    }
}
