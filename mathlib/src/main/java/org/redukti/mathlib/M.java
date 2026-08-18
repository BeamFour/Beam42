package org.redukti.mathlib;

import java.text.DecimalFormat;

public class M {

    public final static double EPSILON = 2.2204460492503131e-016;

    public static boolean isZero(double d) {
        return Math.abs(d) <= EPSILON;
    }

    /**
     * Test for |x| < fuzz.
     * <p>
     * The choice of fuzz depends on the context of the test. The default is
     * appropriate for ignoring small double precision rounding errors. Chunkier
     * values are appropriate for different calculations: ray trace values are
     * typically good to 1e-10 to 1e-12, geometric modelling or rendering might
     * be as loose as 1e-8 or 1e-6.
     */
    public static boolean is_fuzzy_zero(double x, double fuzz) {
        return Math.abs(x) < fuzz;
    }

    public static boolean is_fuzzy_zero(double x) {
        return is_fuzzy_zero(x, 1e-14);
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

    public static double square(double x) { return x*x; }

    public static int trunc(double value) {
        return (int)(value<0 ? Math.ceil(value) : Math.floor(value));
    }

    public static DecimalFormat decimal_format(int maxFractionDigits) {
        DecimalFormat _decimal_format = new DecimalFormat();
        //formatter.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
        _decimal_format.setMinimumIntegerDigits(1);
        _decimal_format.setMaximumFractionDigits(maxFractionDigits);
        _decimal_format.setMinimumFractionDigits(0);
        _decimal_format.setDecimalSeparatorAlwaysShown(false);
        _decimal_format.setGroupingUsed(false);
        return _decimal_format;
    }

    public static DecimalFormat decimal_format() {
        return decimal_format(3);
    }

    public static DecimalFormat decimal_format_scientific(int maxFractionDigits) {
        DecimalFormat _decimal_format = new DecimalFormat("0.0E0");
        //formatter.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
        _decimal_format.setMinimumIntegerDigits(1);
        _decimal_format.setMaximumFractionDigits(maxFractionDigits);
        _decimal_format.setDecimalSeparatorAlwaysShown(false);
        _decimal_format.setGroupingUsed(true);
        return _decimal_format;
    }

    public static double cosd(double deg)
    {
        return Math.cos(Math.toRadians(deg));
    }

    public static double sind(double deg)
    {
        return Math.sin(Math.toRadians(deg));
    }

    public static double tand(double deg)
    {
        return Math.tan(Math.toRadians(deg));
    }
}
