// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.parax;

public class Etendue {

    /**
     * convert numerical aperture to slope
     */
    public static double na2slp(double na, double n) {
        return n*Math.tan(Math.asin(na/n));
    }
    public static double na2slp(double na) {
        return na2slp(na, 1.0);
    }

    /**
     * convert a ray slope to numerical aperture
     */
    public static double slp2na(double slp, double n) {
        return n*Math.sin(Math.atan(slp/n));
    }
    public static double slp2na(double slp) {
        return slp2na(slp, 1.0);
    }

    /**
     * convert paraxial numerical aperture to slope
     */
    public static double na2slp_parax(double na, double n) {
        return na/n;
    }

    /**
     * convert a ray slope to paraxial numerical aperture
     */
    public static double slp2na_parax(double slp, double n) {
        return n*slp;
    }

    /**
     * convert an angle in degrees to a slope
     */
    public static double ang2slp(double ang) {
        return Math.tan(Math.toRadians(ang));
    }

    /**
     * convert a slope to an angle in degrees
     */
    public static double slp2ang(double slp) {
        return Math.toDegrees(Math.atan(slp));
    }
}
