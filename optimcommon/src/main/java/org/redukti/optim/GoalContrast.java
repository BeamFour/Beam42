package org.redukti.optim;

import org.redukti.mathlib.LMLSolver;

/** A single weighted pupil phase-difference residual for contrast optimization. */
public class GoalContrast extends Goal {
    public static final int SAGITTAL = 0;
    public static final int TANGENTIAL = 1;

    public final int _frequency;
    public final int _field;
    public final int _wavelength;
    public final int _sample;
    public final int _orientation;

    public GoalContrast(Analysis analysis, int frequency, int field, int wavelength,
                        int sample, int orientation, double weight) {
        super(analysis, 0.0, weight);
        if (field < 1) throw new IllegalArgumentException("field is one based and must be positive");
        if (wavelength < 0 || sample < 0) throw new IllegalArgumentException("indices must be non-negative");
        if (orientation != SAGITTAL && orientation != TANGENTIAL)
            throw new IllegalArgumentException("invalid contrast orientation");
        _frequency = frequency;
        _field = field - 1;
        _wavelength = wavelength;
        _sample = sample;
        _orientation = orientation;
    }

    @Override
    public double value() {
        if (_analysis._contrasts == null) return LMLSolver.BIGVAL;
        for (var contrast : _analysis._contrasts) {
            if (contrast.spatialFrequency != _frequency) continue;
            if (_field >= contrast.fields.size()) return LMLSolver.BIGVAL;
            var wavelengths = contrast.fields.get(_field).wavelengths();
            if (_wavelength >= wavelengths.size()) return LMLSolver.BIGVAL;
            var samples = wavelengths.get(_wavelength).samples();
            if (_sample >= samples.size()) return LMLSolver.BIGVAL;
            var sample = samples.get(_sample);
            if (!sample.valid()) return LMLSolver.BIGVAL;
            return _orientation == SAGITTAL
                    ? sample.sagittalResidual()
                    : sample.tangentialResidual();
        }
        return LMLSolver.BIGVAL;
    }

    @Override
    public String toString() {
        return "Contrast frequency=" + _frequency + ", field=" + _field
                + ", wavelength=" + _wavelength + ", sample=" + _sample
                + ", orientation=" + _orientation + ", weight=" + _weight;
    }
}
