package org.redukti.jfotoptix.tracing;

import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.sys.Element;
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
            boolean _save_intercepted_list = true;
            boolean _save_generated_list = true;
    }

    Map<Integer, RaysAtElement> raysByElement = new HashMap<>();
    RayTraceParameters _parameters;
    List<RaySource> _sources = new ArrayList<>();
    List<TracedRay> _rays = new ArrayList<>();

    public RayTraceResults(RayTraceParameters parameters) {
        this._parameters = parameters;
    }

    public List<TracedRay> get_generated(Element e) {
        RaysAtElement er = get_element_result (e);
        if (er == null) {
            throw new IllegalArgumentException("No traced rays at element " + e);
        }
        return er._generated;
    }

    public List<RaySource> get_source_list() {
        return _sources;
    }

    public RayTraceParameters get_params() {
        return _parameters;
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

    public TracedRay newRay(Vector3 origin, Vector3 direction) {
        TracedRay ray = new TracedRay(origin, direction);
        _rays.add(ray);
        return ray;
    }

    public double get_max_ray_intensity ()
    {
        double res = 0;

        for (TracedRay r : _rays)
        {
            double i = r.get_intensity ();

            if (i > res)
                res = i;
        }
        return res;
    }

}
