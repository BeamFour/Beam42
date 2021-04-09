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
package org.redukti.jfotoptix.curve;

public abstract class ConicBase extends RotationalRoc {

    double _sh; // Schwarzschild constant + 1

    public ConicBase (double roc, double sc) {
        super (roc);
        _sh = sc + 1;
    }

    public double get_eccentricity ()
    {
        return Math.sqrt (-_sh + 1.0);
    }

    public double get_schwarzschild ()
    {
        return _sh - 1.0;
    }
}
