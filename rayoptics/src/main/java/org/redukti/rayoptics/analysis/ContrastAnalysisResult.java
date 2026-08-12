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

    public record WavelengthResult(double wavelength, double normalizedPupilShift,
                                   List<Sample> samples) {
    }

    public record FieldResult(Field field, List<WavelengthResult> wavelengths) {
    }
}
