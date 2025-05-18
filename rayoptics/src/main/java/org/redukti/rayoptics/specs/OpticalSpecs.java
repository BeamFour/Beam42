package org.redukti.rayoptics.specs;

import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.parax.firstorder.FirstOrder;
import org.redukti.rayoptics.parax.firstorder.ParaxData;
import org.redukti.rayoptics.raytr.Trace;
import org.redukti.rayoptics.util.Pair;
import org.redukti.rayoptics.util.Triple;

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

    public FocusRange defocus() {
        return focus;
    }

    /* returns field, wavelength and defocus data
    Args:
        fi (int): index into the field_of_view list of Fields
        wl (int): index into the spectral_region list of wavelengths
        fr (float): focus range parameter, -1.0 to 1.0
    Returns:
        (**fld**, **wvl**, **foc**)

        - **fld** - :class:`Field` instance for field_of_view[fi]
        - **wvl** - wavelength in nm
        - **foc** - focus shift from image interface
    */
    public Triple<Field, Double, Double> lookup_fld_wvl_focus(int fi, Integer wl, Double fr) {
        double wvl;
        if (fr == null)
            fr = 0.0;
        if (wl == null)
            wvl = spectral_region.central_wvl();
        else
            wvl = spectral_region.wavelengths[wl];
        Field fld = field_of_view.fields[fi];
        double foc = defocus().get_focus(fr);
        return new Triple<>(fld, wvl, foc);
    }

    public Triple<Field, Double, Double> lookup_fld_wvl_focus(int fi) {
        return lookup_fld_wvl_focus(fi, null, 0.0);
    }
}
