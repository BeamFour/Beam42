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


package org.redukti.jfotoptix.material;

public class Vacuum extends Medium {

    public Vacuum() {
        super("Vacuum");
    }

    public boolean is_opaque() {
        return false;
    }

    public boolean is_reflecting() {
        return false;
    }

    public double get_internal_transmittance(double wavelen, double thickness) {
        return 1.0;
    }

    public double get_extinction_coef(double wavelen) {
        return 0.0;
    }

    public double get_refractive_index(double wavelen) {
        return 1.0;
    }
}
