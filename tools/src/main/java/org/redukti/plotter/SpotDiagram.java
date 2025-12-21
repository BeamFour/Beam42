package org.redukti.plotter;

import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector2Pair;
import org.redukti.rayoptics.analysis.SpotAnalysisResult;
import org.redukti.render.plotting.PlotAxes;
import org.redukti.render.plotting.PlotRenderer;
import org.redukti.render.rendering.Renderer;
import org.redukti.render.rendering.RendererSvg;
import org.redukti.render.rendering.Rgb;

public class SpotDiagram {

    public final SpotAnalysisResult.SpotResultsByField result;

    public SpotDiagram(SpotAnalysisResult.SpotResultsByField result) {
        this.result = result;
    }

    public String plot() {
        RendererSvg r = new RendererSvg(640,640, Rgb.rgb_black);
        r.set_window(new Vector2Pair(new Vector2(-600, -600), new Vector2(600, 600)), true);
        var axes = new PlotAxes();
        axes.set_show_axes (false, PlotAxes.AxisMask.XY);
        axes.set_label ("Sagittal distance", PlotAxes.AxisMask.X);
        axes.set_label ("Tangential distance", PlotAxes.AxisMask.Y);
        axes.set_unit ("m", true, true, -3, PlotAxes.AxisMask.XY);
        axes.set_tics_count (3, PlotAxes.AxisMask.XY);
        var plotRenderer = new PlotRenderer();
        plotRenderer.draw_axes_2d (r, axes);
        for (var intercepts: result.intercepts) {
            for (int i = 0; i < intercepts.x.length; i++) {
                r.draw_point (new Vector2(intercepts.x[i]*1000, intercepts.y[i]*1000), Colors.get_wavelen_color(intercepts.wvl), Renderer.PointStyle.PointStyleDot);
            }
        }
        return r.write(new StringBuilder()).toString();
    }
}
