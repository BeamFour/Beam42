package org.redukti.jfotoptix.tracing;

import org.redukti.jfotoptix.model.Element;
import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.model.PointSource;
import org.redukti.jfotoptix.model.RaySource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SequentialRayTracer extends RayTracer {

    RayGenerator rayGenerator = new RayGenerator();

    public RayTraceResults trace(OpticalSystem system, RayTraceParameters parameters) {
        RayTraceResults result = new RayTraceResults(parameters);
        List<Element> seq = parameters.get_sequence();
        List<TracedRay> rays = generateSourceRays(parameters, result, seq);
        seq = seq.stream().filter(e -> !(e instanceof RaySource)).collect(Collectors.toList());
        for (Element e: seq) {
            rays = process_rays(e, result, rays);
            result.get_generated(e).addAll(rays); // Record rays generated
        }
        return result;
    }

    private List<TracedRay> generateSourceRays(RayTraceParameters parameters, RayTraceResults result, List<Element> seq) {
        // Generate rays from all point sources
        // We assume that point sources are all to the left of the first surface
        // Note also that the generated rays will only target the first surface
        // following the point sources
        // FIXME the implementation needs to be made more generic
        List<PointSource> sources = seq.stream().filter(e-> e instanceof PointSource)
                .map(e -> (PointSource) e)
                .collect(Collectors.toList());
        if (sources.isEmpty()) {
            return Collections.emptyList();
        }
        // Find the first surface that will be the target
        Element firstNonSource = seq.stream().filter(e -> !(e instanceof PointSource)).findFirst().orElse(null);
        if (firstNonSource == null) {
            return Collections.emptyList();
        }
        result.add_sources(sources);
        List<Element> elist = List.of(firstNonSource);
        List<TracedRay> rays = new ArrayList<>(); // All generated rays
        for (PointSource source: sources) {
            List<TracedRay> generated = rayGenerator.generate_rays_simple(result, parameters, source, elist);
            rays.addAll(generated);
            result.get_generated(source).addAll(generated); // Track rays by the instance that generated them
        }
        return rays;
    }
}
