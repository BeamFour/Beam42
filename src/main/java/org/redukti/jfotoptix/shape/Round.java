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

import org.redukti.jfotoptix.math.Triangle2;
import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.patterns.Distribution;
import org.redukti.jfotoptix.patterns.Pattern;

import java.util.Random;
import java.util.function.Consumer;

import static org.redukti.jfotoptix.math.MathUtils.square;

public abstract class Round extends ShapeBase {

    static Random random = new Random();

    final boolean hole;

    abstract double get_xy_ratio();

    abstract double get_external_xradius();

    abstract double get_internal_xradius();

    public Round(boolean hole) {
        this.hole = hole;
    }

    public void get_pattern(Consumer<Vector2> f,
                            Distribution d,
                            boolean unobstructed) {
        final double epsilon = 1e-8;
        final double xyr = 1.0 / get_xy_ratio();
        final double tr = get_external_xradius() * d.get_scaling();
        boolean obstructed = hole && !unobstructed;
        final double hr = obstructed
                ? get_internal_xradius() * (2.0 - d.get_scaling())
                : 0.0;
        int rdens = (int) Math.floor((double) d.get_radial_density()
                - (d.get_radial_density() * (hr / tr)));
        rdens = Math.max(1, rdens);
        final double step = (tr - hr) / rdens;

        Pattern p = d.get_pattern();

        switch (p) {
            case MeridionalDist: {

                if (!obstructed)
                    f.accept(Vector2.vector2_0);

                final double bound = obstructed ? hr - epsilon : epsilon;

                for (double r = tr; r > bound; r -= step) {
                    f.accept(new Vector2(0, r * xyr));
                    f.accept(new Vector2(0, -r * xyr));
                }
            }
            break;

            case SagittalDist: {

                if (!obstructed)
                    f.accept(Vector2.vector2_0);

                final double bound = obstructed ? hr - epsilon : epsilon;

                for (double r = tr; r > bound; r -= step) {
                    f.accept(new Vector2(r, 0));
                    f.accept(new Vector2(-r, 0));
                }
            }
            break;

            case CrossDist: {

                if (!obstructed)
                    f.accept(Vector2.vector2_0);

                final double bound = obstructed ? hr - epsilon : epsilon;

                for (double r = tr; r > bound; r -= step) {
                    f.accept(new Vector2(0, r * xyr));
                    f.accept(new Vector2(r, 0));
                    f.accept(new Vector2(0, -r * xyr));
                    f.accept(new Vector2(-r, 0));
                }
            }
            break;

            case RandomDist: {
                if (!obstructed)
                    f.accept(Vector2.vector2_0);

                final double bound = obstructed ? hr - epsilon : epsilon;

                double tr1 = tr / 20.0;
                for (double r = tr1; r > bound; r -= step) {
                    double astep = (Math.PI / 3) / Math.ceil(r / step);
                    // angle
                    for (double a = 0; a < 2 * Math.PI - epsilon; a += astep) {
                        Vector2 v = new Vector2(Math.sin(a) * r + (random.nextDouble() - .5) * step,
                                Math.cos(a) * r * xyr + (random.nextDouble() - .5) * step);
                        double h = Math.hypot(v.x(), v.y() / xyr);
                        if (h < tr && (h > hr || unobstructed))
                            f.accept(v);
                    }
                }
            }
            break;

            case DefaultDist:
            case HexaPolarDist: {

                if (!obstructed)
                    f.accept(Vector2.vector2_0);

                final double bound = obstructed ? hr - epsilon : epsilon;

                for (double r = tr; r > bound; r -= step) {
                    double astep = (Math.PI / 3) / Math.ceil(r / step);

                    for (double a = 0; a < 2 * Math.PI - epsilon; a += astep)
                        f.accept(new Vector2(Math.sin(a) * r, Math.cos(a) * r * xyr));
                }
            }
            break;

            default: {
                Consumer<Vector2> f2 = (Vector2 v) -> {
                    // unobstructed pattern must be inside external
                    // radius
                    if (square(v.x())
                            + square(v.y() / xyr)
                            < square(tr))
                        f.accept(v);
                };
                super.get_pattern(f2, d, unobstructed);
                break;
            }
        }
    }

    @Override
    public int get_contour_count() {
        return hole ? 2 : 1;
    }

    double get_radial_step(double resolution) {
        final double xyr = 1.0 / get_xy_ratio();
        double width = xyr <= 1.
                ? get_external_xradius() - get_internal_xradius()
                : get_external_xradius() * xyr
                - get_internal_xradius() * xyr;

        if (resolution < width / 30.)
            resolution = width / 30.;

        if (resolution > width)
            resolution = width;

        if (hole && resolution > get_internal_xradius())
            resolution = get_internal_xradius();

        return (get_external_xradius() - get_internal_xradius())
                / Math.ceil(width / resolution);
    }

    @Override
    public void get_contour(int contour,
                            Consumer<Vector2> f,
                            double resolution) {
        final double epsilon = 1e-8;
        final double xyr = 1.0 / get_xy_ratio();
        double r;

        assert (contour < get_contour_count());

        if (hole && contour == 1)
            r = get_internal_xradius();
        else
            r = get_external_xradius();

        double astep1 = (Math.PI / 3.0) / Math.round(r / get_radial_step(resolution));

        for (double a1 = 0; a1 < 2 * Math.PI - epsilon; a1 += astep1)
            f.accept(new Vector2(Math.cos(a1) * r, Math.sin(a1) * r * xyr));
    }

    @Override
    public void get_triangles(Consumer<Triangle2> f, double resolution) {
        final double epsilon = 1e-8;
        final double xyr = 1.0 / get_xy_ratio();
        final double rstep = get_radial_step(resolution);

        double astep1;
        double r;

        if (!hole) {
            r = rstep;
            astep1 = Math.PI / 3.0;

            // central hexagon

            for (double a1 = 0; a1 < Math.PI - epsilon; a1 += astep1) {
                Vector2 a = new Vector2(Math.cos(a1) * rstep, Math.sin(a1) * rstep * xyr);
                Vector2 b = new Vector2(Math.cos(a1 + astep1) * rstep,
                        Math.sin(a1 + astep1) * rstep * xyr);
                Vector2 z = Vector2.vector2_0;

                f.accept(new Triangle2(b, a, z));
                f.accept(new Triangle2(b.negate(), a.negate(), z));
            }
        } else {
            r = get_internal_xradius();
            astep1 = (Math.PI / 3.0) / Math.round(r / rstep);
        }

        // hexapolar distributed triangles

        for (; r < get_external_xradius() - epsilon; r += rstep) {
            double astep2 = (Math.PI / 3.0) / Math.round((r + rstep) / rstep);
            double a1 = 0, a2 = 0;

            while ((a1 < Math.PI - epsilon) || (a2 < Math.PI - epsilon)) {
                Vector2 a = new Vector2(Math.cos(a1) * r, Math.sin(a1) * r * xyr);
                Vector2 b = new Vector2(Math.cos(a2) * (r + rstep),
                        Math.sin(a2) * (r + rstep) * xyr);
                Vector2 c;

                if (a1 + epsilon > a2) {
                    a2 += astep2;
                    c = new Vector2(Math.cos(a2) * (r + rstep),
                            Math.sin(a2) * (r + rstep) * xyr);
                } else {
                    a1 += astep1;
                    c = new Vector2(Math.cos(a1) * r, Math.sin(a1) * r * xyr);
                }

                f.accept(new Triangle2(a, c, b));
                f.accept(new Triangle2(a.negate(), c.negate(), b.negate()));
            }

            astep1 = astep2;
        }
    }

}
