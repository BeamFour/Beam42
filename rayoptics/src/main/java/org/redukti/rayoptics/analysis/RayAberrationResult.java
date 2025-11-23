package org.redukti.rayoptics.analysis;

import org.redukti.data.DiscreteSet;
import org.redukti.data.Interpolation;
import org.redukti.data.Range;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.raytr.RayFanType;
import org.redukti.rayoptics.raytr.TraceFanResult;
import org.redukti.render.plotting.Plot;
import org.redukti.render.plotting.PlotAxes;
import org.redukti.render.plotting.PlotRenderer;
import org.redukti.render.plotting.PlotStyleMask;
import org.redukti.render.rendering.RendererSvg;
import org.redukti.render.rendering.Rgb;

import java.util.ArrayList;
import java.util.List;

public class RayAberrationResult {

    public List<TraceFanResult> results = new ArrayList<TraceFanResult>();

    public void add(TraceFanResult fan_result){
        results.add(fan_result);
    }

    public String plot(TraceFanResult fan_result,double yscale) {
        Plot plot = new Plot();
        plot.set_title(fan_result.type.toString() + " " + fan_result.fld.toString());
        plot.get_axes().set_position(Vector3.vector3_0);
        plot.get_axes().set_range(new Range(-1.0, 1.0), PlotAxes.AxisMask.X);
        plot.get_axes().set_tics_step(1.0,  PlotAxes.AxisMask.X);
        plot.get_axes().set_range(new Range(-yscale, yscale), PlotAxes.AxisMask.Y);
        for (var fan: fan_result.fans) {
            var x_data = fan.fan_x;
            var y_data = fan.fan_y;
            var set = new DiscreteSet();
            set.set_interpolation(Interpolation.Cubic);
            for (int i = 0; i < x_data.size(); i++) {
                double x = x_data.get(i);
                double y = y_data.get(i);
                set.add_data(x,y);
            }
            plot.add_plot_data(set, Analysis.get_wavelen_color(fan.wvl),"label", PlotStyleMask.InterpolatePlot.value());
        }
        String x_label;
        String y_label;
        if (fan_result.type == RayFanType.TransverseRayFan) {
            if (fan_result.xy == 1) {
                x_label = "Py";
                y_label = "eY";
            }
            else {
                x_label = "Px";
                y_label = "eX";
            }
        }
        else {
            y_label = "W";
            if (fan_result.xy == 1) {
                x_label = "Py";
            }
            else {
                x_label = "Px";
            }
        }
        plot.get_axes ().set_label (x_label, PlotAxes.AxisMask.X);
        plot.get_axes ().set_label (y_label, PlotAxes.AxisMask.Y);
        //plot.get_axes().set_unit("",false,false,0, PlotAxes.AxisMask.X);
        //plot.get_axes().set_unit("",false,false,0, PlotAxes.AxisMask.Y);
        RendererSvg r = new RendererSvg(640,640);
        PlotRenderer plotRenderer = new PlotRenderer();
        plotRenderer.draw_plot(r,plot);
        return r.write(new StringBuilder()).toString();
    }

}
