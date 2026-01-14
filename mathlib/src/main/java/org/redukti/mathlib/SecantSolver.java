package org.redukti.mathlib;

public class SecantSolver {

    public static RootResult find_root(ScalarObjectiveFunction f, double x0, int maxiter, double tol) {
        final double eps = 1e-4;
        double p0 = x0;
        double p1 = x0 * (1 + eps);
        p1 += (p1 >= 0 ? eps : -eps);
        double q0 = f.eval(p0);
        double q1 = f.eval(p1);
        if (Math.abs(q1) < Math.abs(q0)) {
            double tmp = p0;
            p0 = p1;
            p1 = tmp;
            tmp = q0;
            q0 = q1;
            q1 = tmp;
        }
        double p = 0.0;
        for (int i = 0; i < maxiter; i++) {
            double diff = Math.abs(q1 - q0);
            if (diff < tol) {
                return new RootResult((p1 + p0) / 2.0, false, i);
            } else {
                if (Math.abs(q1) > Math.abs(q0)) {
                    p = (-q0 / q1 * p1 + p0) / (1.0 - q0 / q1);
                } else {
                    p = (-q1 / q0 * p0 + p1) / (1.0 - q1 / q0);
                }
                if (Math.abs(p - p1) < tol) {
                    return new RootResult(p, true, i);
                }
            }
            p0 = p1;
            q0 = q1;
            p1 = p;
            q1 = f.eval(p1);
        }
        return new RootResult(p,false,maxiter);
    }

}
