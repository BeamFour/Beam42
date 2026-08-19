package org.redukti.rayoptics.analysis;

import org.junit.jupiter.api.Test;
import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.integration.US003549241Example05;
import org.redukti.rayoptics.raytr.ContrastRayTriplet;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.util.Orientation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class US003549241Example05Test {

    @Test
    void exitPupilAimingCorrectsFullFieldShear() {
        var model = US003549241Example05.build();
        int fieldIndex = 2;
        int wavelengthIndex = model.optical_spec.wvls.reference_wvl;
        double wavelength = model.optical_spec.wvls.wavelengths[wavelengthIndex];
        double normalizedShift = ContrastAnalysis.normalized_entry_pupil_shift(
                model, wavelength, 40.0);
        double requestedShift = normalizedShift
                * Math.abs(model.optical_spec.parax_data.fod.exp_radius);
        Vector2 sagittalShift = new Vector2(normalizedShift, 0.0);
        Vector2 tangentialShift = new Vector2(0.0, normalizedShift);
        Field field = model.optical_spec.fov.fields[fieldIndex];

        var unaimed = model.seq_model.trace_contrast(
                US003549241Example05Test::exitPupilSeparations,
                fieldIndex, wavelengthIndex, 1, 6,
                sagittalShift, tangentialShift, new TraceOptions(), false);
        var aimed = model.seq_model.trace_contrast(
                US003549241Example05Test::exitPupilSeparations,
                fieldIndex, wavelengthIndex, 1, 6,
                sagittalShift, tangentialShift, new TraceOptions(), true);

        var calibrationOptions = new ContrastOptions(40.0).calibrate_frequency(true);
        double sagittalScale = ContrastAnalysis.exit_pupil_frequency_calibration(
                model, field, wavelength, normalizedShift, Orientation.X, calibrationOptions);
        double tangentialScale = ContrastAnalysis.exit_pupil_frequency_calibration(
                model, field, wavelength, normalizedShift, Orientation.Y, calibrationOptions);
        var calibrated = model.seq_model.trace_contrast(
                US003549241Example05Test::exitPupilSeparations,
                fieldIndex, wavelengthIndex, 1, 6,
                sagittalShift.times(sagittalScale), tangentialShift.times(tangentialScale),
                new TraceOptions(), false);

        double unaimedError = maxSeparationError(unaimed.get(0).samples(), requestedShift);
        double calibratedError = maxSeparationError(
                calibrated.get(0).samples(), requestedShift);
        double aimedError = maxSeparationError(aimed.get(0).samples(), requestedShift);

        assertTrue(unaimedError > 0.5,
                "unaimed shear should expose pupil mapping error: " + unaimedError);
        assertTrue(calibratedError < unaimedError * 0.2,
                "block calibration should materially improve the shear: " + calibratedError);
        assertTrue(calibratedError > 0.05,
                "block calibration should retain its across-pupil approximation error: "
                        + calibratedError);
        assertTrue(aimedError < 2.0e-6,
                "aimed shear should reach the reference-sphere target: " + aimedError);
        assertTrue(aimedError * 100_000.0 < unaimedError,
                "aiming should improve full-field shear by at least five orders of magnitude");
        assertTrue(aimedError * 50_000.0 < calibratedError,
                "direct aiming should be substantially more accurate than block calibration");
    }

    private static double[] exitPupilSeparations(
            ContrastRayTriplet rays, Field field, double wavelength, double focus) {
        assertNull(rays.referenceError());
        assertNull(rays.sagittalError());
        assertNull(rays.tangentialError());
        Vector3 reference = PupilShear.exit_pupil_sphere_coord(
                rays.reference(), field.chief_ray, field.ref_sphere);
        Vector3 sagittal = PupilShear.exit_pupil_sphere_coord(
                rays.sagittal(), field.chief_ray, field.ref_sphere);
        Vector3 tangential = PupilShear.exit_pupil_sphere_coord(
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
