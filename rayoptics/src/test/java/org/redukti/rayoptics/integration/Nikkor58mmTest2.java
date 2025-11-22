package org.redukti.rayoptics.integration;

import org.junit.jupiter.api.Test;
import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.output.data.DiscreteSet;
import org.redukti.output.data.Interpolation;
import org.redukti.output.data.Range;
import org.redukti.mathlib.Vec2Pair;
import org.redukti.output.plotting.Plot;
import org.redukti.output.plotting.PlotAxes;
import org.redukti.output.plotting.PlotRenderer;
import org.redukti.output.plotting.PlotStyleMask;
import org.redukti.output.rendering.Renderer;
import org.redukti.output.rendering.RendererSvg;
import org.redukti.output.rendering.Rgb;
import org.redukti.rayoptics.analysis.SpotAnalysis;
import org.redukti.rayoptics.analysis.TransverseRayAberrationAnalysis;
import org.redukti.rayoptics.elem.profiles.EvenPolynomial;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.*;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.seq.SurfaceData;
import org.redukti.rayoptics.specs.*;
import org.redukti.rayoptics.util.Pair;

public class Nikkor58mmTest2 {

    @Test
    public void test() {
        OpticalModel opm = new OpticalModel();
        SequentialModel sm = opm.seq_model;
        OpticalSpecs osp = opm.optical_spec;
        osp.pupil = new PupilSpec(osp, new Pair<>(ImageKey.Image, ValueKey.Fnum), 0.98);
        osp.fov = new FieldSpec(osp, new Pair<>(ImageKey.Object, ValueKey.Angle), new double[]{19.98});
        osp.wvls = new WvlSpec(new WvlWt[]{
                new WvlWt(587.5618, 1.0)}, 0);
        opm.system_spec.title = "WO2019-229849 Example 1 (Nikkor Z 58mm f/0.95 S)";
        opm.system_spec.dimensions = "MM";
        opm.radius_mode = true;
        sm.gaps.get(0).thi = 1e10;
        sm.add_surface(new SurfaceData(108.488, 7.65)
                .rindex(1.90265, 35.77, "J-LASFH9A", "Hikari")
                .max_aperture(33.4));
        sm.ifcs.get(sm.cur_surface).profile = new EvenPolynomial()
                .r(108.488)
                .cc(0)
                .coefs(new double[]{0.0, -3.82177e-07, -6.06486e-11, -3.80172e-15, -1.32266e-18, 0, 0});
        sm.add_surface(new SurfaceData(-848.55, 2.8)
                .rindex(1.55298, 55.07, "J-KZFH4", "Hikari")
                .max_aperture(32.91));
        sm.add_surface(new SurfaceData(50.252, 18.12)
                .max_aperture(28.97));
        sm.add_surface(new SurfaceData(-60.72, 2.8)
                .rindex(1.61266, 44.46, "J-KZFH1", "Hikari")
                .max_aperture(29.14));
        sm.add_surface(new SurfaceData(2497.5, 9.15)
                .rindex(1.59319, 67.9, "J-PSKH1", "Hikari")
                .max_aperture(32.66));
        sm.add_surface(new SurfaceData(-77.239, 0.4)
                .max_aperture(32.66));
        sm.add_surface(new SurfaceData(113.763, 10.95)
                .rindex(1.8485, 43.79, "J-LASFH22", "Hikari")
                .max_aperture(35.45));
        sm.add_surface(new SurfaceData(-178.06, 0.4)
                .max_aperture(35.45));
        sm.add_surface(new SurfaceData(70.659, 9.74)
                .rindex(1.59319, 67.9, "J-PSKH1", "Hikari")
                .max_aperture(32.5));
        sm.add_surface(new SurfaceData(-1968.5, 0.2)
                .max_aperture(32.5));
        sm.add_surface(new SurfaceData(289.687, 8)
                .rindex(1.59319, 67.9, "J-PSKH1", "Hikari")
                .max_aperture(30.53));
        sm.add_surface(new SurfaceData(-97.087, 2.8)
                .rindex(1.738, 32.33, "J-KZFH9", "Hikari")
                .max_aperture(29.71));
        sm.add_surface(new SurfaceData(47.074, 8.7)
                .max_aperture(25.12));
        sm.add_surface(new SurfaceData(0, 5.29)
                .max_aperture(23.959)); // TODO check this is correct for stop
        sm.set_stop();
        sm.add_surface(new SurfaceData(-95.23, 2.2)
                .rindex(1.61266, 44.46,"J-KZFH1", "Hikari")
                .max_aperture(24.96));
        sm.add_surface(new SurfaceData(41.204, 11.55)
                .rindex(1.49782, 82.57, "J-FKH1", "Hikari")
                .max_aperture(24.96));
        sm.add_surface(new SurfaceData(-273.092, 0.2)
                .max_aperture(24.96));
        sm.add_surface(new SurfaceData(76.173, 9.5)
                .rindex(1.883, 40.69, "J-LASF08A", "Hikari")
                .max_aperture(25.56));
        sm.add_surface(new SurfaceData(-101.575, 0.2)
                .max_aperture(25.56));
        sm.add_surface(new SurfaceData(176.128, 7.45)
                .rindex(1.95375, 32.33, "J-LASFH21", "Hikari")
                .max_aperture(23.4));
        sm.ifcs.get(sm.cur_surface).profile = new EvenPolynomial()
                .r(176.128)
                .cc(0)
                .coefs(new double[]{0.0, -1.15028e-06, -4.51771e-10, 2.7267e-13, -7.66812e-17, 0, 0});
        sm.add_surface(new SurfaceData(-67.221, 1.8)
                .rindex(1.738, 32.33, "J-KZFH9", "Hikari")
                .max_aperture(22.68));
        sm.add_surface(new SurfaceData(55.51, 2.68)
                .max_aperture(19.92));
        sm.add_surface(new SurfaceData(71.413, 6.35)
                .rindex(1.883, 40.69, "J-LASF08A", "Hikari")
                .max_aperture(19.73));
        sm.add_surface(new SurfaceData(-115.025, 1.81)
                .rindex(1.69895, 30.13, "J-SF15", "Hikari")
                .max_aperture(19.73));
        sm.add_surface(new SurfaceData(46.943, 0.8)
                .max_aperture(19.73));
        sm.add_surface(new SurfaceData(55.281,9.11)
                .rindex(1.883,40.69, "J-LASF08A", "Hikari")
                .max_aperture(19.47));
        sm.add_surface(new SurfaceData(-144.041, 3)
                .rindex(1.76554, 46.76, "J-LASFH2", "Hikari")
                .max_aperture(19.14));
        sm.add_surface(new SurfaceData(52.858, 14.5)
                .max_aperture(19.14));
        sm.ifcs.get(sm.cur_surface).profile = new EvenPolynomial()
                .r(52.858)
                .cc(0)
                .coefs(new double[]{0.0, 3.18645e-06, -1.14718e-08, 7.74567e-11, -2.24225e-13, 3.3479e-16, -1.7047e-19});
        sm.add_surface(new SurfaceData(0, 1.6)
                .rindex(1.5168, 64.14, "J-BK7A", "Hikari")
                .max_aperture(22.15));
        sm.add_surface(new SurfaceData(0, 1)
                .max_aperture(22.15));
        System.out.println(sm.list_surfaces(new StringBuilder()).toString());
        System.out.println(sm.list_gaps(new StringBuilder()).toString());
        sm.do_apertures = false;
        opm.update_model();
        Trace.apply_paraxial_vignetting(opm);
        //VigCalc.set_vig(opm,true);
        //opm.update_model();
        var transAber = TransverseRayAberrationAnalysis.eval_abr_fan(opm,0,1,21,new TraceOptions());

        double[] xvals = {-2.46527285e-01, -2.21874556e-01, -1.97221828e-01, -1.72569099e-01,
  -1.47916371e-01, -1.23263642e-01, -9.86109139e-02, -7.39581854e-02,
  -4.93054570e-02, -2.46527285e-02, -3.42125335e-17,  4.75361045e-02,
   9.50722090e-02,  1.42608313e-01,  1.90144418e-01,  2.37680522e-01,
   2.85216627e-01,  3.32752731e-01,  3.80288836e-01,  4.27824940e-01,
   4.75361045e-01};
        double[] yvals = {-0.01041473, -0.00961803, -0.00852592, -0.00726078, -0.00592679, -0.00460919,
  -0.00337449, -0.00227133, -0.00133184, -0.00057331,  0.,          0.00061566,
   0.0007114,   0.00049971,  0.00022418,  0.00011907,  0.00037392,  0.0011027,
   0.00231606,  0.00389579,  0.00557028};

        for (int i = 0; i < xvals.length; i++) {
            System.out.println("x expected " + xvals[i] + " got " + transAber.fans.get(0).fan_x.get(i));
            System.out.println("y expected " + yvals[i] + " got " + transAber.fans.get(0).fan_y.get(i));
        }


        Plot plot = new Plot();
        plot.set_title("Transverse ray aberration");
        plot.get_axes().set_position(Vector3.vector3_0);
        plot.get_axes().set_range(new Range(-1.0, 1.0), PlotAxes.AxisMask.X);
        plot.get_axes().set_tics_step(1.0,  PlotAxes.AxisMask.X);
        plot.get_axes().set_range(new Range(-0.03, 0.03), PlotAxes.AxisMask.Y);
        var set = new DiscreteSet();
        set.set_interpolation(Interpolation.Cubic);
        var x_data = transAber.fans.get(0).fan_x;
        var y_data = transAber.fans.get(0).fan_y;
        // p.set_color (light::SpectralLine::get_wavelen_color (w));
        for (int i = 0; i < x_data.size(); i++) {
            double x = x_data.get(i);
            double y = y_data.get(i);
            set.add_data(x,y);
        }
        plot.add_plot_data(set, Rgb.rgb_red,"label", PlotStyleMask.InterpolatePlot.value());
        plot.get_axes ().set_label ("X", PlotAxes.AxisMask.X);
        plot.get_axes ().set_label ("Y", PlotAxes.AxisMask.Y);
        //plot.get_axes().set_unit("",false,false,0, PlotAxes.AxisMask.X);
        //plot.get_axes().set_unit("",false,false,0, PlotAxes.AxisMask.Y);
        RendererSvg r = new RendererSvg(640,480);
        PlotRenderer plotRenderer = new PlotRenderer();
        plotRenderer.draw_plot(r,plot);
        //System.out.println(r.write(new StringBuilder()));

        r = new RendererSvg(480,480);
        r.set_window(new Vec2Pair(new Vector2(-100, -100), new Vector2(100, 100)), true);

        var axes = new PlotAxes();
        axes.set_show_axes (false, PlotAxes.AxisMask.XY);
        axes.set_label ("Sagittal distance", PlotAxes.AxisMask.X);
        axes.set_label ("Tangential distance", PlotAxes.AxisMask.Y);
        axes.set_unit ("m", true, true, -3, PlotAxes.AxisMask.XY);
        axes.set_tics_count (3, PlotAxes.AxisMask.XY);

        plotRenderer = new PlotRenderer();
        plotRenderer.draw_axes_2d (r, axes);

        var spot = SpotAnalysis.eval(opm,21, new TraceOptions());
        for (var byfld : spot.spot_results)
        {
            for (var ray: byfld.trace_results) {
                for (var g: ray.grid) {
                    r.draw_point (new Vector2(g.pupil.x()*1000, g.pupil.y()*1000), get_wavelen_color(ray.wvl), Renderer.PointStyle.PointStyleDot);
                }
            }
        }
        System.out.println(r.write(new StringBuilder()));
    }

        /** get rgb color associated with wavelen */
    public static Rgb get_wavelen_color (double wl) {
        // based on algorithm from Dan Bruton
        // (www.physics.sfasu.edu/astro/color.html)
        // http://www.physics.sfasu.edu/astro/color/spectra.html

        if (wl < 380.0 || wl > 780.0)
            return Rgb.rgb_black;

        double s = 1.0;

        if (wl < 420.0)
            s = 0.3 + 0.7 * (wl - 380.0f) / 40.0;
        else if (wl > 700.0)
            s = 0.3 + 0.7 * (780.0 - wl) / 80.0;

        if (wl < 510.0)
        {
            if (wl < 490.0)
            {
                if (wl < 440.0)
                    // 380 to 440
                    return new Rgb (s * -(wl - 440.0) / 60.0, 0.0, s, 1.0);
          else
                // 440 to 490
                return new Rgb (0.0, s * (wl - 440.0) / 50.0, s, 1.0);
            }
            else
                // 490 to 510
                return new Rgb (0.0, s, s * -(wl - 510.0) / 20.0, 1.0);
        }
        else
        {
            if (wl < 645.0)
            {
                if (wl < 580.0)
                    // 510 to 580
                    return new Rgb (s * (wl - 510.0) / 70.0, s, 0.0, 1.0);
          else
                // 580 to 645
                return new Rgb (s, s * -(wl - 645.0) / 65.0, 0.0, 1.0);
            }
            else
            {
                // 645 to 780
                return new Rgb (s, 0.0, 0.0, 1.0);
            }
        }
    }
}
