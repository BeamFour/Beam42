package org.redukti.rayoptics.integration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.elem.profiles.EvenPolynomial;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.parax.firstorder.FirstOrderData;
import org.redukti.rayoptics.parax.firstorder.ParaxialModel;
import org.redukti.rayoptics.raytr.*;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.seq.SurfaceData;
import org.redukti.rayoptics.specs.*;
import org.redukti.rayoptics.util.Lists;
import org.redukti.rayoptics.util.Pair;

public class WideNikkorTest {

    @Test
    public void test() {
        OpticalModel opm = new OpticalModel();
        SequentialModel sm = opm.seq_model;
        OpticalSpecs osp = opm.optical_spec;
        ParaxialModel pm = opm.parax_model;
        osp.pupil = new PupilSpec(osp, new Pair<>(ImageKey.Image, ValueKey.Fnum), 4.0);
        osp.fov = new FieldSpec(osp, new Pair<>(ImageKey.Object, ValueKey.Angle), new double[]{0., 57.68}, true);
        osp.spectral_region = new WvlSpec(
                new WvlWt[]{
                        new WvlWt(486.1327, 0.5),
                        new WvlWt(587.5618, 1.0),
                        new WvlWt(656.2725, 0.5)}, 1);
        opm.system_spec.title = "JP2019-008031 Example 1 (Nikon Nikkor Z 14-30mm f/4 S)";
        opm.system_spec.dimensions = "MM";
        opm.radius_mode = true;
        sm.gaps.get(0).thi = 1e10;
        sm.add_surface(new SurfaceData(190.7535,3.0)
                .rindex(1.6937,53.32)
                .max_aperture(29.285));
        sm.add_surface(new SurfaceData(18.8098,9.5)
                .max_aperture(22.485));
        sm.ifcs.get(sm.cur_surface).profile = new EvenPolynomial()
                .r(18.8098)
                .cc(-1.0)
                .coefs(new double[]{0.0,-1.33157E-5,-3.07345E-8,6.9126E-11,-3.76684E-14,0.0,0.0});
        sm.add_surface(new SurfaceData(51.563,2.9)
                        .rindex(1.6937,53.32)
                .max_aperture(19.205));
        sm.add_surface(new SurfaceData(22.702,9.7)
                .max_aperture(14.475));
        sm.ifcs.get(sm.cur_surface).profile = new EvenPolynomial()
                .r(22.702)
                .cc(-1.0)
                .coefs(new double[]{0.0,3.67009E-5,1.37031E-7,-5.20756E-10,3.14884E-12,-5.6153E-15,0.0});
        sm.add_surface(new SurfaceData(-71.0651,1.9)
                .rindex(1.49782,82.57)
                .max_aperture(15.05));
        sm.add_surface(new SurfaceData(44.4835,0.1)
                .max_aperture(15.05));
        sm.add_surface(new SurfaceData(32.608,4.5)
                .rindex(1.90265,35.73)
                .max_aperture(15.05));
        sm.add_surface(new SurfaceData(296.5863,28.616)
                .max_aperture(15.05));
        sm.add_surface(new SurfaceData(63.0604,2.0)
                .rindex(1.59349,67.0)
                .max_aperture(9.04));
        sm.add_surface(new SurfaceData(499.8755,0.1)
                .max_aperture(9.04));
        sm.add_surface(new SurfaceData(24.0057,1.2)
                .rindex(1.883,40.66)
                .max_aperture(9.605));
        sm.add_surface(new SurfaceData(13.347,4.5)
                .rindex(1.56883,56.0)
                .max_aperture(8.54));
        sm.add_surface(new SurfaceData(333.9818,2.5)
                .max_aperture(8.54));
        sm.add_surface(new SurfaceData(0.0,7.483)
                .max_aperture(5.6335));
        sm.set_stop();
        sm.add_surface(new SurfaceData(36.3784,1.1)
                .rindex(1.816,46.59)
                .max_aperture(8.59));
        sm.add_surface(new SurfaceData(14.0097,4.71)
                .rindex(1.51612,64.08)
                .max_aperture(8.42));
        sm.add_surface(new SurfaceData(61.0448,0.2)
                .max_aperture(8.42));
        sm.ifcs.get(sm.cur_surface).profile = new EvenPolynomial()
                .r(61.0448)
                .cc(0)
                .coefs(new double[]{0.0,1.75905E-5,-6.64635E-8,2.26551E-10,-4.40763E-12,0.0,0.0});
        sm.add_surface(new SurfaceData(27.9719,3.15)
                .rindex(1.49782,82.57)
                .max_aperture(8.55));
        sm.add_surface(new SurfaceData(-75.3921,0.25)
                .max_aperture(8.55));
        sm.add_surface(new SurfaceData(91.9654,3.05)
                .rindex(1.49782,82.57)
                .max_aperture(8.915));
        sm.add_surface(new SurfaceData(-29.3923,1.579)
                .max_aperture(8.915));
        sm.add_surface(new SurfaceData(72.093,1.0)
                        .rindex(1.795,45.31)
                .max_aperture(9.065));
        sm.add_surface(new SurfaceData(20.9929,5.766)
                .max_aperture(9.065));
        sm.add_surface(new SurfaceData(-538.2301,4.8)
                .rindex(1.49782,82.57)
                .max_aperture(10.935));
        sm.add_surface(new SurfaceData(-20.1257,0.1)
                .max_aperture(10.935));
        sm.add_surface(new SurfaceData(-38.9341,1.4)
                .rindex(1.76546,46.75)
                .max_aperture(11.06));
        sm.ifcs.get(sm.cur_surface).profile = new EvenPolynomial()
                .r(-38.9341)
                .cc(-1.0)
                .coefs(new double[]{0.0,-2.67902E-5,-3.34364E-8,-1.13765E-10,-1.88017E-13,0.0,0.0});
        sm.add_surface(new SurfaceData(154.832,21.36)
                .max_aperture(11.815));
        System.out.println(sm.list_surfaces(new StringBuilder()).toString());
        System.out.println(sm.list_gaps(new StringBuilder()).toString());
        sm.do_apertures = false;
        opm.update_model();
        pm.first_order_data();
        FirstOrderData fod = pm.parax_data.fod;
        Assertions.assertEquals(14.42, fod.efl, 0.001);
        Assertions.assertEquals(15.5, fod.ffl, 0.01);
        Assertions.assertEquals(29.92, fod.pp1, 0.01);
        Assertions.assertEquals(6.939, fod.ppk, 0.01);
        Assertions.assertEquals(21.36, fod.bfl, 0.001);
        Assertions.assertEquals(4.0, fod.fno, 0.001);
        Assertions.assertEquals(-1.442e-09, fod.m, 1e-9);
        //Assertions.assertEquals(-6.935e+08, fod.red, 1e-4);
        Assertions.assertEquals(57.68, fod.obj_ang, 0.01);
        Assertions.assertEquals(19.84, fod.enp_dist, 0.01);
        Assertions.assertEquals(1.803, fod.enp_radius, 0.01);
        Assertions.assertEquals(1.803e-10, fod.obj_na, 1e-9);
        Assertions.assertEquals(1.000, fod.n_obj, 1e-9);
        Assertions.assertEquals(21.36, fod.img_dist, 0.01);
        Assertions.assertEquals(22.79, fod.img_ht, 0.01);
        Assertions.assertEquals(-26.59, fod.exp_dist, 0.01);
        Assertions.assertEquals(5.993, fod.exp_radius, 0.01);
        Assertions.assertEquals(1.000, fod.n_img, 0.001);
        Assertions.assertEquals(2.849, fod.opt_inv, 0.001);

        // Test a ray trace
//        RaySeg[] expected = new RaySeg[]{
//                new RaySeg(new Vector3(0.00000000e+00, -3635749788.7098503, 0.00000000e+00), new Vector3(0., 0.34169210791780597, 0.9398119510767493), 10640426514.13607, new Vector3(-0., 0., 1.)),
//                new RaySeg(new Vector3(0., -23.95018415, 2.53899499), new Vector3(0., 0.27421071, 0.96166963), 5.002303876715387, new Vector3(-0., 0.19829001, 0.98014339)),
//                new RaySeg(new Vector3(0., -22.57849885, -0.30044127), new Vector3(0., 0.34230345, 0.93958946), 7.692858280761749, new Vector3(0., -0.02660833, 0.99964594)),
//                new RaySeg(new Vector3(0., -19.94520693, 4.12768727), new Vector3(0., 0.31152002, 0.95023959), 12.447211581007771, new Vector3(-0., 0.39690374, 0.91786024)),
//                new RaySeg(new Vector3(0., -16.06765133, -2.1644795), new Vector3(0., 0.08136805, 0.99668412), 5.030245533347532, new Vector3(0., -0.26461876, 0.9643531)),
//                new RaySeg(new Vector3(0., -15.65835008, 0.04908635), new Vector3(0., 0.08228559, 0.99660879), 7.650632327993517, new Vector3(-0., 0.00626961, 0.99998035)),
//                new RaySeg(new Vector3(0., -15.0288133, -1.47622621), new Vector3(0., 0.25421056, 0.9671489), 2.8727458940940274, new Vector3(0., -0.19457545, 0.98088755)),
//                new RaySeg(new Vector3(0., -14.29853097, 0.90214682), new Vector3(0., 0.19548399, 0.98070689), 9.806035736108708, new Vector3(-0., 0.12568701, 0.99206995)),
//                new RaySeg(new Vector3(0., -12.38160795, -0.43100635), new Vector3(0., 0.42460646, 0.90537802), 1.9669098146840422, new Vector3(0., -0.06953616, 0.99757943)),
//                new RaySeg(new Vector3(0., -11.54644533, 0.94979056), new Vector3(0., 0.32881212, 0.94439536), 9.288367323830421, new Vector3(-0., 0.16341082, 0.98655811)),
//                new RaySeg(new Vector3(0., -8.4923176, -0.01831846), new Vector3(0., 0.52668396, 0.85006118), 0.396182105669868, new Vector3(0., -0.00431411, 0.99999069)),
//                new RaySeg(new Vector3(0., -8.28365484, 0.11846056), new Vector3(0., 0.34220086, 0.93962683), 8.223855304588305, new Vector3(-0., 0.02859519, 0.99959107)),
//                new RaySeg(new Vector3(0., -5.46944451, -0.15418437), new Vector3(0., 0.308619, 0.95118574), 3.3266936194814365, new Vector3(0., -0.0563355, 0.99841189)),
//                new RaySeg(new Vector3(0., -4.44276366, 0.21011916), new Vector3(0., 0.46365344, 0.88601664), 9.582078331237566, new Vector3(-0., 0.09437829, 0.99553641)),
//                new RaySeg(new Vector3(0.00000000e+00, -4.464396993270628e-08, 0.00000000e+00), new Vector3(0., 0.46365344341486237, 0.8860166389010661), 5.925799449326848, new Vector3(-0., 0., 1.)),
//                new RaySeg(new Vector3(0., 2.74751728, -0.03964309), new Vector3(0., 0.29919181, 0.95419299), 2.5029036780746132, new Vector3(0., 0.02885138, 0.99958371)),
//                new RaySeg(new Vector3(0., 3.49636556, 0.14861005), new Vector3(0., 0.32920781, 0.94425749), 11.967214785345057, new Vector3(-0., -0.084855, 0.99639331)),
//                new RaySeg(new Vector3(0., 7.43606616, -0.1012577), new Vector3(0., 0.47848744, 0.87809439), 0.8013660514588059, new Vector3(0., 0.02722916, 0.99962922)),
//                new RaySeg(new Vector3(0., 7.81950975, 0.40241734), new Vector3(0., 0.20114341, 0.97956181), 8.823788024758601, new Vector3(-0., -0.10265461, 0.99471706)),
//                new RaySeg(new Vector3(0., 9.59435653, -0.45413693), new Vector3(0., 0.29441994, 0.95567615), 0.9626656586072206, new Vector3(0., 0.09445589, 0.99552905)),
//                new RaySeg(new Vector3(0., 9.87778449, 0.26585968), new Vector3(0., 0.12478787, 0.99218345), 6.381136241513929, new Vector3(-0., -0.05143318, 0.99867644)),
//                new RaySeg(new Vector3(0., 10.67407286, -0.85288258), new Vector3(0., 0.12055377, 0.9927068), 3.8085690823354215, new Vector3(0., 0.15879075, 0.98731226)),
//                new RaySeg(new Vector3(0., 11.13321022, 1.12790984), new Vector3(0., 0.37276772, 0.92792469), 2.7996917389269838, new Vector3(-0., -0.20056225, 0.97968096)),
//                new RaySeg(new Vector3(0., 12.17684492, 1.04581293), new Vector3(0., 0.11126818, 0.99379042), 4.6305622838414315, new Vector3(-0., -0.170513, 0.98535543)),
//                new RaySeg(new Vector3(0., 12.69207917, -0.70237865), new Vector3(0., 0.11136852, 0.99377918), 4.429743067228947, new Vector3(0., 0.11034192, 0.99389369)),
//                new RaySeg(new Vector3(0., 13.1854131, 1.88980777), new Vector3(0., 0.41738482, 0.90872983), 0.6280719531302639, new Vector3(-0., -0.28088135, 0.9597425)),
//                new RaySeg(new Vector3(0., 13.4475608, 1.66055549), new Vector3(0., 0.09298565, 0.99566745), 6.7891599551877855, new Vector3(-0., -0.24325828, 0.96996155)),
//                new RaySeg(new Vector3(0., 14.07885527, -0.68969894), new Vector3(0., 0.09266914, 0.99569696), 5.902738301769976, new Vector3(0., 0.097742, 0.99521179)),
//                new RaySeg(new Vector3(0., 14.62585694, 2.18763963), new Vector3(0., 0.44345013, 0.89629905), 13.73688884020393, new Vector3(-0., -0.30889122, 0.95109737)),
//                new RaySeg(new Vector3(0., 20.7174821, 0.), new Vector3(0., 0.292359, 0.95630864), 1.673100014808453, new Vector3(-0., -0., 1.0)),
//                new RaySeg(new Vector3(0., 21.20662794, 0.), new Vector3(0., 0.44345013, 0.89629905), 1.1156990554757744, new Vector3(-0., -0., 1.)),
//                new RaySeg(new Vector3(0., 21.70138483624297, 0.), new Vector3(0., 0.4434501309129979, 0.8962990468550369), 0.0, new Vector3(-0., -0., 1.))
//        };
//
//        RayTraceOptions options = new RayTraceOptions();
//        options.first_surf = 1;
//        options.last_surf = sm.get_num_surfaces() - 2;
//        RayPkg raypkg = RayTrace.trace_raw(sm.path(587.5618, null, null, 1),
//                new Vector3(0, -3635749788.7098503, 0.0),
//                new Vector3(0.0, 0.34169210791780597, 0.9398119510767493), 587.5618, options);
//
//        Assertions.assertTrue(compare(expected[0], raypkg.ray.get(0)));
//        Assertions.assertTrue(compare(expected[14], raypkg.ray.get(14)));
//        Assertions.assertTrue(compare(expected[31], raypkg.ray.get(31)));
//
        double cr_expected_op_delta[] = { 129.68211720000002, 144.5443500746337 };
        RaySeg[] cr_expected_final_rayseg = {
            new RaySeg(new Vector3(0., 0., 0.), new Vector3(0., 0., 1.), 0.0, new Vector3(-0., -0., 1.)),
            new RaySeg(new Vector3(0., 20.362505270553246, 0.), new Vector3(0., 0.4476132717362105, 0.8942272412343552), 0.0, new Vector3(-0., -0., 1.))
        };

        for (int fi = 1; fi < osp.fov.fields.length; fi++) {
            var fld = osp.fov.fields[fi];
            var wvl = sm.central_wavelength();
            var foc = osp.defocus().get_focus();

            var t = Trace.setup_pupil_coords(opm,fld,wvl,foc,null,null);
            Assertions.assertEquals(cr_expected_op_delta[fi],t.chief_ray_pkg.chief_ray.op_delta,1e-8);
            Assertions.assertTrue(compare(Lists.get(t.chief_ray_pkg.chief_ray.ray,-1),cr_expected_final_rayseg[fi]));
            fld.chief_ray = t.chief_ray_pkg;
            fld.ref_sphere = t.ref_sphere;
        }
//
//        var result = Trace.trace_boundary_rays(opm,new TraceOptions());
//        System.out.println(result);
    }

    static boolean compare(RaySeg s1, RaySeg s2) {
        return s1.p.effectivelyEqual(s2.p)
                && s1.d.effectivelyEqual(s2.d)
                && Math.abs(s1.dst - s2.dst) < 1e-13;
    }
}
