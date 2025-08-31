package org.redukti.jfotoptix.optim;

import org.redukti.jfotoptix.spec.Prescription;

public class VarAoV extends Var {
    public VarAoV(Prescription prescription, double originalValue, double dDelta) {
        super(prescription, originalValue, dDelta);
    }

    @Override
    public void shift(double delta) {
        //System.out.println("Shifting AOV from " + originalValue + " to " + (originalValue+delta));
        prescription._var_angle_of_view = originalValue + delta;
    }

    @Override
    public String toString() {
        return "AOV : " + prescription._var_angle_of_view;
    }

}
