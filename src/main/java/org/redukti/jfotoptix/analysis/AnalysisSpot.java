package org.redukti.jfotoptix.analysis;

import org.redukti.jfotoptix.math.MathUtils;
import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.math.Vector2Pair;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.plotting.PlotAxes;
import org.redukti.jfotoptix.plotting.PlotRenderer;
import org.redukti.jfotoptix.rendering.Renderer;
import org.redukti.jfotoptix.rendering.RendererViewport;
import org.redukti.jfotoptix.sys.OpticalSystem;
import org.redukti.jfotoptix.sys.Surface;
import org.redukti.jfotoptix.tracing.RayTraceRenderer;
import org.redukti.jfotoptix.tracing.TracedRay;

public class AnalysisSpot extends AnalysisPointImage {

    Vector3 _centroid;

    boolean _processed_analysis;
    double _max_radius;
    double _rms_radius;
    double _tot_intensity;
    double _useful_radius;

    PlotAxes _axes;

    public AnalysisSpot(OpticalSystem system) {
        super(system);
        _axes = new PlotAxes();
        _axes.set_show_axes (false, PlotAxes.AxisMask.XY);
        _axes.set_label ("Saggital distance", PlotAxes.AxisMask.X);
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
        double _max_intensity = _results.get_max_ray_intensity ();

        for (TracedRay ray : _results.get_intercepted (s))
        {
            // dont need global transform here, draw ray intercept points in
            // surface local coordinates.
            renderer.draw_point (ray.get_intercept_point ().project_xy (), RayTraceRenderer.ray_to_rgb (ray), Renderer.PointStyle.PointStyleDot);
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
}
