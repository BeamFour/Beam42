package org.redukti.jfotoptix.tracing;

import org.redukti.jfotoptix.sys.Element;
import org.redukti.jfotoptix.sys.OpticalSystem;
import org.redukti.jfotoptix.sys.RaySource;
import org.redukti.jfotoptix.sys.Surface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RayTraceResults {

    static final class RaysAtElement {
            List<TracedRay> _intercepted = new ArrayList<>(); // list of rays for each intercepted surfaces
            List<TracedRay> _generated = new ArrayList<>(); // list of rays for each generator surfaces
            boolean _save_intercepted_list = false;
            boolean _save_generated_list = false;
    }

    Map<Integer, RaysAtElement> raysByElement = new HashMap<>();
    RayTraceParameters parameters;
    List<RaySource> _sources = new ArrayList<>();

    public RayTraceResults(OpticalSystem system) {
    }

    RaysAtElement get_element_result (Element e)
    {
        RaysAtElement re = raysByElement.get(e.id());
        if (re == null) {
            re = new RaysAtElement();
            raysByElement.put(e.id(), re);
        }
        return re;
    }

    public void add_intercepted (Surface s, TracedRay ray)
    {
        RaysAtElement er = get_element_result(s);
        er._intercepted.add(ray);
    }

    public void add_source(RaySource source) {
        _sources.add(source);
    }
}
