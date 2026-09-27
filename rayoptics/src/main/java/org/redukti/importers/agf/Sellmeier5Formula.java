// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.importers.agf;

public class Sellmeier5Formula extends AGFBase {

    public Sellmeier5Formula(String make,String name,double[] coefs) {
        super(make,name,coefs);
    }

    @Override
    public double get_measurement_index(double wavelen) {
        var wv = 0.001*wavelen;
        var wv2 = wv*wv;
        var n2 = 1.0 + _coefs[0]*wv2/(wv2 - _coefs[1]);
        n2 += _coefs[2]*wv2/(wv2 - _coefs[3]);
        n2 += _coefs[4]*wv2/(wv2 - _coefs[5]);
        n2 += _coefs[6]*wv2/(wv2 - _coefs[7]);
        n2 += _coefs[8]*wv2/(wv2 - _coefs[9]);
        return Math.sqrt(n2);
    }
}
