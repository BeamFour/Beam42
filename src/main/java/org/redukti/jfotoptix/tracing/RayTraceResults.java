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

import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.model.Element;
import org.redukti.jfotoptix.model.Image;
import org.redukti.jfotoptix.model.RaySource;
import org.redukti.jfotoptix.model.Surface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RayTraceResults {

    static final class RaysAtElement {
        List<TracedRay> _intercepted = new ArrayList<>(); // list of rays for each intercepted surfaces
        List<TracedRay> _generated = new ArrayList<>(); // list of rays for each generator surfaces
        boolean _save_intercepted_list = true;
        boolean _save_generated_list = true;
    }

    Map<Integer, RaysAtElement> raysByElement = new HashMap<>();
    RayTraceParameters _parameters;
    List<RaySource> _sources = new ArrayList<>();
    List<TracedRay> _rays = new ArrayList<>();

    public RayTraceResults(RayTraceParameters parameters) {
        this._parameters = parameters;
    }

    public List<TracedRay> get_generated(Element e) {
        RaysAtElement er = get_element_result(e);
        if (er == null) {
            throw new IllegalArgumentException("No generated rays at element " + e);
        }
        return er._generated;
    }

    public Vector3 get_intercepted_center(Image image) {
        Vector3Pair win = get_intercepted_window(image);
        return (win.v0.plus(win.v1)).divide(2);
    }

    public List<TracedRay> get_intercepted(Element e) {
        RaysAtElement er = get_element_result(e);
        if (er == null) {
            throw new IllegalArgumentException("No intercepted rays at element " + e);
        }
        return er._intercepted;
    }

    public Vector3Pair get_intercepted_window(Surface s) {
        List<TracedRay> intercepts = get_intercepted(s);

        if (intercepts.isEmpty())
            throw new IllegalArgumentException("No ray intercepts found on the surface " + s);

        Vector3 first = intercepts.get(0).get_intercept_point();
        Vector3 second = first;
        for (TracedRay i : intercepts) {
            Vector3 ip = i.get_intercept_point();

            if (first.x() > ip.x())
                first = first.x(ip.x());
            else if (second.x() < ip.x())
                second = second.x(ip.x());

            if (first.y() > ip.y())
                first = first.y(ip.y());
            else if (second.y() < ip.y())
                second = second.y(ip.y());

            if (first.z() > ip.z())
                first = first.z(ip.z());
            else if (second.z() < ip.z())
                second = second.z(ip.z());
        }
        return new Vector3Pair(first, second);
    }

    public List<RaySource> get_source_list() {
        return _sources;
    }

    public RayTraceParameters get_params() {
        return _parameters;
    }

    RaysAtElement get_element_result(Element e) {
        RaysAtElement re = raysByElement.get(e.id());
        if (re == null) {
            re = new RaysAtElement();
            raysByElement.put(e.id(), re);
        }
        return re;
    }

    public void add_intercepted(Surface s, TracedRay ray) {
        RaysAtElement er = get_element_result(s);
        er._intercepted.add(ray);
    }

    public void add_source(RaySource source) {
        _sources.add(source);
    }

    public TracedRay newRay(Vector3 origin, Vector3 direction) {
        TracedRay ray = new TracedRay(origin, direction);
        _rays.add(ray);
        return ray;
    }

    public double get_max_ray_intensity() {
        double res = 0;
        for (TracedRay r : _rays) {
            double i = r.get_intensity();
            if (i > res)
                res = i;
        }
        return res;
    }

    public Vector3 get_intercepted_centroid(Image image) {
        List<TracedRay> intercepts = get_intercepted(image);
        int count = 0;
        Vector3 center = Vector3.vector3_0;
        if (intercepts.isEmpty())
            throw new IllegalArgumentException("no ray intercepts found on the surface");
        for (TracedRay i : intercepts) {
            center = center.plus(i.get_intercept_point());
            count++;
        }
        center = center.divide(count);
        return center;
    }

    public void report() {
        System.out.println("Ray Trace Report");
        System.out.println("================");
        List<RaySource> sl = get_source_list();
        if (sl.isEmpty())
            throw new IllegalArgumentException("No source found in trace result");

        for (RaySource s : sl) {
            List<TracedRay> rl = get_generated(s);
            for (TracedRay ray : rl) {
                if (hitImage(ray, true)) {
                    report(ray);
                }
            }
        }
    }

    private boolean hitImage(TracedRay ray, boolean tangential) {
        if (ray.get_intercept_element() != null && ray.get_intercept_element() instanceof Image) {
            return true;
        }
        if (ray.get_first_child() == null) {
            return false;
        }
        if (tangential && Math.abs(ray.get_ray().x1()) > 1e-6)
            return false;
        return hitImage(ray.get_first_child(), tangential);
    }

    private void report(TracedRay ray) {
        System.out.println(ray);
    }
}
