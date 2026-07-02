package org.redukti.optim;

/**
 * Targets sagittal (xy=0) or tangential (xy=1) MTF at given field and freq
 */
public class GeoMTF extends Goal {
    public final int _freq;
    public final int _xy;
    public final int _field;
    public GeoMTF(Analysis analysis, int field, int xy, int freq, double target, double weight) {
        super(analysis, target, weight);
        this._freq = freq;
        this._xy = xy;
        this._field = field;
    }

    @Override
    public double value() {
        for (int i = 0; i < _analysis._mtfs.length; i++) {
            if (_analysis._mtfs[i].freq == _freq)
                return _xy == 1 ? _analysis._mtfs[i].tan_mtf_by_field[_field -1] : _analysis._mtfs[i].sag_mtf_by_field[_field -1];
        }
        throw new IllegalArgumentException();
    }

    @Override
    public String toString() {
        return "GeoMTF " +
                "field=" + _field +
                ", xy=" + _xy +
                ", freq=" + _freq +
                ", target=" + _target +
                ", weight=" + _weight +
                ", value=" + value() +
                '}';
    }
}
