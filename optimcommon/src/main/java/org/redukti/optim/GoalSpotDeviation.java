package org.redukti.optim;

import org.redukti.mathlib.LMLSolver;

/** One signed, Gaussian-weighted image-plane ray deviation for RMS spot optimization. */
public class GoalSpotDeviation extends Goal {
    /** Zero based, as stored; the constructor takes a one based field. */
    public final int _field;
    public final int _wavelength_index;
    public final int _sample_index;
    public final int _orientation;

    public GoalSpotDeviation(Analysis analysis, int field, int wavelength_index,
                             int sample_index, int orientation, double weight) {
        super(analysis, 0.0, weight);
        if (field < 1) throw new IllegalArgumentException("field is one based and must be positive");
        if (wavelength_index < 0 || sample_index < 0)
            throw new IllegalArgumentException("indices must be non-negative");
        Orientation.checked(orientation);
        _field = field - 1;
        _wavelength_index = wavelength_index;
        _sample_index = sample_index;
        _orientation = orientation;
    }

    @Override
    public double value() {
        if (_analysis._spots == null || _field >= _analysis._spots.length)
            return LMLSolver.BIGVAL;
        var field = _analysis._spots[_field];
        if (field == null || _wavelength_index >= field.intercepts.size())
            return LMLSolver.BIGVAL;
        var intercepts = field.intercepts.get(_wavelength_index);
        if (_sample_index >= intercepts.x.length || !intercepts.valid[_sample_index])
            return LMLSolver.BIGVAL;
        double deviation = _orientation == Orientation.X
                ? intercepts.x[_sample_index] : intercepts.y[_sample_index];
        // SpotAnalysis stores system units (normally mm); public spot radii and
        // optimization targets use microns.
        return 1000.0 * Math.sqrt(intercepts.weights[_sample_index]) * deviation;
    }

    @Override
    public String toString() {
        return "SpotDeviation field=" + _field
                + ", wavelength=" + _wavelength_index
                + ", sample=" + _sample_index
                + ", orientation=" + (_orientation == Orientation.X ? "x" : "y")
                + ", target=" + _target + ", weight=" + _weight + " = " + value();
    }
}
