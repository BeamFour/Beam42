package org.redukti.rayoptics.parax;

import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.seq.SequentialModel;

public class ParaxialModel {
    OpticalModel opt_model;
    SequentialModel seq_model;
    double opt_inv;

    public ParaxialModel(OpticalModel opt_model, SequentialModel seq_model) {
        this.opt_model = opt_model;
        this.seq_model = seq_model;
        this.opt_inv = 1.0;

    }

}
