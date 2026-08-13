package org.redukti.rayoptics.analysis;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.ContrastRayTriplet;
import org.redukti.rayoptics.raytr.PupilType;
import org.redukti.rayoptics.raytr.Trace;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.util.Lists;

import java.util.ArrayList;

/** Wavefront-difference analysis used by contrast optimization. */
public class ContrastAnalysis {

    private static final int X = 0;
    private static final int Y = 1;

    public static ContrastAnalysisResult eval(OpticalModel opticalModel, ContrastOptions options) {
        var result = new ContrastAnalysisResult(options.spatialFrequency);
        var fields = opticalModel.optical_spec.fov.fields;
        var wavelengths = opticalModel.optical_spec.wvls.wavelengths;
        for (int fieldIndex = 0; fieldIndex < fields.length; fieldIndex++) {
            var wavelengthResults = new ArrayList<ContrastAnalysisResult.WavelengthResult>();
            for (int wavelengthIndex = 0; wavelengthIndex < wavelengths.length; wavelengthIndex++) {
                double wavelength = wavelengths[wavelengthIndex];
                double shift = normalized_pupil_shift(opticalModel, wavelength, options.spatialFrequency);
                double sagittalShift = shift * frequency_calibration(
                        opticalModel, fields[fieldIndex], wavelength, shift, X, options);
                double tangentialShift = shift * frequency_calibration(
                        opticalModel, fields[fieldIndex], wavelength, shift, Y, options);
                var traced = opticalModel.seq_model.trace_contrast(
                        (rays, field, tracedWavelength, focus) -> sample(
                                opticalModel, rays, field, tracedWavelength, focus),
                        fieldIndex, wavelengthIndex,
                        options.numRings, options.numSpokes,
                        new Vector2(sagittalShift, 0.0), new Vector2(0.0, tangentialShift),
                        options.traceOptions);
                wavelengthResults.add(new ContrastAnalysisResult.WavelengthResult(
                        wavelength, shift, traced.get(0).samples()));
            }
            result.fields.add(new ContrastAnalysisResult.FieldResult(fields[fieldIndex], wavelengthResults));
        }
        return result;
    }

    /**
     * Convert frequency to a displacement in normalized pupil-radius units.
     * The displacement is 2 at the incoherent diffraction cutoff 1/(lambda F#).
     */
    public static double normalized_pupil_shift(
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
     * {@link #normalized_pupil_shift} derives its displacement from an exit-pupil relation
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
    private static double frequency_calibration(
            OpticalModel opticalModel, Field field, double wavelength,
            double shift, int axis, ContrastOptions options) {
        if (!options.calibrateFrequency || !(shift > 0.0)) return 1.0;
        double required = opticalModel.nm_to_sys_units(wavelength) * options.spatialFrequency;
        if (!(required > 0.0)) return 1.0;

        var traceOptions = options.traceOptions.copy();
        traceOptions.check_apertures = false;
        traceOptions.pupil_type = PupilType.REL_PUPIL;
        traceOptions.apply_vignetting = false;

        double focus = opticalModel.optical_spec.defocus().get_focus();
        // Establish the same ray aiming the sampling trace will use.
        Trace.setup_pupil_coords(opticalModel, field, wavelength, focus, null, null);

        double half = 0.5 * shift;
        Double low = image_direction(opticalModel, field, wavelength, traceOptions, axis,
                axis == X ? new Vector2(-half, 0.0) : new Vector2(0.0, -half));
        Double high = image_direction(opticalModel, field, wavelength, traceOptions, axis,
                axis == X ? new Vector2(half, 0.0) : new Vector2(0.0, half));
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
        return axis == X ? segment.d.x : segment.d.y;
    }

    private static ContrastAnalysisResult.Sample sample(
            OpticalModel opticalModel, ContrastRayTriplet rays,
            org.redukti.rayoptics.specs.Field field,
            double wavelength, double focus) {
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
        return new ContrastAnalysisResult.Sample(
                rays.pupil(), sagittal, tangential, rays.weight(), valid,
                valid ? null : new ContrastAnalysisResult.Failure(
                        "OPD", "NonFiniteWavefrontDifference", -1));
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
        return WavefrontAberrationAnalysis.opd(
                opticalModel, pupil, 0, ray, field, wavelength, focus);
    }

}
