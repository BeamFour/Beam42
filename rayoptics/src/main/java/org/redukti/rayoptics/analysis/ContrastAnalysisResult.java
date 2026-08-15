package org.redukti.rayoptics.analysis;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.specs.Field;

import java.util.ArrayList;
import java.util.List;

public class ContrastAnalysisResult {
    public final double spatialFrequency;
    public final List<FieldResult> fields = new ArrayList<>();

    public ContrastAnalysisResult(double spatialFrequency) {
        this.spatialFrequency = spatialFrequency;
    }

    public record Sample(Vector2 pupil, double sagittalDifference,
                         double tangentialDifference, double weight, boolean valid,
                         Failure failure) {
        public double sagittalResidual() { return Math.sqrt(weight) * sagittalDifference; }
        public double tangentialResidual() { return Math.sqrt(weight) * tangentialDifference; }
    }

    public record Failure(String ray, String exceptionType, int surface) {
    }

    /**
     * One (frequency, field, wavelength) block of samples, with the constant part of the
     * wavefront difference that is to be removed from each orientation.
     *
     * <p>The offsets are zero unless {@link ContrastOptions#center_residuals(boolean)} is
     * enabled, in which case they hold the <em>reference wavelength's</em> weighted mean
     * difference for this field. See
     * {@link ContrastAnalysis#center_residuals(ContrastAnalysisResult, int)}.
     */
    public record WavelengthResult(double wavelength, double normalizedPupilShift,
                                   List<Sample> samples,
                                   double sagittalOffset, double tangentialOffset) {

        public WavelengthResult(double wavelength, double normalizedPupilShift,
                                List<Sample> samples) {
            this(wavelength, normalizedPupilShift, samples, 0.0, 0.0);
        }

        public WavelengthResult withOffsets(double sagittal, double tangential) {
            return new WavelengthResult(wavelength, normalizedPupilShift, samples,
                    sagittal, tangential);
        }

        /**
         * The optimizer residual for a sample: the sample's own residual less the
         * constant part, which is a pure image displacement and costs no MTF.
         *
         * <p>Note this is {@code sqrt(w) * (dW - offset)}, not
         * {@code sqrt(w) * dW - offset}.
         */
        public double sagittalResidual(int index) {
            var sample = samples.get(index);
            return Math.sqrt(sample.weight()) * (sample.sagittalDifference() - sagittalOffset);
        }

        public double tangentialResidual(int index) {
            var sample = samples.get(index);
            return Math.sqrt(sample.weight()) * (sample.tangentialDifference() - tangentialOffset);
        }
    }

    public record FieldResult(Field field, List<WavelengthResult> wavelengths) {
    }
}
