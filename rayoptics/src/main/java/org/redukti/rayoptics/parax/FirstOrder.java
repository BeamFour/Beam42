package org.redukti.rayoptics.parax;

import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.seq.SequentialModel;

public class FirstOrder {
    /**
     * Returns paraxial axial and chief rays, plus first order data.
     *
     * @param opt_model
     * @param stop
     * @param wvl
     */
    public void compute_first_order(OpticalModel opt_model, int stop, double wvl) {
        SequentialModel seq_model = opt_model.seq_model;
        int start = 1;
        double n_0 = seq_model.z_dir.get(start - 1).value * seq_model.central_rndx(start - 1);
        double uq0 = 1.0/n_0;

    }
}
