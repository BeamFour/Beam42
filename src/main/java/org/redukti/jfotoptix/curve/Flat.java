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

import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;

public class Flat extends Rotational {

    public static Flat flat = new Flat();

    @Override
    public double sagitta(double s) {
        return 0;
    }

    @Override
    public double derivative (double r)
    {
        return 1.0;
    }

    /*

intersection d'un plan defini par :

P(Px, Py, Pz) appartenant au plan
N(Px, Py, Pz) normal au plan

avec une droite AB definie par l'ensemble des points tel que:

A + * t B

on a :

t=(Nz*Pz+Ny*Py+Nx*Px-Az*Nz-Ay*Ny-Ax*Nx)/(Bz*Nz+By*Ny+Bx*Nx)

*/

    @Override
    public Vector3 intersect (Vector3Pair ray)
    {
        double s = ray.direction ().z ();
        if (s == 0)
            return null;
        double a = -ray.origin ().z () / s;
        if (a < 0)
            return null;
        return ray.origin ().plus(ray.direction ().times(a));
    }

    @Override
    public Vector3 normal (Vector3 point)
    {
        return new Vector3 (0, 0, -1);
    }
}
