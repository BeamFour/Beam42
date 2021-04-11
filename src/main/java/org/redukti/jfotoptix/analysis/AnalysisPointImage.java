package org.redukti.jfotoptix.analysis;

import org.redukti.jfotoptix.patterns.Distribution;
import org.redukti.jfotoptix.patterns.Pattern;
import org.redukti.jfotoptix.sys.Image;
import org.redukti.jfotoptix.sys.OpticalSystem;
import org.redukti.jfotoptix.tracing.RayTraceParameters;
import org.redukti.jfotoptix.tracing.RayTraceResults;
import org.redukti.jfotoptix.tracing.RayTracer;
import org.redukti.jfotoptix.tracing.TracedRay;

import java.util.ArrayList;
import java.util.List;

public class AnalysisPointImage {
    OpticalSystem _system;
    RayTracer _tracer;
    RayTraceParameters _params;
    boolean _processed_trace;
    Image _image;
    List<TracedRay> _intercepts;
    RayTraceResults _results;

    public AnalysisPointImage (OpticalSystem system)
    {
        _system = system;
        _tracer = new RayTracer();
        _processed_trace = false;
        _image = null;
        _intercepts = new ArrayList<>();
        _params = new RayTraceParameters(system);
        _params.set_default_distribution (
                new Distribution (Pattern.HexaPolarDist, 20, 0.999));
        _params.get_default_distribution ().set_uniform_pattern ();
    }

    public void trace() {
        _image = (Image) _params.get_sequence().stream().filter(e-> e instanceof Image).findFirst().get();
        _results = _tracer.trace(_system, _params);
        _intercepts = _results.get_intercepted(_image);
        return;
    }
}
