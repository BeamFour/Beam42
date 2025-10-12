// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.seq;

/**
 * Gap container class.
 *
 *     The gap class represents the space between 2 surfaces. It contains the
 *     media definition for the space and a (z) displacement between the
 *     adjacent surfaces.
 *
 *     The most common use case is an optical system with surfaces centered on a
 *     common axis. The Gap structure implements this case in the simplest manner.
 *     More complicated transformations between surfaces are implemented using
 *     transformations associated with the surfaces themselves.
 *
 *     Attributes:
 *         thi: the length (along z) of the gap
 *         medium: a :class:`~optical.medium.Medium` or a catalog glass instance
 */
public class Gap {
    public double thi;
    public Medium medium;

    public Gap(double thi, Medium medium) {
        this.thi = thi;
        this.medium = medium;
    }

    public Gap() {
        this(0.0, Air.INSTANCE);
    }

    public void apply_scale_factor(double scale_factor) {
        thi *= scale_factor;
    }

    public StringBuilder toString(StringBuilder sb) {
        sb.append(getClass().getSimpleName()).append("(")
                .append("t=").append(thi).append(", medium=");
        medium.toString(sb);
        sb.append(")");
        return sb;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }
}
