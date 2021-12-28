package org.redukti.rayoptics.specs;

import org.redukti.rayoptics.math.Vector3;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.parax.FirstOrder;
import org.redukti.rayoptics.parax.ParaxData;
import org.redukti.rayoptics.raytr.Trace;
import org.redukti.rayoptics.util.Pair;

/**
 * The OpticalSpecs class holds the optical usage definition of the model.
 * Aperture, field of view, wavelength, and focal position are all aspects of
 * the OpticalSpecs.
 * *
 * The first order properties are calculated and maintained by OpticalSpecs
 * in the parax_data variable. This is an instance of ParaxData that includes
 * the paraxial axial and chief rays, and the FirstOrderData that contains
 * first order properties.
 */
public class OpticalSpecs {

    public static boolean do_aiming_default = true;

    /**
     * Aperture specification
     */
    public PupilSpec pupil;
    /**
     * Field of view specification
     */
    public FieldSpec field_of_view;
    /**
     * Wavelengths
     */
    public WvlSpec spectral_region;
    /**
     * Focal position
     */
    public FocusRange focus;

    public ParaxData parax_data;
    public OpticalModel opt_model;
    public boolean do_aiming;

    public OpticalSpecs(OpticalModel opt_model) {
        this.opt_model = opt_model;
        this.spectral_region = new WvlSpec(new WvlWt[]{new WvlWt("d", 1.)}, 0);
        this.pupil = new PupilSpec(this, new Pair<>("object", "pupil"), 1.0);
        this.field_of_view = new FieldSpec(this, new Pair<>("object", "angle"), new double[]{0.});
        this.focus = new FocusRange();
        this.parax_data = null;
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
