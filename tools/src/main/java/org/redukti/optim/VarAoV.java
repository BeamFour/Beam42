package org.redukti.optim;

import org.redukti.spec.Prescription;

public class VarAoV extends Var {
    public VarAoV(Prescription prescription, double originalValue, double dDelta) {
        super(prescription, originalValue, dDelta);
    }

    @Override
    public void shift(double delta, boolean apply_scale) {
        //System.out.println("Shifting AOV from " + originalValue + " to " + (originalValue+delta));
        prescription._var_angle_of_view = originalValue + delta;
    }

    @Override
    public double get_value() {
        return prescription._var_angle_of_view;
    }

    @Override
    public void set_value(double value) {
        prescription._var_angle_of_view = value;
    }

    @Override
    public String toString() {
        return "AOV : " + prescription._var_angle_of_view;
    }

}
