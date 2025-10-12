// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.optical;

import org.redukti.rayoptics.parax.firstorder.ParaxialModel;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.specs.OpticalSpecs;
import org.redukti.rayoptics.specs.SystemSpec;

public class OpticalModel {

    public SequentialModel seq_model;
    public OpticalSpecs optical_spec;
    public ParaxialModel parax_model;
    public SystemSpec system_spec;
    public boolean radius_mode;
    public String dimensions = "mm";

    public OpticalModel() {
        seq_model = new SequentialModel(this);
        optical_spec = new OpticalSpecs(this);
        parax_model = new ParaxialModel(this, seq_model);
        system_spec = new SystemSpec();
        seq_model.update_model();
    }

    public void update_model() {
        seq_model.update_model();
        optical_spec.update_model();
        parax_model.update_model();
    }



    /**
     * convert nm to system units
     * <p>
     * Args:
     * nm (float): value in nm
     * <p>
     * Returns:
     * float: value converted to system units
     *
     * @param nm
     * @return
     */
    public double nm_to_sys_units(double nm) {
        if ("m".equals(dimensions))
            return 1e-9 * nm;
        else if ("cm".equals(dimensions))
            return 1e-7 * nm;
        else if ("mm".equals(dimensions))
            return 1e-6 * nm;
        else if ("in".equals(dimensions))
            return 1e-6 * nm / 25.4;
        else if ("ft".equals(dimensions))
            return 1e-6 * nm / 304.8;
        else
            return nm;
    }
}
