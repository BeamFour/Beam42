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


package org.redukti.jfotoptix.tracing;

import org.redukti.jfotoptix.rendering.Renderer;
import org.redukti.jfotoptix.rendering.Rgb;
import org.redukti.jfotoptix.light.LightRay;
import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector2Pair;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.sys.Element;
import org.redukti.jfotoptix.sys.Image;
import org.redukti.jfotoptix.sys.RaySource;

import java.util.List;

public class RayTraceRenderer {

    /**
     * Draw all tangential rays using specified renderer. Only rays
     * which end up hitting the image plane are drawn when @tt
     * hit_image is set.
     */
    public static void draw_2d(Renderer r, RayTraceResults result, boolean hit_image /*= false*/,
                               Element ref /* = null */) {
        r.group_begin("rays");
        draw_trace_result2d(r, result, ref, hit_image);
        r.group_end();

    }

    private static void draw_trace_result2d(Renderer renderer, RayTraceResults result, Element ref, boolean hit_image) {
        List<RaySource> sl = result.get_source_list();
        double lost_len = result.get_params().get_lost_ray_length();

        if (sl.isEmpty())
            throw new IllegalArgumentException("No source found in trace result");

        double max_intensity = result.get_max_ray_intensity();

        for (RaySource s : sl) {
            try {
                List<TracedRay> rl = result.get_generated(s);
                for (TracedRay ray : rl) {
                    renderer.group_begin("ray");
                    draw_traced_ray_recurs(renderer, ray, lost_len, ref, hit_image, 2, false);
                    renderer.group_end();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get color to use for a ray
     */
    static Rgb ray_to_rgb(LightRay ray) {
        return SpectralLine.get_wavelen_color(ray.get_wavelen());
    }

    static void draw_ray_line(Renderer r, Vector2Pair l, TracedRay ray) {
        r.draw_segment(l, ray_to_rgb(ray));
    }

    static void draw_ray_line(Renderer r, Vector3Pair l, TracedRay ray) {
        r.draw_segment(l, ray_to_rgb(ray));
    }

    static boolean draw_traced_ray_recurs(Renderer renderer, TracedRay ray, double lost_len,
                                          Element ref, boolean hit_image, int D, boolean draw_lost) {

        Transform3 t1 = ray.get_creator().get_transform_to(ref);
        Element i_element = null;

        Vector3 v0 = t1.transform(ray.get_ray().origin());
        Vector3 v1;
        if (ray.is_lost()) {
            if (!draw_lost)
                return false;
            v1 = t1.transform(ray.get_ray().origin().plus(ray.get_ray().direction().times(lost_len)));
        } else {
            i_element = ray.get_intercept_element();
            Transform3 t2 = i_element.get_transform_to(ref);
            v1 = t2.transform(ray.get_intercept_point());
        }
        Vector3Pair p = new Vector3Pair(v0, v1);
        boolean done = false;

        for (TracedRay child_ray = ray.get_first_child(); child_ray != null; child_ray = child_ray.get_next_child()) {
            if (draw_traced_ray_recurs(renderer, child_ray, lost_len, ref, hit_image, 2, false))
                done = true;
        }

        if (!done && hit_image && !(i_element instanceof Image))
            return false;

        switch (D) {
            case 2:
                // skip non tangential rays in 2d mode
                if (Math.abs(p.x1()) > 1e-6)
                    return false;

                draw_ray_line(renderer, new Vector2Pair(p.v0.project_zy(), p.v1.project_zy()), ray);
                break;

            case 3:
                draw_ray_line(renderer, p, ray);
                break;
        }

        return true;
    }
}
