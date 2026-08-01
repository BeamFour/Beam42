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
    public boolean check_apertures = false;

    /**
     * if True, apply the `fld` vignetting factors to **pupil**
     */
    public boolean apply_vignetting = true;

    public PupilType pupil_type = PupilType.REL_PUPIL;

    /**
     *             - if None, append entire ray
     *             - if 'last', append the last ray segment only
     *             - else treat as callable and append the return value
     */
    public String output_filter = null;

    /**
     *             - if None, on ray error append nothing
     *             - if 'summary', append the exception without ray data
     *             - if 'full', append the exception with ray data up to error
     *             - else append nothing
     */
    public String rayerr_filter = null;

    public Vector2 image_pt_2d;
    public Vector2 image_delta;

    public TraceOptions copy() {
        var traceOptions = new TraceOptions();
        traceOptions.pt_inside_fuzz = this.pt_inside_fuzz;
        traceOptions.check_apertures = this.check_apertures;
        traceOptions.apply_vignetting = this.apply_vignetting;
        traceOptions.pupil_type = this.pupil_type;
        traceOptions.output_filter = this.output_filter;
        traceOptions.rayerr_filter = this.rayerr_filter;
        traceOptions.image_pt_2d = this.image_pt_2d;
        traceOptions.image_delta = this.image_delta;
        return traceOptions;
    }
}
