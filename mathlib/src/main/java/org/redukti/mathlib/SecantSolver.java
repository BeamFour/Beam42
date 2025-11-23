package org.redukti.mathlib;

public class SecantSolver {

    public interface ObjectiveFunction {
        double eval(double x);
    }

    public static double find_root(ObjectiveFunction f, double x0, int maxiter, double tol) {
        double p0 = x0;
        double eps = 1e-4;
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
        for (int i = 0; i < maxiter; i++) {
            double p;
            if (q1 == q0) {
                return (p1 + p0) / 2.0;
            } else {
                if (Math.abs(q1) > Math.abs(q0)) {
                    p = (-q0 / q1 * p1 + p0) / (1.0 - q0 / q1);
                } else {
                    p = (-q1 / q0 * p0 + p1) / (1.0 - q1 / q0);
                }
                if (Math.abs(p - p1) < tol) {
                    return p;
                }
            }
            p0 = p1;
            q0 = q1;
            p1 = p;
            q1 = f.eval(p1);
        }
        throw new IllegalStateException();
    }

}
