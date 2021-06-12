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

public interface InterpolatableDataSet {
    double get_x_interval(int x);
    double get_x_interval(int x1, int x2);
    int get_interval(double x);
    /** Get x data at index n in data set */
    double get_x_value(int n);
    /** Get y data stored at index n in data set */
    double get_y_value(int n);
    double get_d_value(int n);
    int get_nearest(double x);
    /** Get total number of data stored in data set */
    int get_count();
}
