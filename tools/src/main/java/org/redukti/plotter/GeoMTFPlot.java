package org.redukti.plotter;

import org.redukti.data.DiscreteSet;
import org.redukti.data.Interpolation;
import org.redukti.data.Range;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.analysis.GeometricMTF;
import org.redukti.rayoptics.specs.Field;
import org.redukti.render.plotting.Plot;
import org.redukti.render.plotting.PlotAxes;
import org.redukti.render.plotting.PlotRenderer;
import org.redukti.render.plotting.PlotStyleMask;
import org.redukti.render.rendering.RendererSvg;
import org.redukti.render.rendering.Rgb;

public class GeoMTFPlot {

    public final Field fld;
    public final GeometricMTF mtf;

    public GeoMTFPlot(Field fld, GeometricMTF mtf) {
        this.fld = fld;
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
        plot.set_title("MTF for " + fld + " wvl " + mtf.intercepts.wvl);
        plot.get_axes().set_position(Vector3.vector3_0);
        plot.get_axes().set_range(new Range(0, 100.0), PlotAxes.AxisMask.X);
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
            plot.add_plot_data(set, xy==0 ? Rgb.rgb_black : Rgb.rgb_blue, xy== 0? "Sagittal" : "Tangential", PlotStyleMask.InterpolatePlot.value());
        }
        String x_label = "Spatial Frequency";
        String y_label = "Modulation";
        plot.get_axes ().set_label (x_label, PlotAxes.AxisMask.X);
        plot.get_axes ().set_label (y_label, PlotAxes.AxisMask.Y);
        plot.get_axes().set_unit("cycles/mm",false,false,0, PlotAxes.AxisMask.X);
        //plot.get_axes().set_unit("",true,false,0, PlotAxes.AxisMask.Y);
        RendererSvg r = new RendererSvg(640,640);
        PlotRenderer plotRenderer = new PlotRenderer();
        plotRenderer.draw_plot(r,plot);
        return r.write(new StringBuilder()).toString();
    }

}
