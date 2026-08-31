package org.redukti.rayoptics.analysis;

import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.GridItem;
import org.redukti.rayoptics.raytr.Trace;
import org.redukti.rayoptics.raytr.TraceRingsDef;

import java.util.ArrayList;

/**
 * Computes a Zemax-style polychromatic RMS wavefront error for every field.
 *
 * <p>The primary wavelength's chief-ray image point is shared by all wavelengths.
 * Each wavelength is nevertheless traced with its own chief ray and reference sphere.
 * The final value is the weighted second moment of all valid OPDs in waves, using the
 * product of spectral weight and Gaussian pupil-quadrature weight.
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
        var focus = opticalSpecs.defocus().get_focus();
        var definition = new TraceRingsDef();
        definition.num_rings = options.numRings;
        definition.min_radius = options.innerPupilRadius;
        var fields = new ArrayList<PolychromaticRMSWavefrontResult.FieldResult>();

        for (var field : opticalSpecs.fov.fields) {
            var primary = Trace.setup_pupil_coords(
                    model, field, wavelengths.central_wvl(), focus, null, null);
            var referenceImagePoint = primary.ref_sphere.image_pt.project_xy();
            var wavelengthResults = new ArrayList<PolychromaticRMSWavefrontResult.WavelengthResult>();
            double totalWeight = 0.0;
            double totalWeightedSquares = 0.0;
            int validSamples = 0;

            for (int wi = 0; wi < wavelengths.wavelengths.length; wi++) {
                double wavelength = wavelengths.wavelengths[wi];
                double spectralWeight = wavelengths.spectral_wts[wi];
                validateSpectralWeight(spectralWeight, wi);

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

                double pupilWeight = 0.0;
                double weightedSquareSum = 0.0;
                int wavelengthValidSamples = 0;
                for (var sample : samples) {
                    if (!sample.valid || sample.result == null ||
                            !Double.isFinite(sample.result) || !(sample.weight > 0.0)) continue;
                    pupilWeight += sample.weight;
                    weightedSquareSum += sample.weight * sample.result * sample.result;
                    wavelengthValidSamples++;
                }
                wavelengthResults.add(new PolychromaticRMSWavefrontResult.WavelengthResult(
                        wavelength, spectralWeight, pupilWeight, weightedSquareSum,
                        wavelengthValidSamples));
                totalWeight += spectralWeight * pupilWeight;
                totalWeightedSquares += spectralWeight * weightedSquareSum;
                validSamples += wavelengthValidSamples;
            }

            double rms = totalWeight > 0.0
                    ? Math.sqrt(totalWeightedSquares / totalWeight)
                    : Double.NaN;
            fields.add(new PolychromaticRMSWavefrontResult.FieldResult(
                    field, rms, totalWeight, validSamples, wavelengthResults));
        }
        return new PolychromaticRMSWavefrontResult(fields);
    }

    private static void validateSpectralWeight(double weight, int wavelengthIndex) {
        if (!Double.isFinite(weight) || weight < 0.0) {
            throw new IllegalArgumentException(
                    "Spectral weight at wavelength index " + wavelengthIndex +
                            " must be finite and non-negative");
        }
    }
}
