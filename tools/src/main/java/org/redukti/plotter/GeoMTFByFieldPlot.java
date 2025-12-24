package org.redukti.plotter;

import org.redukti.data.DiscreteSet;
import org.redukti.data.Interpolation;
import org.redukti.data.Range;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.analysis.MTFResultByFreq;
import org.redukti.render.plotting.Plot;
import org.redukti.render.plotting.PlotAxes;
import org.redukti.render.plotting.PlotRenderer;
import org.redukti.render.plotting.PlotStyleMask;
import org.redukti.render.rendering.RendererSvg;
import org.redukti.render.rendering.Rgb;

import java.util.List;

public class GeoMTFByFieldPlot {

    List<MTFResultByFreq> mtfs_by_freq;

    public GeoMTFByFieldPlot(List<MTFResultByFreq> mtfs_by_freq) {
        this.mtfs_by_freq = mtfs_by_freq;
    }
    public String plot(double[] fields) {
        Plot plot = new Plot();
        plot.set_title("MTF");
        plot.get_axes().set_position(Vector3.vector3_0);
        plot.get_axes().set_range(new Range(0, 1.0), PlotAxes.AxisMask.X);
        plot.get_axes().set_range(new Range(0, 1.0), PlotAxes.AxisMask.Y);
        double[] x_data = fields.clone();
        // for each freq
        for (var i = 0; i < mtfs_by_freq.size(); i++) {
            var mtf = mtfs_by_freq.get(i);
            for (int xy = 0; xy < 2; xy++) {
                var set = new DiscreteSet();
                set.set_interpolation(Interpolation.Cubic);
                double[] mtf_data = (xy == 0) ? mtf.sag_mtf_by_field : mtf.tan_mtf_by_field;
                for (int j = 0; j < mtf_data.length; j++) {
                    double x = x_data[j];
                    set.add_data(x, mtf_data[j]);
                }
                plot.add_plot_data(set, xy == 0 ? Rgb.rgb_black : Rgb.rgb_blue, xy == 0 ? "Sagittal" : "Tangential", PlotStyleMask.InterpolatePlot.value());
            }
        }
        String x_label = "Fields";
        String y_label = "MTF";
        plot.get_axes ().set_label (x_label, PlotAxes.AxisMask.X);
        plot.get_axes ().set_label (y_label, PlotAxes.AxisMask.Y);
        //plot.get_axes().set_unit("cycles/mm",false,false,0, PlotAxes.AxisMask.X);
        //plot.get_axes().set_unit("",true,false,0, PlotAxes.AxisMask.Y);
        RendererSvg r = new RendererSvg(1024,640);
        PlotRenderer plotRenderer = new PlotRenderer();
        plotRenderer.draw_plot(r,plot);
        return r.write(new StringBuilder()).toString();
    }
}
