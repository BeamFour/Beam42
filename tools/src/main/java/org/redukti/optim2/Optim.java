package org.redukti.optim2;

import org.redukti.spec.Prescription;

public class Optim {

    public static int run(Prescription prescription, Var[] vars, Goal[] outs, double[] fields) {
        var f = new LMDerMeritFunction(new Analysis(prescription, fields), vars, outs);
        var solver = f.getSolver();
        return solver.solve();
    }

}
