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
            if (e instanceof RaySource) {
                continue;
            }
            rays = process_rays(e, TraceIntensityMode.Simpletrace, result, rays);
            result.get_generated(e).addAll(rays); // Record rays generated
        }
        return result;
    }

    private List<TracedRay> generateSourceRays(RayTraceParameters parameters, RayTraceResults result, List<Element> seq) {
        List<PointSource> sources = seq.stream().filter(e-> e instanceof PointSource)
                .map(e -> (PointSource) e)
                .collect(Collectors.toList());
        if (sources.isEmpty()) {
            return Collections.emptyList();
        }
        Element firstNonSource = seq.stream().filter(e -> !(e instanceof PointSource)).findFirst().orElse(null);
        if (firstNonSource == null) {
            return Collections.emptyList();
        }
        result.add_sources(sources);
        List<Element> elist = List.of(firstNonSource);
        List<TracedRay> rays = new ArrayList<>();
        for (PointSource source: sources) {
            rays.addAll(rayGenerator.generate_rays_simple(result, parameters, source, elist));
            result.get_generated(source).addAll(rays);
        }
        return rays;
    }
}
