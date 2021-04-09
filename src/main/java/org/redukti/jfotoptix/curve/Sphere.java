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
package org.redukti.jfotoptix.curve;

import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;

import static org.redukti.jfotoptix.math.MathUtils.square;

public class Sphere extends ConicBase {

    public Sphere(double roc) {
        super(roc, 0.0);
    }

    @Override
    public double sagitta(double r) {
        double x = Math.abs(_roc) - Math.sqrt(square(_roc) - square(r));
        return _roc < 0 ? -x : x;
    }

    @Override
    public double derivative(double r) {
        return r / Math.sqrt(square(_roc) - square(r));
    }

    /*

ligne AB A + t * B
sphere passant par C(Cx, 0, 0), rayon R

d = Ax - R - Cx
(Bz*t+Az)^2+(By*t+Ay)^2+(Bx*t+d)^2=R^2

t=-(sqrt((Bz^2+By^2+Bx^2)*R^2+(-Bz^2-By^2)*d^2+(2*Az*Bx*Bz+2*Ay*Bx*By)
*d-Ay^2*Bz^2+2*Ay*Az*By*Bz-Az^2*By^2+(-Az^2-Ay^2)*Bx^2)+Bx*d+Az*Bz+Ay*By)/(Bz^2+By^2+Bx^2),

t= (sqrt((Bz^2+By^2+Bx^2)*R^2+(-Bz^2-By^2)*d^2+(2*Az*Bx*Bz+2*Ay*Bx*By)
*d-Ay^2*Bz^2+2*Ay*Az*By*Bz-Az^2*By^2+(-Az^2-Ay^2)*Bx^2)-Bx*d-Az*Bz-Ay*By)/(Bz^2+By^2+Bx^2)

*/

    public Vector3 intersect(Vector3Pair ray) {
        final double ax = (ray.origin().x());
        final double ay = (ray.origin().y());
        final double az = (ray.origin().z());
        final double bx = (ray.direction().x());
        final double by = (ray.direction().y());
        final double bz = (ray.direction().z());

        // double bz2_by2_bx2 = math::square(bx) + math::square(by) +
        // math::square(bx);
        // == 1.0

        double d = az - _roc;
        double ay_by = ay * by;
        double ax_bx = ax * bx;

        double s = +square(_roc) // * bz2_by2_bx2
                + 2.0 * (ax_bx + ay_by) * bz * d + 2.0 * ax_bx * ay_by
                - square(ay * bx) - square(ax * by)
                - (square(bx) + square(by)) * square(d)
                - (square(ax) + square(ay)) * square(bz);

        // no sphere/ray colision
        if (s < 0)
            return null;

        s = Math.sqrt(s);

        // there are 2 possible sphere/line colision point, keep the right
        // one depending on ray direction
        if (_roc * bz > 0)
            s = -s;

        double t = (s - (bz * d + ax_bx + ay_by)); // / bz2_by2_bx2;

        // do not colide if line intersection is before ray start position
        if (t <= 0)
            return null;

        // intersection point
        return ray.origin().plus(ray.direction().times(t));
    }

    @Override
    public Vector3 normal (Vector3 point)
    {
        // normalized vector to sphere center
        Vector3 normal = new Vector3(point.x(), point.y(), point.z() - _roc).normalize();
        if (_roc < 0)
            normal = normal.negate();
        return normal;
    }
}
