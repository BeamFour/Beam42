package org.redukti.jfotoptix.curve;

import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Position;

public interface Curve {
    /** Get curve sagitta (z) at specified point */
    double sagitta (Vector2 xy);

    /** Get curve dz/dx and dx/dy partial derivatives (gradient) at specified
     * point */
    Vector2 derivative (Vector2 xy);

    /** Get intersection point between curve and 3d ray. Return
     false if no intersection occurred. ray must have a position vector and
     direction vector (cosines). */
    Vector3 intersect(Vector3Position ray);

    /** Get normal to curve surface at specified point. */
    Vector3 normal (Vector3 point);
}
