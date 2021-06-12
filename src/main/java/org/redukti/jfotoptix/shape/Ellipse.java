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


package org.redukti.jfotoptix.shape;

import org.redukti.jfotoptix.math.MathUtils;
import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.math.Vector2Pair;

public class Ellipse extends Round {

    double _xr, _yr;
    double _xy_ratio;
    double _e2;

    void set_radius(double x_radius, double y_radius) {
        _xr = x_radius;
        _yr = y_radius;
        _xy_ratio = x_radius / y_radius;
        _e2 = MathUtils.square(Math.sqrt(Math.abs(_xr * _xr - _yr * _yr))
                / Math.max(_xr, _yr));
    }

    public Ellipse(double x_radius, double y_radius) {
        super(false);
        set_radius(x_radius, y_radius);
    }


    @Override
    public double get_xy_ratio() {
        return _xy_ratio;
    }

    @Override
    public double get_external_xradius() {
        return _xr;
    }

    @Override
    public double get_internal_xradius() {
        return 0.0;
    }

    @Override
    public boolean inside(Vector2 point) {
        return (MathUtils.square(point.x()) + MathUtils.square(point.y() * _xy_ratio)
                <= MathUtils.square(_xr));
    }

    @Override
    public double max_radius() {
        return Math.max(_yr, _xr);
    }

    @Override
    public double min_radius() {
        return Math.min(_yr, _xr);
    }

    @Override
    public double get_outter_radius(Vector2 dir) {
        return _xr > _yr
                ? Math.sqrt(MathUtils.square(_yr) / (1. - _e2 * MathUtils.square(dir.x())))
                : Math.sqrt(MathUtils.square(_xr)
                / (1. - _e2 * MathUtils.square(dir.y())));
    }

    @Override
    public Vector2Pair get_bounding_box() {
        Vector2 hs = new Vector2(_xr, _yr);

        return new Vector2Pair(hs.negate(), hs);
    }
}
