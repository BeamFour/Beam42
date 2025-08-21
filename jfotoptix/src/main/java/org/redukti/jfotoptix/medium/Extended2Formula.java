package org.redukti.jfotoptix.medium;

public class Extended2Formula extends AGFBase {

    public Extended2Formula(String make,String name, double[] coefs) {
        super(make,name,coefs);
    }

    @Override
    public double get_measurement_index(double wavelen) {
        var wv = 0.001*wavelen;
        var wv2 = wv*wv;
        var n2 = _coefs[0] + wv2*_coefs[1];
        var wvm2 = 1/wv2;
        n2 = n2 + wvm2*(_coefs[2] +
                        wvm2*(_coefs[3] +
                              wvm2*(_coefs[4] +
                                    wvm2*(_coefs[5]))));
        n2 = n2 + wv2*wv2*(_coefs[6] +
                           wv2*_coefs[7]);
        return Math.sqrt(n2);
    }
}
