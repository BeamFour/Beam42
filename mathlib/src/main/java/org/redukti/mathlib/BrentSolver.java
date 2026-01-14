package org.redukti.mathlib;

public class BrentSolver {
    static final double TOL         = 5e-14;

    public static RootResult find_root(double a, double b, ScalarObjectiveFunction fn)
    //  Given a bracket (t[0], t[1]), Brent() sets t[0] to root,
    //  and returns the number of calls to zDiff() taken.
    //  Press et al NUMERICAL RECIPES IN C 2nd edition 1992 p.361
    //  R.P.Brent ALGORITHMS... Prentice-Hall, NJ 1973.
    {
        int MAXIT = 50;
        double c, d=0, e=0, min1, min2;
        double fc, p, q, r, s, toler, xm, fa, fb;
        c=b;
        fa=fn.eval(a);
        if (fa == 0.)
            return new RootResult(a,true,0);
        fb=fn.eval(b);
        if (fb == 0.)
            return new RootResult(b, true, 0);
        if (fa*fb > 0)
            // bad starting bracket
            return new RootResult(0.,false,0);
        fc = fb;
        for (int iter=1; iter<MAXIT; iter++)
        {
            if (fb*fc > 0)
            {
                c = a;
                fc = fa;
                e = d = b-a;
            }
            if (Math.abs(fc) < Math.abs(fb))
            {
                a = b;
                b = c;
                c = a;
                fa = fb;
                fb = fc;
                fc = fa;
            }
            toler = 2.0 * TOL * Math.abs(b) + TOL;
            xm = 0.5*(c-b);
            if ((Math.abs(xm) <= toler) || (fb == 0.0))
                return new RootResult(b, true, iter);
            if ((Math.abs(e) >= toler) && (Math.abs(fa) > Math.abs(fb)))
            {
                s = fb/fa;
                if (a == c)
                {
                    p = 2.0*xm*s;
                    q = 1.0-s;
                }
                else
                {
                    q = fa/fc;
                    r = fb/fc;
                    p = s*(2.0*xm*q*(q-r) - (b-a)*(r-1.0));
                    q = (q-1.0)*(r-1.0)*(s-1.0);
                }
                if (p > 0.0)
                    q = -q;
                p = Math.abs(p);
                min1 = 3.0*xm*q - Math.abs(toler*q);
                min2 = Math.abs(e*q);
                if (2.0*p < (min1 < min2 ? min1 : min2))
                {
                    e = d;
                    d = p/q;
                }
                else
                {
                    d = xm;
                    e = d;
                }
            }
            else
            {
                d = xm;
                e = d;
            }
            a = b;
            fa = fb;
            if (Math.abs(d) > toler)
                b += d;
            else
                b += (xm > 0.0) ? Math.abs(toler) : -Math.abs(toler);
            fb = fn.eval(b);
        }
        return new RootResult(b, false, MAXIT); // SNH
    }

}
