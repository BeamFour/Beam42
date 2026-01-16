package org.redukti.rayoptics.integration;

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
//        osp.wvls = new WvlSpec(
//                new WvlWt[]{new WvlWt(486.1327, 1.0),
//                new WvlWt(587.5618, 1.0),
//                new WvlWt(656.2725, 1.0)},
//                1);
        osp.wvls = new WvlSpec(
                new WvlWt[]{new WvlWt(587.5618, 1.0)},
                0);

        opm.system_spec.title = "Leica Summicron R 50mm f/2)";
        opm.system_spec.dimensions = "mm";
        opm.radius_mode = true;
        sm.gaps.get(0).thi = 1e10;
        sm.add_surface(new SurfaceData(42.71,3.99)
                .rindex(1.73430, 28.19, "N-SF10","Schott")
                .max_aperture(14.47));
        sm.add_surface(new SurfaceData(195.38,0.2)
                .max_aperture(13.53));
        sm.add_surface(new SurfaceData(20.5,7.18)
                .rindex(1.67133, 41.64, "J-BASF6","Hikari")
                .max_aperture(12.01));
        sm.add_surface(new SurfaceData(0.0,1.29)
                .rindex(1.79190, 25.55, "N-SF11","Schott")
                .max_aperture(10.745));
        sm.add_surface(new SurfaceData(14.94,5.35)
                .max_aperture(9.195));
        sm.add_surface(new SurfaceData(0.0,7.61)
                .max_aperture(9.0295));
        sm.set_stop();
        sm.add_surface(new SurfaceData(-14.94,1.0)
                .rindex(1.65222, 33.60, "N-SF2","Schott")
                .max_aperture(8.75));
        sm.add_surface(new SurfaceData(0.0,5.22)
                .rindex(1.79227, 47.15, "N-LAF21","Schott")
                .max_aperture(9.635));
        sm.add_surface(new SurfaceData(-20.5,0.2)
                .max_aperture(10.19));
        sm.add_surface(new SurfaceData(0.0,3.69)
                .rindex(1.79227, 47.15, "N-LAF21","Schott")
                .max_aperture(11.48));
        sm.add_surface(new SurfaceData(-42.71,37.32)
                .max_aperture(11.985));
        sm.do_apertures = false;
        opm.update_model();
        //if (osp.fov.is_wide_angle)
        //    VigCalc.set_vig(opm, false);
        VigCalc.set_stop_aperture(opm);
        //else
        //    Trace.apply_paraxial_vignetting(opm);
        opm.update_model();
        System.out.println(sm.list_surfaces(new StringBuilder()).toString());
        System.out.println(sm.list_gaps(new StringBuilder()).toString());
        System.out.println(osp.list_str(new StringBuilder()).toString());
        var fod = opm.optical_spec.parax_data.fod;
        System.out.println(fod.toString());
    }
}