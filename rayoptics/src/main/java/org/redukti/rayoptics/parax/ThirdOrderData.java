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

    /** Surface index this contribution belongs to. */
    public int surface() {
        return c;
    }

    /** Surface contribution as {S-I, S-II, S-III, S-IV, S-V}. */
    public double[] seidel() {
        return new double[]{SI, SII, SIII, SIV, SV};
    }

    /**
     * Aspheric contribution as {S-I, S-II, S-III, S-IV, S-V}, all zero unless
     * the surface has a non-zero 4th order aspheric term.
     */
    public double[] aspheric() {
        return new double[]{SI_star, SII_star, SIII_star, SIV_star, SV_star};
    }

    /** True when this surface carries a non-zero aspheric contribution. */
    public boolean has_aspheric() {
        for (double v : aspheric()) {
            if (v != 0.0)
                return true;
        }
        return false;
    }
}
