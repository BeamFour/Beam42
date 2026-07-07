package org.redukti.plotter;

import org.redukti.data.DiscreteSet;
import org.redukti.data.Interpolation;
import org.redukti.data.Range;
import org.redukti.mathlib.M;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.analysis.MTFResultByFreq;
import org.redukti.render.plotting.Plot;
import org.redukti.render.plotting.PlotAxes;
import org.redukti.render.plotting.PlotData;
import org.redukti.render.plotting.PlotRenderer;
import org.redukti.render.plotting.PlotStyleMask;
import org.redukti.render.rendering.RendererSvg;
import org.redukti.render.rendering.Rgb;

import java.text.DecimalFormat;
import java.util.List;

public class GeoMTFByFieldPlot {

    List<MTFResultByFreq> mtfs_by_freq;
    double[] fields;

    static final DecimalFormat df = M.decimal_format();

    public GeoMTFByFieldPlot(List<MTFResultByFreq> mtfs_by_freq,double[] fields) {
        this.mtfs_by_freq = mtfs_by_freq;
        this.fields = fields.clone();
    }
    // Distinct colors cycled per frequency (10, 30, 50, ...)
    static final Rgb[] FREQ_COLORS = {
            Rgb.rgb_red,
            Rgb.rgb_blue,
            Rgb.rgb_black,
            Rgb.rgb_magenta,
            Rgb.rgb_cyan,
            Rgb.rgb_green,
    };

    public String plot() {
        Plot plot = new Plot();
        plot.set_title("MTF");
        plot.get_axes().set_position(Vector3.vector3_0);
        plot.get_axes().set_range(new Range(0, 1.0), PlotAxes.AxisMask.X);
        // MTF is plotted as a percentage on a 0-100 scale
        plot.get_axes().set_range(new Range(0, 100.0), PlotAxes.AxisMask.Y);
        double[] x_data = fields.clone();
        // for each freq
        for (var i = 0; i < mtfs_by_freq.size(); i++) {
            var mtf = mtfs_by_freq.get(i);
            // color encodes the frequency
            Rgb color = FREQ_COLORS[i % FREQ_COLORS.length];
            // xy == 0 sagittal
            // xy == 1 tangential
            for (int xy = 0; xy < 2; xy++) {
                var set = new DiscreteSet();
                set.set_interpolation(Interpolation.Cubic);
                double[] mtf_data = (xy == 0) ? mtf.sag_mtf_by_field : mtf.tan_mtf_by_field;
                for (int j = 0; j < mtf_data.length; j++) {
                    double x = x_data[j];
                    // scale 0..1 MTF to a 0..100 percentage
                    set.add_data(x, mtf_data[j] * 100.0);
                }
                String label = df.format(mtf.freq) + (xy == 0 ? " Sagittal" : " Tangential");
                PlotData pd = plot.add_plot_data(set, color, label, PlotStyleMask.InterpolatePlot.value());
                // line pattern encodes sagittal vs tangential
                pd.set_line_style(xy == 0 ? PlotData.LineStyle.Solid : PlotData.LineStyle.Dashed);
            }
        }
        String x_label = "Fields";
        String y_label = "MTF";
        plot.get_axes ().set_label (x_label, PlotAxes.AxisMask.X);
        plot.get_axes ().set_label (y_label, PlotAxes.AxisMask.Y);
        // keep both axes on a plain scale (no x10^n factor)
        plot.get_axes().set_unit("", false, false, 0, PlotAxes.AxisMask.Y);
        plot.get_axes().set_unit("", false, false, 0, PlotAxes.AxisMask.X);
        RendererSvg r = new RendererSvg(1024,640);
        PlotRenderer plotRenderer = new PlotRenderer();
        plotRenderer.draw_plot(r,plot);
        return r.write(new StringBuilder()).toString();
    }
    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append(",");
        for (int i = 0; i < mtfs_by_freq.get(0).tan_mtf_by_field.length; i++) {
            if (i > 0)
                sb.append(",");
            sb.append(df.format(fields[i]));
        }
        sb.append("\n");
        // for each freq
        for (var i = 0; i < mtfs_by_freq.size(); i++) {
            var mtf = mtfs_by_freq.get(i);
            for (int xy = 0; xy < 2; xy++) {
                var set = new DiscreteSet();
                set.set_interpolation(Interpolation.Cubic);
                double[] mtf_data = (xy == 0) ? mtf.sag_mtf_by_field : mtf.tan_mtf_by_field;
                sb.append(mtf.freq).append(" ").append(xy==0?"sag":"tan").append(",");
                for (int j = 0; j < mtf_data.length; j++) {
                    if (j > 0)
                        sb.append(",");
                    sb.append(df.format(mtf_data[j]));
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
