// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

public class RayTraceOptions {
    public Integer first_surf;
    public Integer last_surf;

    public boolean print_details;

    /**
     * accuracy tolerance for surface intersection calculation
     */
    public double eps = 1.0e-12;

    /**
     * if True, do point_inside() test on inc_pt
     */
    public boolean check_apertures = false;
    /**
     * if True, intersect the ray with the object, otherwise
     *                        trace input ray coords directly.
     */
    public boolean intersect_obj = true;
    /**
     * if True, no ray data is saved for phantom interfaces
     */
    public boolean filter_out_phantoms = false;

    public Double pt_inside_fuzz;

    public RayTraceOptions() {}

    public RayTraceOptions(TraceOptions trace_options) {
        this.check_apertures = trace_options.check_apertures;
        this.pt_inside_fuzz = trace_options.pt_inside_fuzz;
    }
}
