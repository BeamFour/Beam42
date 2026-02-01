package org.redukti.optim;

import org.redukti.mathlib.LMLSolver;

public class LMBeamSolver implements Solver {

    LMLSolver _solver;
    public LMBeamSolver(LMLSolver solver) {
        this._solver = solver;
    }

    public int solve() {
        int istatus = 0;
        while (istatus!= LMLSolver.BADITER &&
                istatus!= LMLSolver.LEVELITER &&
                istatus!= LMLSolver.MAXITER) {
            istatus = _solver.iLMiter();
        }
        return istatus;
    }
}
