package org.redukti.jfotoptix.optim;

import org.redukti.jfotoptix.spec.Prescription;

public class VarAoV extends Var {
    public VarAoV(Prescription prescription, double originalValue, double dDelta) {
        super(prescription, originalValue, dDelta);
    }

    @Override
    public void shift(double delta) {
        System.out.println("Shifting AOV from " + originalValue + " to " + (originalValue+delta));
        prescription.angleOfViewDegrees = originalValue + delta;
    }

    @Override
    public String toString() {
        return "AOV : " + prescription.angleOfViewDegrees;
    }

}
