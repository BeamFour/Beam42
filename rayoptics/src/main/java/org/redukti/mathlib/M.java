package org.redukti.mathlib;

public class M {

    public final static double EPSILON = 2.2204460492503131e-016;

    public static boolean isZero(double d) {
        return Math.abs(d) <= EPSILON;
    }

    public static boolean is_kinda_big(double x) {
        return is_kinda_big(x, 1e8);
    }
    public static boolean is_kinda_big(double x, double kinda_big) {
        if (Double.isInfinite(x))
            return true;
        if (Math.abs(x) > kinda_big)
            return true;
        return false;
    }

    /**
     * Replace IEEE inf with a signed big number.
     */
    public static double infinity_guard(double x, double big) {
        if (Double.isInfinite(x))
            return x == Double.NEGATIVE_INFINITY ? -big : big;
        return x;
    }
    public static double infinity_guard(double x) {
        return infinity_guard(x,1e12);
    }
}
