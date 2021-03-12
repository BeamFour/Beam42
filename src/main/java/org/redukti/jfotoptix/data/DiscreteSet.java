package org.redukti.jfotoptix.data;

public class DiscreteSet extends DiscreteSetBase implements InterpolatableDataSet {

    Interpolated1d interpolated1d;

    public DiscreteSet() {
        this.interpolated1d = new Interpolated1d(this);
    }

    @Override
    public double interpolate(double x) {
        return interpolated1d.interpolate(x);
    }

    @Override
    public double interpolate(double x, int deriv) {
        return interpolated1d.interpolate(x, deriv);
    }

    @Override
    protected void invalidate() {
        this.interpolated1d.invalidate();
    }

    public void setInterpolation(Interpolation i) {
        this.interpolated1d.set_interpolation(i);
    }
}
