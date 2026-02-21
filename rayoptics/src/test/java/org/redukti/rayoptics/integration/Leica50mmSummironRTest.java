package org.redukti.rayoptics.integration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.Trace;
import org.redukti.rayoptics.raytr.VigCalc;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.seq.SurfaceData;
import org.redukti.rayoptics.specs.*;
import org.redukti.rayoptics.util.Pair;

public class Leica50mmSummironRTest {

    @Test
    public void test() {
        OpticalModel opm = new OpticalModel();
        SequentialModel sm = opm.seq_model;
        OpticalSpecs osp = opm.optical_spec;
        osp.pupil = new PupilSpec(osp, new Pair<>(ImageKey.Image, ValueKey.Fnum), 2.0);
        osp.fov = new FieldSpec(osp, new Pair<>(ImageKey.Object, ValueKey.Angle), 22.5, new double[]{0., 1.}, true, true);
        osp.wvls = new WvlSpec(
                new WvlWt[]{new WvlWt(587.5618, 1.0)},
                0);
        opm.system_spec.title = "Leica Summicron R 50mm f/2)";
        opm.system_spec.dimensions = "mm";
        opm.radius_mode = true;
        sm.gaps.get(0).thi = 1e10;
        sm.add_surface(new SurfaceData(42.71,3.99)
                .rindex(1.73430, 28.19)
                .max_aperture(14.47));
        sm.add_surface(new SurfaceData(195.38,0.2)
                .max_aperture(13.53));
        sm.add_surface(new SurfaceData(20.5,7.18)
                .rindex(1.67133, 41.64)
                .max_aperture(12.01));
        sm.add_surface(new SurfaceData(0.0,1.29)
                .rindex(1.79190, 25.55)
                .max_aperture(10.745));
        sm.add_surface(new SurfaceData(14.94,5.35)
                .max_aperture(9.195));
        sm.add_surface(new SurfaceData(0.0,7.61)
                .max_aperture(9.0295));
        sm.set_stop();
        sm.add_surface(new SurfaceData(-14.94,1.0)
                .rindex(1.65222, 33.60)
                .max_aperture(8.75));
        sm.add_surface(new SurfaceData(0.0,5.22)
                .rindex(1.79227, 47.15)
                .max_aperture(9.635));
        sm.add_surface(new SurfaceData(-20.5,0.2)
                .max_aperture(10.19));
        sm.add_surface(new SurfaceData(0.0,3.69)
                .rindex(1.79227, 47.15)
                .max_aperture(11.48));
        sm.add_surface(new SurfaceData(-42.71,37.32)
                .max_aperture(11.985));
        sm.do_apertures = false;
        opm.update_model();
        //if (osp.fov.is_wide_angle)
        VigCalc.set_vig(opm);
        //VigCalc.set_stop_aperture(opm);
        //else
        //    Trace.apply_paraxial_vignetting(opm);
        opm.update_model();
        System.out.println(sm.list_surfaces(new StringBuilder()).toString());
        System.out.println(sm.list_gaps(new StringBuilder()).toString());
        System.out.println(osp.list_str(new StringBuilder()).toString());
        var fod = opm.optical_spec.parax_data.fod;
        System.out.println(fod.toString());

        /* python ray-optics
        aperture: image f/#; value=   2.00000
        field: object angle; value=   22.5000
        y =  0.000 ( 0.00) vlx= 0.000 vux= 0.000 vly= 0.000 vuy= 0.000
        y = 22.500 ( 1.00) vlx= 0.189 vux= 0.189 vly= 0.559 vuy= 0.648
        is_relative=True, is_wide_angle=True
        central wavelength=  587.5618 nm
        wavelength (weight) =  587.5618 (1.000)*
        focus shift=0.0

        aim_info:  20.2096230285742
        aim_info:  22.012190023680645
        efl               52.02
        f                 52.02
        f'                52.02
        ffl              -23.89
        pp1               28.13
        bfl               37.36
        ppk              -14.66
        pp sep           -7.058
        f/#                   2
        m            -5.202e-09
        red          -1.922e+08
        obj_dist          1e+10
        obj_ang            22.5
        enp_dist          20.21
        enp_radius           13
        na obj          1.3e-09
        n obj                 1
        img_dist          37.36
        img_ht            21.55
        exp_dist         -23.96
        exp_radius        15.34
        na img            -0.25
        n img                 1
        optical invariant        5.386
         */

        Assertions.assertEquals( 5.386,fod.opt_inv,1e-3);
        var fov = osp.fov;
        Assertions.assertEquals(20.2096230285742,fov.fields[0].z_enp,1e-13);
        Assertions.assertEquals(22.012190023680645,fov.fields[1].z_enp,1e-5);
    }
}