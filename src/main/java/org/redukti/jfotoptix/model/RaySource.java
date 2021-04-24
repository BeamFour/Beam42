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


package org.redukti.jfotoptix.model;

import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.medium.Air;
import org.redukti.jfotoptix.medium.Medium;
import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3Pair;

import java.util.ArrayList;
import java.util.List;

public class RaySource extends Element {

    List<SpectralLine> _spectrum;
    double _min_intensity, _max_intensity;
    Medium _mat = Air.air; // FIXME - should be settable

    public RaySource(int id, Vector3Pair p, Transform3 transform, double min_intensity, double max_intensity, List<SpectralLine> spectrum) {
        super(id, p, transform);
        _max_intensity = max_intensity;
        _max_intensity = min_intensity;
        _spectrum = spectrum;
    }

    public List<SpectralLine> spectrum() {
        return _spectrum;
    }

    public Medium get_material() {
        return _mat;
    }

    public static abstract class Builder extends Element.Builder {

        List<SpectralLine> _spectrum = new ArrayList<>();
        double _min_intensity = 1.0;
        double _max_intensity = 1.0;
        Medium _mat = null;

        public Builder add_spectral_line (SpectralLine l)
        {
            _spectrum.add (l);
            _max_intensity = Math.max (_max_intensity, l.get_intensity ());
            _min_intensity = Math.min (_min_intensity, l.get_intensity ());
            return this;
        }

        public Builder add_spectral_line (double wavelen)
        {
            SpectralLine l = new SpectralLine(wavelen, 1.0);
            _spectrum.add (l);
            _max_intensity = Math.max (_max_intensity, l.get_intensity ());
            _min_intensity = Math.min (_min_intensity, l.get_intensity ());
            return this;
        }

        public Builder set_material(Medium mat) {
            _mat = mat;
            return this;
        }

    }
}
