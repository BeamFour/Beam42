package org.redukti.jfotoptix.tracing;

import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.patterns.Distribution;
import org.redukti.jfotoptix.sys.Element;
import org.redukti.jfotoptix.sys.OpticalSurface;
import org.redukti.jfotoptix.sys.PointSource;

import java.util.*;
import java.util.function.Consumer;

public class RayGenerator {

       List<TracedRay> get_lightrays_(RayTraceParameters parameters,
                                   PointSource source,
                                   Element target,
                                   PointSource.SourceInfinityMode mode) {
        OpticalSurface starget;
        if (!(target instanceof OpticalSurface)) {
            return Collections.emptyList();
        }
        starget = (OpticalSurface) target;

        double rlen = parameters.get_lost_ray_length();
        Distribution d = parameters.get_distribution(starget);
        final List<TracedRay> rays = new ArrayList<>();

        Consumer<Vector3> de = (Vector3 i) -> {
            Vector3 r = starget.get_transform_to(source).transform(
                    i); // pattern point on target surface
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
                    position = new Vector3Pair(starget.get_position(source).minus(Vector3.vector3_001.times(rlen)),
                            Vector3.vector3_001).pl_ln_intersect(new Vector3Pair(r, direction));
                    break;
            }

            for (SpectralLine l : source.spectrum()) {
                // generated rays use source coordinates
                TracedRay ray = new TracedRay(position, direction);
                ray.set_creator(source);
                ray.set_intensity(l.get_intensity()); // FIXME depends on distance from
                // source and pattern density
                ray.set_wavelen(l.get_wavelen());
//                ray.set_material (_mat.operator bool ()
//                        ? _mat.get ()
//                        : get_system ()->get_environment_proxy ().get ());
                rays.add(ray);
            }
        };
        starget.get_pattern(de, d, parameters.get_unobstructed());
        return rays;
    }

    public List<TracedRay> generate_rays_simple(RayTraceParameters parameters, PointSource source, List<Element> targets) {
//        Set<Double> wavelengths = new HashSet<>();
//        for (SpectralLine l : source.spectrum()) {
//            wavelengths.add(l.get_wavelen());
//        }
        List<TracedRay> rays = new ArrayList<>();
        for (Element target : targets) {
            rays.addAll(get_lightrays_(parameters, source, target, source.mode()));
        }
        return rays;
    }
}
