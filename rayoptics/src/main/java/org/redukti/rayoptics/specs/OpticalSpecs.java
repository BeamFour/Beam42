package org.redukti.rayoptics.specs;

import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.parax.ParaxialModel;

public class OpticalSpecs {

    public PupilSpec pupil;
    public FieldSpec field_of_view;
    public WvlSpec spectral_region;
    public ParaxialModel parax_model;
    public OpticalModel opt_model;

    public OpticalSpecs(OpticalModel opt_model) {
        this.opt_model = opt_model;
    }

    public void update_model() {
        spectral_region.update_model();
        pupil.update_model();
        field_of_view.update_model();
        Integer stop = opt_model.seq_model.stop_surface;
        double wvl = spectral_region.central_wvl();

        if (opt_model.seq_model.get_num_surfaces() > 2) {

        }
    }
}
