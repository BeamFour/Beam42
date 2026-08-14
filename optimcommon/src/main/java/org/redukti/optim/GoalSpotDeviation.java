package org.redukti.optim;

import org.redukti.mathlib.LMLSolver;

/** One signed, Gaussian-weighted image-plane ray deviation for RMS spot optimization. */
public class GoalSpotDeviation extends Goal {
    public static final int X = 0;
    public static final int Y = 1;

    public final int _field_index;
    public final int _wavelength_index;
    public final int _sample_index;
    public final int _orientation;

    public GoalSpotDeviation(Analysis analysis, int fieldIndex, int wavelengthIndex,
                             int sampleIndex, int orientation, double weight) {
        super(analysis, 0.0, weight);
        if (fieldIndex < 0 || wavelengthIndex < 0 || sampleIndex < 0)
            throw new IllegalArgumentException("spot indices must be non-negative");
        if (orientation != X && orientation != Y)
            throw new IllegalArgumentException("invalid spot orientation");
        _field_index = fieldIndex;
        _wavelength_index = wavelengthIndex;
        _sample_index = sampleIndex;
        _orientation = orientation;
    }

    @Override
    public double value() {
        if (_analysis._spots == null || _field_index >= _analysis._spots.length)
            return LMLSolver.BIGVAL;
        var field = _analysis._spots[_field_index];
        if (field == null || _wavelength_index >= field.intercepts.size())
            return LMLSolver.BIGVAL;
        var intercepts = field.intercepts.get(_wavelength_index);
        if (_sample_index >= intercepts.x.length || !intercepts.valid[_sample_index])
            return LMLSolver.BIGVAL;
        double deviation = _orientation == X
                ? intercepts.x[_sample_index] : intercepts.y[_sample_index];
        // SpotAnalysis stores system units (normally mm); public spot radii and
        // optimization targets use microns.
        return 1000.0 * Math.sqrt(intercepts.weights[_sample_index]) * deviation;
    }

    @Override
    public String toString() {
        return "SpotDeviation field=" + _field_index
                + ", wavelength=" + _wavelength_index
                + ", sample=" + _sample_index
                + ", orientation=" + (_orientation == X ? "x" : "y")
                + ", target=" + _target + ", weight=" + _weight + " = " + value();
    }
}
