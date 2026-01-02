package org.redukti.optim;

/**
 * Targets sagittal (xy=0) or tangential (xy=1) MTF at given field and freq
 */
public class GeoMTF extends Goal {

    int freq;
    int xy;
    int field;
    public GeoMTF(Analysis analysis, int field, int xy, int freq, double target, double weight) {
        super(analysis, target, weight);
        this.freq = freq;
        this.xy = xy;
        this.field = field;
    }

    @Override
    public double value() {
        for (int i = 0; i < analysis.mtfs.length; i++) {
            if (analysis.mtfs[i].freq == freq)
                return xy == 0 ? analysis.mtfs[i].tan_mtf_by_field[field-1] : analysis.mtfs[i].sag_mtf_by_field[field-1];
        }
        throw new IllegalArgumentException();
    }

    @Override
    public String toString() {
        return "GeoMTF " +
                "field=" + field +
                ", xy=" + xy +
                ", freq=" + freq +
                ", target=" + target +
                ", weight=" + weight +
                ", value=" + value() +
                '}';
    }
}
