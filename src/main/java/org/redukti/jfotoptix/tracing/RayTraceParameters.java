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
import org.redukti.jfotoptix.model.Container;
import org.redukti.jfotoptix.model.Element;
import org.redukti.jfotoptix.model.OpticalSurface;
import org.redukti.jfotoptix.model.OpticalSystem;

import java.util.ArrayList;
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
    RayTracer.TraceIntensityMode _intensity_mode;
    boolean _sequential_mode;
    PropagationMode _propagation_mode;
    boolean _unobstructed;
    double _lost_ray_length;

    public RayTraceParameters(OpticalSystem system) {
        this._sequence = new ArrayList<>();
        this._sequential_mode = true;
        this._intensity_mode = RayTracer.TraceIntensityMode.Simpletrace;
        this._propagation_mode = PropagationMode.RayPropagation;
        this._max_bounce = 50;
        this._unobstructed = false;
        this._lost_ray_length = 1000;
        for (Element e: system.elements()) {
            add(e);
        }
        _sequence.sort((a,b) -> {
            double z1 = a.get_position().z();
            double z2 = b.get_position().z();
            if (z1 > z2)
                return 1;
            else if (z1 < z2)
                return -1;
            else
                return 0;
        });
        this._default_distribution = new Distribution(Pattern.MeridionalDist, 10, 0.999);
    }

    private void add(Element e) {
        if (e instanceof Container) {
            Container c = (Container) e;
            for (Element e1: c.elements()) {
                add(e1);
            }
        }
        else
            _sequence.add(e);
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
