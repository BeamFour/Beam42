package org.redukti.rayoptics.analysis;

import org.redukti.rayoptics.specs.Field;

import java.util.List;

/** One chief-ray-referenced polychromatic RMS wavefront value per field. */
public record PolychromaticRMSWavefrontResult(List<FieldResult> fields) {

    public PolychromaticRMSWavefrontResult {
        fields = List.copyOf(fields);
    }

    public record WavelengthResult(
            double wavelength,
            double spectralWeight,
            double pupilWeight,
            double weightedSquareSum,
            int validSamples) {

        /** Monochromatic RMS over the valid pupil samples, in waves. */
        public double rmsWaves() {
            return pupilWeight > 0.0
                    ? Math.sqrt(weightedSquareSum / pupilWeight)
                    : Double.NaN;
        }
    }

    public record FieldResult(
            Field field,
            double rmsWaves,
            double totalWeight,
            int validSamples,
            List<WavelengthResult> wavelengths) {

        public FieldResult {
            wavelengths = List.copyOf(wavelengths);
        }
    }

    public double[] rmsWaves() {
        return fields.stream().mapToDouble(FieldResult::rmsWaves).toArray();
    }
}
