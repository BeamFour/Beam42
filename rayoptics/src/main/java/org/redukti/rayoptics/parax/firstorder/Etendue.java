package org.redukti.rayoptics.parax.firstorder;

public class Etendue {

    /**
     * convert numerical aperture to slope
     */
    public static double na2slp(double na, double n) {
        return na/n;
    }
    public static double na2slp(double na) {
        return na2slp(na, 1.0);
    }

    /**
     * convert a ray slope to numerical aperture
     */
    public static double slp2na(double slp, double n) {
        return n*slp;
    }
    public static double slp2na(double slp) {
        return slp2na(slp, 1.0);
    }

}
