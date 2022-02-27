package org.redukti.mathlib;

public class M {

    public final static double EPSILON = 2.2204460492503131e-016;

    public static boolean isZero(double d) {
        return Math.abs(d) <= EPSILON;
    }

}
