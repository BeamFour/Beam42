package org.redukti.mathlib;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestLmdir implements MinPack.Lmder_Function {

    /*      driver for lmder example. */


/* define the preprocessor symbol BOX_CONSTRAINTS to enable the simulated box constraints
   using a change of variables. */

    /* the following struct defines the data points */
    static final class TestData {
        int m;
        double[] y;
    }

    TestData data;

    @Test
    public void test() {
        int i, j, ldfjac, maxfev, mode, nprint, info;
        int[] nfev = new int[1], njev = new int[1];
        int[] ipvt = new int[3];
        double ftol, xtol, gtol, factor, fnorm;
        double[] x = new double[3];
        double[] fvec = new double[15];
        double[] fjac = new double[15 * 3];
        double[] diag = new double[3];
        double[] qtf = new double[3];
        double[] wa1 = new double[3];
        double[] wa2 = new double[3];
        double[] wa3 = new double[3];
        double[] wa4 = new double[15];
        int k;
        final int m = 15;
        final int n = 3;
        /* auxiliary data (e.g. measurements) */
        double[] y = {1.4e-1, 1.8e-1, 2.2e-1, 2.5e-1, 2.9e-1, 3.2e-1, 3.5e-1,
                3.9e-1, 3.7e-1, 5.8e-1, 7.3e-1, 9.6e-1, 1.34, 2.1, 4.39};

        data = new TestData();
        data.m = m;
        data.y = y;


        /*      the following starting values provide a rough fit. */

        x[0] = 1.;
        x[1] = 1.;
        x[2] = 1.;

        ldfjac = 15;

        /*      set ftol and xtol to the square root of the machine */
        /*      and gtol to zero. unless high solutions are */
        /*      required, these are the recommended settings. */

        ftol = Math.sqrt(MinPack.dpmpar(1));
        xtol = Math.sqrt(MinPack.dpmpar(1));
        gtol = 0.;

        maxfev = 400;
        mode = 1;
        factor = 1.e2;
        nprint = 0;

        info = MinPack.lmder(this, m, n, x, fvec, fjac, ldfjac, ftol, xtol, gtol,
                maxfev, diag, mode, factor, nprint, nfev, njev,
                ipvt, qtf, wa1, wa2, wa3, wa4);

        fnorm = MinPack.enorm(m, 0, fvec);
        Assertions.assertEquals(0.09063596, fnorm, 1e-8);
        Assertions.assertEquals(6, nfev[0]);
        Assertions.assertEquals(5, njev[0]);
        Assertions.assertEquals(1, info);
        Assertions.assertEquals(0.08241058, x[0], 1e-6);
        Assertions.assertEquals(1.133037, x[1], 1e-6);
        Assertions.assertEquals(2.343695, x[2], 1e-6);

        System.out.println(String.format("      final l2 norm of the residuals%15.7g\n\n", (double) fnorm));
        System.out.println(String.format("      number of function evaluations%10d\n\n", nfev[0]));
        System.out.println(String.format("      number of Jacobian evaluations%10d\n\n", njev[0]));
        System.out.println(String.format("      exit parameter                %10d\n\n", info));
        System.out.println("      final approximate solution\n");
        for (j = 0; j < n; ++j) {
            System.out.println(String.format("%s%15.7g", j % 3 == 0 ? "\n     " : "", (double) x[j]));
        }
        System.out.println("\n");
        ftol = MinPack.dpmpar(1);

//            /* test covar1, which also estimates the rank of the Jacobian */
//            k = __cminpack_func__(covar1)(m, n, fnorm*fnorm, fjac, ldfjac, ipvt, ftol, wa1);
//        printf("      covariance\n");
//        for (i=0; i<n; ++i) {
//            for (j=0; j<n; ++j) {
//
//                printf("%s%15.7g", j%3==0?"\n     ":"", (double)fjac[i*ldfjac+j]);
//            }
//        }
//        printf("\n");
//        (void)k;

        /* printf("      rank(J) = %d\n", k != 0 ? k : n); */
    }

    @Override
    public int apply(int m, int n, double[] x, double[] fvec, double[] fjac, int ldfjac, int iflag) {

        /*      subroutine fcn for lmder example. */

        int i;
        double tmp1, tmp2, tmp3, tmp4;
        final double[] y = data.y;
        assert (m == 15 && n == 3);

        if (iflag == 0) {
            /*      insert print statements here when nprint is positive. */
    /* if the nprint parameter to lmder is positive, the function is
       called every nprint iterations with iflag=0, so that the
       function may perform special operations, such as printing
       residuals. */
            return 0;
        }

        if (iflag != 2) {
            /* compute residuals */
            for (i = 0; i < 15; ++i) {
                tmp1 = i + 1;
                tmp2 = 15 - i;
                tmp3 = (i > 7) ? tmp2 : tmp1;
                fvec[i] = y[i] - (x[0] + tmp1 / (x[1] * tmp2 + x[2] * tmp3));
            }
        } else {
            /* compute Jacobian */
            for (i = 0; i < 15; ++i) {
                tmp1 = i + 1;
                tmp2 = 15 - i;
                tmp3 = (i > 7) ? tmp2 : tmp1;
                tmp4 = (x[1] * tmp2 + x[2] * tmp3);
                tmp4 = tmp4 * tmp4;
                fjac[i + ldfjac * 0] = -1.;
                fjac[i + ldfjac * 1] = tmp1 * tmp2 / tmp4;
                fjac[i + ldfjac * 2] = tmp1 * tmp3 / tmp4;
            }
        }
        return 0;
    }

    @Override
    public boolean hasJacobian() {
        return true;
    }
}
