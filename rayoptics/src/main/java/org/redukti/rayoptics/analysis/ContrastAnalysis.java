package org.redukti.rayoptics.analysis;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.ContrastRayTriplet;

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
                double shift = normalized_pupil_shift(opticalModel, wavelength, options.spatialFrequency);
                if (shift >= 2.0) {
                    wavelengthResults.add(new ContrastAnalysisResult.WavelengthResult(
                            wavelength, shift, java.util.List.of()));
                    continue;
                }
                var traced = opticalModel.seq_model.trace_contrast(
                        (rays, field, tracedWavelength, focus) -> sample(
                                opticalModel, rays, field, tracedWavelength, focus),
                        fieldIndex, wavelengthIndex,
                        options.numRings, options.numSpokes,
                        new Vector2(shift, 0.0), new Vector2(0.0, shift),
                        options.traceOptions);
                wavelengthResults.add(new ContrastAnalysisResult.WavelengthResult(
                        wavelength, shift, normalize_weights(traced.get(0).samples())));
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

    private static ContrastAnalysisResult.Sample sample(
            OpticalModel opticalModel, ContrastRayTriplet rays,
            org.redukti.rayoptics.specs.Field field,
            double wavelength, double focus) {
        double reference = WavefrontAberrationAnalysis.opd(
                opticalModel, rays.pupil(), 0, rays.reference(), field, wavelength, focus);
        double sagittal = opd(opticalModel, rays.sagittal(), field, wavelength, focus,
                rays.sagittal().input_pupil) - reference;
        double tangential = opd(opticalModel, rays.tangential(), field, wavelength, focus,
                rays.tangential().input_pupil) - reference;
        return new ContrastAnalysisResult.Sample(rays.pupil(), sagittal, tangential, rays.weight());
    }

    private static double opd(OpticalModel opticalModel,
                              org.redukti.rayoptics.raytr.RayPkg ray,
                              org.redukti.rayoptics.specs.Field field,
                              double wavelength, double focus, Vector2 pupil) {
        return WavefrontAberrationAnalysis.opd(
                opticalModel, pupil, 0, ray, field, wavelength, focus);
    }

    private static java.util.List<ContrastAnalysisResult.Sample> normalize_weights(
            java.util.List<ContrastAnalysisResult.Sample> samples) {
        double total = samples.stream().mapToDouble(ContrastAnalysisResult.Sample::weight).sum();
        if (total == 0.0) return samples;
        var normalized = new ArrayList<ContrastAnalysisResult.Sample>(samples.size());
        for (var sample : samples) {
            normalized.add(new ContrastAnalysisResult.Sample(
                    sample.pupil(), sample.sagittalDifference(), sample.tangentialDifference(),
                    sample.weight() / total));
        }
        return normalized;
    }
}
