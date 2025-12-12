package org.redukti.optim;

import org.redukti.mathlib.LMLSolver;
import org.redukti.spec.Prescription;

public class Optim {

    public static void run(Prescription prescription, Var[] vars, Goal[] outs) {

        var f = new MeritFunction(new Analysis(prescription), vars, outs);
        var solver = f.getSolver();
        int istatus = 0;
        while (istatus!= LMLSolver.BADITER &&
                istatus!= LMLSolver.LEVELITER &&
                istatus!= LMLSolver.MAXITER) {
            istatus = solver.iLMiter();
        }

    }

}
