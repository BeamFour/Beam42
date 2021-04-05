package org.redukti.jfotoptix.data;

public class DiscreteSet extends DiscreteSetBase implements InterpolatableDataSet {

    Interpolated1d _interpolated_1d;

    public DiscreteSet() {
        this._interpolated_1d = new Interpolated1d(this);
    }

    @Override
    public double interpolate(double x) {
        return _interpolated_1d.interpolate(x);
    }

    @Override
    public double interpolate(double x, int deriv) {
        return _interpolated_1d.interpolate(x, deriv);
    }

    @Override
    protected void invalidate() {
        this._interpolated_1d.invalidate();
    }

    public void setInterpolation(Interpolation i) {
        this._interpolated_1d.set_interpolation(i);
    }
}
