package org.redukti.rayoptics.specs;

import org.redukti.rayoptics.math.Vector3;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.parax.FirstOrder;
import org.redukti.rayoptics.parax.ParaxData;
import org.redukti.rayoptics.raytr.Trace;

public class OpticalSpecs {

    public static boolean do_aiming_default = true;

    public PupilSpec pupil;
    public FieldSpec field_of_view;
    public WvlSpec spectral_region;
    public ParaxData parax_data;
    public OpticalModel opt_model;
    public boolean do_aiming;

    public OpticalSpecs(OpticalModel opt_model) {
        this.opt_model = opt_model;
        this.do_aiming = OpticalSpecs.do_aiming_default;
    }

    public void update_model() {
        spectral_region.update_model();
        pupil.update_model();
        field_of_view.update_model();
        Integer stop = opt_model.seq_model.stop_surface;
        double wvl = spectral_region.central_wvl();

        if (opt_model.seq_model.get_num_surfaces() > 2) {
            parax_data = FirstOrder.compute_first_order(opt_model, stop, wvl);
            if (do_aiming) {
                for (int i = 0; i < field_of_view.fields.length; i++) {
                    Field fld = field_of_view.fields[i];
                    double[] aim_pt = Trace.aim_chief_ray(opt_model, fld, wvl);
                    fld.aim_pt = aim_pt;
                }
            }
        }
    }

    public Vector3 obj_coords(Field fld) {
        return field_of_view.obj_coords(fld);
    }
}
