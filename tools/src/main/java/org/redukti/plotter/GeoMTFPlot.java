package org.redukti.plotter;

import org.redukti.data.DiscreteSet;
import org.redukti.data.Interpolation;
import org.redukti.data.Range;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.analysis.GeometricMTF;
import org.redukti.render.plotting.Plot;
import org.redukti.render.plotting.PlotAxes;
import org.redukti.render.plotting.PlotRenderer;
import org.redukti.render.plotting.PlotStyleMask;
import org.redukti.render.rendering.RendererSvg;

public class GeoMTFPlot {

    public final GeometricMTF mtf;

    public GeoMTFPlot(GeometricMTF mtf) {
        this.mtf = mtf;
    }

    public String plot() {
        int count = 0;
        for (int i = 0; i < mtf.freq.length; i++) {
            if (mtf.freq[i] > 100.)
                break;
            count++;
        }

        Plot plot = new Plot();
        plot.set_title("MTF");
        plot.get_axes().set_position(Vector3.vector3_0);
        plot.get_axes().set_range(new Range(0, 100.0), PlotAxes.AxisMask.X);
        //plot.get_axes().set_tics_step(25.0,  PlotAxes.AxisMask.X);
        plot.get_axes().set_range(new Range(0, 1.0), PlotAxes.AxisMask.Y);
        for (int xy = 0; xy < 2; xy++) {
            var x_data = mtf.freq;
            var y_data = xy == 0 ? mtf.mag_x : mtf.mag_y;
            var set = new DiscreteSet();
            set.set_interpolation(Interpolation.Linear);
            for (int i = 0; i < count; i++) {
                double x = x_data[i];
                double y = y_data[i];
                set.add_data(x, y);
            }
            plot.add_plot_data(set, Colors.get_wavelen_color(mtf.intercepts.wvl), "label", PlotStyleMask.InterpolatePlot.value());
        }
        String x_label = "freq";
        String y_label = "mtf";
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
