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


package org.redukti.jfotoptix.patterns;

import static org.redukti.jfotoptix.patterns.Pattern.DefaultDist;

/**
 Ray distribution pattern descriptor

 This class describes distribution pattern and ray density used
 for light ray distribution over surfaces during light
 propagation.

 Ray density is expressed as average number of rays along
 surface radius.
 */
public class Distribution {

    Pattern _pattern;
    int _radial_density;
    double _scaling;

    /** Creates a distribution pattern with specified pattern,
     radial ray density and scaling.

     The scaling ratio parameter may be used to avoid
     distributing rays too close to the surface edge. */
    public Distribution (Pattern pattern,
                         int radial_density,
                         double scaling)
    {
        if (radial_density < 1)
            throw new IllegalArgumentException ("ray distribution radial density must be greater than 1");
        this._pattern = pattern;
        this._radial_density = radial_density;
        this._scaling = scaling;
    }

    public Distribution() {
        _pattern = DefaultDist;
        _radial_density = 5;
        _scaling = 0.999;
    }

    void set_pattern (Pattern p)
    {
        _pattern = p;
    }

    public Pattern get_pattern ()
    {
        return _pattern;
    }

    public int get_radial_density ()
    {
        return _radial_density;
    }

    void set_radial_density (int density)
    {
        _radial_density = density;
    }

    public double get_scaling ()
    {
        return _scaling;
    }

    void set_scaling (double margin)
    {
        _scaling = margin;
    }

    public void set_uniform_pattern ()
    {
        switch (_pattern)
        {
            case SagittalDist:
            case MeridionalDist:
            case CrossDist:
                _pattern = DefaultDist;
            default:;
        }
    }

}
