package org.redukti.rayoptics.seq;

import org.redukti.rayoptics.elem.DecenterData;
import org.redukti.rayoptics.elem.IntersectionResult;
import org.redukti.rayoptics.elem.SurfaceProfile;
import org.redukti.rayoptics.math.Vector3;
import org.redukti.rayoptics.util.ZDir;

/**
 *     Basic part of a sequential model
 *
 *     The :class:`~sequential.SequentialModel` is a sequence of Interfaces and
 *     Gaps. The Interface class is a boundary between two adjacent Gaps and
 *     their associated media. It specifies several methods that must be
 *     implemented to model the optical behavior of the interface.
 *
 *     The Interface class addresses the following use cases:
 *
 *         - support for ray intersection calculation during ray tracing
 *             - interfaces can be tilted and decentered wrt the adjacent gaps
 *         - support for getting and setting the optical power of the interface
 *         - support for various optical properties, i.e. does it reflect or
 *           transmit
 *         - supports a basic idea of size, the max_aperture
 *
 *     Attributes:
 *         interact_mode: 'transmit' | 'reflect' | 'dummy'
 *         delta_n: refractive index difference across the interface
 *         decenter: :class:`~rayoptics.elem.surface.DecenterData` for the interface, if specified
 *         max_aperture: the maximum aperture radius on the interface
 *
 */
public class Interface {

    public String interact_mode;
    public double delta_n;
    public DecenterData decenter;
    public double max_aperture;
    public SurfaceProfile profile;

    public Interface(String interact_mode, double delta_n,
                     double max_ap, DecenterData decenter) { // TODO phase element
        this.interact_mode = interact_mode;
        this.delta_n = delta_n;
        this.decenter = decenter;
        this.max_aperture = max_ap;
        // TODO phase element
    }

    public void update() {
        if (decenter != null)
            decenter.update();
    }

    public double profile_cv() {
        return 0.0;
    }

    public void set_optical_power(double pwr, double n_before, double n_after) {
    }

    public double surface_od() {
        throw new UnsupportedOperationException();
    }

    public void set_max_aperture(double max_ap) {
        this.max_aperture = max_ap;
    }

    /**
     * Intersect an :class:`~.Interface`, starting from an arbitrary point.
     *
     * @param p0 start point of the ray in the interface's coordinate system
     * @param d direction cosine of the ray in the interface's coordinate system
     * @param eps numeric tolerance for convergence of any iterative procedure
     * @param z_dir +1 if propagation positive direction, -1 if otherwise
     * @return tuple: distance to intersection point *s1*, intersection point *p*
     */
    public IntersectionResult intersect(Vector3 p0, Vector3 d, double eps, ZDir z_dir) {
        throw new UnsupportedOperationException();
    }

    public IntersectionResult intersect(Vector3 p0, Vector3 d) {
        return intersect(p0, d, 1.0e-12, ZDir.PROPAGATE_RIGHT);
    }

    /**
     * Returns the unit normal of the profile at point *p*.
     */
    public Vector3 normal(Vector3 p) {
        throw new UnsupportedOperationException();
    }

    // TODO phase() method

    public void apply_scale_factor(double scale_factor) {
        this.max_aperture *= scale_factor;
        if (decenter != null)
            decenter.apply_scale_factor(scale_factor);
    }

    public StringBuilder toString(StringBuilder sb) {
        return sb;
    }

    public double optical_power() {
        return 0.0;
    }
}
