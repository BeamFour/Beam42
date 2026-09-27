// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.importers.agf;

public class SchottFormula extends AGFBase {

    public SchottFormula(String make,String name, double[] coefs) {
        super(make,name,coefs);
    }

    @Override
    public double get_measurement_index(double wavelen) {
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
