package org.redukti.optim;

import org.redukti.mathlib.MinPack;

import java.util.Arrays;

public class LMDerSolver implements Solver {
    private Analysis analysis;
    /** number of vars in lmder parlance */
    private Var[] vars;
    /** number of functions in lmder parlance */
    private Goal[] functions;
    private boolean use_native = false;

    public LMDerSolver(Analysis analysis, Var[] vars, Goal[] functions, boolean use_native) {
        this.analysis = analysis;
        this.vars = vars;
        this.functions = functions;
        this.use_native = use_native;
    }

    @Override
    public int solve() {
        var fcn = new LMDerMeritFunction(analysis, vars, functions, use_native);
        int m = functions.length;           // number of functions
        int n = vars.length;                // number of variables, must not exceed m
        if (m < n)
            throw new IllegalArgumentException("Number of goals must be >= number of variables");

        // Setup initial solution vector - this is just read from the values in
        // the prescription
        double[] x  = new double[n];
        for (int i = 0; i < x.length; i++) {
            x[i] = vars[i].read_from_prescription();
        }
        double[] diag = new double[n];
        Arrays.fill(diag, 1.0);

        int info = 0;
        if (use_native) {
//            try {
//                info = MinpackFFM.lmder(fcn, m, n, x, diag,2);
//            }
//            catch (Throwable t) {
//                info = -99;
//            }
        }
        else {
            double[] fvec = new double[m];      // Results of goals
            double[] fjac = new double[m * n];    // Space for jacobian
            int ldfjac = m;
            double ftol = Math.sqrt(MinPack.dpmpar(1));
            double xtol = Math.sqrt(MinPack.dpmpar(1));
            double gtol = 0.;
            int maxfev = (n + 1) * 100;
            int mode = 2; // scale using diag
            double factor = 100;
            int nprint = 0;

            int[] nfev = new int[1], njev = new int[1];
            int[] ipvt = new int[n];
            double epsfcn = 0.0;

            double[] qtf = new double[n];
            double[] wa1 = new double[n];
            double[] wa2 = new double[n];
            double[] wa3 = new double[n];
            double[] wa4 = new double[m];
            info = MinPack.lmder(fcn, m, n, x, fvec, fjac, ldfjac,
                    ftol, xtol, gtol, maxfev, diag, mode, factor, nprint,
                    nfev, njev, ipvt, qtf, wa1, wa2, wa3, wa4, epsfcn);
        }

        // Set final solution vector
        for (int i = 0; i < x.length; i++) {
            vars[i].set_scaled_value(x[i]);
            // Update prescription
            vars[i].write_to_prescription();
        }

        return info;
    }}
