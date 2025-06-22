package org.redukti.jfotoptix.analysis;

import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.patterns.Distribution;
import org.redukti.jfotoptix.tracing.RayTraceParameters;
import org.redukti.jfotoptix.tracing.RayTraceResults;
import org.redukti.jfotoptix.tracing.SequentialRayTracer;

public class AnalysisRayFinder {

    public final OpticalSystem opticalSystem;
    public final Distribution distribution;
    public final RayTraceParameters parameters;
    public RayTraceResults results;

    public AnalysisRayFinder(OpticalSystem system, Distribution distribution) {
        this.opticalSystem = system;
        this.distribution = distribution;
        this.parameters = new RayTraceParameters(system);
        this.parameters.set_default_distribution(distribution);
    }

    public RayTraceResults compute() {
        SequentialRayTracer rayTracer = new SequentialRayTracer();
        results = rayTracer.trace(opticalSystem, parameters);
        //RayTraceRenderer.draw_2d(renderer, result, false, null);
        //System.out.println(renderer.write(new StringBuilder()).toString());

        return results;
    }
}
