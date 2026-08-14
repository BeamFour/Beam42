package org.redukti.optim;

/**
 * Targets sagittal or tangential MTF at given field and freq
 */
public class GoalGeoMTF extends Goal {
    public final int _freq;
    public final int _orientation;
    public final int _field;

    /**
     * @param analysis Results from current iteration
     * @param field Fields start at 1
     * @param orientation {@link Orientation#SAGITTAL} or {@link Orientation#TANGENTIAL}
     * @param freq  MTF frequency
     * @param target    The goal
     * @param weight    Weighting
     */
    public GoalGeoMTF(Analysis analysis, int field, int orientation, int freq, double target, double weight) {
        super(analysis, target, weight);
        this._freq = freq;
        this._orientation = Orientation.checked(orientation);
        this._field = field;
    }

    @Override
    public double value() {
        for (int i = 0; i < _analysis._mtfs.length; i++) {
            if (_analysis._mtfs[i].freq == _freq)
                return _orientation == Orientation.TANGENTIAL
                        ? _analysis._mtfs[i].tan_mtf_by_field[_field -1]
                        : _analysis._mtfs[i].sag_mtf_by_field[_field -1];
        }
        throw new IllegalArgumentException();
    }

    @Override
    public String toString() {
        return "GeoMTF " +
                "field=" + _field +
                ", orientation=" + Orientation.name(_orientation) +
                ", freq=" + _freq +
                ", target=" + _target +
                ", weight=" + _weight +
                ", value=" + value() +
                '}';
    }
}
