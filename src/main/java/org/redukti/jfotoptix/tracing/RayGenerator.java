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

import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.medium.Air;
import org.redukti.jfotoptix.medium.Medium;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.patterns.Distribution;
import org.redukti.jfotoptix.model.Element;
import org.redukti.jfotoptix.model.OpticalSurface;
import org.redukti.jfotoptix.model.PointSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class RayGenerator {

    List<TracedRay> generate_rays(
            RayTraceResults result,
            RayTraceParameters parameters,
            PointSource source,
            Element target,
            PointSource.SourceInfinityMode mode) {
        if (!(target instanceof OpticalSurface)) {
            return Collections.emptyList();
        }
        final OpticalSurface target_surface = (OpticalSurface) target;
        double rlen = parameters.get_lost_ray_length();
        Distribution d = parameters.get_distribution(target_surface);
        final List<TracedRay> rays = new ArrayList<>();
        Consumer<Vector3> de = (Vector3 v) -> {
            Vector3 r = target_surface.get_transform_to(source).transform(v); // pattern point on target surface
            Vector3 direction;
            Vector3 position;

            switch (mode) {
                case SourceAtFiniteDistance:
                    position = Vector3.vector3_0;
                    direction = r.normalize();
                    break;
                default:
                case SourceAtInfinity:
                    direction = Vector3.vector3_001;
                    position = new Vector3Pair(
                            target_surface.get_position(source).minus(Vector3.vector3_001.times(rlen)),
                            Vector3.vector3_001)
                            .pl_ln_intersect(new Vector3Pair(r, direction));
                    break;
            }
            for (SpectralLine l : source.spectrum()) {
                // generated rays use source coordinates
                TracedRay ray = result.newRay(position, direction);
                ray.set_creator(source);
                ray.set_intensity(l.get_intensity()); // FIXME depends on distance from
                // source and pattern density
                ray.set_wavelen(l.get_wavelen());
                Medium material = source.get_material();
                if (material == null) {
                    material = Air.air; // FIXME centralize as env - original uses env proxy.
                }
                ray.set_material(material);
                rays.add(ray);
            }
        };
        target_surface.get_pattern(de, d, parameters.get_unobstructed());
        return rays;
    }

    public List<TracedRay> generate_rays_simple(RayTraceResults result, RayTraceParameters parameters, PointSource source, List<Element> targets) {
        List<TracedRay> rays = new ArrayList<>();
        for (Element target : targets) {
            rays.addAll(generate_rays(result, parameters, source, target, source.mode()));
        }
        return rays;
    }
}
