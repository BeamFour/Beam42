/*
The software is ported from Goptical, hence is licensed under the GPL.
Copyright (c) 2021 Dibyendu Majumdar

Original GNU Optical License and Authors are as follows:

      The Goptical library is free software; you can redistribute it
      and/or modify it under the terms of the GNU General Public
      License as published by the Free Software Foundation; either
      version 3 of the License, or (at your option) any later version.

      The Goptical library is distributed in the hope that it will be
      useful, but WITHOUT ANY WARRANTY; without even the implied
      warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
      See the GNU General Public License for more details.

      You should have received a copy of the GNU General Public
      License along with the Goptical library; if not, write to the
      Free Software Foundation, Inc., 59 Temple Place, Suite 330,
      Boston, MA 02111-1307 USA

      Copyright (C) 2010-2011 Free Software Foundation, Inc
      Author: Alexandre Becoulet
 */


package org.redukti.mathlib;

import java.util.Objects;

/*
Notes:
Plane can be represented as two vectors: point and normal
Ray can be represented as two vectors: origin and direction
*/

public class Vec3Pair {

    public static final Vec3Pair position_000_001 = new Vec3Pair(Vector3.vector3_0, Vector3.vector3_001);

    public final Vector3 v0;
    public final Vector3 v1;

    /**
     * @param v0 First vector, origin / point
     * @param v1 Second vector, direction / normal
     */
    public Vec3Pair(Vector3 v0, Vector3 v1) {
        Objects.requireNonNull(v0);
        Objects.requireNonNull(v1);
        this.v0 = v0;
        this.v1 = v1;
    }

    public final Vector3 point() {
        return v0;
    }

    public final Vector3 origin() {
        return v0;
    }

    public final Vector3 direction() {
        return v1;
    }

    public final Vector3 normal() {
        return v1;
    }

    public final double z0() { return  v0.z(); }
    public final double z1() { return  v1.z(); }

    public final boolean isEquals(Vec3Pair other, double tolerance) {
        return v0.isEqual(other.v0, tolerance) && v1.isEqual(other.v1, tolerance);
    }

    public double pl_ln_intersect_scale(Vec3Pair line) {
        // See https://en.wikipedia.org/wiki/Line%E2%80%93plane_intersection
        return (origin().dot(normal()) - normal().dot(line.origin())) /
                (line.normal().dot(normal()));
    }
    public Vector3 pl_ln_intersect (Vec3Pair line)
    {
        return line.v0.plus(line.v1.times(pl_ln_intersect_scale (line)));
    }
    /**
     * Swap the given element between the member vectors and return a new pair
     */
    public static Vec3Pair swapElement(Vec3Pair p, int j) {
        double[] n0 = new double[3];
        double[] n1 = new double[3];

        for (int i = 0; i < 3; i++) {
            if (i == j) {
                // swap
                n0[i] = p.v1.v(i);
                n1[i] = p.v0.v(i);
            }
            else {
                // retain original
                n0[i] = p.v0.v(i);
                n1[i] = p.v1.v(i);
            }
        }
        return new Vec3Pair(new Vector3(n0[0],n0[1],n0[2]), new Vector3(n0[0],n0[1],n0[2]));
    }

    public String toString() {
        return "[" + v0.toString() + "," + v1.toString() + "]";
    }

    public double x1() {
        return v1.x();
    }
}
