package org.redukti.jfotoptix.medium;

public class HerzbergerFormula extends AGFBase {

    public HerzbergerFormula(String name, double[] coefs) {
        super("Hoya",name,coefs);
    }

    @Override
    public double get_measurement_index(double wavelen) {
        var wv = 0.001*wavelen;
        var wv2 = wv*wv;
        var L = 1.0 / (wv2 - 0.028);
        var n2 = _coefs[0] + _coefs[1]*L + _coefs[2]*L*L;
        n2 = n2 + wv2*(_coefs[3] +
                        wv2*(_coefs[4] +
                              wv2*(_coefs[5])));
        return n2;
    }
}
