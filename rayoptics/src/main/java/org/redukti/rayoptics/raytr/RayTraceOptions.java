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

    public double fuzz = 1e-5;

    public RayTraceOptions() {}

    public RayTraceOptions(TraceOptions trace_options) {
        this.check_apertures = trace_options.check_apertures;
    }
}
