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


package org.redukti.jfotoptix.analysis;

import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.math.MathUtils;
import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.math.Vector2Pair;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.model.Surface;
import org.redukti.jfotoptix.plotting.PlotAxes;
import org.redukti.jfotoptix.plotting.PlotRenderer;
import org.redukti.jfotoptix.rendering.Renderer;
import org.redukti.jfotoptix.rendering.RendererViewport;
import org.redukti.jfotoptix.tracing.TracedRay;

/**
 Spot diagram analysis

 This class is designed to plot spot diagram and perform
 related analysis.
 */
public class AnalysisSpot extends AnalysisPointImage {

    /** spot centroid */
    protected Vector3 _centroid;

    protected boolean _processed_analysis;
    /** spot maximum radius */
    protected double _max_radius;
    /** spot root mean square radius */
    protected double _rms_radius;
    /** amount of light intensity in the whole spot */
    protected double _tot_intensity;
    protected double _useful_radius;

    protected PlotAxes _axes;

    public AnalysisSpot(OpticalSystem system, int radial_density) {
        super(system, radial_density);
        _axes = new PlotAxes();
        _axes.set_show_axes (false, PlotAxes.AxisMask.XY);
        _axes.set_label ("Sagittal distance", PlotAxes.AxisMask.X);
        _axes.set_label ("Tangential distance", PlotAxes.AxisMask.Y);
        _axes.set_unit ("m", true, true, -3, PlotAxes.AxisMask.XY);
    }

    void process_trace ()
    {
        if (_processed_trace)
            return;
        trace ();
        _centroid = _results.get_intercepted_centroid (_image);
    }

    void process_analysis ()
    {
        if (_processed_analysis)
            return;

        process_trace ();

        double mean = 0;      // rms radius
        double max = 0;       // max radius
        double intensity = 0; // total intensity

        for (TracedRay i : _intercepts)
        {
            double dist = (i.get_intercept_point ().minus(_centroid)).len ();

            if (max < dist)
                max = dist;

            mean += MathUtils.square (dist);
            intensity += i.get_intensity ();
        }

        _useful_radius = _max_radius = max;
        _rms_radius = Math.sqrt (mean / _intercepts.size ());
        _tot_intensity = intensity;

        _processed_analysis = true;
    }

    void draw_intercepts (RendererViewport renderer, Surface s)
    {
//        double max_intensity = _results.get_max_ray_intensity ();
//        renderer.set_max_intensity(max_intensity);
        for (TracedRay ray : _results.get_intercepted (s))
        {
            // dont need global transform here, draw ray intercept points in
            // surface local coordinates.
            renderer.draw_point (ray.get_intercept_point ().project_xy (), SpectralLine.get_wavelen_color(ray.get_wavelen()), Renderer.PointStyle.PointStyleDot);
        }
    }

    public void draw_diagram (RendererViewport renderer, boolean centroid_origin)
    {
        process_analysis ();

        Vector3 center3 = _results.get_intercepted_center (_image);
        Vector2 center = new Vector2(center3.x(), center3.y());
        Vector2 radius = new Vector2 (_useful_radius, _useful_radius);

        renderer.set_window (new Vector2Pair(center.minus(radius), center.plus(radius)), true);

        _axes.set_position (_centroid);
        _axes.set_origin (centroid_origin ? _centroid : Vector3.vector3_0);
        _axes.set_tics_count (3, PlotAxes.AxisMask.XY);

        PlotRenderer plotRenderer = new PlotRenderer();
        plotRenderer.draw_axes_2d (renderer, _axes);
        draw_intercepts (renderer, _image);
    }

    @Override
    public String toString() {
        // Convert to mm (diff is um but squared?)
        return "Spot Result: RMS Radius " + _rms_radius * 1000.0 + " max radius " + _max_radius * 1000.0;
    }
}
