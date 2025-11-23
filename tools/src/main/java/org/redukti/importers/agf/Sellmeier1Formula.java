package org.redukti.importers.agf;

public class Sellmeier1Formula extends AGFBase {

    public Sellmeier1Formula(String make,String name,double[] coefs) {
        super(make,name,coefs);
    }

    @Override
    public double get_measurement_index(double wavelen) {
        var wv = 0.001*wavelen;
        var wv2 = wv*wv;
        // K1, L1, K2, L2, K3, L3
        var n2 = 1.0 + _coefs[0]*wv2/(wv2 - _coefs[1]);
        n2 += _coefs[2]*wv2/(wv2 - _coefs[3]);
        n2 += _coefs[4]*wv2/(wv2 - _coefs[5]);
        return Math.sqrt(n2);
    }
}
