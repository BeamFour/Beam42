// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.seq;

import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.elem.profiles.SurfaceProfile;
import org.redukti.rayoptics.elem.surface.DecenterData;
import org.redukti.rayoptics.elem.surface.IntersectionResult;
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

    public InteractMode interact_mode = InteractMode.TRANSMIT;
    public double delta_n = 0.0;
    public DecenterData decenter = null;
    public double max_aperture = 1.0;
    public SurfaceProfile profile;

    public Interface(InteractMode interact_mode, double delta_n,
                     double max_ap, DecenterData decenter) { // TODO phase element
        this.interact_mode = interact_mode == null ? InteractMode.TRANSMIT : interact_mode;
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

    public void set_optical_power(double pwr) {}

    public void set_optical_power(double pwr, double n_before, double n_after) {}

    public double surface_od() {
        throw new UnsupportedOperationException();
    }

    /**
     * Get a target for ray aiming to aperture boundaries.
     *
     *         The main use case for this function is iterating a ray to the internal
     *         edge of a surface.
     *
     *         Although `rel_dir` is given as a 2d vector, in practice only the 4
     *         quadrant axes are handled in the implementation, a 1D directional
     *         search along a coordinate axis.
     *
     *         Args:
     *             rel_dir: 2d vector encoding coord axis and direction for edge sample
     *
     *         Returns:
     *             edge_pt: intersection point of rel_dir with the aperture boundary
     */
    public Vector2 edge_pt_target(Vector2 rel_dir) {
        return rel_dir.times(max_aperture);
    }

    /**
     * Returns True if the point (x, y) is inside the clear aperture.
     *
     *         Args:
     *             x: x coodinate of the test point
     *             y: y coodinate of the test point
     *             fuzz: tolerance on test pt/aperture comparison,
     *                   i.e. pt fuzzy <= surface_od
     */
    public boolean point_inside(double x, double y, Double fuzz) {
        if (fuzz == null) fuzz = 1e-5;
        return Math.sqrt(x*x + y*y) <= max_aperture + fuzz;
    }

    public void set_max_aperture(double max_ap) {
        this.max_aperture = max_ap;
    }

    /**
     * default behavior is returning +/-max_aperture
     */
    public Vector2 get_y_aperture_extent() {
        return new Vector2(-max_aperture,max_aperture);
    }

    /**
     * Intersect an :class:`~.Interface`, starting from an arbitrary point.
     *
     *         Args:
     *             p0:  start point of the ray in the interface's coordinate system
     *             d:  direction cosine of the ray in the interface's coordinate system
     *             z_dir: +1 if propagation positive direction, -1 if otherwise
     *             eps: numeric tolerance for convergence of any iterative procedure
     *
     *         Returns:
     *             tuple: distance to intersection point *s1*, intersection point *p*
     *
     *         Raises:
     *             :exc:`~rayoptics.raytr.traceerror.TraceMissedSurfaceError`
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
