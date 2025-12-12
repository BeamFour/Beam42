package org.redukti.rayoptics.analysis;

import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector2Pair;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.raytr.GridItem;
import org.redukti.rayoptics.raytr.TraceGridByWvl;
import org.redukti.rayoptics.specs.Field;
import org.redukti.render.plotting.PlotAxes;
import org.redukti.render.plotting.PlotRenderer;
import org.redukti.render.rendering.Renderer;
import org.redukti.render.rendering.RendererSvg;
import org.redukti.render.rendering.Rgb;

import java.util.ArrayList;
import java.util.List;

public class SpotAnalysisResult extends Analysis {

    public static class SpotResultsByField {
        public Field fld;
        public Vector3 image_pt;
        public List<TraceGridByWvl> trace_results;
        public double max_radius;
        public double mean_radius;

        public SpotResultsByField(Field fld, List<TraceGridByWvl> trace_results) {
            this.fld = fld;
            this.image_pt = fld.ref_sphere.image_pt;
            this.trace_results = trace_results;
            computeMeanMax();
        }

        private void computeMeanMax() {
            max_radius = 0;
            mean_radius = 0;
            int count = 0;
            for (int wl = 0; wl < trace_results.size(); wl++) {
                var grids = trace_results.get(wl);
                for (var grid : grids.grid) {
                    //System.out.println("pupil = " + grid.pupil.toString());
                    var l = grid.pupil.len();
                    //System.out.println("len = " + l);
                    if (l > max_radius) {
                        max_radius = l;
                    }
                    mean_radius += (l * l);
                }
                count += grids.grid.size();
            }
            mean_radius = Math.sqrt(mean_radius / count);
        }

        @Override
        public String toString() {
            return "Field " + fld + " mean radius " + get_mean_radius() + " max radius " + get_max_radius();
        }

        public double get_max_radius() {
            return max_radius * 1000;
        }

        public double get_mean_radius() {
            return mean_radius * 1000;
        }


        public String plot() {
            RendererSvg r = new RendererSvg(640,480);
            r = new RendererSvg(640,640, Rgb.rgb_black);
            r.set_window(new Vector2Pair(new Vector2(-600, -600), new Vector2(600, 600)), true);
            var axes = new PlotAxes();
            axes.set_show_axes (false, PlotAxes.AxisMask.XY);
            axes.set_label ("Sagittal distance", PlotAxes.AxisMask.X);
            axes.set_label ("Tangential distance", PlotAxes.AxisMask.Y);
            axes.set_unit ("m", true, true, -3, PlotAxes.AxisMask.XY);
            axes.set_tics_count (3, PlotAxes.AxisMask.XY);
            var plotRenderer = new PlotRenderer();
            plotRenderer.draw_axes_2d (r, axes);
            for (var ray: trace_results) {
                for (var g: ray.grid) {
                    r.draw_point (new Vector2(g.pupil.x()*1000, g.pupil.y()*1000), get_wavelen_color(ray.wvl), Renderer.PointStyle.PointStyleDot);
                }
            }
            return r.write(new StringBuilder()).toString();
        }
    }

    public List<SpotResultsByField> spot_results = new ArrayList<>();
    public SpotAnalysisResult add(Field fld, List<TraceGridByWvl> trace_results) {
        spot_results.add(new SpotResultsByField(fld, trace_results));
        return this;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        for (var result: spot_results) {
            sb.append(result.toString());
        }
        return sb.toString();
    }
}
