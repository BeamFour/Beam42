package org.redukti.jfotoptix.medium;

public class Sellmeier3Formula extends AGFBase {

    public Sellmeier3Formula(String name, double[] coefs) {
        super("Schott",name,coefs);
    }

    @Override
    public double get_measurement_index(double wavelen) {
        // adapted from https://github.com/mjhoptics/opticalglass/blob/master/src/opticalglass/schott.py
        var wv = 0.001*wavelen;
        var wv2 = wv*wv;
        var n2 = 1.0 + _coefs[0]*wv2/(wv2 - _coefs[1]);
        n2 += _coefs[2]*wv2/(wv2 - _coefs[3]);
        n2 += _coefs[4]*wv2/(wv2 - _coefs[5]);
        n2 += _coefs[6]*wv2/(wv2 - _coefs[7]);
        return Math.sqrt(n2);
    }
}
