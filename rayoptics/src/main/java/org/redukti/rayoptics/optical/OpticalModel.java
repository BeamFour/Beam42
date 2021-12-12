package org.redukti.rayoptics.optical;

import org.redukti.rayoptics.parax.ParaxialModel;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.specs.OpticalSpecs;

public class OpticalModel {

    public SequentialModel sequential_model;
    public OpticalSpecs optical_spec;
    public ParaxialModel parax_model;

    public OpticalModel() {
        sequential_model = new SequentialModel();
        optical_spec = new OpticalSpecs();
        parax_model = new ParaxialModel();
    }

}
