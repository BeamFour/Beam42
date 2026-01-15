package org.redukti.rayoptics.integration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.elem.profiles.EvenPolynomial;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.parax.FirstOrderData;
import org.redukti.rayoptics.raytr.*;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.seq.SurfaceData;
import org.redukti.rayoptics.specs.*;
import org.redukti.rayoptics.util.Lists;
import org.redukti.rayoptics.util.Pair;

public class Nikkor14mmTest {

    @Test
    public void test() {
        OpticalModel opm = new OpticalModel();
        SequentialModel sm = opm.seq_model;
        OpticalSpecs osp = opm.optical_spec;
        osp.pupil = new PupilSpec(osp, new Pair<>(ImageKey.Image, ValueKey.Fnum), 4.0);
        osp.fov = new FieldSpec(osp, new Pair<>(ImageKey.Object, ValueKey.Angle), new double[]{0., 57.68}, true);
        osp.wvls = new WvlSpec(
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
        FirstOrderData fod = osp.parax_data.fod;
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

        double[] cr_expected_op_delta = { 129.68211720000002, 144.54435163923452 };
        RaySeg[] cr_expected_final_rayseg = {
            new RaySeg(new Vector3(0., 0., 0.), new Vector3(0., 0., 1.), 0.0, new Vector3(-0., -0., 1.)),
            new RaySeg(new Vector3(0., 20.362505270545146, 0.), new Vector3(0., 0.4476133316595901, 0.8942272112391799), 0.0, new Vector3(-0., -0., 1.))
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

        var result = Wideangle.eval_real_image_ht(opm,osp.fov.fields[1],587.5618);
        var pt = new Vector3(0.0, -2866312975.4227800369262695,  419590299.1519107818603516);
        var dir = new Vector3(-0.,  0.2866312938130761,  0.9580409706307147);
        Assertions.assertEquals(130.10449270101637,result.z_enp,1e-5);
        Assertions.assertTrue(pt.isEqual(result.ray_data.pt,1e-7));
        Assertions.assertTrue(dir.isEqual(result.ray_data.dir,1e-7));
    }

    static boolean compare(RaySeg s1, RaySeg s2) {
        return s1.p.effectivelyEqual(s2.p)
                && s1.d.effectivelyEqual(s2.d)
                && Math.abs(s1.dst - s2.dst) < 1e-13;
    }
}
