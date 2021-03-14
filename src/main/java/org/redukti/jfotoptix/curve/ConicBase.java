package org.redukti.jfotoptix.curve;

public abstract class ConicBase extends RotationalRoc {

    double _sh; // Schwarzschild constant + 1

    public ConicBase (double roc, double sc) {
        super (roc);
        _sh = sc + 1;
    }

    public double get_eccentricity ()
    {
        return Math.sqrt (-_sh + 1.0);
    }

    public double get_schwarzschild ()
    {
        return _sh - 1.0;
    }
}
