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

package org.redukti.jfotoptix.plotting;

import org.redukti.jfotoptix.data.DataSet;
import org.redukti.jfotoptix.data.Range;
import org.redukti.jfotoptix.rendering.Rgb;

import java.util.ArrayList;
import java.util.List;

/**
 * data plots container
 * <p>
 * This class is used to describe a data plot. It contains a list
 * of PlotData objects and describes some plot properties
 * (title, range, ...).
 * <p>
 * Plots can be built from data sets or obtained directly from
 * various analysis functions.
 */
public class Plot {

    String _title = "";
    List<PlotData> _plots = new ArrayList<>();

    PlotAxes _axes = new PlotAxes();

    boolean _xy_swap = false;

    /**
     * Create and add plot data from specified data set.
     */
    PlotData add_plot_data(DataSet data, Rgb color,
                           String label, int style) {
        PlotData plotdata = new PlotData(data);
        _plots.add(plotdata);

        plotdata.set_color(color);
        plotdata.set_label(label);
        plotdata.set_style(style);

        return plotdata;
    }

    /**
     * Add plot data
     */
    void add_plot_data(PlotData data) {
        _plots.add(data);
    }

    /**
     * Discard all plot data set
     */
    void erase_plot_data() {
        _plots.clear();
    }

    /**
     * Get plot data set count
     */
    int get_plot_count() {
        return _plots.size();
    }

    /**
     * Get plot data set at given index
     */
    PlotData get_plot_data(int index) {
        return _plots.get(index);
    }

    /**
     * Set plot main title
     */
    void set_title(String title) {
        _title = title;
    }

    /**
     * Get plot main title
     */
    String get_title() {
        return _title;
    }

    /**
     * Set color for all plots
     */
    void set_color(Rgb color) {
        for (PlotData i : _plots)
            i.set_color(color);
    }

    /**
     * Automatically choose different colors for each plot
     */
    void set_different_colors() {
        Rgb color;
        int n = 1;

        for (PlotData i : _plots) {
            double r = (double) ((n >> 0) & 0x01);
            double g = (double) ((n >> 1) & 0x01);
            double b = (double) ((n >> 2) & 0x01);

            i.set_color(new Rgb(r, g, b, 1.0f));
            n++;
        }
    }

    /**
     * Set plot style for all plot
     */
    void set_style(int style) {
        for (PlotData i : _plots)
            i.set_style(style);
    }

    /**
     * Swap x and y axis for 2d plots
     */
    void set_xy_swap(boolean doswap) {
        _xy_swap = doswap;
    }

    /**
     * Get x and y axis swap state for 2d plots
     */
    boolean get_xy_swap() {
        return _xy_swap;
    }


    /**
     * Set axis position to dataset range
     */
    void fit_axes_range() {
        switch (get_dimensions()) {
            case 1:
                _axes.set_range(get_x_data_range(0), PlotAxes.AxisMask.X);
                _axes.set_range(get_y_data_range(), PlotAxes.AxisMask.Y);
                break;
            case 2:
                _axes.set_range(get_x_data_range(0), PlotAxes.AxisMask.X);
                _axes.set_range(get_x_data_range(1), PlotAxes.AxisMask.Y);
                _axes.set_range(get_y_data_range(), PlotAxes.AxisMask.Z);
                break;
            default:
                throw new IllegalArgumentException("inconsistent dimensions of data sets in plot");
        }
    }

    /**
     * Get plot axes object
     */
    PlotAxes get_axes() {
        return _axes;
    }

    /**
     * Get data sets dimensions, return 0 if inconsistent
     */
    int get_dimensions() {
        int dimension = 0;

        for (PlotData i : _plots) {
            int d = i.get_set().get_dimensions();

            if (dimension == 0)
                dimension = d;
            else if (dimension != d)
                return 0;
        }

        return dimension;
    }

    /**
     * Get range of x data in sets
     */
    Range get_x_data_range(int dimension) {
        Range r = new Range(Double.MAX_VALUE, Double.MIN_VALUE);

        for (PlotData i : _plots) {
            Range ri = i.get_set().get_x_range(dimension);

            if (ri.first < r.first)
                r.first = ri.first;

            if (ri.second > r.second)
                r.second = ri.second;
        }

        return r;
    }

    /**
     * Get range of y data in sets
     */
    Range get_y_data_range() {
        Range r = new Range(Double.MAX_VALUE, Double.MIN_VALUE);

        for (PlotData i : _plots) {
            Range ri = i.get_set().get_y_range();

            if (ri.first < r.first)
                r.first = ri.first;

            if (ri.second > r.second)
                r.second = ri.second;
        }

        return r;
    }

}
