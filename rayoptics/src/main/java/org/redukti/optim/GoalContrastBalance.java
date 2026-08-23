package org.redukti.optim;

import org.redukti.mathlib.LMLSolver;

/**
 * Holds sagittal and tangential contrast in balance at one field and frequency.
 *
 * <p>The contrast merit minimizes {@code sum(sagittal^2) + sum(tangential^2)}, which at a
 * fixed total barely discriminates how astigmatism is split between the two meridians. A
 * designer discriminates sharply: on the Leica 75/2 a solve produced 0.156 sagittal
 * against 0.725 tangential at field 0.8, 50 cyc/mm, while the <em>sum</em> of the two MTFs
 * stayed within 15% of its value everywhere else across the field. The lens was not worse
 * in that zone, it was lopsided, and nothing in the merit had an opinion about that.
 *
 * <p>This supplies the opinion. The value is the difference between what the two
 * orientations contribute to the merit,
 *
 * <pre>{@code sum_wavelengths w * (
 *     w_sagittal * sum_samples r_sagittal^2
 *   - w_tangential * sum_samples r_tangential^2 )}</pre>
 *
 * <p>against a target of zero. Defining it on the residuals rather than on the raw
 * wavefront differences means it automatically follows whatever those residuals already
 * account for - residual centering, quadrature weights, wavelength and orientation
 * weights, and the frequency calibration.
 *
 * <p>It is smooth and quadratic in the wavefront differences, with no modulus and no
 * square root, so unlike the phasor MTF goal that was tried and reverted it has no kink to
 * fall into.
 *
 * <p>The orientation weights are the ones the ordinary contrast goals use, so the ratio
 * {@code w_sagittal / w_tangential} <em>is</em> the instruction for how the two meridians
 * should differ: balance is reached when the weighted contributions match, not when the
 * two residual energies do. Weights of 0.5 and 0.1 ask for tangential to carry five times
 * the energy of sagittal, and the goal will deliver that.
 *
 * <p><b>Not meaningful on axis.</b> At field zero the meridians are identical by
 * rotational symmetry, so there is nothing to balance and the value reduces to
 * {@code (w_sagittal - w_tangential) * S}, where {@code S} is the axial residual energy.
 * Equal weights make that exactly zero and the goal inert; unequal weights turn it into a
 * second axial contrast goal whose strength is a number that usually fell out of a field
 * taper rather than a decision. On the Leica 75/2 with weights 8 and 4 it came to 6.3% of
 * the merit, none of it balance. It is also shaped unlike the goals it shadows - {@code S}
 * is already a sum of squares, so this residual is quadratic where the per-sample ones are
 * linear, and it fades quadratically as the design improves. Prefer raising the field-zero
 * contrast weights if axial emphasis is what is wanted.
 *
 * <p>Two further things to be clear about. This is a design preference, not a correction:
 * it tells the optimizer something it cannot infer, rather than fixing an error. And it is
 * one residual against the thousands in a contrast merit, so its weight has to be set
 * deliberately - see {@link OptimizationBuilder#contrastBalanceGoals(boolean[], double)}.
 */
public class GoalContrastBalance extends Goal {

    public final int _contrast_index;
    public final int _frequency;
    /** Zero based, as stored; the constructor takes a one based field. */
    public final int _field;
    private final double[] _wavelength_weights;
    private final double _sagittal_weight;
    private final double _tangential_weight;

    /**
     * @param field              one based, as for every other field-addressed goal
     * @param wavelengthWeights  one per wavelength, pooling the per-wavelength blocks
     */
    public GoalContrastBalance(Analysis analysis, int contrast_index, int frequency,
                               int field, double[] wavelengthWeights, double weight) {
        this(analysis, contrast_index, frequency, field, wavelengthWeights, 1.0, 1.0, weight);
    }

    /**
     * @param sagittalWeight    field/orientation weight used by the sagittal contrast goals
     * @param tangentialWeight  field/orientation weight used by the tangential contrast goals
     */
    public GoalContrastBalance(Analysis analysis, int contrast_index, int frequency,
                               int field, double[] wavelengthWeights,
                               double sagittalWeight, double tangentialWeight, double weight) {
        super(analysis, 0.0, weight);
        if (contrast_index < 0)
            throw new IllegalArgumentException("contrast index must be non-negative");
        if (field < 1)
            throw new IllegalArgumentException("field is one based and must be positive");
        if (wavelengthWeights == null || wavelengthWeights.length == 0)
            throw new IllegalArgumentException("balance needs at least one wavelength weight");
        if (!Double.isFinite(sagittalWeight) || sagittalWeight < 0.0
                || !Double.isFinite(tangentialWeight) || tangentialWeight < 0.0)
            throw new IllegalArgumentException(
                    "contrast orientation weights must be finite and non-negative");
        _contrast_index = contrast_index;
        _frequency = frequency;
        _field = field - 1;
        _wavelength_weights = wavelengthWeights.clone();
        _sagittal_weight = sagittalWeight;
        _tangential_weight = tangentialWeight;
    }

    @Override
    public double value() {
        if (_analysis._contrasts == null || _contrast_index >= _analysis._contrasts.length)
            return LMLSolver.BIGVAL;
        var contrast = _analysis._contrasts[_contrast_index];
        if (contrast == null || _field >= contrast.fields.size()) return LMLSolver.BIGVAL;
        var wavelengths = contrast.fields.get(_field).wavelengths();
        if (wavelengths.size() > _wavelength_weights.length) return LMLSolver.BIGVAL;

        double difference = 0.0;
        boolean sampled = false;
        for (int wi = 0; wi < wavelengths.size(); wi++) {
            var block = wavelengths.get(wi);
            double sagittal = 0.0;
            double tangential = 0.0;
            for (int i = 0; i < block.samples().size(); i++) {
                if (!block.samples().get(i).valid()) continue;
                double rs = block.sagittalResidual(i);
                double rt = block.tangentialResidual(i);
                if (!Double.isFinite(rs) || !Double.isFinite(rt)) continue;
                sagittal += rs * rs;
                tangential += rt * rt;
                sampled = true;
            }
            difference += _wavelength_weights[wi]
                    * (_sagittal_weight * sagittal - _tangential_weight * tangential);
        }
        if (!sampled || !Double.isFinite(difference)) return LMLSolver.BIGVAL;
        return difference;
    }

    @Override
    public String toString() {
        return "ContrastBalance frequency=" + _frequency + ", field=" + _field
                + ", weight=" + _weight + " = " + value();
    }
}
