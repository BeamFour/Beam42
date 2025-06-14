package org.redukti.jfotoptix.optim;

import org.redukti.jfotoptix.math.LMLSolver;
import org.redukti.jfotoptix.spec.Prescription;

public class Optim {

    public static void run(Prescription prescription, In[] vars, Out[] outs) {

        var f = new MeritFunction(prescription, vars, outs);
        var solver = f.getSolver();
        int istatus = 0;
        while (istatus!= LMLSolver.BADITER &&
                istatus!= LMLSolver.LEVELITER &&
                istatus!= LMLSolver.MAXITER) {
            istatus = solver.iLMiter();
        }

    }

}
