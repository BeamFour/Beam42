package org.redukti.rayoptics.analysis;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.ContrastRayTriplet;
import org.redukti.rayoptics.raytr.PupilType;
import org.redukti.rayoptics.raytr.Trace;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.util.Lists;
import org.redukti.rayoptics.util.Orientation;
import java.util.ArrayList;

/** Wavefront-difference analysis used by contrast optimization. */
public class ContrastAnalysis {

    public static ContrastAnalysisResult eval(OpticalModel opticalModel, ContrastOptions options) {
        var result = new ContrastAnalysisResult(options.spatialFrequency);
        var fields = opticalModel.optical_spec.fov.fields;
        var wavelengths = opticalModel.optical_spec.wvls.wavelengths;
        for (int fieldIndex = 0; fieldIndex < fields.length; fieldIndex++) {
            var wavelengthResults = new ArrayList<ContrastAnalysisResult.WavelengthResult>();
            for (int wavelengthIndex = 0; wavelengthIndex < wavelengths.length; wavelengthIndex++) {
                double wavelength = wavelengths[wavelengthIndex];
                double shift = normalized_entry_pupil_shift(opticalModel, wavelength, options.spatialFrequency);
                double sagittalShift = shift * exit_pupil_frequency_calibration(
                        opticalModel, fields[fieldIndex], wavelength, shift, Orientation.X, options);
                double tangentialShift = shift * exit_pupil_frequency_calibration(
                        opticalModel, fields[fieldIndex], wavelength, shift, Orientation.Y, options);
                var traced = opticalModel.seq_model.trace_contrast(
                        (rays, field, tracedWavelength, focus) -> sample(
                                opticalModel, rays, field, tracedWavelength, focus, options),
                        fieldIndex, wavelengthIndex,
                        options.numRings, options.numSpokes,
                        new Vector2(sagittalShift, 0.0), new Vector2(0.0, tangentialShift),
                        options.traceOptions);
                wavelengthResults.add(new ContrastAnalysisResult.WavelengthResult(
                        wavelength, shift, traced.get(0).samples()));
            }
            result.fields.add(new ContrastAnalysisResult.FieldResult(fields[fieldIndex], wavelengthResults));
        }
        if (options.centerResiduals)
            center_residuals(result, opticalModel.optical_spec.wvls.reference_wvl);
        return result;
    }

    /**
     * Remove the constant part of the wavefront difference from every residual.
     *
     * <p>{@code |OTF| = |<exp(i.Phi)>| ~ 1 - Var(Phi)/2} depends on the <em>variance</em>
     * of the phase difference. A constant {@code dW} across the pupil is wavefront tilt,
     * which is an image displacement: it moves the phase transfer function and leaves the
     * modulus alone. Left in, it contributes {@code mean^2} to the sum of squares - up to
     * 57% of an outer-field tangential block - and, being reducible by adding tilt, offers
     * the solver merit reduction that corresponds to no optical improvement at all.
     *
     * <p>The mean is subtracted per (field, orientation), <em>not</em> per wavelength. A
     * tilt common to every wavelength is a harmless image shift, but one that differs
     * between wavelengths is lateral colour, and that genuinely does reduce polychromatic
     * MTF because the per-wavelength complex OTFs acquire different phases and partly
     * cancel. Subtracting the reference wavelength's mean from all of them discards the
     * common part and preserves the difference.
     *
     * <p>Defocus is untouched: it makes {@code dW} linear in the shear direction rather
     * than constant, so its mean over a symmetric pupil is already zero. The sagittal mean
     * is identically zero by symmetry on a rotationally symmetric system, so in practice
     * only tangential residuals move.
     */
    static void center_residuals(ContrastAnalysisResult result, int referenceWavelengthIndex) {
        for (var field : result.fields) {
            var wavelengths = field.wavelengths();
            int reference = referenceWavelengthIndex;
            if (reference < 0 || reference >= wavelengths.size()) continue;
            double sagittalOffset = weighted_mean(wavelengths.get(reference), Orientation.SAGITTAL);
            double tangentialOffset = weighted_mean(wavelengths.get(reference), Orientation.TANGENTIAL);
            if (!Double.isFinite(sagittalOffset) || !Double.isFinite(tangentialOffset)) continue;
            for (int i = 0; i < wavelengths.size(); i++)
                wavelengths.set(i, wavelengths.get(i).withOffsets(sagittalOffset, tangentialOffset));
        }
    }

    /** Quadrature-weighted mean difference over the valid samples of one block. */
    private static double weighted_mean(
            ContrastAnalysisResult.WavelengthResult wavelength, int orientation) {
        double weightedSum = 0.0;
        double weightSum = 0.0;
        for (var sample : wavelength.samples()) {
            if (!sample.valid()) continue;
            double difference = orientation == Orientation.SAGITTAL
                    ? sample.sagittalDifference() : sample.tangentialDifference();
            if (!Double.isFinite(difference)) continue;
            weightedSum += sample.weight() * difference;
            weightSum += sample.weight();
        }
        return weightSum > 0.0 ? weightedSum / weightSum : 0.0;
    }

    /**
     * Convert frequency to a displacement in normalized <em>entrance</em> pupil-radius
     * units. The displacement is 2 at the incoherent diffraction cutoff 1/(lambda F#).
     *
     * <p>Note the mismatch the name records: the magnitude comes from an exit-pupil
     * relation, since that is where the OTF autocorrelation is defined, but it is applied
     * in entrance-pupil coordinates because that is what {@code REL_PUPIL} means. Those
     * coincide only where the pupil imaging is aberration free. See
     * {@link #exit_pupil_frequency_calibration}, which measures and corrects the
     * difference when {@link ContrastOptions#calibrate_frequency(boolean)} is enabled.
     */
    public static double normalized_entry_pupil_shift(
            OpticalModel opticalModel, double wavelength, double spatialFrequency) {
        double wavelengthInSystemUnits = opticalModel.nm_to_sys_units(wavelength);
        double fNumber = Math.abs(opticalModel.optical_spec.parax_data.fod.fno);
        return 2.0 * wavelengthInSystemUnits * fNumber * spatialFrequency;
    }

    /**
     * Scale for the entrance-pupil shift so that the traced pair actually realises the
     * requested image-space spatial frequency.
     *
     * <p>{@code REL_PUPIL} coordinates are normalised on the <em>entrance</em> pupil, but
     * the OTF is the autocorrelation of the <em>exit</em> pupil, and
     * {@link #normalized_entry_pupil_shift} derives its displacement from an exit-pupil relation
     * ({@code 2*lambda*F#*nu}). The two agree only where the pupil imaging is aberration
     * free; real pupil aberration lands a rigid entrance-pupil shift as a smaller,
     * field-dependent exit-pupil shift - around 8% low at full field on a fast lens.
     *
     * <p>A pair of rays forms image-plane fringes of frequency {@code nu} exactly when
     * their image-space direction cosines differ by {@code lambda*nu}, and that holds
     * wherever the exit pupil happens to lie. So one probe pair per field, wavelength and
     * direction measures the frequency the shift really delivers, and its reciprocal
     * corrects it.
     *
     * <p>This removes the field-dependent bias, which is the dominant term. The remaining
     * variation across the pupil (a few percent, from pupil spherical aberration) would
     * need per-ray aiming and is left uncorrected. Cost is four rays per field and
     * wavelength against the couple of hundred used for the samples.
     */
    static double exit_pupil_frequency_calibration(
            OpticalModel opticalModel, Field field, double wavelength,
            double shift, int axis, ContrastOptions options) {
        if (!options.calibrateFrequency || !(shift > 0.0)) return 1.0;
        double required = opticalModel.nm_to_sys_units(wavelength) * options.spatialFrequency;
        if (!(required > 0.0)) return 1.0;

        var traceOptions = options.traceOptions.copy();
        traceOptions.pupil_type = PupilType.REL_PUPIL;
        traceOptions.apply_vignetting = false;

        // Set the pupil up exactly as SequentialModel.trace_contrast does for the sampling
        // rays: take the reference image point from the central wavelength, then establish
        // this wavelength's chief ray and reference sphere against that point. Probing
        // through a differently configured pupil would measure a mapping the samples never
        // see, which is the one way this correction could do harm rather than nothing.
        double focus = opticalModel.optical_spec.defocus().get_focus();
        var reference = Trace.setup_pupil_coords(opticalModel, field,
                opticalModel.seq_model.central_wavelength(), focus, null, null);
        var coordinates = Trace.setup_pupil_coords(opticalModel, field, wavelength, focus,
                reference.ref_sphere.image_pt.project_xy(), null);
        field.chief_ray = coordinates.chief_ray_pkg;
        field.ref_sphere = coordinates.ref_sphere;

        double half = 0.5 * shift;
        Double low = image_direction(opticalModel, field, wavelength, traceOptions, axis,
                axis == Orientation.X ? new Vector2(-half, 0.0) : new Vector2(0.0, -half));
        Double high = image_direction(opticalModel, field, wavelength, traceOptions, axis,
                axis == Orientation.X ? new Vector2(half, 0.0) : new Vector2(0.0, half));
        if (low == null || high == null) return 1.0;

        double realized = Math.abs(high - low);
        if (!Double.isFinite(realized) || realized < 1.0e-12) return 1.0;
        double scale = required / realized;
        // A correction this far from unity says the probe failed rather than that the
        // pupil mapping is unusual; leave the shift alone rather than destabilise the merit.
        return scale > 0.5 && scale < 2.0 ? scale : 1.0;
    }

    /** Direction cosine of the traced ray in image space, or null if it did not get there. */
    private static Double image_direction(
            OpticalModel opticalModel, Field field, double wavelength,
            TraceOptions traceOptions, int axis, Vector2 pupil) {
        var pkg = Trace.trace_safe(opticalModel, pupil, field, wavelength, traceOptions).pkg;
        if (pkg == null || pkg.ray == null || pkg.ray.size() < 2) return null;
        var segment = Lists.get(pkg.ray, -2);
        return axis == Orientation.X ? segment.d.x : segment.d.y;
    }

    private static ContrastAnalysisResult.Sample sample(
            OpticalModel opticalModel, ContrastRayTriplet rays,
            org.redukti.rayoptics.specs.Field field,
            double wavelength, double focus, ContrastOptions options) {
        if (rays.reference() == null || rays.sagittal() == null || rays.tangential() == null) {
            return new ContrastAnalysisResult.Sample(
                    rays.pupil(), 0.0, 0.0, rays.weight(), false, failure(rays));
        }
        double reference = WavefrontAberrationAnalysis.opd(
                opticalModel, rays.pupil(), 0, rays.reference(), field, wavelength, focus);
        double sagittal = opd(opticalModel, rays.sagittal(), field, wavelength, focus,
                rays.sagittal().input_pupil) - reference;
        double tangential = opd(opticalModel, rays.tangential(), field, wavelength, focus,
                rays.tangential().input_pupil) - reference;
        boolean valid = Double.isFinite(reference)
                && Double.isFinite(sagittal) && Double.isFinite(tangential);
        var sample = new ContrastAnalysisResult.Sample(
                rays.pupil(), sagittal, tangential, rays.weight(), valid,
                valid ? null : new ContrastAnalysisResult.Failure(
                        "OPD", "NonFiniteWavefrontDifference", -1));

        if (!options.measureFrequency) return sample;
        var shear = measure_shear(opticalModel, rays, field, wavelength);
        sample = sample.withShear(shear);
        if (!options.normalizeFrequency || !valid) return sample;
        return sample.withDifferences(
                normalize(sagittal, shear.sagittalFrequency(), options.spatialFrequency),
                normalize(tangential, shear.tangentialFrequency(), options.spatialFrequency));
    }

    /**
     * The exit-pupil coordinate of the reference ray, the shear its two partners actually
     * produced there, and the spatial frequency each pair realised.
     *
     * <p>The chief ray and reference sphere on {@code field} are the ones the OPD
     * evaluation just used, so the pupil coordinates are consistent with the wavefront
     * differences they accompany.
     */
    static ContrastAnalysisResult.Shear measure_shear(
            OpticalModel opticalModel, ContrastRayTriplet rays,
            org.redukti.rayoptics.specs.Field field, double wavelength) {
        var chiefRay = field.chief_ray;
        var refSphere = field.ref_sphere;
        var reference = PupilShear.exit_pupil_coord(rays.reference(), chiefRay, refSphere);
        var sagittal = PupilShear.exit_pupil_coord(rays.sagittal(), chiefRay, refSphere);
        var tangential = PupilShear.exit_pupil_coord(rays.tangential(), chiefRay, refSphere);
        var sagittalFrequency = PupilShear.realized_frequency(
                opticalModel, rays.reference(), rays.sagittal(), wavelength, Orientation.X);
        var tangentialFrequency = PupilShear.realized_frequency(
                opticalModel, rays.reference(), rays.tangential(), wavelength, Orientation.Y);
        var sagittalPupil = PupilShear.realized_frequency_vector(
                opticalModel, rays.reference(), rays.sagittal(), chiefRay, refSphere, wavelength);
        var tangentialPupil = PupilShear.realized_frequency_vector(
                opticalModel, rays.reference(), rays.tangential(), chiefRay, refSphere, wavelength);
        return new ContrastAnalysisResult.Shear(
                reference,
                reference != null && sagittal != null ? sagittal.minus(reference) : null,
                reference != null && tangential != null ? tangential.minus(reference) : null,
                sagittalFrequency != null ? sagittalFrequency : Double.NaN,
                tangentialFrequency != null ? tangentialFrequency : Double.NaN,
                sagittalPupil, tangentialPupil);
    }

    /**
     * Rescale a wavefront difference from the frequency the pair realised to the one that
     * was requested. Leaves the difference alone if the measurement failed, or if the
     * discrepancy is large enough that the first-order rescaling is not trustworthy -
     * the same guard rail {@link #exit_pupil_frequency_calibration} uses.
     */
    private static double normalize(double difference, double realized, double requested) {
        if (!Double.isFinite(realized) || !(realized > 0.0) || !(requested > 0.0))
            return difference;
        double scale = requested / realized;
        return scale > 0.5 && scale < 2.0 ? difference * scale : difference;
    }

    private static ContrastAnalysisResult.Failure failure(ContrastRayTriplet rays) {
        if (rays.referenceError() != null) return failure("reference", rays.referenceError());
        if (rays.sagittalError() != null) return failure("sagittal", rays.sagittalError());
        if (rays.tangentialError() != null) return failure("tangential", rays.tangentialError());
        return new ContrastAnalysisResult.Failure("unknown", "MissingRay", -1);
    }

    private static ContrastAnalysisResult.Failure failure(
            String ray, org.redukti.rayoptics.exceptions.TraceException error) {
        return new ContrastAnalysisResult.Failure(
                ray, error.getClass().getSimpleName(), error.surf);
    }

    private static double opd(OpticalModel opticalModel,
                              org.redukti.rayoptics.raytr.RayPkg ray,
                              org.redukti.rayoptics.specs.Field field,
                              double wavelength, double focus, Vector2 pupil) {
        // below xy is set to 0 as it is not used by the opd calculation
        return WavefrontAberrationAnalysis.opd(
                opticalModel, pupil, 0, ray, field, wavelength, focus);
    }

}
