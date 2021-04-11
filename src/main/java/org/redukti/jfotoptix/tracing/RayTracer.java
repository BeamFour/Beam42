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

import org.redukti.jfotoptix.material.MaterialBase;
import org.redukti.jfotoptix.math.*;
import org.redukti.jfotoptix.sys.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RayTracer {

    /**
     * Specifies light intensity calculation mode to use by light propagation
     * algorithms.
     */
    enum TraceIntensityMode {
        /**
         * No Intensity calculation is performed
         */
        Simpletrace,
        /**
         * light intensity computation is performed without taking polarization into
         * account
         */
        Intensitytrace,
        /**
         * @experimental light intensity with polarization computation is performed
         * (not supported yet)
         */
        Polarizedtrace
    }

    public RayTraceResults trace(OpticalSystem system, RayTraceParameters parameters) {
        RayTraceResults result = new RayTraceResults(parameters);
        switch (parameters._intensity_mode) {
            case Simpletrace:
                if (!parameters._sequential_mode)
                    //trace_template<Simpletrace> ();
                    throw new UnsupportedOperationException();
                else
                    trace_sequential(parameters._intensity_mode, parameters, result);
                break;

            case Intensitytrace:
//                if (!parameters._sequential_mode)
//                    trace_template<Intensitytrace> ();
//                else
//                    trace_seq_template<Intensitytrace> ();
                throw new UnsupportedOperationException();
                //break;

            case Polarizedtrace:
//                if (!parameters._sequential_mode)
//                    trace_template<Polarizedtrace> ();
//                else
//                    trace_seq_template<Polarizedtrace> ();
                throw new UnsupportedOperationException();
                //break;
        }
        return result;
    }

    static final class RayCollection {
        List<TracedRay> rays = new ArrayList<>();
    }

    void trace_sequential(RayTracer.TraceIntensityMode m, RayTraceParameters parameters, RayTraceResults result) {
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

//            if (!element->is_enabled ())
//                continue;

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
                List<TracedRay> rays = process_rays(element, m, result, source_rays);
                // swap ray buffers
                generated.addAll(rays);
            }
            source_rays = generated;
            swaped ^= 1;
        }
    }

    List<TracedRay> process_rays(Element e, TraceIntensityMode m, RayTraceResults result, List<TracedRay> input) {
        switch (m) {
            case Simpletrace:
                return process_rays_simple(e, result, input);
            case Intensitytrace:
                return process_rays_intensity(e, result, input);
            case Polarizedtrace:
                return process_rays_polarized(e, result, input);
        }
        return Collections.emptyList();
    }

    private List<TracedRay> process_rays_polarized(Element e, RayTraceResults result, List<TracedRay> input) {
        throw new UnsupportedOperationException();
    }

    private List<TracedRay> process_rays_intensity(Element e, RayTraceResults result, List<TracedRay> input) {
        throw new UnsupportedOperationException();
    }

    private List<TracedRay> process_rays_simple(Element e, RayTraceResults result, List<TracedRay> input) {
        if (e instanceof Stop) {
            Stop surface = (Stop) e;
            return process_rays(surface, TraceIntensityMode.Simpletrace, result, input);
        } else if (e instanceof Surface) {
            Surface surface = (Surface) e;
            return process_rays(surface, TraceIntensityMode.Simpletrace, result, input);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    List<TracedRay> process_rays(Surface surface, TraceIntensityMode m, RayTraceResults result,
                                 List<TracedRay> input) {
        RayTraceParameters params = result._parameters;
        List<TracedRay> rays = new ArrayList<>();
        for (TracedRay i : input) {
            TracedRay ray = i;

            Transform3 t
                    = ray.get_creator().get_transform_to(surface);
            Vector3Pair local = t.transform_line(ray.get_ray());

            Vector3Pair pt = intersect(surface, params, local);
            if (pt != null) {
                result.add_intercepted(surface, ray);
                TracedRay cray = trace_ray(surface, m, result, ray, local, pt);
                if (cray != null)
                    rays.add(cray);
            }
        }
        return rays;
    }

    TracedRay trace_ray(Surface surface, TraceIntensityMode m,
                        RayTraceResults result, TracedRay incident,
                        Vector3Pair local,
                        Vector3Pair pt) {
        incident.set_len((pt.origin().minus(local.origin())).len());
        incident.set_intercept(surface, pt.origin());

        if (m == TraceIntensityMode.Simpletrace) {
            incident.set_intercept_intensity(1.0);
            return trace_ray_simple(surface, result, incident, local, pt);
        } else {
            // apply absorbtion from current material
            double i_intensity
                    = incident.get_intensity()
                    * incident.get_material().get_internal_transmittance(
                    incident.get_wavelen(), incident.get_len());

            incident.set_intercept_intensity(i_intensity);

            // FIXME
//            if (i_intensity < _discard_intensity)
//                return;

            if (m == TraceIntensityMode.Intensitytrace)
                return trace_ray_intensity(surface, result, incident, local, pt);
            else if (m == TraceIntensityMode.Polarizedtrace)
                return trace_ray_polarized(surface, result, incident, local, pt);
            else
                throw new UnsupportedOperationException();
        }
    }

    private TracedRay trace_ray_simple(Surface surface, RayTraceResults result, TracedRay incident, Vector3Pair local, Vector3Pair pt) {
        if (surface instanceof OpticalSurface) {
            return trace_ray_simple((OpticalSurface) surface, result, incident, local, pt);
        } else if (surface instanceof Stop) {
            return trace_ray_simple((Stop) surface, result, incident, local, pt);
        } else {
            return null;
        }
    }


    private TracedRay trace_ray_polarized(Surface surface, RayTraceResults result, TracedRay incident, Vector3Pair local, Vector3Pair pt) {
        throw new UnsupportedOperationException();
    }

    private TracedRay trace_ray_intensity(Surface surface, RayTraceResults result, TracedRay incident, Vector3Pair local, Vector3Pair pt) {
        throw new UnsupportedOperationException();
    }

    private TracedRay trace_ray_simple(OpticalSurface surface, RayTraceResults result, TracedRay incident, Vector3Pair local, Vector3Pair intersect) {
        boolean right_to_left = intersect.normal().z() > 0;
        MaterialBase prev_mat = surface.get_material(right_to_left ? 1 : 0);
        MaterialBase next_mat = surface.get_material(!right_to_left ? 1 : 0);

        // check ray didn't "escaped" from its material
        // std::cout << prev_mat->name << " " << next_mat->name <<
        //          " " << incident.get_material()->name << std::endl;

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

        Vector3 dir = ray.direction().times(refract_index).minus(
                normal.times(refract_index * cosi + Math.sqrt(1.0 - sint2)));

        // This uses Feder refractive formula
        Vector3 dir2 = compute_refraction(surface, ray, normal, refract_index);
//        if ((fabs (dir.x () - dir2.x ()) > 1e-14
//                || fabs (dir.y () - dir2.y ()) > 1e-14
//                || fabs (dir.z () - dir2.z ()) > 1e-14))
//        {
//            printf ("Refract Orig  %.16f, %.16f, %.16f\n", dir.x (), dir.y (),
//                    dir.z ());
//            printf ("Refract Feder %.16f, %.16f, %.16f\n", dir2.x (), dir2.y (),
//                    dir2.z ());
//        }

        dir = dir2;
        return dir;
    }

    Vector3 reflect(OpticalSurface surface, Vector3Pair ray, Vector3 normal) {
        // Algorithm from Bram de Greve article "Reflections & Refractions in
        // Raytracing" http://www.bramz.org/

        assert (Math.abs(normal.len() - 1.0) < 1e-10);
        assert (Math.abs((ray.direction().len()) - 1.0) < 1e-10);

        double cosi = normal.dot(ray.direction());

        return ray.direction().minus(normal.times(2.0 * cosi));
    }

    private Vector3Pair intersect(Surface surface, RayTraceParameters params, Vector3Pair ray) {
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

    private Vector3Pair intersect(Stop surface, RayTraceParameters params, Vector3Pair ray) {
        Vector3 origin = surface.get_curve().intersect(ray);
        if (origin == null)
            return null;

        Vector2 v = origin.project_xy();

        if (v.len() > surface.get_external_radius())
            return null;

        boolean ir = true; // FIXME _intercept_reemit || params.is_sequential ();

        if (!ir && surface.get_shape().inside(v))
            return null;

        Vector3 normal = surface.get_curve().normal(origin);
        if (ray.direction().z() < 0)
            normal = normal.negate();

        return new Vector3Pair(origin, normal);
    }

    TracedRay trace_ray_simple(Stop surface, RayTraceResults result, TracedRay incident, Vector3Pair local, Vector3Pair intersect) {
        Vector2 v = intersect.origin().project_xy();

        boolean ir = true; // FIXME  _intercept_reemit || result.get_params ().is_sequential ();

        if (ir && surface.get_shape().inside(v)) {
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

    List<TracedRay> process_rays(Stop surface, TraceIntensityMode m, RayTraceResults result, List<TracedRay> input) {
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

                    TracedRay cray = trace_ray(surface, m, result, ray, local, new Vector3Pair(origin, normal));
                    if (cray != null)
                        rays.add(cray);
                }
            }
        }
        return rays;
    }
}
