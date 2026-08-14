package org.redukti.optim;

import org.redukti.mathlib.LMLSolver;

/** A single weighted pupil phase-difference residual for contrast optimization. */
public class GoalContrast extends Goal {
    public static final int SAGITTAL = 0;
    public static final int TANGENTIAL = 1;

    public final int _contrast_index;
    public final int _frequency;
    public final int _field;
    public final int _wavelength_index;
    public final int _sample_index;
    public final int _orientation;

    public GoalContrast(Analysis analysis,
                        int contrast_index,
                        int frequency,
                        int field,
                        int wavelength_index,
                        int sample_index,
                        int orientation,
                        double weight) {
        super(analysis, 0.0, weight);
        if (contrast_index < 0) throw new IllegalArgumentException("contrast index must be non-negative");
        if (field < 1) throw new IllegalArgumentException("field is one based and must be positive");
        if (wavelength_index < 0 || sample_index < 0) throw new IllegalArgumentException("indices must be non-negative");
        if (orientation != SAGITTAL && orientation != TANGENTIAL)
            throw new IllegalArgumentException("invalid contrast orientation");
        _contrast_index = contrast_index;
        _frequency = frequency;
        _field = field - 1;
        _wavelength_index = wavelength_index;
        _sample_index = sample_index;
        _orientation = orientation;
    }

    @Override
    public double value() {
        if (_analysis._contrasts == null || _contrast_index >= _analysis._contrasts.length)
            return LMLSolver.BIGVAL;
        var contrast = _analysis._contrasts[_contrast_index];
        if (contrast == null) return LMLSolver.BIGVAL;
        if (_field >= contrast.fields.size()) return LMLSolver.BIGVAL;
        var wavelengths = contrast.fields.get(_field).wavelengths();
        if (_wavelength_index >= wavelengths.size()) return LMLSolver.BIGVAL;
        var samples = wavelengths.get(_wavelength_index).samples();
        if (_sample_index >= samples.size()) return LMLSolver.BIGVAL;
        var sample = samples.get(_sample_index);
        if (!sample.valid()) return LMLSolver.BIGVAL;
        return _orientation == SAGITTAL
                ? sample.sagittalResidual()
                : sample.tangentialResidual();
    }

    @Override
    public String toString() {
        return "Contrast index=" + _contrast_index + ", frequency=" + _frequency + ", field=" + _field
                + ", wavelength=" + _wavelength_index + ", sample=" + _sample_index
                + ", orientation=" + _orientation + ", weight=" + _weight
                + " = " + value();
    }
}
