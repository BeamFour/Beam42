package org.redukti.jfotoptix.tracing;

import org.redukti.jfotoptix.io.Renderer;
import org.redukti.jfotoptix.patterns.Distribution;
import org.redukti.jfotoptix.sys.OpticalSurface;

import java.util.Map;

public class TraceParameters {

    /**
    Specifies physical light propagation algorithm/model */
    public enum PropagationMode
    {
        /** Use classical ray tracing algorithm to propagate light. */
        RayPropagation,
        /** Use Diffraction based light propagation */
        DiffractPropagation,
        /** Used mixed ray tracing/diffraction propagation */
        MixedPropagation
    };


    //std::shared_ptr<Sequence> _sequence;
    Distribution _default_distribution;
    Map<OpticalSurface, Distribution> _s_distribution;
    int _max_bounce;
    Renderer.IntensityMode _intensity_mode;
    boolean _sequential_mode;
    PropagationMode _propagation_mode;
    boolean _unobstructed;
    double _lost_ray_length;


    public double get_lost_ray_length () {
        return 0.0;
    }

    public Distribution get_distribution (OpticalSurface s)
    {
        Distribution d = _s_distribution.get(s);
        if (d == null)
            return _default_distribution;
        else
            return d;
    }

    public boolean get_unobstructed() {
        return _unobstructed;
    }


}
