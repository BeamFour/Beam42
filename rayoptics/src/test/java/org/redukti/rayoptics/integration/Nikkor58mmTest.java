package org.redukti.rayoptics.integration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.analysis.SpotAnalysis;
import org.redukti.rayoptics.analysis.TransverseRayAberrationAnalysis;
import org.redukti.rayoptics.analysis.WavefrontAberrationAnalysis;
import org.redukti.rayoptics.elem.profiles.EvenPolynomial;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.parax.FirstOrderData;
import org.redukti.rayoptics.raytr.*;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.seq.SurfaceData;
import org.redukti.rayoptics.specs.*;
import org.redukti.rayoptics.util.Lists;
import org.redukti.rayoptics.util.Pair;

public class Nikkor58mmTest {

    @Test
    public void test() {
        OpticalModel opm = new OpticalModel();
        SequentialModel sm = opm.seq_model;
        OpticalSpecs osp = opm.optical_spec;
        osp.pupil = new PupilSpec(osp, new Pair<>(ImageKey.Image, ValueKey.Fnum), 0.98);
        osp.fov = new FieldSpec(osp, new Pair<>(ImageKey.Object, ValueKey.Angle), new double[]{0., 19.98});
        osp.wvls = new WvlSpec(new WvlWt[]{new WvlWt(486.1327, 0.5),
                new WvlWt(587.5618, 1.0),
                new WvlWt(656.2725, 0.5)}, 1);
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
                .rindex(1.738, 32.33) // , "J-KZFH9", "Hikari")
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
                .rindex(1.738, 32.33)// , "J-KZFH9", "Hikari")
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
                .rindex(1.76554, 46.76)//, "J-LASFH2", "Hikari")
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
        FirstOrderData fod = osp.parax_data.fod;
        //Assertions.assertEquals(59.62, fod.efl, 0.001);
        Assertions.assertEquals(1.660, fod.ffl, 0.001);
        Assertions.assertEquals(61.28, fod.pp1, 0.001);
        Assertions.assertEquals(-58.63, fod.ppk, 0.01);
        Assertions.assertEquals(0.9925, fod.bfl, 0.001);
        Assertions.assertEquals(0.9800, fod.fno, 0.001);
        Assertions.assertEquals(-5.962e-09, fod.m, 1e-9);
        Assertions.assertEquals(-1.6772757151780143E8, fod.red, 1e-6);
        Assertions.assertEquals(19.98, fod.obj_ang, 0.01);
        Assertions.assertEquals(69.29, fod.enp_dist, 0.01);
        Assertions.assertEquals(30.42, fod.enp_radius, 0.01);
        Assertions.assertEquals(3.042e-09, fod.obj_na, 1e-9);
        Assertions.assertEquals(1.000, fod.n_obj, 1e-9);
        Assertions.assertEquals(0.9925, fod.img_dist, 0.00001);
        Assertions.assertEquals(21.68, fod.img_ht, 0.01);
        Assertions.assertEquals(-51.58, fod.exp_dist, 0.01);
        Assertions.assertEquals(26.82, fod.exp_radius, 0.01);
        Assertions.assertEquals(1.000, fod.n_img, 0.001);
        Assertions.assertEquals(11.06, fod.opt_inv, 0.001);

        // Test a ray trace
        RaySeg[] expected = new RaySeg[]{
                new RaySeg(new Vector3(0.00000000e+00, -3635749788.7098503, 0.00000000e+00), new Vector3(0., 0.34169210791780597, 0.9398119510767493), 10640426514.13607, new Vector3(-0., 0., 1.)),
                new RaySeg(new Vector3(0., -23.95018415, 2.53899499), new Vector3(0., 0.27421071, 0.96166963), 5.002303876715387, new Vector3(-0., 0.19829001, 0.98014339)),
                new RaySeg(new Vector3(0., -22.57849885, -0.30044127), new Vector3(0., 0.34230345, 0.93958946), 7.692858280761749, new Vector3(0., -0.02660833, 0.99964594)),
                new RaySeg(new Vector3(0., -19.94520693, 4.12768727), new Vector3(0., 0.31152002, 0.95023959), 12.447211581007771, new Vector3(-0., 0.39690374, 0.91786024)),
                new RaySeg(new Vector3(0., -16.06765133, -2.1644795), new Vector3(0., 0.08136805, 0.99668412), 5.030245533347532, new Vector3(0., -0.26461876, 0.9643531)),
                new RaySeg(new Vector3(0., -15.65835008, 0.04908635), new Vector3(0., 0.08228559, 0.99660879), 7.650632327993517, new Vector3(-0., 0.00626961, 0.99998035)),
                new RaySeg(new Vector3(0., -15.0288133, -1.47622621), new Vector3(0., 0.25421056, 0.9671489), 2.8727458940940274, new Vector3(0., -0.19457545, 0.98088755)),
                new RaySeg(new Vector3(0., -14.29853097, 0.90214682), new Vector3(0., 0.19548399, 0.98070689), 9.806035736108708, new Vector3(-0., 0.12568701, 0.99206995)),
                new RaySeg(new Vector3(0., -12.38160795, -0.43100635), new Vector3(0., 0.42460646, 0.90537802), 1.9669098146840422, new Vector3(0., -0.06953616, 0.99757943)),
                new RaySeg(new Vector3(0., -11.54644533, 0.94979056), new Vector3(0., 0.32881212, 0.94439536), 9.288367323830421, new Vector3(-0., 0.16341082, 0.98655811)),
                new RaySeg(new Vector3(0., -8.4923176, -0.01831846), new Vector3(0., 0.52668396, 0.85006118), 0.396182105669868, new Vector3(0., -0.00431411, 0.99999069)),
                new RaySeg(new Vector3(0., -8.28365484, 0.11846056), new Vector3(0., 0.34220086, 0.93962683), 8.223855304588305, new Vector3(-0., 0.02859519, 0.99959107)),
                new RaySeg(new Vector3(0., -5.46944451, -0.15418437), new Vector3(0., 0.308619, 0.95118574), 3.3266936194814365, new Vector3(0., -0.0563355, 0.99841189)),
                new RaySeg(new Vector3(0., -4.44276366, 0.21011916), new Vector3(0., 0.46365344, 0.88601664), 9.582078331237566, new Vector3(-0., 0.09437829, 0.99553641)),
                new RaySeg(new Vector3(0.00000000e+00, -4.464396993270628e-08, 0.00000000e+00), new Vector3(0., 0.46365344341486237, 0.8860166389010661), 5.925799449326848, new Vector3(-0., 0., 1.)),
                new RaySeg(new Vector3(0., 2.74751728, -0.03964309), new Vector3(0., 0.29919181, 0.95419299), 2.5029036780746132, new Vector3(0., 0.02885138, 0.99958371)),
                new RaySeg(new Vector3(0., 3.49636556, 0.14861005), new Vector3(0., 0.32920781, 0.94425749), 11.967214785345057, new Vector3(-0., -0.084855, 0.99639331)),
                new RaySeg(new Vector3(0., 7.43606616, -0.1012577), new Vector3(0., 0.47848744, 0.87809439), 0.8013660514588059, new Vector3(0., 0.02722916, 0.99962922)),
                new RaySeg(new Vector3(0., 7.81950975, 0.40241734), new Vector3(0., 0.20114341, 0.97956181), 8.823788024758601, new Vector3(-0., -0.10265461, 0.99471706)),
                new RaySeg(new Vector3(0., 9.59435653, -0.45413693), new Vector3(0., 0.29441994, 0.95567615), 0.9626656586072206, new Vector3(0., 0.09445589, 0.99552905)),
                new RaySeg(new Vector3(0., 9.87778449, 0.26585968), new Vector3(0., 0.12478787, 0.99218345), 6.381136241513929, new Vector3(-0., -0.05143318, 0.99867644)),
                new RaySeg(new Vector3(0., 10.67407286, -0.85288258), new Vector3(0., 0.12055377, 0.9927068), 3.8085690823354215, new Vector3(0., 0.15879075, 0.98731226)),
                new RaySeg(new Vector3(0., 11.13321022, 1.12790984), new Vector3(0., 0.37276772, 0.92792469), 2.7996917389269838, new Vector3(-0., -0.20056225, 0.97968096)),
                new RaySeg(new Vector3(0., 12.17684492, 1.04581293), new Vector3(0., 0.11126818, 0.99379042), 4.6305622838414315, new Vector3(-0., -0.170513, 0.98535543)),
                new RaySeg(new Vector3(0., 12.69207917, -0.70237865), new Vector3(0., 0.11136852, 0.99377918), 4.429743067228947, new Vector3(0., 0.11034192, 0.99389369)),
                new RaySeg(new Vector3(0., 13.1854131, 1.88980777), new Vector3(0., 0.41738482, 0.90872983), 0.6280719531302639, new Vector3(-0., -0.28088135, 0.9597425)),
                new RaySeg(new Vector3(0., 13.4475608, 1.66055549), new Vector3(0., 0.09298565, 0.99566745), 6.7891599551877855, new Vector3(-0., -0.24325828, 0.96996155)),
                new RaySeg(new Vector3(0., 14.07885527, -0.68969894), new Vector3(0., 0.09266914, 0.99569696), 5.902738301769976, new Vector3(0., 0.097742, 0.99521179)),
                new RaySeg(new Vector3(0., 14.62585694, 2.18763963), new Vector3(0., 0.44345013, 0.89629905), 13.73688884020393, new Vector3(-0., -0.30889122, 0.95109737)),
                new RaySeg(new Vector3(0., 20.7174821, 0.), new Vector3(0., 0.292359, 0.95630864), 1.673100014808453, new Vector3(-0., -0., 1.0)),
                new RaySeg(new Vector3(0., 21.20662794, 0.), new Vector3(0., 0.44345013, 0.89629905), 1.1156990554757744, new Vector3(-0., -0., 1.)),
                new RaySeg(new Vector3(0., 21.70138483624297, 0.), new Vector3(0., 0.4434501309129979, 0.8962990468550369), 0.0, new Vector3(-0., -0., 1.))
        };

        RayTraceOptions options = new RayTraceOptions();
        options.first_surf = 1;
        options.last_surf = sm.get_num_surfaces() - 2;
        RayPkg raypkg = RayTrace.trace_raw(sm.path(587.5618, null, null, 1),
                new Vector3(0, -3635749788.7098503, 0.0),
                new Vector3(0.0, 0.34169210791780597, 0.9398119510767493), 587.5618, options);

        Assertions.assertTrue(compare(expected[0], raypkg.ray.get(0)));
        Assertions.assertTrue(compare(expected[14], raypkg.ray.get(14)));
        Assertions.assertTrue(compare(expected[31], raypkg.ray.get(31)));

        double cr_expected_op_delta[] = { 239.18720860000005, 245.3448599476832 };
        RaySeg[] cr_expected_final_rayseg = {
            new RaySeg(new Vector3(0., 0., 0.), new Vector3(0., 0., 1.), 0.0, new Vector3(-0., -0., 1.)),
            new RaySeg(new Vector3(0., 21.70138483624297, 0.), new Vector3(0., 0.4434501309129979, 0.8962990468550369), 0.0, new Vector3(-0., -0., 1.))
        };

        for (int fi = 0; fi < osp.fov.fields.length; fi++) {
            var fld = osp.fov.fields[fi];
            var wvl = sm.central_wavelength();
            var foc = osp.defocus().get_focus();

            var t = Trace.setup_pupil_coords(opm,fld,wvl,foc,null,null);
            Assertions.assertEquals(cr_expected_op_delta[fi],t.chief_ray_pkg.chief_ray.op_delta,1e-8);
            Assertions.assertTrue(compare(Lists.get(t.chief_ray_pkg.chief_ray.ray,-1),cr_expected_final_rayseg[fi]));
            fld.chief_ray = t.chief_ray_pkg;
            fld.ref_sphere = t.ref_sphere;
        }

        var result = Trace.trace_boundary_rays(opm,new TraceOptions());
        System.out.println(result);

        var transAber = TransverseRayAberrationAnalysis.eval_abr_fan(opm,1,1,21,new TraceOptions());
        Assertions.assertEquals(-3.2620548349684384, transAber.fans.get(1).fan_y.get(0),1e-15);
        Assertions.assertEquals(0.0, transAber.fans.get(1).fan_y.get(9),1e-15);
        Assertions.assertEquals(-0.3119345075482975, transAber.fans.get(1).fan_y.get(19),1e-15);

        var waveAber = WavefrontAberrationAnalysis.eval_opd_fan(opm,1,1,21,new TraceOptions());
        Assertions.assertEquals(-0.0002035451272355, waveAber.fans.get(1).fan_y.get(0),1e-15);
        Assertions.assertEquals(0.0, waveAber.fans.get(1).fan_y.get(9),1e-15);
        Assertions.assertEquals(0.0000240955091344, waveAber.fans.get(1).fan_y.get(19),1e-15);

        var spotAnal = SpotAnalysis.eval_grid(opm,1,1,21,new TraceOptions());
        var grids = spotAnal.get(0);
        double mean = 0, max = 0;
        for (var grid: grids) {
            //System.out.println("pupil = " + grid.pupil.toString());
            var l = grid.pupil.len();
            //System.out.println("len = " + l);
            if (l > max) {
                max = l;
            }
            mean += (l*l);
        }
        mean = Math.sqrt(mean/grids.size());
        Assertions.assertEquals(mean*1000,10.636873278679923,1e-15);
        Assertions.assertEquals(max*1000,26.879611127471307,1e-15);
    }

    static boolean compare(RaySeg s1, RaySeg s2) {
        return s1.p.effectivelyEqual(s2.p)
                && s1.d.effectivelyEqual(s2.d)
                && Math.abs(s1.dst - s2.dst) < 1e-13;
    }
}
