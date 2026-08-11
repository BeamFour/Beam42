package org.redukti.rayoptics.analysis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redukti.data.DiscreteSet;
import org.redukti.data.Interpolation;
import org.redukti.mathlib.M;
import org.redukti.rayoptics.elem.profiles.EvenPolynomial;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.VigCalc;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.seq.SurfaceData;
import org.redukti.rayoptics.specs.*;
import org.redukti.rayoptics.util.Pair;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MtfTest {

    static final DecimalFormat df = M.decimal_format();

    static OpticalModel buildTestModel() {
        OpticalModel opm = new OpticalModel();
        SequentialModel sm = opm.seq_model;
        OpticalSpecs osp = opm.optical_spec;
        osp.pupil = new PupilSpec(osp, new Pair<>(ImageKey.Image, ValueKey.Fnum), 0.98);
        osp.fov = new FieldSpec(osp, new Pair<>(ImageKey.Object, ValueKey.Angle), new double[]{0., 0.7, 1.0});
        osp.fov.value = 19.98;
        osp.fov.is_relative = true;
        osp.fov.is_wide_angle = true;
        osp.wvls = new WvlSpec(new WvlWt[]{
                new WvlWt(587.5618, 1.0),
                new WvlWt(486.1327, 1.0),
                new WvlWt(656.2725, 1.0)}, 0);
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
                .max_aperture(23.85));
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
        sm.do_apertures = false;
        opm.update_model();
        VigCalc.set_pupil(opm);
        opm.update_model();
        System.out.println(sm.list_surfaces(new StringBuilder()).toString());
        System.out.println(sm.list_gaps(new StringBuilder()).toString());
        System.out.println(osp.list_str(new StringBuilder()).toString());

        return opm;
    }

    static List<MTFResultByFreq> generateMTFs(OpticalModel opm, int[] freqs, double[] fields, Map<Double,Double> wv_wts) {
        var spotAnalysis = SpotAnalysis.eval(opm,new SpotOptions().num_rays(64).use_grid(false));
        var mtfs = new ArrayList<PolyMTF>();
        for (int i = 0; i < spotAnalysis.spot_results.size(); i++) {
            var spotFld = spotAnalysis.spot_results.get(i);
            var cfg = spotFld.mtfHistogramConfig();
            PolyMTF polyMtfForField = null;
            for (var intercepts: spotFld.intercepts) {
                var mtf = new MonochromaticGeometricMTF(intercepts, cfg);
                if (polyMtfForField == null)
                    polyMtfForField = new PolyMTF(mtf.mtf.fft_size,mtf.h2d.pixel_size);
                var wt = wv_wts.getOrDefault(intercepts.wvl,0.0);
                if (wt != 0.0)
                    polyMtfForField.add(mtf.mtf, wt);
            }
            if (polyMtfForField != null) {
                polyMtfForField.compute();
                mtfs.add(polyMtfForField);
            }
        }
        var mtfResults = new ArrayList<MTFResultByFreq>();
        for (var freq: freqs)
            mtfResults.add(new MTFResultByFreq(mtfs,freq));
        return mtfResults;
    }

    public Map<Double,Double> get_wvl_wts(double[] wvls, double[] wts) {
        var map = new LinkedHashMap<Double,Double>();
        for (int i = 0; i < wvls.length; i++)
            map.put(wvls[i],wts[i]);
        return map;
    }

    public String toString(List<MTFResultByFreq> mtfs_by_freq, double[] fields) {
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
    @Test
    public void testMtf() {
        var opm = buildTestModel();
        var mtfByFreq = generateMTFs(opm,
                new int[] {10,30,50},
                new double[] { 0.0, 0.7, 1.0 },
                get_wvl_wts(new double[] {587.5618, 486.1327, 656.2725},
                        new double[] {1.0, 1.0, 1.0}));
        var results = toString(mtfByFreq, new double[] { 0.0, 0.7, 1.0 });
        String expected = """
,0,0.7,1
10 sag,0.967,0.955,0.763
10 tan,0.967,0.949,0.939
30 sag,0.754,0.709,0.506
30 tan,0.754,0.648,0.589
50 sag,0.519,0.47,0.414
50 tan,0.519,0.385,0.319
""";
        Assertions.assertEquals(expected,results);
    }
}
