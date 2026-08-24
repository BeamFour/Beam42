package org.redukti.mathlib;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestHybrd implements MinPack.Hybrd_Function {

    @Test
    public void test() {
        int j, n, maxfev, ml, mu, mode, nprint, info, ldfjac, lr;
        int[] nfev = new int[1];
        double xtol, epsfcn, factor, fnorm;
        double[] x = new double[9],
                fvec = new double[9],
                diag = new double[9],
                fjac = new double[9 * 9],
                r = new double[45],
                qtf = new double[9],
                wa1 = new double[9],
                wa2 = new double[9],
                wa3 = new double[9],
                wa4 = new double[9];

        n = 9;

        /*      the following starting values provide a rough solution. */

        for (j = 1; j <= 9; j++) {
            x[j - 1] = -1.;
        }

        ldfjac = 9;
        lr = 45;

        /*      set xtol to the square root of the machine precision. */
        /*      unless high solutions are required, */
        /*      this is the recommended setting. */

        xtol = Math.sqrt(MinPack.dpmpar(1));

        maxfev = 2000;
        ml = 1;
        mu = 1;
        epsfcn = 0.;
        mode = 2;
        for (j = 1; j <= 9; j++) {
            diag[j - 1] = 1.;
        }

        factor = 1.e2;
        nprint = 0;

        info = MinPack.hybrd(this, n, x, fvec, xtol, maxfev, ml, mu, epsfcn,
                diag, mode, factor, nprint, nfev,
                fjac, ldfjac, r, lr, qtf, wa1, wa2, wa3, wa4);
        fnorm = MinPack.enorm(n, 0, fvec);

        Assertions.assertEquals(0.1192636E-07, fnorm, 1e-7);
        Assertions.assertEquals(14, nfev[0]);
        Assertions.assertEquals(1, info);

        double[] expected = {
                -0.5706545, -0.6816283, -0.7017325,
                -0.7042129, -0.7013690, -0.6918656,
                -0.6657920, -0.5960342, -0.4164121
        };
        for (int i = 0; i < x.length; i++) {
            Assertions.assertEquals(expected[i], x[i], 1e-7);
        }

        System.out.println(String.format("     final l2 norm of the residuals %15.7g\n\n", (double) fnorm));
        System.out.println(String.format("     number of function evaluations  %10d\n\n", nfev[0]));
        System.out.println(String.format("     exit parameter                  %10d\n\n", info));
        System.out.println("     final approximate solution\n");
        for (j = 1; j <= n; j++) {
            System.out.print(String.format("%s%15.7g", j % 3 == 1 ? "\n     " : "", (double) x[j - 1]));
        }
        System.out.println();
    }

    @Override
    public void apply(int n, double[] x, double[] fvec, int[] iflag) {
        /*      subroutine fcn for hybrd example. */

        int k;
        double temp, temp1, temp2;
        assert (n == 9);

        if (iflag[0] == 0) {
            /*      insert print statements here when nprint is positive. */
    /* if the nprint parameter to lmder is positive, the function is
       called every nprint iterations with iflag=0, so that the
       function may perform special operations, such as printing
       residuals. */
            return;
        }

        /* compute residuals */
        for (k = 1; k <= n; k++) {
            temp = (3 - 2 * x[k - 1]) * x[k - 1];
            temp1 = 0;
            if (k != 1) {
                temp1 = x[k - 1 - 1];
            }
            temp2 = 0;
            if (k != n) {
                temp2 = x[k + 1 - 1];
            }
            fvec[k - 1] = temp - temp1 - 2 * temp2 + 1;
        }
    }

}
