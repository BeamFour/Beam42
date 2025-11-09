// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector2;

public class TraceOptions {

    public Double pt_inside_fuzz;
    /**
     * if True, do point_inside() test on inc_pt
     */
    boolean check_apertures = false;

    /**
     * if True, apply the `fld` vignetting factors to **pupil**
     */
    boolean apply_vignetting = true;

    PupilType pupil_type = PupilType.REL_PUPIL;

    /**
     *             - if None, append entire ray
     *             - if 'last', append the last ray segment only
     *             - else treat as callable and append the return value
     */
    String output_filter = null;

    /**
     *             - if None, on ray error append nothing
     *             - if 'summary', append the exception without ray data
     *             - if 'full', append the exception with ray data up to error
     *             - else append nothing
     */
    String rayerr_filter = null;

    Vector2 image_pt_2d;
    Vector2 image_delta;
}
