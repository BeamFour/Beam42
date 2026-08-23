package org.redukti.rayoptics.integration;

import org.junit.jupiter.api.Test;
import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.analysis.ContrastAnalysis;
import org.redukti.rayoptics.analysis.ContrastOptions;
import org.redukti.rayoptics.raytr.ExitPupilAiming;
import org.redukti.rayoptics.raytr.VigCalc;
import org.redukti.rayoptics.specs.FieldSpec;
import org.redukti.rayoptics.specs.ImageKey;
import org.redukti.rayoptics.specs.ValueKey;
import org.redukti.rayoptics.raytr.ContrastRayTriplet;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.util.Orientation;
import org.redukti.rayoptics.util.Pair;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class US003549241Example05Test {

    @Test
    void denseOptimizerPatternCanAimEveryConfiguredFieldAndWavelength() {
        var model = US003549241Example05.build();
        double[] fields = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
        model.optical_spec.fov = new FieldSpec(model.optical_spec,
                new Pair<>(ImageKey.Object, ValueKey.Angle), 45.0, fields, true, true);
        model.update_model();
        VigCalc.set_pupil(model);
        model.update_model();

        for (int fieldIndex = 0; fieldIndex < fields.length; fieldIndex++) {
            for (int wavelengthIndex = 0;
                 wavelengthIndex < model.optical_spec.wvls.wavelengths.length;
                 wavelengthIndex++) {
                double wavelength = model.optical_spec.wvls.wavelengths[wavelengthIndex];
                double shift = ContrastAnalysis.normalized_entry_pupil_shift(
                        model, wavelength, 20.0);
                var traced = model.seq_model.trace_contrast(
                        (rays, field, wvl, focus) -> rays,
                        fieldIndex, wavelengthIndex, 6, 12,
                        new Vector2(shift, 0.0), new Vector2(0.0, shift),
                        20.0, new TraceOptions(), true);
                for (var rays : traced.get(0).samples()) {
                    String context = "field=" + fields[fieldIndex]
                            + ", wavelength=" + wavelength + ", pupil=" + rays.pupil();
                    assertNull(rays.referenceError(), context);
                    assertNull(rays.sagittalError(), context + ", " + rays.sagittalError());
                    assertNull(rays.tangentialError(), context + ", " + rays.tangentialError());
                }
            }
        }
    }

    @Test
    void exitPupilAimingCorrectsFullFieldShear() {
        var model = US003549241Example05.build();
        int fieldIndex = 2;
        int wavelengthIndex = model.optical_spec.wvls.reference_wvl;
        double wavelength = model.optical_spec.wvls.wavelengths[wavelengthIndex];
        double normalizedShift = ContrastAnalysis.normalized_entry_pupil_shift(
                model, wavelength, 40.0);
        Vector2 sagittalShift = new Vector2(normalizedShift, 0.0);
        Vector2 tangentialShift = new Vector2(0.0, normalizedShift);
        Field field = model.optical_spec.fov.fields[fieldIndex];

        var unaimed = model.seq_model.trace_contrast(
                US003549241Example05Test::exitPupilSeparations,
                fieldIndex, wavelengthIndex, 1, 6,
                sagittalShift, tangentialShift, 40.0, new TraceOptions(), false);
        var aimed = model.seq_model.trace_contrast(
                US003549241Example05Test::exitPupilSeparations,
                fieldIndex, wavelengthIndex, 1, 6,
                sagittalShift, tangentialShift, 40.0, new TraceOptions(), true);

        var calibrationOptions = new ContrastOptions(40.0).calibrate_frequency(true);
        double sagittalScale = ContrastAnalysis.exit_pupil_frequency_calibration(
                model, field, wavelength, normalizedShift, Orientation.X, calibrationOptions);
        double tangentialScale = ContrastAnalysis.exit_pupil_frequency_calibration(
                model, field, wavelength, normalizedShift, Orientation.Y, calibrationOptions);
        var calibrated = model.seq_model.trace_contrast(
                US003549241Example05Test::exitPupilSeparations,
                fieldIndex, wavelengthIndex, 1, 6,
                sagittalShift.times(sagittalScale), tangentialShift.times(tangentialScale),
                40.0, new TraceOptions(), false);

        double requestedShift = ExitPupilAiming.referenceSphereShift(
                model, field, wavelength, 40.0);
        double legacyParaxialShift = normalizedShift
                * Math.abs(model.optical_spec.parax_data.fod.exp_radius);
        assertTrue(Math.abs(requestedShift - legacyParaxialShift) > 0.01,
                "wide-angle reference-sphere scale should differ from the paraxial pupil scale");

        double unaimedError = maxSeparationError(unaimed.get(0).samples(), requestedShift);
        double calibratedError = maxSeparationError(
                calibrated.get(0).samples(), requestedShift);
        double aimedError = maxSeparationError(aimed.get(0).samples(), requestedShift);

        assertTrue(unaimedError > 0.5,
                "unaimed shear should expose pupil mapping error: " + unaimedError);
        assertTrue(calibratedError < unaimedError * 0.2,
                "block calibration should materially improve the shear: " + calibratedError);
        assertTrue(calibratedError > 0.02,
                "block calibration should retain its across-pupil approximation error: "
                        + calibratedError);
        assertTrue(aimedError < 2.0e-6,
                "aimed shear should reach the reference-sphere target: " + aimedError);
        assertTrue(aimedError * 100_000.0 < unaimedError,
                "aiming should improve full-field shear by at least five orders of magnitude");
        assertTrue(aimedError * 10_000.0 < calibratedError,
                "direct aiming should be substantially more accurate than block calibration: "
                        + aimedError + " against " + calibratedError);
    }

    private static double[] exitPupilSeparations(
            ContrastRayTriplet rays, Field field, double wavelength, double focus) {
        assertNull(rays.referenceError());
        assertNull(rays.sagittalError());
        assertNull(rays.tangentialError());
        Vector3 reference = ExitPupilAiming.sphere_coord(
                rays.reference(), field.chief_ray, field.ref_sphere);
        Vector3 sagittal = ExitPupilAiming.sphere_coord(
                rays.sagittal(), field.chief_ray, field.ref_sphere);
        Vector3 tangential = ExitPupilAiming.sphere_coord(
                rays.tangential(), field.chief_ray, field.ref_sphere);
        assertNotNull(reference);
        assertNotNull(sagittal);
        assertNotNull(tangential);
        return new double[]{sagittal.x - reference.x, sagittal.y - reference.y,
                tangential.x - reference.x, tangential.y - reference.y};
    }

    private static double maxSeparationError(List<double[]> separations, double requested) {
        double maximum = 0.0;
        for (double[] separation : separations) {
            maximum = Math.max(maximum, Math.hypot(separation[0] - requested, separation[1]));
            maximum = Math.max(maximum, Math.hypot(separation[2], separation[3] - requested));
        }
        return maximum;
    }
}
