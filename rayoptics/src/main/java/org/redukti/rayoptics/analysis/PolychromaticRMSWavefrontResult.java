package org.redukti.rayoptics.analysis;

import org.redukti.rayoptics.specs.ReadOnlyField;

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
            double weightedSum,
            double weightedSquareSum,
            double centeredWeightedSquareSum,
            int validSamples) {

        /** Weighted mean OPD before piston removal, in waves. */
        public double meanWaves() {
            return pupilWeight > 0.0 ? weightedSum / pupilWeight : Double.NaN;
        }

        /** Chief-referenced monochromatic RMS with piston removed, in waves. */
        public double rmsWaves() {
            return pupilWeight > 0.0
                    ? Math.sqrt(centeredWeightedSquareSum / pupilWeight)
                    : Double.NaN;
        }

        /** RMS of the raw chief-ray OPDs without piston removal, in waves. */
        public double unreferencedRmsWaves() {
            return pupilWeight > 0.0
                    ? Math.sqrt(weightedSquareSum / pupilWeight)
                    : Double.NaN;
        }
    }

    public record FieldResult(
            ReadOnlyField field,
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
