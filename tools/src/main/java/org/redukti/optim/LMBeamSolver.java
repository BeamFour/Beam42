package org.redukti.optim;

import org.redukti.mathlib.LMLSolver;

public class LMBeamSolver implements Solver {

    LMLSolver solver;
    public LMBeamSolver(LMLSolver solver) {
        this.solver = solver;
    }

    public int solve() {
        int istatus = 0;
        while (istatus!= LMLSolver.BADITER &&
                istatus!= LMLSolver.LEVELITER &&
                istatus!= LMLSolver.MAXITER) {
            istatus = solver.iLMiter();
        }
        return istatus;
    }
}
