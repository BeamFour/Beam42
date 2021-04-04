package org.redukti.jfotoptix.tracing;

import org.redukti.jfotoptix.sys.OpticalSystem;

import java.util.List;

public class RayTraceResults {

    static final class RaysAtElement {
            List<TracedRay> _intercepted; // list of rays for each intercepted surfaces
            List<TracedRay> _generated; // list of rays for each generator surfaces
            boolean _save_intercepted_list;
            boolean _save_generated_list;
    }

    List<RaysAtElement> raysByElement;
    TraceParameters parameters;

    public RayTraceResults(OpticalSystem system) {
    }
}
