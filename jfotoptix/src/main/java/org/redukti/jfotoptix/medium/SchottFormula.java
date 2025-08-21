package org.redukti.jfotoptix.medium;

public class SchottFormula extends AGFBase {

    public SchottFormula(String name, double[] coefs) {
        super("Hoya",name,coefs);
    }

    @Override
    public double get_measurement_index(double wavelen) {
        // adapted from https://github.com/mjhoptics/opticalglass/blob/master/src/opticalglass/hoya.py
        var wv = 0.001*wavelen;
        var wv2 = wv*wv;
        var n2 = _coefs[0] + _coefs[1]*wv2;
        var wvm2 = 1.0/wv2;
        n2 = n2 + wvm2*(_coefs[2] +
                        wvm2*(_coefs[3] +
                              wvm2*(_coefs[4] +
                                    wvm2*_coefs[5])));
        return Math.sqrt(n2);
    }
}
