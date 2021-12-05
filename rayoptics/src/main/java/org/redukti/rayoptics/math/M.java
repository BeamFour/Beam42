package org.redukti.rayoptics.math;

public class M {

    final static double EPSILON = 2.2204460492503131e-016;

    public static boolean isZero(double d) {
        return Math.abs(d) <= EPSILON;
    }

}
