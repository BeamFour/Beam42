// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.parax;

public class ThirdOrderData {
    int c;

    double SI;
    double SII;
    double SIII;
    double SIV;
    double SV;
    double SI_star;
    double SII_star;
    double SIII_star;
    double SIV_star;
    double SV_star;

    public ThirdOrderData(int c, double SI, double SII, double SIII, double SIV, double SV) {
        this.c = c;
        this.SI = SI;
        this.SII = SII;
        this.SIII = SIII;
        this.SIV = SIV;
        this.SV = SV;
    }
}
