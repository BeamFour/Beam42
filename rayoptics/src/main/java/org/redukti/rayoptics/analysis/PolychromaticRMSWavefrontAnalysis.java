package org.redukti.rayoptics.analysis;

import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.GridItem;
import org.redukti.rayoptics.raytr.Trace;
import org.redukti.rayoptics.raytr.TraceRingsDef;
import org.redukti.rayoptics.specs.ReadOnlyField;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes a chief-referenced polychromatic RMS wavefront error for every field.
 *
 * <p>The primary wavelength's chief-ray image point is shared by all wavelengths.
 * Each wavelength is nevertheless traced with its own chief ray and reference sphere.
 * Piston is removed independently from each wavelength's pupil samples; wavefront tilt
 * is retained. The final value pools the centered second moments using the product of
 * spectral weight and Gaussian pupil-quadrature weight.
 *
 * <p>Gaussian quadrature is valid for the circular/elliptical pupil produced by the
 * model's vignetting factors. Surface-aperture checking is therefore disabled by default.
 * A clipped, non-elliptical effective pupil requires rectangular-array integration rather
 * than deletion of clipped Gaussian nodes.
 */
public final class PolychromaticRMSWavefrontAnalysis {
    private PolychromaticRMSWavefrontAnalysis() {}

    public static PolychromaticRMSWavefrontResult eval(OpticalModel model) {
        return eval(model, new PolychromaticRMSWavefrontOptions());
    }

    public static PolychromaticRMSWavefrontResult eval(
            OpticalModel model, PolychromaticRMSWavefrontOptions options) {
        if (model == null) throw new IllegalArgumentException("Optical model cannot be null");
        if (options == null) throw new IllegalArgumentException("Options cannot be null");

        var opticalSpecs = model.optical_spec;
        var wavelengths = opticalSpecs.wvls;
        validateSpectrum(wavelengths.wavelengths, wavelengths.spectral_wts,
                wavelengths.reference_wvl);
        var focus = opticalSpecs.defocus().get_focus();
        var definition = new TraceRingsDef();
        definition.num_rings = options.numRings;
        definition.min_radius = options.innerPupilRadius;
        var fields = new ArrayList<PolychromaticRMSWavefrontResult.FieldResult>();

        for (var field : opticalSpecs.fov.fields) {
            // Chief-ray aiming is cached on Field. Clear it so this analysis depends only
            // on its arguments, not on whichever wavelength a previous analysis traced.
            field.update();
            var primary = Trace.setup_pupil_coords(
                    model, field, wavelengths.central_wvl(), focus, null, null);
            field.chief_ray = primary.chief_ray_pkg;
            field.ref_sphere = primary.ref_sphere;
            var referenceImagePoint = primary.ref_sphere.image_pt.project_xy();
            var wavelengthResults = new ArrayList<PolychromaticRMSWavefrontResult.WavelengthResult>();
            double totalWeight = 0.0;
            double totalWeightedSquares = 0.0;
            int validSamples = 0;

            try {
                for (int wi = 0; wi < wavelengths.wavelengths.length; wi++) {
                    double wavelength = wavelengths.wavelengths[wi];
                    double spectralWeight = wavelengths.spectral_wts[wi];

                    var coordinates = Trace.setup_pupil_coords(
                            model, field, wavelength, focus, referenceImagePoint, null);
                    field.chief_ray = coordinates.chief_ray_pkg;
                    field.ref_sphere = coordinates.ref_sphere;

                    var samples = Trace.trace_gaussian_quadrature(
                            model, definition, options.numSpokes, field, wavelength, focus,
                            (pupil, ray) -> {
                                if (ray == null) return null;
                                Double opd = WavefrontAberrationAnalysis.opd(
                                        model, pupil, 0, ray, field, wavelength, focus);
                                return opd != null && Double.isFinite(opd)
                                        ? new GridItem(pupil, ray, opd)
                                        : null;
                            }, true, options.traceOptions);

                    var wavelengthResult = summarize(wavelength, spectralWeight, samples);
                    if (spectralWeight > 0.0 && !(wavelengthResult.pupilWeight() > 0.0)) {
                        throw new IllegalStateException(
                                "No valid wavefront samples for field " + field.x + ", " +
                                        field.y + " at wavelength " + wavelength);
                    }
                    wavelengthResults.add(wavelengthResult);
                    totalWeight += spectralWeight * wavelengthResult.pupilWeight();
                    totalWeightedSquares += spectralWeight *
                            wavelengthResult.centeredWeightedSquareSum();
                    validSamples += wavelengthResult.validSamples();
                }
            } finally {
                // Leave the public field cache in its documented central-wavelength state,
                // including when a wavelength trace fails.
                field.chief_ray = primary.chief_ray_pkg;
                field.ref_sphere = primary.ref_sphere;
            }

            double rms = Math.sqrt(Math.max(0.0, totalWeightedSquares) / totalWeight);
            fields.add(new PolychromaticRMSWavefrontResult.FieldResult(
                    new ReadOnlyField(field), rms, totalWeight, validSamples,
                    wavelengthResults));
        }
        return new PolychromaticRMSWavefrontResult(fields);
    }

    static PolychromaticRMSWavefrontResult.WavelengthResult summarize(
            double wavelength, double spectralWeight, List<GridItem> samples) {
        double pupilWeight = 0.0;
        double weightedSum = 0.0;
        double weightedSquareSum = 0.0;
        int validSamples = 0;
        for (var sample : samples) {
            if (!sample.valid || sample.result == null ||
                    !Double.isFinite(sample.result) || !(sample.weight > 0.0)) continue;
            pupilWeight += sample.weight;
            weightedSum += sample.weight * sample.result;
            weightedSquareSum += sample.weight * sample.result * sample.result;
            validSamples++;
        }
        double centeredWeightedSquareSum = pupilWeight > 0.0
                ? Math.max(0.0, weightedSquareSum - weightedSum * weightedSum / pupilWeight)
                : 0.0;
        return new PolychromaticRMSWavefrontResult.WavelengthResult(
                wavelength, spectralWeight, pupilWeight, weightedSum, weightedSquareSum,
                centeredWeightedSquareSum, validSamples);
    }

    private static void validateSpectrum(
            double[] wavelengths, double[] spectralWeights, int referenceIndex) {
        if (wavelengths == null || wavelengths.length == 0)
            throw new IllegalArgumentException("At least one wavelength is required");
        if (spectralWeights == null || spectralWeights.length != wavelengths.length)
            throw new IllegalArgumentException(
                    "Wavelength and spectral-weight arrays must have the same length");
        if (referenceIndex < 0 || referenceIndex >= wavelengths.length)
            throw new IllegalArgumentException("Reference wavelength index is out of range");
        boolean hasPositiveWeight = false;
        for (int i = 0; i < wavelengths.length; i++) {
            if (!Double.isFinite(wavelengths[i]) || !(wavelengths[i] > 0.0))
                throw new IllegalArgumentException(
                        "Wavelength at index " + i + " must be finite and positive");
            double weight = spectralWeights[i];
            if (!Double.isFinite(weight) || weight < 0.0)
                throw new IllegalArgumentException(
                        "Spectral weight at wavelength index " + i +
                                " must be finite and non-negative");
            hasPositiveWeight |= weight > 0.0;
        }
        if (!hasPositiveWeight)
            throw new IllegalArgumentException("At least one spectral weight must be positive");
    }
}
