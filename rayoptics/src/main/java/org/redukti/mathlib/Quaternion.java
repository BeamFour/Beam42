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

public class Quaternion {
    public final double x, y, z, w;

    public Quaternion(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    /**
     * Get shortest arc rotation between for a to be in the same direction as b.
     * Both vectors must be unit vectors.
     */
    public static Quaternion get_rotation_between (Vector3 a, Vector3 b)
    {
        // Do not know the source of following equation
        // Believe it generates a Quaternion representing the rotation
        // of vector a to vector b
        // Closest match of the algo:
        // https://stackoverflow.com/questions/1171849/finding-quaternion-representing-the-rotation-from-one-vector-to-another
        // FIXME It seems this implementation is not safe
        // See QuaternionBase<Derived>::setFromTwoVectors in eigen library
        // Also stackoverflow discussion

        Vector3 cp = a.cross(b);
        double _x = cp.x ();
        double _y = cp.y ();
        double _z = cp.z ();
        double _w = a.dot(b) + 1.0;
        double n = norm(_x, _y, _z, _w);
        _x = _x/n;
        _y = _y/n;
        _z = _z/n;
        _w = _w/n;
        return new Quaternion(_x,_y,_z,_w);
    }

    static final double norm (double x, double y, double z, double w)
    {
        return Math.sqrt (x * x + y * y + z * z + w * w);
    }

    public String toString() {
        return "["+x+','+y+','+z+','+w+']';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Quaternion that = (Quaternion) o;

        if (Double.compare(that.x, x) != 0) return false;
        if (Double.compare(that.y, y) != 0) return false;
        if (Double.compare(that.z, z) != 0) return false;
        return Double.compare(that.w, w) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(z);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(w);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
