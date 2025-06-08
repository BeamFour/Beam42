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

import org.redukti.jfotoptix.medium.Medium;
import org.redukti.jfotoptix.math.*;
import org.redukti.jfotoptix.model.*;

import java.util.ArrayList;
import java.util.List;

public class RayTracer {

    static final class RayCollection {
        List<TracedRay> rays = new ArrayList<>();
    }

    public RayTraceResults trace(OpticalSystem system, RayTraceParameters parameters) {
        RayTraceResults result = new RayTraceResults(parameters);
        // stack of rays to propagate
        RayCollection tmp[] = new RayCollection[]{new RayCollection(), new RayCollection()};
        RayGenerator rayGenerator = new RayGenerator();

        int swaped = 0;
        List<TracedRay> generated;
        List<TracedRay> source_rays = tmp[1].rays;
        List<Element> seq = parameters._sequence;
        Element entrance = null;

        // find entry element (first non source)
        for (int i = 0; i < seq.size(); i++) {
            if (!(seq.get(i) instanceof RaySource)) {
                entrance = seq.get(i);
                break;
            }
        }

        for (int i = 0; i < seq.size(); i++) {
            Element element = seq.get(i);
            RayTraceResults.RaysAtElement er = result.get_element_result(element);

            generated = er._generated != null ? er._generated : tmp[swaped].rays;
            generated.clear();

            if (element instanceof PointSource) {
                PointSource source = (PointSource) element;
                result.add_source(source);
                List<Element> elist = new ArrayList<>();
                if (entrance != null)
                    elist.add(entrance);
                List<TracedRay> rays = rayGenerator.generate_rays_simple(result, parameters, source, elist);
                generated.addAll(rays);
            } else {
                List<TracedRay> rays = process_rays(element, result, source_rays);
                // swap ray buffers
                generated.addAll(rays);
            }
            source_rays = generated;
            swaped ^= 1;
        }
        return result;
    }

    protected List<TracedRay> process_rays(Element e, RayTraceResults result, List<TracedRay> input) {
        if (e instanceof Stop stop) {
            return process_rays_stop(stop, result, input);
        }
        else if (e instanceof Surface surface) {
            return process_rays_surface(surface, result, input);
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    List<TracedRay> process_rays_surface(Surface surface,  RayTraceResults result,
                                 List<TracedRay> input) {
        RayTraceParameters params = result._parameters;
        List<TracedRay> rays = new ArrayList<>();
        for (TracedRay i : input) {
            TracedRay ray = i;
            Transform3 t = ray.get_creator().get_transform_to(surface);
            Vector3Pair local = t.transform_line(ray.get_ray());
            Vector3Pair pt = surface instanceof Stop stop ?
                    intersect_with_stop(stop, params, local) :
                    intersect_with_surface(surface, params, local);
            if (pt != null) {
                result.add_intercepted(surface, ray);
                TracedRay cray = trace_ray(surface,  result, ray, local, pt);
                if (cray != null)
                    rays.add(cray);
            }
        }
        return rays;
    }

    TracedRay trace_ray(Surface surface,
                        RayTraceResults result, TracedRay incident,
                        Vector3Pair local,
                        Vector3Pair pt) {
        incident.set_len((pt.origin().minus(local.origin())).len());
        incident.set_intercept(surface, pt.origin());
        incident.set_intercept_intensity(1.0);
        if (surface instanceof Stop stop) {
            return trace_across_stop(stop, result, incident, local, pt);
        }
        else if (surface instanceof OpticalSurface opticalSurface) {
            return trace_across_surface(opticalSurface, result, incident, local, pt);
        }
        else {
            return null;
        }
    }

    private TracedRay trace_across_surface(OpticalSurface surface, RayTraceResults result, TracedRay incident, Vector3Pair local, Vector3Pair intersect) {
        boolean right_to_left = intersect.normal().z() > 0;
        Medium prev_mat = surface.get_material(right_to_left ? 1 : 0);
        Medium next_mat = surface.get_material(!right_to_left ? 1 : 0);
        if (prev_mat != incident.get_material()) {
            return null;
        }

        double wl = incident.get_wavelen();
        double index = prev_mat.get_refractive_index(wl)
                / next_mat.get_refractive_index(wl);

        // refracted ray direction
        Vector3 direction = refract(surface, local, intersect.normal(), index);
        if (direction == null) {
            // total internal reflection
            Vector3 o = intersect.origin();
            Vector3 dir = reflect(surface, local, intersect.normal());
            TracedRay r = result.newRay(o, dir);

            r.set_wavelen(wl);
            r.set_intensity(incident.get_intensity());
            r.set_material(prev_mat);

            r.set_creator(surface);
            incident.add_generated(r);

            return r;
        }

        // transmit
        if (!next_mat.is_opaque()) {
            Vector3 o = intersect.origin();
            TracedRay r = result.newRay(o, direction);

            r.set_wavelen(wl);
            r.set_intensity(incident.get_intensity());
            r.set_material(next_mat);

            r.set_creator(surface);
            incident.add_generated(r);
            return r;
        }

        // reflect
        if (next_mat.is_reflecting()) {
            Vector3 o = intersect.origin();
            Vector3 dir = reflect(surface, local, intersect.normal());

            TracedRay r = result.newRay(o, dir);

            r.set_wavelen(wl);
            r.set_intensity(incident.get_intensity());
            r.set_material(prev_mat);
            r.set_creator(surface);
            incident.add_generated(r);
            return r;
        }
        return null;
    }

    /**
     * Compute refracted ray direction given
     *
     * @param ray    Original ray - position and direction
     * @param normal Normal to the intercept
     * @param mu     Ration of refractive index
     */
    Vector3 compute_refraction(OpticalSurface surface, Vector3Pair ray, Vector3 normal, double mu) {
        Vector3 N = normal.times(-1.0); // Because we changed sign at intersection
        // See Feder paper p632
        double O2 = N.dot(N);
        double E1 = ray.direction().dot(N);
        double E1_ = Math.sqrt(O2 * (1.0 - mu * mu) + mu * mu * E1 * E1);
        if (Double.isNaN(E1_)) {
            return null;
        }
        double g1 = (E1_ - mu * E1) / O2;
        return ray.direction().times(mu).plus(N.times(g1));
    }

    Vector3 refract(OpticalSurface surface, Vector3Pair ray,
                    Vector3 normal,
                    double refract_index) {
        // Algorithm from Bram de Greve article "Reflections & Refractions in
        // Raytracing" http://www.bramz.org/

        assert (Math.abs(normal.len() - 1.0) < 1e-10);
        assert (Math.abs((ray.direction().len()) - 1.0) < 1e-10);

        double cosi = normal.dot(ray.direction());
        double sint2 = MathUtils.square(refract_index) * (1.0 - MathUtils.square(cosi));

        if (sint2 > 1.0)
            return null; // total internal reflection

//        Vector3 dir = ray.direction().times(refract_index).minus(
//                normal.times(refract_index * cosi + Math.sqrt(1.0 - sint2)));

        // This uses Feder refractive formula
       return compute_refraction(surface, ray, normal, refract_index);
    }

    Vector3 reflect(OpticalSurface surface, Vector3Pair ray, Vector3 normal) {
        // Algorithm from Bram de Greve article "Reflections & Refractions in
        // Raytracing" http://www.bramz.org/

        assert (Math.abs(normal.len() - 1.0) < 1e-10);
        assert (Math.abs((ray.direction().len()) - 1.0) < 1e-10);

        double cosi = normal.dot(ray.direction());

        return ray.direction().minus(normal.times(2.0 * cosi));
    }

    private Vector3Pair intersect_with_surface(Surface surface, RayTraceParameters params, Vector3Pair ray) {
        Vector3 origin = surface.get_curve().intersect(ray);
        if (origin == null)
            return null;

        if (!params.get_unobstructed()
                && !surface.get_shape().inside(origin.project_xy()))
            return null;

        Vector3 normal = surface.get_curve().normal(origin);
        if (ray.direction().z() < 0)
            normal = normal.negate();

        return new Vector3Pair(origin, normal);
    }

    private Vector3Pair intersect_with_stop(Stop surface, RayTraceParameters params, Vector3Pair ray) {
        Vector3 origin = surface.get_curve().intersect(ray);
        if (origin == null)
            return null;

        Vector2 v = origin.project_xy();
        if (v.len() > surface.get_external_radius())
            return null;

        Vector3 normal = surface.get_curve().normal(origin);
        if (ray.direction().z() < 0)
            normal = normal.negate();

        return new Vector3Pair(origin, normal);
    }

    TracedRay trace_across_stop(Stop surface, RayTraceResults result, TracedRay incident, Vector3Pair local, Vector3Pair intersect) {
        Vector2 v = intersect.origin().project_xy();

        if (surface.get_shape().inside(v)) {
            // re-emit incident ray
            TracedRay r = result.newRay(intersect.origin(), incident.get_ray().direction());

            r.set_wavelen(incident.get_wavelen());
            r.set_intensity(incident.get_intensity());
            r.set_material(incident.get_material());
            r.set_creator(surface);

            incident.add_generated(r);
            return r;
        }
        return null;
    }

    List<TracedRay> process_rays_stop(Stop surface, RayTraceResults result, List<TracedRay> input) {
        List<TracedRay> rays = new ArrayList<>();
        for (TracedRay i : input) {
            TracedRay ray = i;

            Transform3 t = ray.get_creator().get_transform_to(surface);
            Vector3Pair local = t.transform_line(ray.get_ray());

            Vector3 origin = surface.get_curve().intersect(local);
            if (origin != null) {
                if (origin.project_xy().len() < surface.get_external_radius()) {
                    Vector3 normal = surface.get_curve().normal(origin);

                    if (local.direction().z() < 0)
                        normal = normal.negate();

                    result.add_intercepted(surface, ray);

                    TracedRay cray = trace_ray(surface, result, ray, local, new Vector3Pair(origin, normal));
                    if (cray != null)
                        rays.add(cray);
                }
            }
        }
        return rays;
    }
}
