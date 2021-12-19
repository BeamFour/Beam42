package org.redukti.rayoptics.optical;

import org.redukti.rayoptics.elem.ElementModel;
import org.redukti.rayoptics.parax.ParaxialModel;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.specs.OpticalSpecs;
import org.redukti.rayoptics.specs.SystemSpec;

public class OpticalModel {

    public SequentialModel seq_model;
    public OpticalSpecs optical_spec;
    public ParaxialModel parax_model;
    public ElementModel element_model;
    public SystemSpec system_spec;
    public boolean radius_mode;

    public OpticalModel() {
        seq_model = new SequentialModel(this);
        optical_spec = new OpticalSpecs(this);
        parax_model = new ParaxialModel(this, seq_model);
        element_model = new ElementModel();
        system_spec = new SystemSpec();
    }

    public void update_model() {
        seq_model.update_model();
        optical_spec.update_model();
        parax_model.update_model();
    }
}
