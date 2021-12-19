package org.redukti.rayoptics.raytr;

import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.specs.Field;

public class Trace {

    public static void aim_chief_ray(OpticalModel opt_model, Field fld, Double wvl) {
        // aim chief ray at center of stop surface and save results on **fld**
        SequentialModel seq_model = opt_model.seq_model;
        if (wvl == null)
            wvl = seq_model.central_wavelength();
        Integer stop = seq_model.stop_surface;
        //aim_pt = iterate_ray(opt_model, stop, np.array([0., 0.]), fld, wvl)
        //return aim_pt;
    }
}
