package org.redukti.rayoptics.analysis;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.specs.Field;

import java.util.ArrayList;
import java.util.List;

public class ContrastAnalysisResult {
    public static final int SAGITTAL = 0;
    public static final int TANGENTIAL = 1;

    public final double spatialFrequency;
    public final List<FieldResult> fields = new ArrayList<>();

    public ContrastAnalysisResult(double spatialFrequency) {
        this.spatialFrequency = spatialFrequency;
    }

    /**
     * Polychromatic OTF modulus at this spatial frequency, for one field and one
     * orientation.
     *
     * <p>The MTF is the normalised autocorrelation of the pupil function, so over a
     * sheared pupil it is the mean of {@code exp(i*2*pi*dW)} - not a function of the
     * spread of {@code dW} alone. The least-squares residuals on {@link Sample} are a
     * proxy for that mean which is only faithful while {@code 2*pi*dW} stays well under
     * a radian; beyond that the two part company, and not in a single direction. This
     * evaluates the mean itself, from the same rays, so it stays meaningful at large
     * aberration.
     *
     * <p>Wavelengths are combined as a complex sum before the modulus is taken, which is
     * what makes the result polychromatic rather than an average of monochromatic MTFs.
     *
     * @param fieldIndex        zero based field index
     * @param orientation       {@link #SAGITTAL} or {@link #TANGENTIAL}
     * @param wavelengthWeights one weight per wavelength, or null to weight them equally
     * @return the modulus in [0, 1], or NaN if any contributing ray failed to trace
     */
    public double otf_modulus(int fieldIndex, int orientation, double[] wavelengthWeights) {
        if (orientation != SAGITTAL && orientation != TANGENTIAL)
            throw new IllegalArgumentException("invalid contrast orientation");
        if (fieldIndex < 0 || fieldIndex >= fields.size())
            throw new IllegalArgumentException("field index out of range: " + fieldIndex);
        var wavelengths = fields.get(fieldIndex).wavelengths();
        if (wavelengthWeights != null && wavelengthWeights.length != wavelengths.size())
            throw new IllegalArgumentException("one wavelength weight per wavelength is required");

        double real = 0.0, imaginary = 0.0, total = 0.0;
        for (int wi = 0; wi < wavelengths.size(); wi++) {
            double wavelengthWeight = wavelengthWeights == null ? 1.0 : wavelengthWeights[wi];
            for (var sample : wavelengths.get(wi).samples()) {
                if (!sample.valid()) return Double.NaN;
                double difference = orientation == SAGITTAL
                        ? sample.sagittalDifference()
                        : sample.tangentialDifference();
                if (!Double.isFinite(difference)) return Double.NaN;
                double weight = wavelengthWeight * sample.weight();
                double phase = 2.0 * Math.PI * difference;
                real += weight * Math.cos(phase);
                imaginary += weight * Math.sin(phase);
                total += weight;
            }
        }
        if (!(total > 0.0)) return Double.NaN;
        return Math.hypot(real, imaginary) / total;
    }

    public record Sample(Vector2 pupil, double sagittalDifference,
                         double tangentialDifference, double weight, boolean valid) {
        public double sagittalResidual() { return Math.sqrt(weight) * sagittalDifference; }
        public double tangentialResidual() { return Math.sqrt(weight) * tangentialDifference; }
    }

    public record WavelengthResult(double wavelength, double normalizedPupilShift,
                                   List<Sample> samples) {
    }

    public record FieldResult(Field field, List<WavelengthResult> wavelengths) {
    }
}
