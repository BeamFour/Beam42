package org.redukti.optim;

import org.redukti.mathlib.LMLSolver;
import org.redukti.rayoptics.analysis.ContrastAnalysisResult;

/**
 * Polychromatic MTF at one frequency, field and orientation, evaluated from the rays
 * already traced for contrast optimization.
 *
 * <p>{@link GoalContrast} minimises the squared pupil phase difference, which tracks MTF
 * only while that difference stays well under a wave. Beyond that the least-squares
 * residual and the MTF part company in both directions, and a design can be driven
 * backwards while the merit improves. This goal measures the MTF itself so the optimizer
 * can see what the residuals cannot, and costs no extra ray tracing - it reads the same
 * samples.
 *
 * <p>Intended <em>alongside</em> {@link GoalContrast} rather than instead of it: the
 * dense per-sample residuals are what give the well-conditioned Jacobian, and there are
 * only a handful of these. Because a handful of goals with residuals near 0.5 will
 * otherwise swamp thousands at 0.02, choose the weight deliberately.
 *
 * <p>A target of 1.0 is usually right. The solver forms {@code (value - target)}, which
 * penalises exceeding the target as much as falling short, so a lower target actively
 * pulls a field back down once it is met.
 */
public class GoalContrastMTF extends Goal {
    public static final int SAGITTAL = ContrastAnalysisResult.SAGITTAL;
    public static final int TANGENTIAL = ContrastAnalysisResult.TANGENTIAL;

    public final int _frequency;
    public final int _field;
    public final int _orientation;
    private final double[] _wavelengthWeights;

    /**
     * @param analysis          analysis holding this iteration's contrast results
     * @param frequency         spatial frequency; must be one of the analysis' contrast
     *                          frequencies, since the pupil shear is per frequency
     * @param field             one based field index
     * @param orientation       {@link #SAGITTAL} or {@link #TANGENTIAL}
     * @param target            desired MTF as a fraction, normally 1.0
     * @param weight            goal weight
     * @param wavelengthWeights one weight per wavelength, or null to weight equally
     */
    public GoalContrastMTF(Analysis analysis, int frequency, int field, int orientation,
                           double target, double weight, double[] wavelengthWeights) {
        super(analysis, target, weight);
        if (field < 1) throw new IllegalArgumentException("field is one based and must be positive");
        if (orientation != SAGITTAL && orientation != TANGENTIAL)
            throw new IllegalArgumentException("invalid contrast orientation");
        if (!Double.isFinite(target) || target < 0.0 || target > 1.0)
            throw new IllegalArgumentException("contrast MTF target must be a fraction in [0, 1]");
        _frequency = frequency;
        _field = field - 1;
        _orientation = orientation;
        _wavelengthWeights = wavelengthWeights == null
                ? null : wavelengthWeights.clone();
    }

    @Override
    public double value() {
        if (_analysis._contrasts == null) return LMLSolver.BIGVAL;
        for (var contrast : _analysis._contrasts) {
            if (contrast.spatialFrequency != _frequency) continue;
            if (_field >= contrast.fields.size()) return LMLSolver.BIGVAL;
            double modulus = contrast.otf_modulus(_field, _orientation, _wavelengthWeights);
            return Double.isFinite(modulus) ? modulus : LMLSolver.BIGVAL;
        }
        return LMLSolver.BIGVAL;
    }

    @Override
    public String toString() {
        return "Contrast MTF frequency=" + _frequency + ", field=" + _field
                + ", orientation=" + (_orientation == SAGITTAL ? "sag" : "tan")
                + ", target=" + _target + ", weight=" + _weight
                + ", value=" + value();
    }
}
