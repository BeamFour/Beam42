// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.analysis;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.analysis.WavefrontAberrationAnalysis;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.*;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.util.Lists;
import org.redukti.rayoptics.util.Orientation;
import java.util.ArrayList;
import java.util.List;

/** Wavefront-difference analysis used by contrast optimization. */
public class ContrastAnalysis {

    public static ContrastAnalysisResult eval(OpticalModel opticalModel, ContrastOptions options) {
        if (options.aimExitPupil && options.calibrateFrequency) {
            throw new IllegalArgumentException(
                    "Exit-pupil aiming cannot be combined with contrast frequency calibration");
        }
        var result = new ContrastAnalysisResult(options.spatialFrequency);
        var fields = opticalModel.optical_spec.fov.fields;
        var wavelengths = opticalModel.optical_spec.wvls.wavelengths;
        for (int fieldIndex = 0; fieldIndex < fields.length; fieldIndex++) {
            var wavelengthResults = new ArrayList<ContrastAnalysisResult.WavelengthResult>();
            for (int wavelengthIndex = 0; wavelengthIndex < wavelengths.length; wavelengthIndex++) {
                double wavelength = wavelengths[wavelengthIndex];
                // Convert frequency to a nominal shift in normalized entrance-pupil radius units.
                // The paper requires the shift to be calculated at exit pupil, so this
                // strategy is only good when there is little pupil aberration.
                // In practice, it seems to work well except for outer-most fields / wide lenses
                // For those scenarios we have additional calibration below but these are optional.
                double shift = normalized_entry_pupil_shift(opticalModel, wavelength, options.spatialFrequency);
                double sagittalShift = shift;
                double tangentialShift = shift;
                if (options.calibrateFrequency) {
                    // This is the first calibration:
                    // Block calibration applies one scale per orientation for this field and wavelength.
                    // This traces one probe pair per field, wavelength and direction at ±shift/2,
                    // measures the realised direction-cosine difference, and scales the entrance-pupil
                    // shift by λν / realised.
                    // Four extra rays per field and wavelength.
                    // This is a relatively cheap entrance to exit pupil correction that gets us
                    // 90% of the required correction.
                    sagittalShift *= exit_pupil_frequency_calibration(
                            opticalModel, fields[fieldIndex], wavelength, shift, Orientation.X, options);
                    tangentialShift *= exit_pupil_frequency_calibration(
                            opticalModel, fields[fieldIndex], wavelength, shift, Orientation.Y, options);
                }
                // For greater accuracy in calibration,
                // Direct exit-pupil aiming is the alternative: when enabled, trace_triplets() adjusts each
                // partner separately. This is an expensive iterative adjustment of the shift amount.
                // It is enabled by options.aimExitPupil. By default, aimExitPupil is off.
                var traced = trace_by_wavelength(opticalModel,
                        (rays, field, tracedWavelength, focus) -> sample(
                                opticalModel, rays, field, tracedWavelength, focus, options),
                        fieldIndex, wavelengthIndex,
                        options.numRings, options.numSpokes,
                        new Vector2(sagittalShift, 0.0), new Vector2(0.0, tangentialShift),
                        options.spatialFrequency, options.traceOptions, options.aimExitPupil);
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
    public static double exit_pupil_frequency_calibration(
            OpticalModel opticalModel, Field field, double wavelength,
            double shift, int axis, ContrastOptions options) {
        if (!options.calibrateFrequency || !(shift > 0.0)) return 1.0;
        double required = opticalModel.nm_to_sys_units(wavelength) * options.spatialFrequency;
        if (!(required > 0.0)) return 1.0;

        var traceOptions = options.traceOptions.copy();
        traceOptions.pupil_type = PupilType.REL_PUPIL;
        traceOptions.apply_vignetting = false;

        // Set the pupil up exactly as trace_by_wavelength does for the sampling
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
        return sample;
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

    /**
     * Trace contrast-optimization ray triplets and apply the analysis callback
     * while the wavelength-specific chief ray and reference sphere are active.
     */
    public static <T> List<ContrastTraceByWvl<T>> trace_by_wavelength(
            OpticalModel opt_model,
            ContrastTraceCallback<T> callback, int fi, Integer wl,
            int num_rings, Integer num_spokes,
            Vector2 sagittal_shift, Vector2 tangential_shift,
            TraceOptions trace_options) {
        return trace_by_wavelength(opt_model, callback, fi, wl, num_rings, num_spokes,
                sagittal_shift, tangential_shift, 0.0, trace_options, false);
    }

    public static <T> List<ContrastTraceByWvl<T>> trace_by_wavelength(
            OpticalModel opt_model,
            ContrastTraceCallback<T> callback, int fi, Integer wl,
            int num_rings, Integer num_spokes,
            Vector2 sagittal_shift, Vector2 tangential_shift,
            double spatial_frequency,
            TraceOptions trace_options, boolean aim_exit_pupil) {
        var osp = opt_model.optical_spec;
        var wavelengths = osp.wvls.wavelengths;
        double[] wavelengthList = wl == null
                ? wavelengths
                : new double[]{wavelengths[wl]};
        var field = osp.fov.fields[fi];
        var focus = osp.defocus().get_focus();
        var referenceWavelength = opt_model.optical_spec.wvls.central_wvl();;
        var referenceCoordinates = Trace.setup_pupil_coords(
                opt_model, field, referenceWavelength, focus, null, null);
        var referenceImagePoint = referenceCoordinates.ref_sphere.image_pt.project_xy();

        var result = new ArrayList<ContrastTraceByWvl<T>>();
        var definition = new TraceRingsDef();
        definition.num_rings = num_rings;
        for (double wavelength : wavelengthList) {
            var coordinates = Trace.setup_pupil_coords(
                    opt_model, field, wavelength, focus, referenceImagePoint, null);
            field.chief_ray = coordinates.chief_ray_pkg;
            field.ref_sphere = coordinates.ref_sphere;
            Vector2 sagittalExitShift = null;
            Vector2 tangentialExitShift = null;
            if (aim_exit_pupil) {
                // Convert image-space frequency to a physical separation on this field's
                // wavelength-specific reference sphere: Delta = |R| lambda |nu| / |n_img|,
                // with lambda in system length units and nu in reciprocal length units.
                // The vectors below are target offsets from each reference ray's sphere
                // coordinate. trace_triplets() inverse-aims the partners to those targets;
                // the supplied normalized entrance-pupil shifts provide the initial guesses.
                double physicalShift = ExitPupilAiming.referenceSphereShift(
                        opt_model, field, wavelength, spatial_frequency);
                sagittalExitShift = new Vector2(physicalShift, 0.0);
                tangentialExitShift = new Vector2(0.0, physicalShift);
            }
            var rays = trace_triplets(
                    opt_model, definition, num_spokes,
                    sagittal_shift, tangential_shift,
                    sagittalExitShift, tangentialExitShift,
                    field, wavelength, trace_options, aim_exit_pupil);
            var samples = new ArrayList<T>(rays.size());
            for (var ray : rays) {
                samples.add(callback.apply(ray, field, wavelength, focus));
            }
            result.add(new ContrastTraceByWvl<>(wavelength, samples));
        }
        return result;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Contrast trace is a Beam42 enhancement.
    /// Its main use is in optimization
    /// See Documentation/OPTIMIZER_NOTES.md
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Trace the reference and the two displaced rays required by contrast
     * optimization. Pupil displacements are expressed in relative pupil
     * coordinates of the physically vignetted pupil. Every quadrature sample
     * is returned so clients can keep a stable residual layout; a ray is null
     * when the trace fails.
     */
    public static List<ContrastRayTriplet> trace_triplets(
            OpticalModel opt_model, TraceRingsDef grid_rng, Integer num_spokes,
            Vector2 sagittal_shift, Vector2 tangential_shift,
            Field fld, double wvl, TraceOptions trace_options) {
        return trace_triplets(opt_model, grid_rng, num_spokes,
                sagittal_shift, tangential_shift, null, null,
                fld, wvl, trace_options, false);
    }

    /**
     * Trace contrast triplets, optionally inverse-aiming each displaced partner at its
     * requested coordinate separation on the exit-pupil reference sphere.
     */
    public static List<ContrastRayTriplet> trace_triplets(
            OpticalModel opt_model, TraceRingsDef grid_rng, Integer num_spokes,
            Vector2 sagittal_shift, Vector2 tangential_shift,
            Vector2 sagittal_exit_shift, Vector2 tangential_exit_shift,
            Field fld, double wvl, TraceOptions trace_options,
            boolean aim_exit_pupil) {
        if (aim_exit_pupil
                && (sagittal_exit_shift == null || tangential_exit_shift == null)) {
            throw new IllegalArgumentException(
                    "Physical exit-pupil shifts are required when aiming is enabled");
        }
        trace_options = trace_options.copy();
        // The quadrature generator explicitly maps samples into the physical
        // vignetted pupil. Applying Field vignetting in trace_base as well
        // would independently scale the displaced rays and change their MTF
        // shear, especially when a pair straddles a pupil axis.
        // ContrastOptions defaults aperture checking off so temporary clipping does
        // not become a discontinuous optimizer failure, but honour an explicit caller
        // choice to validate against the physical surface apertures.
        trace_options.pupil_type = PupilType.REL_PUPIL;
        trace_options.apply_vignetting = false;
        trace_options.rayerr_filter = "summary";

        var samples = new ArrayList<ContrastRayTriplet>();
        var points = generate_quadrature(
                grid_rng, num_spokes, sagittal_shift, tangential_shift, fld);
        for (var point : points) {
            // Each sample traces a reference ray and sagittal/tangential partners.
            // The supplied shifts are normalized entrance-pupil offsets.
            // When aim_exit_pupil is enabled, these provide initial guesses;
            // each partner is inverse-aimed to a physical reference-sphere separation
            // Delta = |R| lambda nu / |n_img| from the reference ray.
            // Here lambda is in system length units and nu is image-space frequency.
            var pupil = point.pupil();
            var sagittalEntrancePupil = pupil.plus(sagittal_shift);
            var tangentialEntrancePupil = pupil.plus(tangential_shift);
            var reference = Trace.trace_safe(opt_model, pupil, fld, wvl, trace_options);
            RayResult sagittal;
            RayResult tangential;
            if (aim_exit_pupil && reference.pkg != null) {
                var referenceCoordinate = ExitPupilAiming.sphere_coord(
                        reference.pkg, fld.chief_ray, fld.ref_sphere);
                if (referenceCoordinate == null) {
                    var error = new ExitPupilAiming.ExitPupilAimException(
                            "Reference ray has no finite exit-pupil coordinate");
                    error.surf = -1;
                    sagittal = new RayResult(null, error);
                    tangential = new RayResult(null, error);
                } else {
                    var sagittalExitPupilTarget = new Vector2(
                            referenceCoordinate.x + sagittal_exit_shift.x,
                            referenceCoordinate.y + sagittal_exit_shift.y);
                    var tangentialExitPupilTarget = new Vector2(
                            referenceCoordinate.x + tangential_exit_shift.x,
                            referenceCoordinate.y + tangential_exit_shift.y);
                    sagittal = ExitPupilAiming.aim(opt_model, sagittalEntrancePupil, sagittalExitPupilTarget,
                            fld, wvl, trace_options).ray();
                    tangential = ExitPupilAiming.aim(opt_model, tangentialEntrancePupil, tangentialExitPupilTarget,
                            fld, wvl, trace_options).ray();
                }
            } else {
                sagittal = Trace.trace_safe(opt_model, sagittalEntrancePupil, fld, wvl, trace_options);
                tangential = Trace.trace_safe(opt_model, tangentialEntrancePupil, fld, wvl, trace_options);
            }
            samples.add(new ContrastRayTriplet(
                    pupil, reference.pkg, sagittal.pkg, tangential.pkg,
                    reference.err, sagittal.err, tangential.err, point.weight()));
        }
        return samples;
    }

    /**
     * Smallest usable contraction of the contrast quadrature pattern. Below this
     * the samples crowd around the overlap centre, all three rays of a triplet
     * report nearly the same wavefront, and the residuals collapse towards zero -
     * which an optimizer would read as perfect contrast rather than as an
     * unusable request. The pattern reaches this limit a little below the
     * 0.707/(lambda*F#) frequency at which the overlap centre itself leaves the
     * pupil, so both bounds are reported as errors rather than silently honoured.
     */
    private static final double MIN_CONTRACTION = 0.1;

    /**
     * Generate fixed-count quadrature samples in the physical vignetted pupil.
     * The complete pattern is contracted about the overlap centre until every
     * sample and both of its displaced partners are valid. A common contraction
     * preserves relative quadrature weights and avoids frequency-dependent ray
     * loss at the pupil boundary.
     */
    public static List<GaussianQuadraturePoint> generate_quadrature(
            TraceRingsDef grid_rng, Integer num_spokes,
            Vector2 sagittal_shift, Vector2 tangential_shift, Field fld) {
        var nominal = Trace.generate_gaussian_quadrature(
                grid_rng, grid_rng.num_rings, num_spokes);
        var nominalCenter = new Vector2(grid_rng.cx, grid_rng.cy);
        var physicalCenter = apply_vignetting(nominalCenter, fld);
        var overlapCenter = physicalCenter.minus(
                sagittal_shift.plus(tangential_shift).times(0.5));

        if (!valid_point(overlapCenter, sagittal_shift, tangential_shift, fld)) {
            throw new IllegalArgumentException(
                    "The requested contrast shear has no common vignetted pupil overlap");
        }

        // Map the nominal pattern into the physical pupil once. The contraction
        // search only rescales these offsets, so the map must not be repeated
        // inside the bisection loop.
        var offsets = new ArrayList<Vector2>(nominal.size());
        for (var point : nominal)
            offsets.add(apply_vignetting(point.pupil(), fld).minus(physicalCenter));

        double contraction = 1.0;
        if (!valid_pattern(offsets, overlapCenter, contraction,
                sagittal_shift, tangential_shift, fld)) {
            double low = 0.0;
            double high = 1.0;
            // Converge to machine precision: the contraction must be a
            // repeatable function of the design so that the finite-difference
            // Jacobian does not see quantisation steps.
            for (int iteration = 0; iteration < 60; iteration++) {
                double trial = 0.5 * (low + high);
                if (valid_pattern(offsets, overlapCenter, trial,
                        sagittal_shift, tangential_shift, fld)) {
                    low = trial;
                } else {
                    high = trial;
                }
            }
            contraction = low;
        }
        if (contraction < MIN_CONTRACTION) {
            throw new IllegalArgumentException(
                    "The requested contrast shear leaves too little vignetted pupil overlap"
                            + " to sample: the pattern would contract to " + contraction
                            + " of its nominal extent");
        }

        var points = new ArrayList<GaussianQuadraturePoint>(nominal.size());
        double weightSum = 0.0;
        for (int i = 0; i < nominal.size(); i++) {
            var point = nominal.get(i);
            var pupil = overlapCenter.plus(offsets.get(i).times(contraction));
            double weight = point.weight() * vignetting_jacobian(point.pupil(), fld);
            points.add(new GaussianQuadraturePoint(pupil, weight));
            weightSum += weight;
        }
        if (!(weightSum > 0.0)) {
            throw new IllegalArgumentException("The vignetted pupil has zero area");
        }
        final double normalization = weightSum;
        return points.stream()
                .map(point -> new GaussianQuadraturePoint(
                        point.pupil(), point.weight() / normalization))
                .toList();
    }

    private static boolean valid_pattern(
            List<Vector2> offsets, Vector2 overlapCenter, double contraction,
            Vector2 sagittalShift, Vector2 tangentialShift, Field fld) {
        for (var offset : offsets) {
            var pupil = overlapCenter.plus(offset.times(contraction));
            if (!valid_point(pupil, sagittalShift, tangentialShift, fld)) {
                return false;
            }
        }
        return true;
    }

    private static boolean valid_point(
            Vector2 pupil, Vector2 sagittalShift, Vector2 tangentialShift, Field fld) {
        return inside_vignetted_pupil(pupil, fld)
                && inside_vignetted_pupil(pupil.plus(sagittalShift), fld)
                && inside_vignetted_pupil(pupil.plus(tangentialShift), fld);
    }

    public static boolean inside_vignetted_pupil(Vector2 pupil, Field fld) {
        double xScale = fld.vignetting_scale_x(pupil.x);
        double yScale = fld.vignetting_scale_y(pupil.y);
        if (!(xScale > 0.0) || !(yScale > 0.0)) {
            return false;
        }
        double x = pupil.x / xScale;
        double y = pupil.y / yScale;
        return x * x + y * y <= 1.0 + 1.0e-14;
    }

    private static Vector2 apply_vignetting(Vector2 pupil, Field fld) {
        double[] vignetted = fld.apply_vignetting(pupil.as_array());
        return new Vector2(vignetted[0], vignetted[1]);
    }

    /** Area element of {@link Field#apply_vignetting} at a nominal pupil point. */
    private static double vignetting_jacobian(Vector2 pupil, Field fld) {
        return fld.vignetting_scale_x(pupil.x) * fld.vignetting_scale_y(pupil.y);
    }
}
