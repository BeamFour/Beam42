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

import org.redukti.jfotoptix.patterns.Distribution;
import org.redukti.jfotoptix.patterns.Pattern;
import org.redukti.jfotoptix.model.Element;
import org.redukti.jfotoptix.model.OpticalSurface;
import org.redukti.jfotoptix.model.OpticalSystem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RayTraceParameters {

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

    List<Element> _sequence;
    Distribution _default_distribution;
    Map<OpticalSurface, Distribution> _s_distribution = new HashMap<>();
    int _max_bounce;
    boolean _sequential_mode;
    PropagationMode _propagation_mode;
    boolean _unobstructed;
    double _lost_ray_length;

    public RayTraceParameters(OpticalSystem system) {
        _sequential_mode = true;
        _propagation_mode = PropagationMode.RayPropagation;
        _max_bounce = 50;
        _unobstructed = false;
        _lost_ray_length = 1000;
        _sequence = system.get_sequence();
        _default_distribution = new Distribution(Pattern.MeridionalDist, 10, 0.999);
    }

    public double get_lost_ray_length () {
        return _lost_ray_length;
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

    public void set_default_distribution(Distribution distribution) {
        _default_distribution = distribution;
    }
    public Distribution get_default_distribution() { return _default_distribution; }

    public StringBuilder sequenceToString(StringBuilder sb) {
        for (Element e: _sequence) {
            sb.append(e.toString()).append(System.lineSeparator());
        }
        return sb;
    }

    public List<Element> get_sequence() {
        return _sequence;
    }
}
