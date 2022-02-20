package org.redukti.rayoptics.raytr;

public class RayTraceOptions {
    public Integer first_surf;
    public Integer last_surf;
    public boolean print_details;

    /**
     * accuracy tolerance for surface intersection calculation
     */
    public double eps = 1.0e-12;
}
