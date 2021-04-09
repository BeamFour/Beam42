/* Ported from deriv/deriv.c in GNU Scientific Library
 * Copyright (c) 2021 Dibyendu Majumdar
 *
 * Copyright (C) 2004, 2007 Brian Gough
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.redukti.jfotoptix.math;

public class Derivatives {

    static final double GSL_DBL_EPSILON = 2.2204460492503131e-16;

    static final class EvalResult {
        double result;
        double abserr_round;
        double abserr_trunc;
    }

    static EvalResult central_deriv(DerivFunction f, double x, double h) {
  /* Compute the derivative using the 5-point rule (x-h, x-h/2, x,
     x+h/2, x+h). Note that the central point is not used.

     Compute the error using the difference between the 5-point and
     the 3-point rule (x-h,x,x+h). Again the central point is not
     used. */

        double fm1 = f.apply(x - h);
        double fp1 = f.apply(x + h);

        double fmh = f.apply(x - h / 2);
        double fph = f.apply(x + h / 2);

        double r3 = 0.5 * (fp1 - fm1);
        double r5 = (4.0 / 3.0) * (fph - fmh) - (1.0 / 3.0) * r3;

        double e3 = (Math.abs(fp1) + Math.abs(fm1)) * GSL_DBL_EPSILON;
        double e5 = 2.0 * (Math.abs(fph) + Math.abs(fmh)) * GSL_DBL_EPSILON + e3;

        /* The next term is due to finite precision in x+h = O (eps * x) */

        double dy =
                Math.max(Math.abs(r3 / h), Math.abs(r5 / h)) * (Math.abs(x) / h) * GSL_DBL_EPSILON;

  /* The truncation error in the r5 approximation itself is O(h^4).
     However, for safety, we estimate the error from r5-r3, which is
     O(h^2).  By scaling h we will minimise this estimated error, not
     the actual truncation error in r5. */
        EvalResult result = new EvalResult();
        result.result = r5 / h;
        result.abserr_trunc = Math.abs((r5 - r3) / h); /* Estimated truncation error O(h^2) */
        result.abserr_round = Math.abs(e5 / h) + dy;   /* Rounding error (cancellations) */
        return result;
    }

    public static DerivResult central_derivative(DerivFunction f, double x, double h) {
        double r_0;
        EvalResult res = central_deriv(f, x, h);
        double error = res.abserr_round + res.abserr_trunc;
        r_0 = res.result;

        if (res.abserr_round < res.abserr_trunc && (res.abserr_round > 0 && res.abserr_trunc > 0)) {
            double error_opt;

      /* Compute an optimised stepsize to minimize the total error,
         using the scaling of the truncation error (O(h^2)) and
         rounding error (O(1/h)). */

            double h_opt = h * Math.pow(res.abserr_round / (2.0 * res.abserr_trunc), 1.0 / 3.0);
            res = central_deriv(f, x, h_opt);
            error_opt = res.abserr_round + res.abserr_trunc;

      /* Check that the new error is smaller, and that the new derivative
         is consistent with the error bounds of the original estimate. */

            if (error_opt < error && Math.abs(res.result - r_0) < 4.0 * error) {
                r_0 = res.result;
                error = error_opt;
            }
        }
        return new DerivResult(r_0, error);
    }
}
