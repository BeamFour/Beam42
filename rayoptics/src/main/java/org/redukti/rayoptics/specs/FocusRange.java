// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.specs;

/**
 Focus range specification

    Attributes:
        focus_shift: focus shift (z displacement) from nominal image interface
        defocus_range: +/- half the total focal range, from the focus_shift
                       position
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

    public void apply_scale_factor(double scale_factor) {
        focus_shift *= scale_factor;
        defocus_range *= scale_factor;
    }

    /**
     * return focus position for input focus range parameter
     *
     *         Args:
     *             fr (float): focus range parameter, -1.0 to 1.0
     *
     *         Returns:
     *             focus position for input focus range parameter
     */
    public double get_focus(double fr) {
        return focus_shift + fr * defocus_range;
    }
    public double get_focus() { return get_focus(0.0); }
}
