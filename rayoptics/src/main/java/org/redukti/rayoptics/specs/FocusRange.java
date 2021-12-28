package org.redukti.rayoptics.specs;

/**
 Focus range specification
 */
public class FocusRange {
    /**
     * focus shift (z displacement) from nominal image interface
     */
    public double focus_shift;
    /**
     * +/- half the total focal range, from the focus_shift position
     */
    public double defocus_range;

    public FocusRange(double focus_shift, double defocus_range) {
        this.focus_shift = focus_shift;
        this.defocus_range = defocus_range;
    }

    public FocusRange() {
        this(0.0, 0.0);
    }

    @Override
    public String toString() {
        return "FocusRange{" +
                "focus_shift=" + focus_shift +
                ", defocus_range=" + defocus_range +
                '}';
    }

    /**
     * return focus position for input focus range parameter
     *
     * @param fr focus range parameter, -1.0 to 1.0
     * @return focus position for input focus range parameter
     */
    public double get_focus(double fr) {
        return focus_shift + fr * defocus_range;
    }
}
