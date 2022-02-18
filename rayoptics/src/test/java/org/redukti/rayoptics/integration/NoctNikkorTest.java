package org.redukti.rayoptics.integration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redukti.rayoptics.elem.EvenPolynomial;
import org.redukti.rayoptics.elem.LensLayout;
import org.redukti.rayoptics.elem.RayBundle;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.parax.FirstOrderData;
import org.redukti.rayoptics.parax.ParaxialModel;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.seq.SurfaceData;
import org.redukti.rayoptics.specs.*;
import org.redukti.rayoptics.util.Pair;

import java.util.List;

public class NoctNikkorTest {

    @Test
    public void test() {
        OpticalModel opm = new OpticalModel();
        SequentialModel sm = opm.seq_model;
        OpticalSpecs osp = opm.optical_spec;
        ParaxialModel pm = opm.parax_model;
        osp.pupil = new PupilSpec(osp, new Pair<>("image", "f/#"), 0.98);
        osp.field_of_view = new FieldSpec(osp, new Pair<>("object", "angle"), new double[]{0., 19.98});
        osp.spectral_region = new WvlSpec(new WvlWt[]{new WvlWt(486.1327, 0.5),
                new WvlWt(587.5618, 1.0),
                new WvlWt(656.2725, 0.5)}, 1);
        opm.system_spec.title = "WO2019-229849 Example 1 (Nikkor Z 58mm f/0.95 S)";
        opm.system_spec.dimensions = "MM";
        opm.radius_mode = true;
        sm.gaps.get(0).thi = 1e10;
        sm.add_surface(new SurfaceData(108.488, 7.65)
                .rindex(1.90265, 35.77)
                .max_aperture(33.4)); // 'J-LASFH9A', 'Hikari'
        sm.ifcs.get(sm.cur_surface).profile = new EvenPolynomial()
                .r(108.488)
                .cc(0)
                .coefs(new double[]{0.0, -3.82177e-07, -6.06486e-11, -3.80172e-15, -1.32266e-18, 0, 0});
        sm.add_surface(new SurfaceData(-848.55, 2.8)
                .rindex(1.55298, 55.07)
                .max_aperture(32.91)); // ,'J-KZFH4', 'Hikari'])   #
        sm.add_surface(new SurfaceData(50.252, 18.12)
                .max_aperture(28.97));
        sm.add_surface(new SurfaceData(-60.72, 2.8)
                .rindex(1.61266, 44.46) // ,'J-KZFH1', 'Hikari'])    #
                .max_aperture(29.14));
        sm.add_surface(new SurfaceData(2497.5, 9.15)
                .rindex(1.59319, 67.9) // ,'J-PSKH1', 'Hikari'])   #
                .max_aperture(32.66));
        sm.add_surface(new SurfaceData(-77.239, 0.4)
                .max_aperture(32.66));
        sm.add_surface(new SurfaceData(113.763, 10.95)
                .rindex(1.8485, 43.79) // ,'J-LASFH22', 'Hikari']) #
                .max_aperture(35.45));
        sm.add_surface(new SurfaceData(-178.06, 0.4)
                .max_aperture(35.45));
        sm.add_surface(new SurfaceData(70.659, 9.74)
                .rindex(1.59319, 67.9) //,'J-PSKH1', 'Hikari'])   #
                .max_aperture(32.5));
        sm.add_surface(new SurfaceData(-1968.5, 0.2)
                .max_aperture(32.5));
        sm.add_surface(new SurfaceData(289.687, 8)
                .rindex(1.59319, 67.9) // ,'J-PSKH1', 'Hikari'])     #
                .max_aperture(30.53));
        sm.add_surface(new SurfaceData(-97.087, 2.8)
                .rindex(1.738, 32.33) //,'S-NBH53V', 'Ohara'])   # ])
                //sm.gaps[sm.cur_surface].medium = g738323            # New type J-KZFH9
                .max_aperture(29.71));
        sm.add_surface(new SurfaceData(47.074, 8.7)
                .max_aperture(25.12));
        sm.add_surface(new SurfaceData(0, 5.29)
                .max_aperture(23.959)); // TODO check this is correct for stop
        sm.set_stop();
        sm.add_surface(new SurfaceData(-95.23, 2.2)
                .rindex(1.61266, 44.46) //,'J-KZFH1', 'Hikari'])    #
                .max_aperture(24.96));
        sm.add_surface(new SurfaceData(41.204, 11.55)
                .rindex(1.49782, 82.57) //,'J-FKH1','Hikari'])     #
                .max_aperture(24.96));
        sm.add_surface(new SurfaceData(-273.092, 0.2)
                .max_aperture(24.96));
        sm.add_surface(new SurfaceData(76.173, 9.5)
                .rindex(1.883, 40.69) // ,'J-LASF08A', 'Hikari'])   #
                .max_aperture(25.56));
        sm.add_surface(new SurfaceData(-101.575, 0.2)
                .max_aperture(25.56));
        sm.add_surface(new SurfaceData(176.128, 7.45)
                .rindex(1.95375, 32.33) // ,'J-LASFH21','Hikari'])  #
                .max_aperture(23.4));
        sm.ifcs.get(sm.cur_surface).profile = new EvenPolynomial()
                .r(176.128)
                .cc(0)
                .coefs(new double[]{0.0, -1.15028e-06, -4.51771e-10, 2.7267e-13, -7.66812e-17, 0, 0});
        sm.add_surface(new SurfaceData(-67.221, 1.8)
                .rindex(1.738, 32.33) //,'S-NBH53V', 'Ohara'])   # ])
                //sm.gaps[sm.cur_surface].medium = g738323             # New type J-KZFH9
                .max_aperture(22.68));
        sm.add_surface(new SurfaceData(55.51, 2.68)
                .max_aperture(19.92));
        sm.add_surface(new SurfaceData(71.413, 6.35)
                .rindex(1.883, 40.69) // ,'J-LASF08A', 'Hikari'])    #
                .max_aperture(19.73));
        sm.add_surface(new SurfaceData(-115.025, 1.81)
                .rindex(1.69895, 30.13) //,'J-SF15', 'Hikari'])     #
                .max_aperture(19.73));
        sm.add_surface(new SurfaceData(46.943, 0.8)
                .max_aperture(19.73));
        sm.add_surface(new SurfaceData(55.281,9.11)
                .rindex(1.883,40.69) //,'J-LASF08A', 'Hikari'])    #
                .max_aperture(19.47));
        sm.add_surface(new SurfaceData(-144.041, 3)
                .rindex(1.76554, 46.76) // ,'J-LASFH2','Hikari'])      # 1.76554,46.76])    46.78
                .max_aperture(19.14));
        sm.add_surface(new SurfaceData(52.858, 14.5)
                .max_aperture(19.14));
        sm.ifcs.get(sm.cur_surface).profile = new EvenPolynomial()
                .r(52.858)
                .cc(0)
                .coefs(new double[]{0.0, 3.18645e-06, -1.14718e-08, 7.74567e-11, -2.24225e-13, 3.3479e-16, -1.7047e-19});
        sm.add_surface(new SurfaceData(0, 1.6)
                .rindex(1.5168, 64.14) //,'J-BK7A', 'Hikari'])             #
                .max_aperture(22.15));
        sm.add_surface(new SurfaceData(0, 1)
                .max_aperture(22.15));
        System.out.println(sm.list_surfaces(new StringBuilder()).toString());
        System.out.println(sm.list_gaps(new StringBuilder()).toString());
        sm.do_apertures = false;
        opm.update_model();
        pm.first_order_data();
        FirstOrderData fod = pm.parax_data.fod;
        Assertions.assertEquals(59.62, fod.efl, 0.001);
        Assertions.assertEquals(1.660, fod.ffl, 0.001);
        Assertions.assertEquals(61.28, fod.pp1, 0.001);
        Assertions.assertEquals(11.06, fod.opt_inv, 0.001);

        LensLayout layout = new LensLayout(opm);

        System.out.println("---- elements ----");
        System.out.println(opm.ele_model.list_elements());

        System.out.println("---- ele model ----");
        System.out.println(opm.ele_model.list_model());

        List<RayBundle> rays = layout.create_ray_entities(0.0);
        for (RayBundle ray : rays)
            ray.update_shape();
        return;
    }
}
