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

import org.redukti.jfotoptix.math.*;

public abstract class CurveBase implements Curve {

    @Override
    public Vector2 derivative(Vector2 xy) {
        return base_derivative(xy);
    }

    protected Vector2 base_derivative(Vector2 xy) {
        //double abserr;
        DerivFunction dxf = (x) -> this.sagitta(new Vector2(x, xy.y()));
        DerivFunction dyf = (y) -> this.sagitta(new Vector2(xy.x(), y));

        DerivResult result = Derivatives.central_derivative(dxf, xy.x(), 1e-6);
        double dx = result.result;
        result = Derivatives.central_derivative(dyf, xy.y(), 1e-6);
        double dy = result.result;
        // TODO what do we do about error?
        return new Vector2(dx, dy);
    }

    @Override
    public Vector3 intersect(Vector3Pair ray) {
        return base_intersect(ray);
    }

    protected Vector3 base_intersect(Vector3Pair ray) {
        Vector3 origin;
        // initial intersection with z=0 plane
        {
            double s = ray.direction ().z ();

            if (s == 0)
                return null;

            double a = -ray.origin ().z () / s;

            if (a < 0)
                return null;

            origin  = ray.origin ().plus(ray.direction ().times(a));
        }

        int n = 32; // avoid infinite loop

        while (n-- > 0)
        {
            double new_sag = sagitta (origin.project_xy ());
            double old_sag = origin.z ();

            // project previous intersection point on curve
            origin = new Vector3(origin.x(), origin.y(), new_sag);

            // stop if close enough
            if (Math.abs (old_sag - new_sag) < 1e-10)
                break;

            // get curve tangeante plane at intersection point
            Vector3 norm = normal (origin);

            // intersect again with new tangeante plane
            Vector3Pair p = new Vector3Pair(origin, norm);
            double a = p.pl_ln_intersect_scale (ray);

            if (a < 0)
                return null;
            // See https://en.wikipedia.org/wiki/Line%E2%80%93plane_intersection
            origin  = ray.origin ().plus(ray.direction ().times(a));
        }
        return origin;
    }

    @Override
    public Vector3 normal(Vector3 point) {
        return base_normal(point);
    }

    protected Vector3 base_normal(Vector3 point) {
        Vector2 d = derivative (point.project_xy ());
        return new Vector3 (d.x (), d.y (), -1.0).normalize();
    }
}
