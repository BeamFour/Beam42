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

package org.redukti.jfotoptix.data;

/**
 Base class for 1d y = f(x) numerical data set
 */
public abstract class Set1d extends DataSet {

    /** Get total number of data stored in data set */
    public abstract int get_count ();

    /** Get x data at index n in data set */
    public abstract  double get_x_value (int n);
    /** Get y data stored at index n in data set */
    public abstract  double get_y_value (int n);

    /** Interpolate y value corresponding to given x value in data set. */
    public abstract  double interpolate (double x);
    /** Interpolate y value corresponding to given x value in data
     set. data may be differentiated several times.
     @param deriv Differentiation count, 0 means y value, 1 means 1st
     derivative...
     */
    public abstract double interpolate (double x, int deriv);

    /** Get minimal and maximal x values on found in data set */
    public abstract Range get_x_range ();

    @Override
    public int get_dimensions ()
    {
        return 1;
    }

    @Override
    public int get_count (int dimension)
    {
        assert (dimension == 0);
        return get_count ();
    }

    @Override
    public double get_x_value (int x, int dimension)
    {
        assert (dimension == 0);
        return get_x_value (x);
    }

    @Override
    public double get_y_value (int x[])
    {
        return get_y_value (x[0]);
    }

    @Override
    public Range get_x_range (int dimension)
    {
        assert (dimension == 0);
        return get_x_range ();
    }

    @Override
    public double interpolate (double x[])
    {
        return interpolate (x[0]);
    }

    @Override
    public double interpolate (double x[], int deriv, int dimension)
    {
        assert (dimension == 0);
        return interpolate (x[0], deriv);
    }
}
