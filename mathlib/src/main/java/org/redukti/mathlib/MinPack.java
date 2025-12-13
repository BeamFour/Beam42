package org.redukti.mathlib;

public class MinPack {

    /**
     * Function dpmpar
     * <p>
     * This function provides double precision machine parameters
     * when the appropriate set of data statements is activated (by
     * removing the c from column 1) and all other data statements are
     * rendered inactive. Most of the parameter values were obtained
     * from the corresponding Bell Laboratories Port Library function.
     * <p>
     * The function statement is
     * <p>
     * double precision function dpmpar(i)
     * <p>
     * where
     *
     * @param i is an integer input variable set to 1, 2, or 3 which
     *          selects the desired machine parameter. If the machine has
     *          t base b digits and its smallest and largest exponents are
     *          emin and emax, respectively, then these parameters are
     *          <p>
     *          dpmpar(1) = b**(1 - t), the machine precision,
     *          <p>
     *          dpmpar(2) = b**(emin - 1), the smallest magnitude,
     *          <p>
     *          dpmpar(3) = b**emax*(1 - b**(-t)), the largest magnitude.
     *          <p>
     *          Argonne National Laboratory. MINPACK Project. November 1996.
     *          Burton S. Garbow, Kenneth E. Hillstrom, Jorge J. More'
     * @return precision
     */
    public static double dpmpar(int i) {
        switch (i) {
            case 1:
                return 2.22044604926e-16;
            case 2:
                return 2.22507385852e-308;
            default:
                return 1.79769313485e+308;
        }
    }

    public interface Lmder_Function {
        /**
         * for lmder1 and lmder
         * Used if the implementation can generate jacobian
         *
         * @param m      is a positive integer input variable set to the number
         *               of functions
         * @param n      is a positive integer input variable set to the number
         *               of variables. n must not exceed m.
         * @param x      is an array of length n. current estimate of the solution
         *               vector.
         * @param fvec   is an output array of length m which contains
         *               the functions evaluated at the output x.
         * @param fjac   is an output m by n array. the upper n by n submatrix
         *               of fjac contains an upper triangular matrix r with
         *               diagonal elements of nonincreasing magnitude
         * @param ldfjac is a positive integer input variable not less than m
         *               which specifies the leading dimension of the array fjac.
         * @param iflag  flag
         *               if iflag = 1 calculate the functions at x and
         *               return this vector in fvec. do not alter fjac.
         *               <p>
         *               if iflag = 2 calculate the jacobian at x and
         *               return this matrix in fjac. do not alter fvec.
         * @return return a negative value to terminate lmder1/lmder
         */
        default int apply(int m, int n, double[] x, double[] fvec,
                  double[] fjac, int ldfjac, int iflag ) {
            return -1;
        }
        /**
         * for lmder1 and lmder
         * Used if the implementation does not generate jacobian
         *
         * @param m      is a positive integer input variable set to the number
         *               of functions
         * @param n      is a positive integer input variable set to the number
         *               of variables. n must not exceed m.
         * @param x      is an array of length n. current estimate of the solution
         *               vector.
         * @param fvec   is an output array of length m which contains
         *               the functions evaluated at the output x.
         * @param iflag  flag
         *               if iflag = 1 calculate the functions at x and
         *               return this vector in fvec.
         * @return return a negative value to terminate lmder1/lmder
         */
        default int apply(int m, int n, double[] x, double[] fvec,
                          int iflag ) {
            return -1;
        }
        default boolean hasJacobian() {
            return false;
        }
    }

    /**

     subroutine lmder1

     the purpose of lmder1 is to minimize the sum of the squares of
     m nonlinear functions in n variables by a modification of the
     levenberg-marquardt algorithm. this is done by using the more
     general least-squares solver lmder. the user must provide a
     subroutine which calculates the functions and the jacobian.

     the subroutine statement is

     subroutine lmder1(fcn,m,n,x,fvec,fjac,ldfjac,tol,info,
     ipvt,wa,lwa)

     where

     fcn is the name of the user-supplied subroutine which
     calculates the functions and the jacobian. fcn must
     be declared in an external statement in the user
     calling program, and should be written as follows.

     subroutine fcn(m,n,x,fvec,fjac,ldfjac,iflag)
     integer m,n,ldfjac,iflag
     double precision x(n),fvec(m),fjac(ldfjac,n)
     ----------
     if iflag = 1 calculate the functions at x and
     return this vector in fvec. do not alter fjac.
     if iflag = 2 calculate the jacobian at x and
     return this matrix in fjac. do not alter fvec.
     ----------
     return
     end

     the value of iflag should not be changed by fcn unless
     the user wants to terminate execution of lmder1.
     in this case set iflag to a negative integer.

     @param m is a positive integer input variable set to the number
     of functions.

     @param n is a positive integer input variable set to the number
     of variables. n must not exceed m.

     @param x is an array of length n. on input x must contain
     an initial estimate of the solution vector. on output x
     contains the final estimate of the solution vector.

     @param fvec is an output array of length m which contains
     the functions evaluated at the output x.

     @param fjac is an output m by n array. the upper n by n submatrix
     of fjac contains an upper triangular matrix r with
     diagonal elements of nonincreasing magnitude such that

     t     t           t
     p *(jac *jac)*p = r *r,

     where p is a permutation matrix and jac is the final
     calculated jacobian. column j of p is column ipvt(j)
     (see below) of the identity matrix. the lower trapezoidal
     part of fjac contains information generated during
     the computation of r.

     @param ldfjac is a positive integer input variable not less than m
     which specifies the leading dimension of the array fjac.

     @param tol is a nonnegative input variable. termination occurs
     when the algorithm estimates either that the relative
     error in the sum of squares is at most tol or that
     the relative error between x and the solution is at
     most tol.

     @return  info is an integer output variable. if the user has
     terminated execution, info is set to the (negative)
     value of iflag. see description of fcn. otherwise,
     info is set as follows.

     info = 0  improper input parameters.

     info = 1  algorithm estimates that the relative error
     in the sum of squares is at most tol.

     info = 2  algorithm estimates that the relative error
     between x and the solution is at most tol.

     info = 3  conditions for info = 1 and info = 2 both hold.

     info = 4  fvec is orthogonal to the columns of the
     jacobian to machine precision.

     info = 5  number of calls to fcn with iflag = 1 has
     reached 100*(n+1).

     info = 6  tol is too small. no further reduction in
     the sum of squares is possible.

     info = 7  tol is too small. no further improvement in
     the approximate solution x is possible.

     @param ipvt is an integer output array of length n. ipvt
     defines a permutation matrix p such that jac*p = q*r,
     where jac is the final calculated jacobian, q is
     orthogonal (not stored), and r is upper triangular
     with diagonal elements of nonincreasing magnitude.
     column j of p is column ipvt(j) of the identity matrix.

     @param wa is a work array of length lwa.

     @param lwa is a positive integer input variable not less than 5*n+m.

     @author argonne national laboratory. minpack project. march 1980.
     @author burton s. garbow, kenneth e. hillstrom, jorge j. more

     */
    public static int lmder1(Lmder_Function fcn, int m, int n, double[] x,
                             double[] fvec, double[] fjac, int ldfjac, double tol,
                             int[] ipvt, double[] wa, int lwa) {
        // Setting epsfcn to 0.0 causes machine precision to be used
        return lmder1(fcn, m, n, x, fvec, fjac, ldfjac, tol, ipvt, wa, lwa, 0.0);
    }
    /**

     subroutine lmder1

     the purpose of lmder1 is to minimize the sum of the squares of
     m nonlinear functions in n variables by a modification of the
     levenberg-marquardt algorithm. this is done by using the more
     general least-squares solver lmder. the user must provide a
     subroutine which calculates the functions and the jacobian.

     the subroutine statement is

     subroutine lmder1(fcn,m,n,x,fvec,fjac,ldfjac,tol,info,
     ipvt,wa,lwa)

     where

     fcn is the name of the user-supplied subroutine which
     calculates the functions and the jacobian. fcn must
     be declared in an external statement in the user
     calling program, and should be written as follows.

     subroutine fcn(m,n,x,fvec,fjac,ldfjac,iflag)
     integer m,n,ldfjac,iflag
     double precision x(n),fvec(m),fjac(ldfjac,n)
     ----------
     if iflag = 1 calculate the functions at x and
     return this vector in fvec. do not alter fjac.
     if iflag = 2 calculate the jacobian at x and
     return this matrix in fjac. do not alter fvec.
     ----------
     return
     end

     the value of iflag should not be changed by fcn unless
     the user wants to terminate execution of lmder1.
     in this case set iflag to a negative integer.

     @param m is a positive integer input variable set to the number
     of functions.

     @param n is a positive integer input variable set to the number
     of variables. n must not exceed m.

     @param x is an array of length n. on input x must contain
     an initial estimate of the solution vector. on output x
     contains the final estimate of the solution vector.

     @param fvec is an output array of length m which contains
     the functions evaluated at the output x.

     @param fjac is an output m by n array. the upper n by n submatrix
     of fjac contains an upper triangular matrix r with
     diagonal elements of nonincreasing magnitude such that

     t     t           t
     p *(jac *jac)*p = r *r,

     where p is a permutation matrix and jac is the final
     calculated jacobian. column j of p is column ipvt(j)
     (see below) of the identity matrix. the lower trapezoidal
     part of fjac contains information generated during
     the computation of r.

     @param ldfjac is a positive integer input variable not less than m
     which specifies the leading dimension of the array fjac.

     @param tol is a nonnegative input variable. termination occurs
     when the algorithm estimates either that the relative
     error in the sum of squares is at most tol or that
     the relative error between x and the solution is at
     most tol.

     @return  info is an integer output variable. if the user has
     terminated execution, info is set to the (negative)
     value of iflag. see description of fcn. otherwise,
     info is set as follows.

     info = 0  improper input parameters.

     info = 1  algorithm estimates that the relative error
     in the sum of squares is at most tol.

     info = 2  algorithm estimates that the relative error
     between x and the solution is at most tol.

     info = 3  conditions for info = 1 and info = 2 both hold.

     info = 4  fvec is orthogonal to the columns of the
     jacobian to machine precision.

     info = 5  number of calls to fcn with iflag = 1 has
     reached 100*(n+1).

     info = 6  tol is too small. no further reduction in
     the sum of squares is possible.

     info = 7  tol is too small. no further improvement in
     the approximate solution x is possible.

     @param ipvt is an integer output array of length n. ipvt
     defines a permutation matrix p such that jac*p = q*r,
     where jac is the final calculated jacobian, q is
     orthogonal (not stored), and r is upper triangular
     with diagonal elements of nonincreasing magnitude.
     column j of p is column ipvt(j) of the identity matrix.

     @param wa is a work array of length lwa.

     @param lwa is a positive integer input variable not less than 5*n+m.

     @author argonne national laboratory. minpack project. march 1980.
     @author burton s. garbow, kenneth e. hillstrom, jorge j. more

     */
    public static int lmder1(Lmder_Function fcn, int m, int n, double[] x,
                             double[] fvec, double[] fjac, int ldfjac, double tol,
                             int[] ipvt, double[] wa, int lwa, double epsfcn) {
        /* Initialized data */

        final double factor = 100.;

        /* Local variables */
        int mode;
        int[] nfev = new int[1], njev = new int[1];
        double ftol, gtol, xtol;
        int maxfev, nprint;
        int info;


        /*     check the input parameters for errors. */

        if (n <= 0 || m < n || ldfjac < m || tol < 0. || lwa < n * 5 + m) {
            return 0;
        }

        /*     call lmder. */

        maxfev = (n + 1) * 100;
        ftol = tol;
        xtol = tol;
        gtol = 0.;
        mode = 1;
        nprint = 0;

        double[] wa1 = new double[n];
        double[] wa2 = new double[n];
        double[] wa3 = new double[n];
        double[] wa4 = new double[n];
        double[] wa5 = new double[m];

        info = lmder(fcn, m, n, x, fvec, fjac, ldfjac,
                ftol, xtol, gtol, maxfev, wa, mode, factor, nprint,
                nfev, njev, ipvt, wa1, wa2, wa3, wa4, wa5, epsfcn);
        if (info == 8) {
            info = 4;
        }
        return info;

        /*     last card of subroutine lmder1. */

    }

    /**
      subroutine lmder

      the purpose of lmder is to minimize the sum of the squares of
      m nonlinear functions in n variables by a modification of
      the levenberg-marquardt algorithm. the user must provide a
      subroutine which calculates the functions and the jacobian.

      the subroutine statement is

        subroutine lmder(fcn,m,n,x,fvec,fjac,ldfjac,ftol,xtol,gtol,
                         maxfev,diag,mode,factor,nprint,info,nfev,
                         njev,ipvt,qtf,wa1,wa2,wa3,wa4)

      where

        @param fcn is the name of the user-supplied subroutine which
          calculates the functions and the jacobian. fcn must
          be declared in an external statement in the user
          calling program, and should be written as follows.

          subroutine fcn(m,n,x,fvec,fjac,ldfjac,iflag)
          integer m,n,ldfjac,iflag
          double precision x(n),fvec(m),fjac(ldfjac,n)
          ----------
          if iflag = 1 calculate the functions at x and
          return this vector in fvec. do not alter fjac.
          if iflag = 2 calculate the jacobian at x and
          return this matrix in fjac. do not alter fvec.
          ----------
          return
          end

          the value of iflag should not be changed by fcn unless
          the user wants to terminate execution of lmder.
          in this case set iflag to a negative integer.

        @param m is a positive integer input variable set to the number
          of functions.

        @param n is a positive integer input variable set to the number
          of variables. n must not exceed m.

        @param x is an array of length n. on input x must contain
          an initial estimate of the solution vector. on output x
          contains the final estimate of the solution vector.

        @param fvec is an output array of length m which contains
          the functions evaluated at the output x.

        @Param fjac is an output m by n array. the upper n by n submatrix
          of fjac contains an upper triangular matrix r with
          diagonal elements of nonincreasing magnitude such that

                 t     t           t
                p *(jac *jac)*p = r *r,

          where p is a permutation matrix and jac is the final
          calculated jacobian. column j of p is column ipvt(j)
          (see below) of the identity matrix. the lower trapezoidal
          part of fjac contains information generated during
          the computation of r.

        @param ldfjac is a positive integer input variable not less than m
          which specifies the leading dimension of the array fjac.

        @param ftol is a nonnegative input variable. termination
          occurs when both the actual and predicted relative
          reductions in the sum of squares are at most ftol.
          therefore, ftol measures the relative error desired
          in the sum of squares.

        @param xtol is a nonnegative input variable. termination
          occurs when the relative error between two consecutive
          iterates is at most xtol. therefore, xtol measures the
          relative error desired in the approximate solution.

        @param gtol is a nonnegative input variable. termination
          occurs when the cosine of the angle between fvec and
          any column of the jacobian is at most gtol in absolute
          value. therefore, gtol measures the orthogonality
          desired between the function vector and the columns
          of the jacobian.

        @param maxfev is a positive integer input variable. termination
          occurs when the number of calls to fcn with iflag = 1
          has reached maxfev.

        @param diag is an array of length n. if mode = 1 (see
          below), diag is internally set. if mode = 2, diag
          must contain positive entries that serve as
          multiplicative scale factors for the variables.

        @param mode is an integer input variable. if mode = 1, the
          variables will be scaled internally. if mode = 2,
          the scaling is specified by the input diag. other
          values of mode are equivalent to mode = 1.

        @param factor is a positive input variable used in determining the
          initial step bound. this bound is set to the product of
          factor and the euclidean norm of diag*x if nonzero, or else
          to factor itself. in most cases factor should lie in the
          interval (.1,100.).100. is a generally recommended value.

        @param nprint is an integer input variable that enables controlled
          printing of iterates if it is positive. in this case,
          fcn is called with iflag = 0 at the beginning of the first
          iteration and every nprint iterations thereafter and
          immediately prior to return, with x, fvec, and fjac
          available for printing. fvec and fjac should not be
          altered. if nprint is not positive, no special calls
          of fcn with iflag = 0 are made.

        @return info is an integer output variable. if the user has
          terminated execution, info is set to the (negative)
          value of iflag. see description of fcn. otherwise,
          info is set as follows.

          info = 0  improper input parameters.

          info = 1  both actual and predicted relative reductions
                    in the sum of squares are at most ftol.

          info = 2  relative error between two consecutive iterates
                    is at most xtol.

          info = 3  conditions for info = 1 and info = 2 both hold.

          info = 4  the cosine of the angle between fvec and any
                    column of the jacobian is at most gtol in
                    absolute value.

          info = 5  number of calls to fcn with iflag = 1 has
                    reached maxfev.

          info = 6  ftol is too small. no further reduction in
                    the sum of squares is possible.

          info = 7  xtol is too small. no further improvement in
                    the approximate solution x is possible.

          info = 8  gtol is too small. fvec is orthogonal to the
                    columns of the jacobian to machine precision.

        @param nfev is an integer output variable set to the number of
          calls to fcn with iflag = 1.

        @param njev is an integer output variable set to the number of
          calls to fcn with iflag = 2.

        @param ipvt is an integer output array of length n. ipvt
          defines a permutation matrix p such that jac*p = q*r,
          where jac is the final calculated jacobian, q is
          orthogonal (not stored), and r is upper triangular
          with diagonal elements of nonincreasing magnitude.
          column j of p is column ipvt(j) of the identity matrix.

        @param qtf is an output array of length n which contains
          the first n elements of the vector (q transpose)*fvec.

        @param wa1, wa2, and wa3 are work arrays of length n.

        @param wa4 is a work array of length m.

      @author argonne national laboratory. minpack project. march 1980.
      @author burton s. garbow, kenneth e. hillstrom, jorge j. more
     */
    public static int lmder(Lmder_Function fcn, int m, int n, double[] x,
                            double[] fvec, double[] fjac, int ldfjac, double ftol,
                            double xtol, double gtol, int maxfev, double[] diag, int mode, double factor, int nprint,
                            int[] nfev, int[] njev, int[] ipvt, double[] qtf,
                            double[] wa1, double[] wa2, double[] wa3, double[] wa4) {
        // Setting epsfcn to 0.0 causes machine precision to be used
        return lmder(fcn, m, n, x, fvec, fjac, ldfjac,
                ftol, xtol, gtol, maxfev, diag, mode, factor, nprint,
                nfev, njev, ipvt, qtf, wa1, wa2, wa3, wa4, 0.0);
    }

    /**
     subroutine lmder

     the purpose of lmder is to minimize the sum of the squares of
     m nonlinear functions in n variables by a modification of
     the levenberg-marquardt algorithm. the user must provide a
     subroutine which calculates the functions and the jacobian.

     the subroutine statement is

     subroutine lmder(fcn,m,n,x,fvec,fjac,ldfjac,ftol,xtol,gtol,
     maxfev,diag,mode,factor,nprint,info,nfev,
     njev,ipvt,qtf,wa1,wa2,wa3,wa4)

     where

     @param fcn is the name of the user-supplied subroutine which
     calculates the functions and the jacobian. fcn must
     be declared in an external statement in the user
     calling program, and should be written as follows.

     subroutine fcn(m,n,x,fvec,fjac,ldfjac,iflag)
     integer m,n,ldfjac,iflag
     double precision x(n),fvec(m),fjac(ldfjac,n)
     ----------
     if iflag = 1 calculate the functions at x and
     return this vector in fvec. do not alter fjac.
     if iflag = 2 calculate the jacobian at x and
     return this matrix in fjac. do not alter fvec.
     ----------
     return
     end

     the value of iflag should not be changed by fcn unless
     the user wants to terminate execution of lmder.
     in this case set iflag to a negative integer.

     @param m is a positive integer input variable set to the number
     of functions.

     @param n is a positive integer input variable set to the number
     of variables. n must not exceed m.

     @param x is an array of length n. on input x must contain
     an initial estimate of the solution vector. on output x
     contains the final estimate of the solution vector.

     @param fvec is an output array of length m which contains
     the functions evaluated at the output x.

     @Param fjac is an output m by n array. the upper n by n submatrix
     of fjac contains an upper triangular matrix r with
     diagonal elements of nonincreasing magnitude such that

     t     t           t
     p *(jac *jac)*p = r *r,

     where p is a permutation matrix and jac is the final
     calculated jacobian. column j of p is column ipvt(j)
     (see below) of the identity matrix. the lower trapezoidal
     part of fjac contains information generated during
     the computation of r.

     @param ldfjac is a positive integer input variable not less than m
     which specifies the leading dimension of the array fjac.

     @param ftol is a nonnegative input variable. termination
     occurs when both the actual and predicted relative
     reductions in the sum of squares are at most ftol.
     therefore, ftol measures the relative error desired
     in the sum of squares.

     @param xtol is a nonnegative input variable. termination
     occurs when the relative error between two consecutive
     iterates is at most xtol. therefore, xtol measures the
     relative error desired in the approximate solution.

     @param gtol is a nonnegative input variable. termination
     occurs when the cosine of the angle between fvec and
     any column of the jacobian is at most gtol in absolute
     value. therefore, gtol measures the orthogonality
     desired between the function vector and the columns
     of the jacobian.

     @param maxfev is a positive integer input variable. termination
     occurs when the number of calls to fcn with iflag = 1
     has reached maxfev.

     @param diag is an array of length n. if mode = 1 (see
     below), diag is internally set. if mode = 2, diag
     must contain positive entries that serve as
     multiplicative scale factors for the variables.

     @param mode is an integer input variable. if mode = 1, the
     variables will be scaled internally. if mode = 2,
     the scaling is specified by the input diag. other
     values of mode are equivalent to mode = 1.

     @param factor is a positive input variable used in determining the
     initial step bound. this bound is set to the product of
     factor and the euclidean norm of diag*x if nonzero, or else
     to factor itself. in most cases factor should lie in the
     interval (.1,100.).100. is a generally recommended value.

     @param nprint is an integer input variable that enables controlled
     printing of iterates if it is positive. in this case,
     fcn is called with iflag = 0 at the beginning of the first
     iteration and every nprint iterations thereafter and
     immediately prior to return, with x, fvec, and fjac
     available for printing. fvec and fjac should not be
     altered. if nprint is not positive, no special calls
     of fcn with iflag = 0 are made.

     @return info is an integer output variable. if the user has
     terminated execution, info is set to the (negative)
     value of iflag. see description of fcn. otherwise,
     info is set as follows.

     info = 0  improper input parameters.

     info = 1  both actual and predicted relative reductions
     in the sum of squares are at most ftol.

     info = 2  relative error between two consecutive iterates
     is at most xtol.

     info = 3  conditions for info = 1 and info = 2 both hold.

     info = 4  the cosine of the angle between fvec and any
     column of the jacobian is at most gtol in
     absolute value.

     info = 5  number of calls to fcn with iflag = 1 has
     reached maxfev.

     info = 6  ftol is too small. no further reduction in
     the sum of squares is possible.

     info = 7  xtol is too small. no further improvement in
     the approximate solution x is possible.

     info = 8  gtol is too small. fvec is orthogonal to the
     columns of the jacobian to machine precision.

     @param nfev is an integer output variable set to the number of
     calls to fcn with iflag = 1.

     @param njev is an integer output variable set to the number of
     calls to fcn with iflag = 2.

     @param ipvt is an integer output array of length n. ipvt
     defines a permutation matrix p such that jac*p = q*r,
     where jac is the final calculated jacobian, q is
     orthogonal (not stored), and r is upper triangular
     with diagonal elements of nonincreasing magnitude.
     column j of p is column ipvt(j) of the identity matrix.

     @param qtf is an output array of length n which contains
     the first n elements of the vector (q transpose)*fvec.

     @param wa1, wa2, and wa3 are work arrays of length n.

     @param wa4 is a work array of length m.

     @author argonne national laboratory. minpack project. march 1980.
     @author burton s. garbow, kenneth e. hillstrom, jorge j. more
     */
    public static int lmder(Lmder_Function fcn, int m, int n, double[] x,
                            double[] fvec, double[] fjac, int ldfjac, double ftol,
                            double xtol, double gtol, int maxfev, double[] diag, int mode, double factor, int nprint,
                            int[] nfev, int[] njev, int[] ipvt, double[] qtf,
                            double[] wa1, double[] wa2, double[] wa3, double[] wa4, double epsfcn)
    {
        /* Initialized data */

        final double p1 = .1;
        final double p5 = .5;
        final double p25 = .25;
        final double p75 = .75;
        final double p0001 = 1e-4;

        /* System generated locals */
        double d1, d2;

        /* Local variables */
        int i, j, l;
        double[] par = new double[1];
        double sum;
        int iter;
        double temp, temp1, temp2;
        int iflag;
        double delta = 0.;
        double ratio;
        double fnorm, gnorm, pnorm, xnorm = 0., fnorm1, actred, dirder,
                epsmch, prered;
        int info;

        /*     epsmch is the machine precision. */

        epsmch = dpmpar(1);

        info = 0;
        iflag = 0;
        nfev[0] = 0;
        njev[0] = 0;

        /*     check the input parameters for errors. */
        processing:
        do {
            if (n <= 0 || m < n || ldfjac < m || ftol < 0. || xtol < 0. ||
                    gtol < 0. || maxfev <= 0 || factor <= 0.) {
                break processing;
            }
            if (mode == 2) {
                for (j = 0; j < n; ++j) {
                    if (diag[j] <= 0.) {
                        break processing;
                    }
                }
            }

            /*     evaluate the function at the starting point */
            /*     and calculate its norm. */

            iflag = fcn.hasJacobian() ?
                    fcn.apply(m, n, x, fvec, fjac, ldfjac, 1) :
                    fcn.apply(m, n, x, fvec, 1);
            nfev[0] = 1;
            if (iflag < 0) {
                break processing;
            }
            fnorm = enorm(m, 0, fvec);

            /*     initialize levenberg-marquardt parameter and iteration counter. */

            par[0] = 0.;
            iter = 1;

            /*     beginning of the outer loop. */

            for (; ; ) {

                /*        calculate the jacobian matrix. */

                iflag = fcn.hasJacobian() ?
                        fcn.apply(m, n, x, fvec, fjac, ldfjac, 2) :
                        fdjac2(fcn, m, n, x, fvec, fjac, ldfjac, epsfcn, wa4);
                njev[0] = njev[0]+1;
                if (iflag < 0) {
                    break processing;
                }

                /*        if requested, call fcn to enable printing of iterates. */

                if (nprint > 0) {
                    iflag = 0;
                    if ((iter - 1) % nprint == 0) {
                        iflag = fcn.hasJacobian() ?
                                fcn.apply(m, n, x, fvec, fjac, ldfjac, 0) :
                                fcn.apply(m, n, x, fvec, 0);
                    }
                    if (iflag < 0) {
                        break processing;
                    }
                }

                /*        compute the qr factorization of the jacobian. */

                qrfac(m, n, fjac, ldfjac, 1, ipvt, n,
                        wa1, wa2, wa3);

                /*        on the first iteration and if mode is 1, scale according */
                /*        to the norms of the columns of the initial jacobian. */

                if (iter == 1) {
                    if (mode != 2) {
                        for (j = 0; j < n; ++j) {
                            diag[j] = wa2[j];
                            if (wa2[j] == 0.) {
                                diag[j] = 1.;
                            }
                        }
                    }

                    /*        on the first iteration, calculate the norm of the scaled x */
                    /*        and initialize the step bound delta. */

                    for (j = 0; j < n; ++j) {
                        wa3[j] = diag[j] * x[j];
                    }
                    xnorm = enorm(n, 0, wa3);
                    delta = factor * xnorm;
                    if (delta == 0.) {
                        delta = factor;
                    }
                }

                /*        form (q transpose)*fvec and store the first n components in */
                /*        qtf. */

                for (i = 0; i < m; ++i) {
                    wa4[i] = fvec[i];
                }
                for (j = 0; j < n; ++j) {
                    if (fjac[j + j * ldfjac] != 0.) {
                        sum = 0.;
                        for (i = j; i < m; ++i) {
                            sum += fjac[i + j * ldfjac] * wa4[i];
                        }
                        temp = -sum / fjac[j + j * ldfjac];
                        for (i = j; i < m; ++i) {
                            wa4[i] += fjac[i + j * ldfjac] * temp;
                        }
                    }
                    fjac[j + j * ldfjac] = wa1[j];
                    qtf[j] = wa4[j];
                }

                /*        compute the norm of the scaled gradient. */

                gnorm = 0.;
                if (fnorm != 0.) {
                    for (j = 0; j < n; ++j) {
                        l = ipvt[j]-1;
                        if (wa2[l] != 0.) {
                            sum = 0.;
                            for (i = 0; i <= j; ++i) {
                                sum += fjac[i + j * ldfjac] * (qtf[i] / fnorm);
                            }
                            /* Computing MAX */
                            d1 = Math.abs(sum / wa2[l]);
                            gnorm = Math.max(gnorm, d1);
                        }
                    }
                }

                /*        test for convergence of the gradient norm. */

                if (gnorm <= gtol) {
                    info = 4;
                }
                if (info != 0) {
                    break processing;
                }

                /*        rescale if necessary. */

                if (mode != 2) {
                    for (j = 0; j < n; ++j) {
                        /* Computing MAX */
                        d1 = diag[j];
                        d2 = wa2[j];
                        diag[j] = Math.max(d1, d2);
                    }
                }

                /*        beginning of the inner loop. */

                do {

                    /*           determine the levenberg-marquardt parameter. */
                    lmpar (n, fjac, ldfjac, ipvt, diag, qtf, delta,
                            par, wa1, wa2, wa3, wa4);

                    /*           store the direction p and x + p. calculate the norm of p. */

                    for (j = 0; j < n; ++j) {
                        wa1[j] = -wa1[j];
                        wa2[j] = x[j] + wa1[j];
                        wa3[j] = diag[j] * wa1[j];
                    }
                    pnorm = enorm(n, 0, wa3);

                    /*           on the first iteration, adjust the initial step bound. */

                    if (iter == 1) {
                        delta = Math.min(delta, pnorm);
                    }

                    /*           evaluate the function at x + p and calculate its norm. */

                    iflag = fcn.hasJacobian() ?
                            fcn.apply(m, n, wa2, wa4, fjac, ldfjac, 1) :
                            fcn.apply(m, n, wa2, wa4, 1);
                    nfev[0] = nfev[0] + 1;
                    if (iflag < 0) {
                        break processing;
                    }
                    fnorm1 = enorm(m, 0, wa4);

                    /*           compute the scaled actual reduction. */

                    actred = -1.;
                    if (p1 * fnorm1 < fnorm) {
                        /* Computing 2nd power */
                        d1 = fnorm1 / fnorm;
                        actred = 1 - d1 * d1;
                    }

                    /*           compute the scaled predicted reduction and */
                    /*           the scaled directional derivative. */

                    for (j = 0; j < n; ++j) {
                        wa3[j] = 0.;
                        l = ipvt[j] - 1;
                        temp = wa1[l];
                        for (i = 0; i <= j; ++i) {
                            wa3[i] += fjac[i + j * ldfjac] * temp;
                        }
                    }
                    temp1 = enorm(n, 0, wa3) / fnorm;
                    temp2 = (Math.sqrt(par[0]) * pnorm) / fnorm;
                    prered = temp1 * temp1 + temp2 * temp2 / p5;
                    dirder = -(temp1 * temp1 + temp2 * temp2);

                    /*           compute the ratio of the actual to the predicted */
                    /*           reduction. */

                    ratio = 0.;
                    if (prered != 0.) {
                        ratio = actred / prered;
                    }

                    /*           update the step bound. */

                    if (ratio <= p25) {
                        if (actred >= 0.) {
                            temp = p5;
                        } else {
                            temp = p5 * dirder / (dirder + p5 * actred);
                        }
                        if (p1 * fnorm1 >= fnorm || temp < p1) {
                            temp = p1;
                        }
                        /* Computing MIN */
                        d1 = pnorm / p1;
                        delta = temp * Math.min(delta, d1);
                        par[0] = par[0] / temp;
                    } else {
                        if (par[0] == 0. || ratio >= p75) {
                            delta = pnorm / p5;
                            par[0] = p5 * par[0];
                        }
                    }

                    /*           test for successful iteration. */

                    if (ratio >= p0001) {

                        /*           successful iteration. update x, fvec, and their norms. */

                        for (j = 0; j < n; ++j) {
                            x[j] = wa2[j];
                            wa2[j] = diag[j] * x[j];
                        }
                        for (i = 0; i < m; ++i) {
                            fvec[i] = wa4[i];
                        }
                        xnorm = enorm(n, 0, wa2);
                        fnorm = fnorm1;
                        ++iter;
                    }

                    /*           tests for convergence. */

                    if (Math.abs(actred) <= ftol && prered <= ftol && p5 * ratio <= 1.) {
                        info = 1;
                    }
                    if (delta <= xtol * xnorm) {
                        info = 2;
                    }
                    if (Math.abs(actred) <= ftol && prered <= ftol && p5 * ratio <= 1. && info == 2) {
                        info = 3;
                    }
                    if (info != 0) {
                        break processing;
                    }

                    /*           tests for termination and stringent tolerances. */

                    if (nfev[0] >= maxfev){
                        info = 5;
                    }
                    if (Math.abs(actred) <= epsmch && prered <= epsmch && p5 * ratio <= 1.) {
                        info = 6;
                    }
                    if (delta <= epsmch * xnorm) {
                        info = 7;
                    }
                    if (gnorm <= epsmch) {
                        info = 8;
                    }
                    if (info != 0) {
                        break processing;
                    }

                    /*           end of the inner loop. repeat if iteration unsuccessful. */

                } while (ratio < p0001);

                /*        end of the outer loop. */

            }
        } while (false);

        /*     termination, either normal or user imposed. */

        if (iflag < 0) {
            info = iflag;
        }
        if (nprint > 0) {
            int t = fcn.hasJacobian() ?
                    fcn.apply(m, n, x, fvec, fjac, ldfjac, 0) :
                    fcn.apply(m, n, x, fvec, 0);
        }
        return info;

        /*     last card of subroutine lmder. */

    }


    /**
     * function enorm
     * <p>
     * given an n-vector x, this function calculates the
     * euclidean norm of x.
     * <p>
     * the euclidean norm is computed by accumulating the sum of
     * squares in three different sums. the sums of squares for the
     * small and large components are scaled so that no overflows
     * occur. non-destructive underflows are permitted. underflows
     * and overflows do not occur in the computation of the unscaled
     * sum of squares for the intermediate components.
     * the definitions of small, intermediate and large components
     * depend on two constants, rdwarf and rgiant. the main
     * restrictions on these constants are that rdwarf**2 not
     * underflow and rgiant**2 not overflow. the constants
     * given here are suitable for every known computer.
     * <p>
     * the function statement is
     * <p>
     * double precision function enorm(n,x)
     * <p>
     * where
     *
     * @param n is a positive integer input variable.
     * @param x is an input array of length n.
     * @author argonne national laboratory. minpack project. march 1980.
     * @author burton s. garbow, kenneth e. hillstrom, jorge j. more
     */
    public static double enorm(int n, int start, double[] x) {
        int i;
        double agiant, floatn, s1, s2, s3, xabs,
                x1max, x3max;
        double enorm;

        final double one = 1.0;
        final double zero = 0.0;
        final double rdwarf = 3.834e-20;
        final double rgiant = 1.304e+19;

        s1 = 0.0;
        s2 = 0.0;
        s3 = 0.0;
        x1max = 0.0;
        x3max = 0.0;
        floatn = n;
        agiant = rgiant / floatn;

        for (i = start; i < n + start; i++) {
            xabs = Math.abs(x[i]);
            if (xabs <= rdwarf || xabs >= agiant) {
                if (xabs > rdwarf) {
                    // Sum for large components.
                    if (xabs > x1max) {
                        s1 = one + s1 * (x1max / xabs) * (x1max / xabs);
                        x1max = xabs;
                    } else {
                        s1 += (xabs / x1max) * (xabs / x1max);
                    }
                } else {
                    // Sum for small components.
                    if (xabs > x3max) {
                        s3 = one + s3 * (x3max / xabs) * (x3max / xabs);
                        x3max = xabs;
                    } else {
                        if (xabs != zero) s3 += (xabs / x3max) * (xabs / x3max);
                    }
                }
            } else {
                // Sum for intermediate components.
                s2 += xabs * xabs;
            }
        }

        // Calculation of norm.
        if (s1 != zero) {
            enorm = x1max * Math.sqrt(s1 + (s2 / x1max) / x1max);
        } else {
            if (s2 != zero) {
                if (s2 >= x3max) {
                    enorm = Math.sqrt(s2 * (one + (x3max / s2) * (x3max * s3)));
                } else {
                    enorm = Math.sqrt(x3max * ((s2 / x3max) + (x3max * s3)));
                }
            } else {
                enorm = x3max * Math.sqrt(s3);
            }
        }
        return enorm;
    }

    /**
     * subroutine qrfac
     * <p>
     * this subroutine uses householder transformations with column
     * pivoting (optional) to compute a qr factorization of the
     * m by n matrix a. that is, qrfac determines an orthogonal
     * matrix q, a permutation matrix p, and an upper trapezoidal
     * matrix r with diagonal elements of nonincreasing magnitude,
     * such that a*p = q*r. the householder transformation for
     * column k, k = 1,2,...,min(m,n), is of the form
     *
     * <pre>
     *                 t
     * i - (1/u(k))*u*u
     * </pre>
     * <p>
     * where u has zeros in the first k-1 positions. the form of
     * this transformation and the method of pivoting first
     * appeared in the corresponding linpack subroutine.
     * <p>
     * the subroutine statement is
     * <p>
     * subroutine qrfac(m,n,a,lda,pivot,ipvt,lipvt,rdiag,acnorm,wa)
     * <p>
     * where
     *
     * @param m      is a positive integer input variable set to the number
     *               of rows of a.
     * @param n      is a positive integer input variable set to the number
     *               of columns of a.
     * @param a      is an m by n array. on input a contains the matrix for
     *               which the qr factorization is to be computed. on output
     *               the strict upper trapezoidal part of a contains the strict
     *               upper trapezoidal part of r, and the lower trapezoidal
     *               part of a contains a factored form of q (the non-trivial
     *               elements of the u vectors described above).
     * @param lda    is a positive integer input variable not less than m
     *               which specifies the leading dimension of the array a.
     * @param pivot  is a logical input variable. if pivot is set true,
     *               then column pivoting is enforced. if pivot is set false,
     *               then no column pivoting is done.
     * @param ipvt   is an integer output array of length lipvt. ipvt
     *               defines the permutation matrix p such that a*p = q*r.
     *               column j of p is column ipvt(j) of the identity matrix.
     *               if pivot is false, ipvt is not referenced.
     * @param lipvt  is a positive integer input variable. if pivot is false,
     *               then lipvt may be as small as 1. if pivot is true, then
     *               lipvt must be at least n.
     * @param rdiag  is an output array of length n which contains the
     *               diagonal elements of r.
     * @param acnorm is an output array of length n which contains the
     *               norms of the corresponding columns of the input matrix a.
     *               if this information is not needed, then acnorm can coincide
     *               with rdiag.
     * @param wa     is a work array of length n. if pivot is false, then wa
     *               can coincide with rdiag.
     * @author argonne national laboratory. minpack project. march 1980.
     * @author burton s. garbow, kenneth e. hillstrom, jorge j. more
     */
    public static void qrfac(int m, int n, double[] a, int lda,
                             int pivot, int[] ipvt, int lipvt, double[] rdiag,
                             double[] acnorm, double[] wa) {
        /* Initialized data */

        final double p05 = .05;

        /* System generated locals */
        double d1;

        /* Local variables */
        int i, j, k, jp1;
        double sum;
        double temp;
        int minmn;
        double epsmch;
        double ajnorm;

        /* epsmch is the machine precision. */

        epsmch = dpmpar(1);

        /* compute the initial column norms and initialize several arrays. */

        for (j = 0; j < n; ++j) {
            acnorm[j] = enorm(m, j * lda, a);
            rdiag[j] = acnorm[j];
            wa[j] = rdiag[j];
            if (pivot != 0) {
                ipvt[j] = j + 1;
            }
        }

        /* reduce a to r with householder transformations. */

        minmn = Math.min(m, n);
        for (j = 0; j < minmn; j++) {
            if (pivot != 0) {

                /* bring the column of largest norm into the pivot position. */

                int kmax = j;
                for (k = j; k < n; ++k) {
                    if (rdiag[k] > rdiag[kmax]) {
                        kmax = k;
                    }
                }
                if (kmax != j) {
                    for (i = 0; i < m; ++i) {
                        temp = a[i + j * lda];
                        a[i + j * lda] = a[i + kmax * lda];
                        a[i + kmax * lda] = temp;
                    }
                    rdiag[kmax] = rdiag[j];
                    wa[kmax] = wa[j];
                    k = ipvt[j];
                    ipvt[j] = ipvt[kmax];
                    ipvt[kmax] = k;
                }
            }

            /* compute the householder transformation to reduce the */
            /* j-th column of a to a multiple of the j-th unit vector. */

            ajnorm = enorm(m - j, j + j * lda, a);
            if (ajnorm != 0.) {
                if (a[j + j * lda] < 0.) {
                    ajnorm = -ajnorm;
                }
                for (i = j; i < m; ++i) {
                    a[i + j * lda] /= ajnorm;
                }
                a[j + j * lda] += 1;

                /* apply the transformation to the remaining columns */
                /* and update the norms. */

                jp1 = j + 1;
                if (n > jp1) {
                    for (k = jp1; k < n; ++k) {
                        sum = 0.;
                        for (i = j; i < m; ++i) {
                            sum += a[i + j * lda] * a[i + k * lda];
                        }
                        temp = sum / a[j + j * lda];
                        for (i = j; i < m; ++i) {
                            a[i + k * lda] -= temp * a[i + j * lda];
                        }
                        if (pivot != 0 && rdiag[k] != 0.) {
                            temp = a[j + k * lda] / rdiag[k];
                            /* Computing MAX */
                            d1 = 1 - temp * temp;
                            rdiag[k] *= Math.sqrt((Math.max(0., d1)));
                            /* Computing 2nd power */
                            d1 = rdiag[k] / wa[k];
                            if (p05 * (d1 * d1) <= epsmch) {
                                rdiag[k] = enorm(m - (j + 1), jp1 + k * lda, a);
                                wa[k] = rdiag[k];
                            }
                        }
                    }
                }
            }
            rdiag[j] = -ajnorm;
        }
    }

    /**
     * subroutine qrsolv
     * <p>
     * given an m by n matrix a, an n by n diagonal matrix d,
     * and an m-vector b, the problem is to determine an x which
     * solves the system
     * <pre>
     * a*x = b ,     d*x = 0 ,
     * </pre>
     * in the least squares sense.
     * <p>
     * this subroutine completes the solution of the problem
     * if it is provided with the necessary information from the
     * qr factorization, with column pivoting, of a. that is, if
     * a*p = q*r, where p is a permutation matrix, q has orthogonal
     * columns, and r is an upper triangular matrix with diagonal
     * elements of nonincreasing magnitude, then qrsolv expects
     * the full upper triangle of r, the permutation matrix p,
     * and the first n components of (q transpose)*b. the system
     * a*x = b, d*x = 0, is then equivalent to
     * <pre>
     * t       t
     * r*z = q *b ,  p *d*p*z = 0 ,
     * </pre>
     * where x = p*z. if this system does not have full rank,
     * then a least squares solution is obtained. on output qrsolv
     * also provides an upper triangular matrix s such that
     * <pre>
     * t   t               t
     * p *(a *a + d*d)*p = s *s .
     * </pre>
     * s is computed within qrsolv and may be of separate interest.
     * <p>
     * the subroutine statement is
     * <p>
     * subroutine qrsolv(n,r,ldr,ipvt,diag,qtb,x,sdiag,wa)
     * <p>
     * where
     *
     * @param n     is a positive integer input variable set to the order of r.
     * @param r     is an n by n array. on input the full upper triangle
     *              must contain the full upper triangle of the matrix r.
     *              on output the full upper triangle is unaltered, and the
     *              strict lower triangle contains the strict upper triangle
     *              (transposed) of the upper triangular matrix s.
     * @param ldr   is a positive integer input variable not less than n
     *              which specifies the leading dimension of the array r.
     * @param ipvt  is an integer input array of length n which defines the
     *              permutation matrix p such that a*p = q*r. column j of p
     *              is column ipvt(j) of the identity matrix.
     * @param diag  is an input array of length n which must contain the
     *              diagonal elements of the matrix d.
     * @param qtb   is an input array of length n which must contain the first
     *              n elements of the vector (q transpose)*b.
     * @param x     is an output array of length n which contains the least
     *              squares solution of the system a*x = b, d*x = 0.
     * @param sdiag is an output array of length n which contains the
     *              diagonal elements of the upper triangular matrix s.
     * @param wa    is a work array of length n.

     * @author argonne national laboratory. minpack project. march 1980.
     * @author burton s. garbow, kenneth e. hillstrom, jorge j. more
     */
    public static void qrsolv(int n, double[] r, int ldr,
                              int[] ipvt, double[] diag, double[] qtb, double[] x,
                              double[] sdiag, double[] wa) {
        /* Initialized data */

        final double p5 = .5;
        final double p25 = .25;

        /* Local variables */
        int i, j, k, l;
        double cos, sin, sum, temp;
        int nsing;
        double qtbpj;

        /* copy r and (q transpose)*b to preserve input and initialize s. */
        /* in particular, save the diagonal elements of r in x. */

        for (j = 0; j < n; ++j) {
            for (i = j; i < n; ++i) {
                r[i + j * ldr] = r[j + i * ldr];
            }
            x[j] = r[j + j * ldr];
            wa[j] = qtb[j];
        }

        /* eliminate the diagonal matrix d using a givens rotation. */

        for (j = 0; j < n; ++j) {

            /* prepare the row of d to be eliminated, locating the */
            /* diagonal element using p from the qr factorization. */

            l = ipvt[j] - 1;
            if (diag[l] != 0.) {
                for (k = j; k < n; ++k) {
                    sdiag[k] = 0.;
                }
                sdiag[j] = diag[l];

                /* the transformations to eliminate the row of d */
                /* modify only a single element of (q transpose)*b */
                /* beyond the first n, which is initially zero. */

                qtbpj = 0.;
                for (k = j; k < n; ++k) {

                    /* determine a givens rotation which eliminates the */
                    /* appropriate element in the current row of d. */

                    if (sdiag[k] != 0.) {
                        if (Math.abs(r[k + k * ldr]) < Math.abs(sdiag[k])) {
                            double cotan;
                            cotan = r[k + k * ldr] / sdiag[k];
                            sin = p5 / Math.sqrt(p25 + p25 * (cotan * cotan));
                            cos = sin * cotan;
                        } else {
                            double tan;
                            tan = sdiag[k] / r[k + k * ldr];
                            cos = p5 / Math.sqrt(p25 + p25 * (tan * tan));
                            sin = cos * tan;
                        }

                        /* compute the modified diagonal element of r and */
                        /* the modified element of ((q transpose)*b,0). */

                        temp = cos * wa[k] + sin * qtbpj;
                        qtbpj = -sin * wa[k] + cos * qtbpj;
                        wa[k] = temp;

                        /* accumulate the tranformation in the row of s. */
                        r[k + k * ldr] = cos * r[k + k * ldr] + sin * sdiag[k];
                        if (n > k + 1) {
                            for (i = k + 1; i < n; ++i) {
                                temp = cos * r[i + k * ldr] + sin * sdiag[i];
                                sdiag[i] = -sin * r[i + k * ldr] + cos * sdiag[i];
                                r[i + k * ldr] = temp;
                            }
                        }
                    }
                }
            }

            /* store the diagonal element of s and restore */
            /* the corresponding diagonal element of r. */

            sdiag[j] = r[j + j * ldr];
            r[j + j * ldr] = x[j];
        }

        /* solve the triangular system for z. if the system is */
        /* singular, then obtain a least squares solution. */

        nsing = n;
        for (j = 0; j < n; ++j) {
            if (sdiag[j] == 0. && nsing == n) {
                nsing = j;
            }
            if (nsing < n) {
                wa[j] = 0.;
            }
        }
        if (nsing >= 1) {
            for (k = 1; k <= nsing; ++k) {
                j = nsing - k;
                sum = 0.;
                if (nsing > j + 1) {
                    for (i = j + 1; i < nsing; ++i) {
                        sum += r[i + j * ldr] * wa[i];
                    }
                }
                wa[j] = (wa[j] - sum) / sdiag[j];
            }
        }

        /* permute the components of z back to components of x. */
        for (j = 0; j < n; ++j) {
            l = ipvt[j] - 1;
            x[l] = wa[j];
        }
    }

    /**
     * subroutine lmpar
     * <p>
     * given an m by n matrix a, an n by n nonsingular diagonal
     * matrix d, an m-vector b, and a positive number delta,
     * the problem is to determine a value for the parameter
     * par such that if x solves the system
     * <pre>
     * a*x = b ,     sqrt(par)*d*x = 0 ,
     * </pre>
     * in the least squares sense, and dxnorm is the euclidean
     * norm of d*x, then either par is zero and
     * <pre>
     * (dxnorm-delta) .le. 0.1*delta ,
     * </pre>
     * or par is positive and
     * <pre>
     * abs(dxnorm-delta) .le. 0.1*delta .
     * </pre>
     * this subroutine completes the solution of the problem
     * if it is provided with the necessary information from the
     * qr factorization, with column pivoting, of a. that is, if
     * a*p = q*r, where p is a permutation matrix, q has orthogonal
     * columns, and r is an upper triangular matrix with diagonal
     * elements of nonincreasing magnitude, then lmpar expects
     * the full upper triangle of r, the permutation matrix p,
     * and the first n components of (q transpose)*b. on output
     * lmpar also provides an upper triangular matrix s such that
     * <pre>
     * t   t                   t
     * p *(a *a + par*d*d)*p = s *s .
     * </pre>
     * s is employed within lmpar and may be of separate interest.
     * <p>
     * only a few iterations are generally needed for convergence
     * of the algorithm. if, however, the limit of 10 iterations
     * is reached, then the output par will contain the best
     * value obtained so far.
     * <p>
     * the subroutine statement is
     * <p>
     * subroutine lmpar(n,r,ldr,ipvt,diag,qtb,delta,par,x,sdiag,
     * wa1,wa2)
     * <p>
     * where
     *
     * @param n     is a positive integer input variable set to the order of r.
     * @param r     is an n by n array. on input the full upper triangle
     *              must contain the full upper triangle of the matrix r.
     *              on output the full upper triangle is unaltered, and the
     *              strict lower triangle contains the strict upper triangle
     *              (transposed) of the upper triangular matrix s.
     * @param ldr   is a positive integer input variable not less than n
     *              which specifies the leading dimension of the array r.
     * @param ipvt  is an integer input array of length n which defines the
     *              permutation matrix p such that a*p = q*r. column j of p
     *              is column ipvt(j) of the identity matrix.
     * @param diag  is an input array of length n which must contain the
     *              diagonal elements of the matrix d.
     * @param qtb   is an input array of length n which must contain the first
     *              n elements of the vector (q transpose)*b.
     * @param delta is a positive input variable which specifies an upper
     *              bound on the euclidean norm of d*x.
     * @param par   is a nonnegative variable. on input par contains an
     *              initial estimate of the levenberg-marquardt parameter.
     *              on output par contains the final estimate.
     * @param x     is an output array of length n which contains the least
     *              squares solution of the system a*x = b, sqrt(par)*d*x = 0,
     *              for the output par.
     * @param sdiag is an output array of length n which contains the
     *              diagonal elements of the upper triangular matrix s.
     * @param wa1   and wa2 are work arrays of length n.
     * @author argonne national laboratory. minpack project. march 1980.
     * @author burton s. garbow, kenneth e. hillstrom, jorge j. more
     */
    public static void lmpar(int n, double[] r, int ldr,
                             int[] ipvt, double[] diag, double[] qtb, double delta,
                             double[] par, double[] x, double[] sdiag, double[] wa1,
                             double[] wa2) {
        /* Initialized data */

        final double p1 = .1;
        final double p001 = .001;

        /* System generated locals */
        double d1, d2;

        /* Local variables */
        int j, l;
        double fp;
        double parc, parl;
        int iter;
        double temp, paru, dwarf;
        int nsing;
        double gnorm;
        double dxnorm;

        /* dwarf is the smallest positive magnitude. */

        dwarf = dpmpar(2);

        /* compute and store in x the gauss-newton direction. if the */
        /* jacobian is rank-deficient, obtain a least squares solution. */

        nsing = n;
        for (j = 0; j < n; ++j) {
            wa1[j] = qtb[j];
            if (r[j + j * ldr] == 0. && nsing == n) {
                nsing = j;
            }
            if (nsing < n) {
                wa1[j] = 0.;
            }
        }
        if (nsing >= 1) {
            int k;
            for (k = 1; k <= nsing; ++k) {
                j = nsing - k;
                wa1[j] /= r[j + j * ldr];
                temp = wa1[j];
                if (j >= 1) {
                    int i;
                    for (i = 0; i < j; ++i) {
                        wa1[i] -= r[i + j * ldr] * temp;
                    }
                }
            }
        }
        for (j = 0; j < n; ++j) {
            l = ipvt[j] - 1;
            x[l] = wa1[j];
        }

        /* initialize the iteration counter. */
        /* evaluate the function at the origin, and test */
        /* for acceptance of the gauss-newton direction. */

        iter = 0;
        for (j = 0; j < n; ++j) {
            wa2[j] = diag[j] * x[j];
        }
        dxnorm = enorm(n, 0, wa2);
        fp = dxnorm - delta;

        try {

            if (fp <= p1 * delta) {
                throw new RuntimeException(); // goto terminate
            }

            /* if the jacobian is not rank deficient, the newton */
            /* step provides a lower bound, parl, for the zero of */
            /* the function. otherwise set this bound to zero. */

            parl = 0.;
            if (nsing >= n) {
                for (j = 0; j < n; ++j) {
                    l = ipvt[j] - 1;
                    wa1[j] = diag[l] * (wa2[l] / dxnorm);
                }
                for (j = 0; j < n; ++j) {
                    double sum = 0.;
                    if (j >= 1) {
                        int i;
                        for (i = 0; i < j; ++i) {
                            sum += r[i + j * ldr] * wa1[i];
                        }
                    }
                    wa1[j] = (wa1[j] - sum) / r[j + j * ldr];
                }
                temp = enorm(n, 0, wa1);
                parl = fp / delta / temp / temp;
            }

            /* calculate an upper bound, paru, for the zero of the function. */

            for (j = 0; j < n; ++j) {
                double sum;
                int i;
                sum = 0.;
                for (i = 0; i <= j; ++i) {
                    sum += r[i + j * ldr] * qtb[i];
                }
                l = ipvt[j] - 1;
                wa1[j] = sum / diag[l];
            }
            gnorm = enorm(n, 0, wa1);
            paru = gnorm / delta;
            if (paru == 0.) {
                paru = dwarf / Math.min(delta, p1) /* / p001 ??? */;
            }

            /* if the input par lies outside of the interval (parl,paru), */
            /* set par to the closer endpoint. */

            par[0] = Math.max(par[0], parl);
            par[0] = Math.min(par[0], paru);
            if (par[0] == 0.) {
                par[0] = gnorm / dxnorm;
            }

            /* beginning of an iteration. */

            for (; ; ) {
                ++iter;

                /* evaluate the function at the current value of par. */

                if (par[0] == 0.) {
                    /* Computing MAX */
                    d1 = dwarf;
                    d2 = p001 * paru;
                    par[0] = Math.max(d1, d2);
                }
                temp = Math.sqrt(par[0]);
                for (j = 0; j < n; ++j) {
                    wa1[j] = temp * diag[j];
                }
                qrsolv(n, r, ldr, ipvt, wa1, qtb, x, sdiag, wa2);
                for (j = 0; j < n; ++j) {
                    wa2[j] = diag[j] * x[j];
                }
                dxnorm = enorm(n, 0, wa2);
                temp = fp;
                fp = dxnorm - delta;

                /* if the function is small enough, accept the current value */
                /* of par. also test for the exceptional cases where parl */
                /* is zero or the number of iterations has reached 10. */

                if (Math.abs(fp) <= p1 * delta || (parl == 0. && fp <= temp && temp < 0.) || iter == 10) {
                    throw new RuntimeException(); // goto terminate
                }

                /* compute the newton correction. */

                for (j = 0; j < n; ++j) {
                    l = ipvt[j] - 1;
                    wa1[j] = diag[l] * (wa2[l] / dxnorm);
                }
                for (j = 0; j < n; ++j) {
                    wa1[j] /= sdiag[j];
                    temp = wa1[j];
                    if (n > j + 1) {
                        int i;
                        for (i = j + 1; i < n; ++i) {
                            wa1[i] -= r[i + j * ldr] * temp;
                        }
                    }
                }
                temp = enorm(n, 0, wa1);
                parc = fp / delta / temp / temp;

                /* depending on the sign of the function, update parl or paru. */

                if (fp > 0.) {
                    parl = Math.max(parl, par[0]);
                }
                if (fp < 0.) {
                    paru = Math.min(paru, par[0]);
                }

                /* compute an improved estimate for par. */

                /* Computing MAX */
                d1 = parl;
                d2 = par[0] + parc;
                par[0] = Math.max(d1, d2);

                /* end of an iteration. */
            }
        } catch (Exception e) {
            // :TERMINATE
        }

        /* termination. */

        if (iter == 0) {
            par[0] = 0.;
        }
    }

    public static int fdjac2(Lmder_Function fcn_mn, int m, int n, double[] x,
                             double[] fvec, double[] fjac, int ldfjac,
                             double epsfcn, double[] wa)
    {
        /* Local variables */
        double h;
        int i, j;
        double eps, temp, epsmch;
        int iflag;

        /*     epsmch is the machine precision. */
        epsmch = dpmpar(1);

        eps = Math.sqrt((Math.max(epsfcn,epsmch)));
        for (j = 0; j < n; ++j) {
            temp = x[j];
            h = eps * Math.abs(temp);
            if (h == 0.) {
                h = eps;
            }
            x[j] = temp + h;
        /* the last parameter of fcn_mn() is set to 2 to differentiate
           calls made to compute the function from calls made to compute
           the Jacobian (see fcn() in examples/lmfdrv.c, and how njev
           is used to compute the number of Jacobian evaluations) */
            iflag = fcn_mn.apply(m, n, x, wa, 2);
            if (iflag < 0) {
                return iflag;
            }
            x[j] = temp;
            for (i = 0; i < m; ++i) {
                fjac[i + j * ldfjac] = (wa[i] - fvec[i]) / h;
            }
        }
        return 0;
    }

    /***********

     subroutine r1mpyq

     given an m by n matrix a, this subroutine computes a*q where
     q is the product of 2*(n - 1) transformations

     gv(n-1)*...*gv(1)*gw(1)*...*gw(n-1)

     and gv(i), gw(i) are givens rotations in the (i,n) plane which
     eliminate elements in the i-th and n-th planes, respectively.
     q itself is not given, rather the information to recover the
     gv, gw rotations is supplied.

     the subroutine statement is

     subroutine r1mpyq(m,n,a,lda,v,w)

     where

     @param m is a positive integer input variable set to the number
     of rows of a.

     @param n is a positive integer input variable set to the number
     of columns of a.

     @param a is an m by n array. on input a must contain the matrix
     to be postmultiplied by the orthogonal matrix q
     described above. on output a*q has replaced a.

     @param lda is a positive integer input variable not less than m
     which specifies the leading dimension of the array a.

     @param v is an input array of length n. v(i) must contain the
     information necessary to recover the givens rotation gv(i)
     described above.

     @param w is an input array of length n. w(i) must contain the
     information necessary to recover the givens rotation gw(i)
     described above.

     @author argonne national laboratory. minpack project. march 1980.
     @author burton s. garbow, kenneth e. hillstrom, jorge j. more

    */
    public static void r1mpyq(int m, int n, double a[], int lda, double v[], double w[]) {
        double cos,sin,temp;
        int i, j, nmj, nm1;

        /*     apply the first set of givens rotations to a. */

        nm1 = n - 1;
        if (nm1 < 1)
            return;
        for (nmj = 1; nmj <= nm1; nmj++) {
            j = n - nmj;
            if (Math.abs(v[j-1]) > 1.0) {
                cos = 1.0 / v[j-1];
                sin = Math.sqrt(1.0 - cos * cos);
            } else {
                sin = v[j-1];
                cos = Math.sqrt(1.0 - sin * sin);
            }
            for (i = 1; i <= m; i++) {
                temp = cos * a[(i-1) + (j-1) * lda] - sin * a[(i-1) + (n - 1) * lda];
                a[(i-1) + (n - 1) * lda] = sin * a[(i-1) + (j-1) * lda] + cos * a[(i-1) + (n - 1) * lda];
                a[(i-1) + (j-1) * lda] = temp;
            }
        }

        /*     apply the second set of givens rotations to a. */

        for (j = 1; j <= nm1; j++) {
            if (Math.abs(w[j-1]) > 1.0) {
                cos = 1.0 / w[j-1];
                sin = Math.sqrt(1.0 - cos * cos);
            } else {
                sin = w[j-1];
                cos = Math.sqrt(1.0 - sin * sin);
            }
            for (i = 1; i <= m; i++) {
                temp = cos * a[(i-1) + (j-1) * lda] + sin * a[(i-1) + (n - 1) * lda];
                a[(i-1) + (n - 1) * lda] = -sin * a[(i-1) + (j-1) * lda] + cos * a[(i-1) + (n - 1) * lda];
                a[(i-1) + (j-1) * lda] = temp;
            }
        }
    }

    /***********

     subroutine r1updt

     given an m by n lower trapezoidal matrix s, an m-vector u,
     and an n-vector v, the problem is to determine an
     orthogonal matrix q such that

     t
     (s + u*v )*q

     is again lower trapezoidal.

     this subroutine determines q as the product of 2*(n - 1)
     transformations

     gv(n-1)*...*gv(1)*gw(1)*...*gw(n-1)

     where gv(i), gw(i) are givens rotations in the (i,n) plane
     which eliminate elements in the i-th and n-th planes,
     respectively. q itself is not accumulated, rather the
     information to recover the gv, gw rotations is returned.

     the subroutine statement is

     subroutine r1updt(m,n,s,ls,u,v,w,sing)

     where

     @param m is a positive integer input variable set to the number
     of rows of s.

     @param n is a positive integer input variable set to the number
     of columns of s. n must not exceed m.

     @param s is an array of length ls. on input s must contain the lower
     trapezoidal matrix s stored by columns. on output s contains
     the lower trapezoidal matrix produced as described above.

     @param ls is a positive integer input variable not less than
     (n*(2*m-n+1))/2.

     @param u is an input array of length m which must contain the
     vector u.

     @param v is an array of length n. on input v must contain the vector
     v. on output v(i) contains the information necessary to
     recover the givens rotation gv(i) described above.

     @param w is an output array of length m. w(i) contains information
     necessary to recover the givens rotation gw(i) described
     above.

     @return sing is a logical output variable. sing is set true if any
     of the diagonal elements of the output s are zero. otherwise
     sing is set false.


     @author argonne national laboratory. minpack project. march 1980.
     @author burton s. garbow, kenneth e. hillstrom, jorge j. more, john l. nazareth
     */
    public static boolean r1updt(int m, int n, double s[], int ls, double u[], double v[],
                                 double w[]) {
        final double p25 = 0.25;
        final double p5 = 0.5;

        int i,j,jj,l,nm1;
        double cos,cotan,giant;
        double sin,tan,tau,temp;

        boolean sing;

        /*     giant is the largest magnitude. */

        giant = dpmpar(3);

        /*     initialize the diagonal element pointer. */

        jj = (n * (2 * m - n + 1)) / 2 - (m - n);

        /*     move the nontrivial part of the last column of s into w. */

        l = jj;
        for (i = n; i <= m; i++) {
            w[i - 1] = s[l - 1];
            l = l + 1;
        }

        /*     rotate the vector v into a multiple of the n-th unit vector */
        /*     in such a way that a spike is introduced into w. */

        nm1 = n - 1;
        if (nm1 >= 1) {
            for (int nmj = 1; nmj <= nm1; nmj++) {
                j = n - nmj;
                jj = jj - (m - j + 1);
                w[j - 1] = 0.0;

                if (v[j - 1] != 0.0) {

                    /*        determine a givens rotation which eliminates the */
                    /*        j-th element of v. */

                    if (!(Math.abs(v[n - 1]) >= Math.abs(v[j - 1]))) {
                        cotan = v[n - 1] / v[j - 1];
                        sin = p5 / Math.sqrt(p25 + p25 * cotan * cotan);
                        cos = sin * cotan;
                        tau = 1.0;
                        if (Math.abs(cos) * giant > 1.0) {
                            tau = 1.0 / cos;
                        }
                    } else {
                        tan = v[j - 1] / v[n - 1];
                        cos = p5 / Math.sqrt(p25 + p25 * tan * tan);
                        sin = cos * tan;
                        tau = sin;
                    }

                    /*        apply the transformation to v and store the information */
                    /*        necessary to recover the givens rotation. */

                    v[n - 1] = sin * v[j - 1] + cos * v[n - 1];
                    v[j - 1] = tau;

                    /*        apply the transformation to s and extend the spike in w. */

                    l = jj;
                    for (i = j; i <= m; i++) {
                        temp = cos * s[l - 1] - sin * w[i - 1];
                        w[i - 1] = sin * s[l - 1] + cos * w[i - 1];
                        s[l - 1] = temp;
                        l = l + 1;
                    }
                }
            }
        }
        /*     add the spike from the rank 1 update to w. */

        for (i = 1; i <= m; i++) {
            w[i - 1] = w[i - 1] + v[n - 1] * u[i - 1];
        }
        /*     eliminate the spike. */

        sing = false;
        if (nm1 >= 1) {
            for (j = 1; j <= nm1; j++) {

                if (w[j - 1] != 0.0) {

                    /*        determine a givens rotation which eliminates the */
                    /*        j-th element of the spike. */

                    if (!(Math.abs(s[jj - 1]) >= Math.abs(w[j - 1]))) {
                        cotan = s[jj - 1] / w[j - 1];
                        sin = p5 / Math.sqrt(p25 + p25 * cotan * cotan);
                        cos = sin * cotan;
                        tau = 1.0;
                        if (Math.abs(cos) * giant > 1.0) {
                            tau = 1.0 / cos;
                        }
                    } else {
                        tan = w[j - 1] / s[jj - 1];
                        cos = p5 / Math.sqrt(p25 + p25 * tan * tan);
                        sin = cos * tan;
                        tau = sin;
                    }

                    /*        apply the transformation to s and reduce the spike in w. */

                    l = jj;
                    for (i = j; i <= m; i++) {
                        temp = cos * s[l - 1] + sin * w[i - 1];
                        w[i - 1] = -sin * s[l - 1] + cos * w[i - 1];
                        s[l - 1] = temp;
                        l = l + 1;
                    }
                    /*        store the information necessary to recover the */
                    /*        givens rotation. */

                    w[j - 1] = tau;
                }
                /*        test for zero diagonal elements in the output s. */
                if (s[jj - 1] == 0.0) {
                    sing = true;
                }
                jj = jj + (m - j + 1);
            }
        }
        /*     move w back into the last column of the output s. */
        l = jj;
        for (i = n; i <= m; i++) {
            s[l - 1] = w[i - 1];
            l = l + 1;
        }
        if (s[jj - 1] == 0.0) {
            sing = true;
        }
        return sing;
    }

    /***********

     subroutine dogleg

     given an m by n matrix a, an n by n nonsingular diagonal
     matrix d, an m-vector b, and a positive number delta, the
     problem is to determine the convex combination x of the
     gauss-newton and scaled gradient directions that minimizes
     (a*x - b) in the least squares sense, subject to the
     restriction that the euclidean norm of d*x be at most delta.

     this subroutine completes the solution of the problem
     if it is provided with the necessary information from the
     qr factorization of a. that is, if a = q*r, where q has
     orthogonal columns and r is an upper triangular matrix,
     then dogleg expects the full upper triangle of r and
     the first n components of (q transpose)*b.

     the subroutine statement is

     subroutine dogleg(n,r,lr,diag,qtb,delta,x,wa1,wa2)

     where

     @param n is a positive integer input variable set to the order of r.

     @param r is an input array of length lr which must contain the upper
     triangular matrix r stored by rows.

     @param lr is a positive integer input variable not less than
     (n*(n+1))/2.

     @param diag is an input array of length n which must contain the
     diagonal elements of the matrix d.

     @param qtb is an input array of length n which must contain the first
     n elements of the vector (q transpose)*b.

     @param delta is a positive input variable which specifies an upper
     bound on the euclidean norm of d*x.

     @param x is an output array of length n which contains the desired
     convex combination of the gauss-newton direction and the
     scaled gradient direction.

     @param wa1 and wa2 are work arrays of length n.

     @author argonne national laboratory. minpack project. march 1980.
     @author burton s. garbow, kenneth e. hillstrom, jorge j. more

     */
    public static void dogleg(int n, double r[], int lr, double diag[], double qtb[],
                              double delta, double x[], double wa1[], double wa2[]) {

        int i,j,jj,jp1,k,l;
        double alpha,bnorm,epsmch,gnorm,qnorm,sgnorm,sum,temp;
        /*     epsmch is the machine precision. */
        epsmch = dpmpar(1);
        /*     first, calculate the gauss-newton direction. */
        jj = (n * (n + 1)) / 2 + 1;

        for (k = 1; k <= n; k++) {
            j = n - k + 1;
            jp1 = j + 1;
            jj = jj - k;
            l = jj + 1;
            sum = 0.0;
            if (n >= jp1) {
                for (i = jp1; i <= n; i++) {
                    sum = sum + r[l - 1] * x[i - 1];
                    l = l + 1;
                }
            }
            temp = r[jj - 1];
            if (temp == 0.0) {
                l = j;
                for (i = 1; i <= j; i++) {
                    temp = Math.max(temp, Math.abs(r[l - 1]));
                    l = l + n - i;
                }
                temp = epsmch * temp;
                if (temp == 0.0) {
                    temp = epsmch;
                }
            }
            x[j - 1] = (qtb[j - 1] - sum) / temp;
        }
        /*     test whether the gauss-newton direction is acceptable. */
        for (j = 1; j <= n; j++) {
            wa1[j-1] = 0.0;
            wa2[j-1] = diag[j-1] * x[j-1];
        }
        qnorm = enorm(n, 0, wa2);

        if (qnorm <= delta) {
            return;
        }
        /*     the gauss-newton direction is not acceptable. */
        /*     next, calculate the scaled gradient direction. */
        l = 1;
        for (j = 1; j <= n; j++) {
            temp = qtb[j-1];
            for (i = j; i <= n; i++) {
                wa1[i-1] = wa1[i-1] + r[l-1] * temp;
                l = l + 1;
            }
            wa1[j-1] = wa1[j-1] / diag[j-1];
        }

        /*     calculate the norm of the scaled gradient and test for */
        /*     the special case in which the scaled gradient is zero. */

        gnorm = enorm(n, 0, wa1);
        sgnorm = 0.0;
        alpha = delta / qnorm;

        if (gnorm != 0.0) {

            /*     calculate the point along the scaled gradient */
            /*     at which the quadratic is minimized. */

            for (j = 1; j <= n; j++) {
                wa1[j-1] = (wa1[j-1] / gnorm) / diag[j-1];
            }
            l = 1;
            for (j = 1; j <= n; j++) {
                sum = 0.0;
                for (i = j; i <= n; i++) {
                    sum = sum + r[l-1] * wa1[i-1];
                    l = l + 1;
                }
                wa2[j-1] = sum;
            }
            temp = enorm(n, 0, wa2);
            sgnorm = (gnorm / temp) / temp;

            /*     test whether the scaled gradient direction is acceptable. */

            alpha = 0.0;

            if (sgnorm < delta) {

                /*     the scaled gradient direction is not acceptable. */
                /*     finally, calculate the point along the dogleg */
                /*     at which the quadratic is minimized. */

                bnorm = enorm(n, 0, qtb);
                temp = (bnorm / gnorm) * (bnorm / qnorm) * (sgnorm / delta);
                temp = temp - (delta / qnorm) * (sgnorm / delta) * (sgnorm / delta)
                        + Math.sqrt((temp - (delta / qnorm))*(temp - (delta / qnorm))
                        + (1.0 - (delta / qnorm) * (delta / qnorm))
                        * (1.0 - (sgnorm / delta) * (sgnorm / delta)));
                alpha = ((delta / qnorm)
                        * (1.0 - (sgnorm / delta) * (sgnorm / delta))) / temp;
            }
        }
        /*     form appropriate convex combination of the gauss-newton */
        /*     direction and the scaled gradient direction. */

        temp = (1.0 - alpha) * Math.min(sgnorm, delta);
        for (j = 1; j <= n; j++) {
            x[j-1] = temp * wa1[j-1] + alpha * x[j-1];
        }
    }

    /***********

     subroutine qform

     this subroutine proceeds from the computed qr factorization of
     an m by n matrix a to accumulate the m by m orthogonal matrix
     q from its factored form.

     the subroutine statement is

     subroutine qform(m,n,q,ldq,wa)

     where

     @param m is a positive integer input variable set to the number
     of rows of a and the order of q.

     @param n is a positive integer input variable set to the number
     of columns of a.

     @param q is an m by m array. on input the full lower trapezoid in
     the first min(m,n) columns of q contains the factored form.
     on output q has been accumulated into a square matrix.

     @param ldq is a positive integer input variable not less than m
     which specifies the leading dimension of the array q.

     @param wa is a work array of length m.

     @author argonne national laboratory. minpack project. march 1980.
     @author burton s. garbow, kenneth e. hillstrom, jorge j. more
     */
    public static void qform(int m, int n, double q[], int ldq, double[] wa) {
        int i,j,k,minmn;
        double sum,temp;
        /*     zero out upper triangle of q in the first min(m,n) columns. */

        minmn = Math.min(m, n);
        if (minmn >= 2) {
            for (j = 2; j <= minmn; j++) {
                int jm1 = j - 1;
                for (i = 1; i <= jm1; i++) {
                    q[(i-1) + (j-1) * ldq] = 0.0;
                }
            }
        }
        /*     initialize remaining columns to those of the identity matrix. */

        int np1 = n + 1;
        if (m >= np1) {
            for (j = np1; j <= m; j++) {
                for (i = 1; i <= m; i++) {
                    q[(i-1) + (j-1) * ldq] = 0.0;
                }
                q[(j-1) + (j-1) * ldq] = 1.0;
            }
        }

        /*     accumulate q from its factored form. */

        for (int l = 1; l <= minmn; l++) {
            k = minmn - l + 1;
            for (i = k; i <= m; i++) {
                wa[i-1] = q[(i-1) + (k-1) * ldq];
                q[(i-1) + (k-1) * ldq] = 0.0;
            }
            q[(k-1) + (k-1) * ldq] = 1.0;

            if (wa[k-1] != 0.0) {
                for (j = k; j <= m; j++) {
                    sum = 0.0;
                    for (i = k; i <= m; i++) {
                        sum = sum + q[(i-1) + (j-1) * ldq] * wa[i-1];
                    }
                    temp = sum / wa[k-1];
                    for (i = k; i <= m; i++) {
                        q[(i-1) + (j-1) * ldq] = q[(i-1) + (j-1) * ldq] - temp * wa[i-1];
                    }
                }
            }
        }
    }

    /**
     * Interface for user-supplied subroutine which
     * calculates the functions. fcn must be declared
     * in an external statement in the user calling
     * program, and should be written as follows.
     * <p>
     * Calculate the functions at x and
     * return this vector in fvec.
     * <p>
     * the value of iflag should not be changed by fcn unless
     * the user wants to terminate execution of fdjac1.
     * in this case set iflag to a negative integer.
     */
    public interface Hybrd_Function {
        void apply ( int n, double x[], double fvec[], int[] iflag );
    }

    /**********

     subroutine fdjac1

     this subroutine computes a forward-difference approximation
     to the n by n jacobian matrix associated with a specified
     problem of n functions in n variables. if the jacobian has
     a banded form, then function evaluations are saved by only
     approximating the nonzero terms.

     the subroutine statement is

     subroutine fdjac1(fcn,n,x,fvec,fjac,ldfjac,iflag,ml,mu,epsfcn,
     wa1,wa2)

     where

     @param fcn is the name of the user-supplied subroutine which
     calculates the functions. fcn must be declared
     in an external statement in the user calling
     program, and should be written as follows.

     subroutine fcn(n,x,fvec,iflag)
     integer n,iflag
     double precision x(n),fvec(n)
     ----------
     calculate the functions at x and
     return this vector in fvec.
     ----------
     return
     end

     the value of iflag should not be changed by fcn unless
     the user wants to terminate execution of fdjac1.
     in this case set iflag to a negative integer.

     @param n is a positive integer input variable set to the number
     of functions and variables.

     @param x is an input array of length n.

     @param fvec is an input array of length n which must contain the
     functions evaluated at x.

     @param fjac is an output n by n array which contains the
     approximation to the jacobian matrix evaluated at x.

     @param ldfjac is a positive integer input variable not less than n
     which specifies the leading dimension of the array fjac.

     @param iflag is an integer variable which can be used to terminate
     the execution of fdjac1. see description of fcn.

     @param ml is a nonnegative integer input variable which specifies
     the number of subdiagonals within the band of the
     jacobian matrix. if the jacobian is not banded, set
     ml to at least n - 1.

     @param epsfcn is an input variable used in determining a suitable
     step length for the forward-difference approximation. this
     approximation assumes that the relative errors in the
     functions are of the order of epsfcn. if epsfcn is less
     than the machine precision, it is assumed that the relative
     errors in the functions are of the order of the machine
     precision.

     @param mu is a nonnegative integer input variable which specifies
     the number of superdiagonals within the band of the
     jacobian matrix. if the jacobian is not banded, set
     mu to at least n - 1.

     @param wa1 and wa2 are work arrays of length n. if ml + mu + 1 is at
     least n, then the jacobian is considered dense, and wa2 is
     not referenced.

     @author argonne national laboratory. minpack project. march 1980.
     @author burton s. garbow, kenneth e. hillstrom, jorge j. more

      */
    public static void fdjac1(Hybrd_Function fcn,
                              int n, double x[], double fvec[], double fjac[], int ldfjac, int[] iflag,
                              int ml, int mu, double epsfcn, double wa1[], double wa2[]) {
        int i,j,k,msum;
        double eps,epsmch,h,temp;

        /*     epsmch is the machine precision. */

        epsmch = dpmpar(1);

        eps = Math.sqrt(Math.max(epsfcn, epsmch));
        msum = ml + mu + 1;

        /*        computation of dense approximate jacobian. */

        if (msum >= n) {
            for (j = 1; j <= n; j++) {
                temp = x[j-1];
                h = eps * Math.abs(temp);
                if (h == 0.0) {
                    h = eps;
                }
                x[j-1] = temp + h;
                fcn.apply(n, x, wa1, iflag);
                if (iflag[0] < 0) {
                    break;
                }
                x[j-1] = temp;
                for (i = 1; i <= n; i++) {
                    fjac[(i-1) + (j-1) * ldfjac] = (wa1[i-1] - fvec[i-1]) / h;
                }
            }
            return;
        }

        /*        computation of banded approximate jacobian. */


        for (k = 1; k <= msum; k++) {
            for (j = k; j <= n; j = j + msum) {
                wa2[j-1] = x[j-1];
                h = eps * Math.abs(wa2[j-1]);
                if (h == 0.0) {
                    h = eps;
                }
                x[j-1] = wa2[j-1] + h;
            }
            fcn.apply(n, x, wa1, iflag);
            if (iflag[0] < 0) {
                break;
            }
            for (j = k; j <= n; j = j + msum) {
                x[j-1] = wa2[j-1];
                h = eps * Math.abs(wa2[j-1]);
                if (h == 0.0) {
                    h = eps;
                }
                for (i = 1; i <= n; i++) {
                    fjac[(i-1) + (j-1) * ldfjac] = 0.0;
                    if (i >= j - mu && i <= j + ml) {
                        fjac[(i-1) + (j-1) * ldfjac] = (wa1[i-1] - fvec[i-1]) / h;
                    }
                }
            }
        }
    }

    /**
     * subroutine hybrd
     * <p>
     * the purpose of hybrd is to find a zero of a system of
     * n nonlinear functions in n variables by a modification
     * of the powell hybrid method. the user must provide a
     * subroutine which calculates the functions. the jacobian is
     * then calculated by a forward-difference approximation.
     * <p>
     * the subroutine statement is
     * <p>
     * subroutine hybrd(fcn,n,x,fvec,xtol,maxfev,ml,mu,epsfcn,
     * diag,mode,factor,nprint,info,nfev,fjac,
     * ldfjac,r,lr,qtf,wa1,wa2,wa3,wa4)
     * <p>
     * where
     *
     * @param fcn    is the name of the user-supplied subroutine which
     *               calculates the functions. fcn must be declared
     *               in an external statement in the user calling
     *               program, and should be written as follows.
     *               <p>
     *               subroutine fcn(n,x,fvec,iflag)
     *               integer n,iflag
     *               double precision x(n),fvec(n)
     *               ----------
     *               calculate the functions at x and
     *               return this vector in fvec.
     *               ---------
     *               return
     *               end
     *               <p>
     *               the value of iflag should not be changed by fcn unless
     *               the user wants to terminate execution of hybrd.
     *               in this case set iflag to a negative integer.
     * @param n      is a positive integer input variable set to the number
     *               of functions and variables.
     * @param x      is an array of length n. on input x must contain
     *               an initial estimate of the solution vector. on output x
     *               contains the final estimate of the solution vector.
     * @param fvec   is an output array of length n which contains
     *               the functions evaluated at the output x.
     * @param xtol   is a nonnegative input variable. termination
     *               occurs when the relative error between two consecutive
     *               iterates is at most xtol.
     * @param maxfev is a positive integer input variable. termination
     *               occurs when the number of calls to fcn is at least maxfev
     *               by the end of an iteration.
     * @param ml     is a nonnegative integer input variable which specifies
     *               the number of subdiagonals within the band of the
     *               jacobian matrix. if the jacobian is not banded, set
     *               ml to at least n - 1.
     * @param mu     is a nonnegative integer input variable which specifies
     *               the number of superdiagonals within the band of the
     *               jacobian matrix. if the jacobian is not banded, set
     *               mu to at least n - 1.
     * @param epsfcn is an input variable used in determining a suitable
     *               step length for the forward-difference approximation. this
     *               approximation assumes that the relative errors in the
     *               functions are of the order of epsfcn. if epsfcn is less
     *               than the machine precision, it is assumed that the relative
     *               errors in the functions are of the order of the machine
     *               precision.
     * @param diag   is an array of length n. if mode = 1 (see
     *               below), diag is internally set. if mode = 2, diag
     *               must contain positive entries that serve as
     *               multiplicative scale factors for the variables.
     * @param mode   is an integer input variable. if mode = 1, the
     *               variables will be scaled internally. if mode = 2,
     *               the scaling is specified by the input diag. other
     *               values of mode are equivalent to mode = 1.
     * @param factor is a positive input variable used in determining the
     *               initial step bound. this bound is set to the product of
     *               factor and the euclidean norm of diag*x if nonzero, or else
     *               to factor itself. in most cases factor should lie in the
     *               interval (.1,100.). 100. is a generally recommended value.
     * @param nprint is an integer input variable that enables controlled
     *               printing of iterates if it is positive. in this case,
     *               fcn is called with iflag = 0 at the beginning of the first
     *               iteration and every nprint iterations thereafter and
     *               immediately prior to return, with x and fvec available
     *               for printing. if nprint is not positive, no special calls
     *               of fcn with iflag = 0 are made.
     * @param nfev   is an integer output variable set to the number of
     *               calls to fcn.
     * @param fjac   is an output n by n array which contains the
     *               orthogonal matrix q produced by the qr factorization
     *               of the final approximate jacobian.
     * @param ldfjac is a positive integer input variable not less than n
     *               which specifies the leading dimension of the array fjac.
     * @param r      is an output array of length lr which contains the
     *               upper triangular matrix produced by the qr factorization
     *               of the final approximate jacobian, stored rowwise.
     * @param lr     is a positive integer input variable not less than
     *               (n*(n+1))/2.
     * @param qtf    is an output array of length n which contains
     *               the vector (q transpose)*fvec.
     * @param wa1,   wa2, wa3, and wa4 are work arrays of length n.
     *               <p>
     *               subprograms called
     *               <p>
     *               user-supplied ...... fcn
     *               <p>
     *               minpack-supplied ... dogleg,dpmpar,enorm,fdjac1,
     *               qform,qrfac,r1mpyq,r1updt
     *               <p>
     *               fortran-supplied ... dabs,dmax1,dmin1,min0,mod
     * @return info is an integer output variable. if the user has
     * terminated execution, info is set to the (negative)
     * value of iflag. see description of fcn. otherwise,
     * info is set as follows.
     * <p>
     * info = 0   improper input parameters.
     * <p>
     * info = 1   relative error between two consecutive iterates
     * is at most xtol.
     * <p>
     * info = 2   number of calls to fcn has reached or exceeded
     * maxfev.
     * <p>
     * info = 3   xtol is too small. no further improvement in
     * the approximate solution x is possible.
     * <p>
     * info = 4   iteration is not making good progress, as
     * measured by the improvement from the last
     * five jacobian evaluations.
     * <p>
     * info = 5   iteration is not making good progress, as
     * measured by the improvement from the last
     * ten iterations.
     * @author argonne national laboratory. minpack project. march 1980.
     * @athor burton s. garbow, kenneth e. hillstrom, jorge j. more
     */
    public static int hybrd(Hybrd_Function fcn,
                            int n, double x[],
                            double fvec[], double xtol, int maxfev, int ml, int mu, double epsfcn,
                            double diag[], int mode, double factor, int nprint, int[] nfev,
                            double fjac[], int ldfjac, double r[], int lr, double qtf[], double wa1[],
                            double wa2[], double wa3[], double wa4[]) {
        int i,iflag,info,iter,j,l,msum,ncfail,ncsuc,nslow1,nslow2;
        int[] iwa = new int[1];
        boolean jeval;
        final double p001 = 0.001;
        final double p0001 = 0.0001;
        final double p1 = 0.1;
        final double p5 = 0.5;
        double actred,delta,epsmch,fnorm,fnorm1,pnorm,
                prered,ratio,sum,temp,xnorm;
        int[] iflag_ = new int[1];

        /*     epsmch is the machine precision. */
        epsmch = dpmpar(1);

        info = 0;
        iflag = 0;
        nfev[0] = 0;

        /*     check the input parameters for errors. */

        if (n <= 0
            ||xtol < 0.0
            ||maxfev <= 0
            ||ml < 0
            ||mu < 0
            ||factor <= 0.0
            ||ldfjac < n
            ||lr < (n * (n + 1)) / 2) {
            info = 0;
            return info;
        }
        if (mode == 2) {
            for (j = 1; j <= n; j++) {
                if (diag[j-1] <= 0.0) {
                    info = 0;
                    return info;
                }
            }
        }

        /*     evaluate the function at the starting point */
        /*     and calculate its norm. */

        iflag = 1;
        iflag_[0] = iflag;
        fcn.apply(n, x, fvec, iflag_);
        iflag = iflag_[0];
        nfev[0] = 1;
        if (iflag < 0) {
            info = iflag;
            return info;
        }

        fnorm = enorm(n, 0, fvec);

        /*     determine the number of calls to fcn needed to compute */
        /*     the jacobian matrix. */

        msum = Math.min(ml + mu + 1, n);

        /*     initialize iteration counter and monitors. */

        iter = 1;
        ncsuc = 0;
        ncfail = 0;
        nslow1 = 0;
        nslow2 = 0;
        delta = 0;
        xnorm = 0;

        /*     beginning of the outer loop. */
        outerloop:
        for (;;) {
            jeval = true;

            /*        calculate the jacobian matrix. */

            iflag = 2;
            iflag_[0] = iflag;
            fdjac1(fcn, n, x, fvec, fjac, ldfjac, iflag_, ml, mu, epsfcn, wa1, wa2);
            iflag = iflag_[0];

            nfev[0] = nfev[0] + msum;
            if (iflag < 0) {
                break outerloop;
            }

            /*        compute the qr factorization of the jacobian. */

            qrfac(n, n, fjac, ldfjac, 0, iwa, 1, wa1, wa2, wa3);

            /*        on the first iteration and if mode is 1, scale according */
            /*        to the norms of the columns of the initial jacobian. */

            if (iter == 1) {
                if (mode != 2) {
                    for (j = 1; j <= n; j++) {
                        diag[j - 1] = wa2[j - 1];
                        if (wa2[j - 1] == 0.0) {
                            diag[j - 1] = 1.0;
                        }
                    }
                }

                /*        on the first iteration, calculate the norm of the scaled x */
                /*        and initialize the step bound delta. */

                for (j = 1; j <= n; j++) {
                    wa3[j - 1] = diag[j - 1] * x[j - 1];
                }
                xnorm = enorm(n, 0, wa3);
                delta = factor * xnorm;
                if (delta == 0.0) delta = factor;
            }

            /*        form (q transpose)*fvec and store in qtf. */

            for (i = 1; i <= n; i++) {
                qtf[i - 1] = fvec[i - 1];
            }
            for (j = 1; j <= n; j++) {
                if (fjac[(j - 1) + (j - 1) * ldfjac] != 0.0) {
                    sum = 0.0;
                    for (i = j; i <= n; i++) {
                        sum = sum + fjac[(i - 1) + (j - 1) * ldfjac] * qtf[i - 1];
                    }
                    temp = -sum / fjac[(j - 1) + (j - 1) * ldfjac];
                    for (i = j; i <= n; i++) {
                        qtf[i - 1] = qtf[i - 1] + fjac[(i - 1) + (j - 1) * ldfjac] * temp;
                    }
                }
            }
            /*        copy the triangular factor of the qr factorization into r. */

            for (j = 1; j <= n; j++) {
                l = j;
                int jm1 = j - 1;
                if (jm1 >= 1) {
                    for (i = 1; i <= jm1; i++) {
                        r[l - 1] = fjac[(i - 1) + (j - 1) * ldfjac];
                        l = l + n - i;
                    }
                }
                r[l - 1] = wa1[j - 1];
                if (wa1[j - 1] == 0.0) {
                    System.err.println("  Matrix is singular.");
                }
            }

            /*        accumulate the orthogonal factor in fjac. */

            qform(n, n, fjac, ldfjac, wa1);

            /*        rescale if necessary. */

            if (mode != 2) {
                for (j = 1; j <= n; j++) {
                    diag[j - 1] = Math.max(diag[j - 1], wa2[j - 1]);
                }
            }

            /*        beginning of the inner loop. */
            innerloop:
            for (; ; ) {
                /*           if requested, call fcn to enable printing of iterates. */

                if (nprint > 0) {
                    if ((iter - 1) % nprint == 0) {
                        iflag = 0;
                        iflag_[0] = iflag;
                        fcn.apply(n, x, fvec, iflag_);
                        iflag = iflag_[0];
                        if (iflag < 0) {
                            break outerloop;
                        }
                    }
                }

                /*           determine the direction p. */

                dogleg(n, r, lr, diag, qtf, delta, wa1, wa2, wa3);

                /*           store the direction p and x + p. calculate the norm of p. */

                for (j = 1; j <= n; j++) {
                    wa1[j - 1] = -wa1[j - 1];
                    wa2[j - 1] = x[j - 1] + wa1[j - 1];
                    wa3[j - 1] = diag[j - 1] * wa1[j - 1];
                }
                pnorm = enorm(n, 0, wa3);

                /*           on the first iteration, adjust the initial step bound. */

                if (iter == 1) {
                    delta = Math.min(delta, pnorm);
                }

                /*           evaluate the function at x + p and calculate its norm. */

                iflag = 1;
                iflag_[0] = iflag;
                fcn.apply(n, wa2, wa4, iflag_);
                iflag = iflag_[0];
                nfev[0] = nfev[0] + 1;
                if (iflag < 0) {
                    break outerloop;
                }
                fnorm1 = enorm(n, 0, wa4);

                /*           compute the scaled actual reduction. */

                actred = -1.0;
                if (fnorm1 < fnorm) {
                    actred = 1.0 - (fnorm1 / fnorm) * (fnorm1 / fnorm);
                }

                /*           compute the scaled predicted reduction. */

                l = 1;
                for (i = 1; i <= n; i++) {
                    sum = 0.0;
                    for (j = i; j <= n; j++) {
                        sum = sum + r[l - 1] * wa1[j - 1];
                        l = l + 1;
                    }
                    wa3[i - 1] = qtf[i - 1] + sum;
                }
                temp = enorm(n, 0, wa3);
                prered = 0.0;
                if (temp < fnorm) {
                    prered = 1.0 - (temp / fnorm) * (temp / fnorm);
                }

                /*           compute the ratio of the actual to the predicted */
                /*           reduction. */

                ratio = 0.0;
                if (prered > 0.0) {
                    ratio = actred / prered;
                }

                /*           update the step bound. */

                if (!(ratio >= p1)) {
                    ncsuc = 0;
                    ncfail = ncfail + 1;
                    delta = p5 * delta;
                } else {
                    ncfail = 0;
                    ncsuc = ncsuc + 1;
                    if (ratio >= p5 || ncsuc > 1) {
                        delta = Math.max(delta, pnorm / p5);
                    }
                    if (Math.abs(ratio - 1.0) <= p1) {
                        delta = pnorm / p5;
                    }
                }
                /*           test for successful iteration. */

                if (!(ratio < p0001)) {

                    /*           successful iteration. update x, fvec, and their norms. */
                    for (j = 1; j <= n; j++) {
                        x[j - 1] = wa2[j - 1];
                        wa2[j - 1] = diag[j - 1] * x[j - 1];
                        fvec[j - 1] = wa4[j - 1];
                    }
                    xnorm = enorm(n, 0, wa2);
                    fnorm = fnorm1;
                    iter = iter + 1;
                }

                /*           determine the progress of the iteration. */

                nslow1 = nslow1 + 1;
                if (actred >= p001) nslow1 = 0;
                if (jeval) nslow2 = nslow2 + 1;
                if (actred >= p1) nslow2 = 0;
                /*           test for convergence. */

                if (delta <= xtol * xnorm || fnorm == 0.0) info = 1;
                if (info != 0) break outerloop;

                /*           tests for termination and stringent tolerances. */

                if (nfev[0] >= maxfev) info = 2;
                if (p1 * Math.max(p1 * delta, pnorm) <= epsmch * xnorm) info = 3;
                if (nslow2 == 5) info = 4;
                if (nslow1 == 10) info = 5;
                if (info != 0) break outerloop;

                /*           criterion for recalculating jacobian approximation */
                /*           by forward differences. */

                if (ncfail == 2) break innerloop;

                /*           calculate the rank one modification to the jacobian */
                /*           and update qtf if necessary. */

                for (j = 1; j <= n; j++) {
                    sum = 0.0;
                    for (i = 1; i <= n; i++) {
                        sum = sum + fjac[(i - 1) + (j - 1) * ldfjac] * wa4[i - 1];
                    }
                    wa2[j - 1] = (sum - wa3[j - 1]) / pnorm;
                    wa1[j - 1] = diag[j - 1] * ((diag[j - 1] * wa1[j - 1]) / pnorm);
                    if (ratio >= p0001) {
                        qtf[j - 1] = sum;
                    }
                }
                /*           compute the qr factorization of the updated jacobian. */

                r1updt(n, n, r, lr, wa1, wa2, wa3);
                r1mpyq(n, n, fjac, ldfjac, wa2, wa3);
                r1mpyq(1, n, qtf, 1, wa2, wa3);

                jeval = false;
            }
            /* end of the inner loop. */
        }

        /* end of the outer loop. */


        /*     termination, either normal or user imposed. */

        if (iflag < 0) {
            info = iflag;
        }
        if (nprint > 0) {
            iflag_[0] = 0;
            fcn.apply(n, x, fvec, iflag_);
        }
        return info;

        /*     last card of subroutine hybrd. */
    }

    /**
     * subroutine hybrd1
     * <p>
     * the purpose of hybrd1 is to find a zero of a system of
     * n nonlinear functions in n variables by a modification
     * of the powell hybrid method. this is done by using the
     * more general nonlinear equation solver hybrd. the user
     * must provide a subroutine which calculates the functions.
     * the jacobian is then calculated by a forward-difference
     * approximation.
     * <p>
     * the subroutine statement is
     * <p>
     * subroutine hybrd1(fcn,n,x,fvec,tol,info,wa,lwa)
     * <p>
     * where
     *
     * @param fcn  is the name of the user-supplied subroutine which
     *             calculates the functions. fcn must be declared
     *             in an external statement in the user calling
     *             program, and should be written as follows.
     *             <p>
     *             subroutine fcn(n,x,fvec,iflag)
     *             integer n,iflag
     *             double precision x(n),fvec(n)
     *             ----------
     *             calculate the functions at x and
     *             return this vector in fvec.
     *             ---------
     *             return
     *             end
     *             <p>
     *             the value of iflag should not be changed by fcn unless
     *             the user wants to terminate execution of hybrd1.
     *             in this case set iflag to a negative integer.
     * @param n    is a positive integer input variable set to the number
     *             of functions and variables.
     * @param x    is an array of length n. on input x must contain
     *             an initial estimate of the solution vector. on output x
     *             contains the final estimate of the solution vector.
     * @param fvec is an output array of length n which contains
     *             the functions evaluated at the output x.
     * @param tol  is a nonnegative input variable. termination occurs
     *             when the algorithm estimates that the relative error
     *             between x and the solution is at most tol.
     * @param wa   is a work array of length lwa.
     * @param lwa  is a positive integer input variable not less than
     *             (n*(3*n+13))/2.
     * @return info is an integer output variable. if the user has
     * terminated execution, info is set to the (negative)
     * value of iflag. see description of fcn. otherwise,
     * info is set as follows.
     * <p>
     * info = 0   improper input parameters.
     * <p>
     * info = 1   algorithm estimates that the relative error
     * between x and the solution is at most tol.
     * <p>
     * info = 2   number of calls to fcn has reached or exceeded
     * 200*(n+1).
     * <p>
     * info = 3   tol is too small. no further improvement in
     * the approximate solution x is possible.
     * <p>
     * info = 4   iteration is not making good progress.
     * @author argonne national laboratory. minpack project. march 1980.
     * @author burton s. garbow, kenneth e. hillstrom, jorge j. more
     */
    public static int hybrd1 (Hybrd_Function fcn, int n,
                              double x[], double fvec[], double tol, double wa[], int lwa) {
        // Setting epsfcn to 0 means default machine precision will be used
        return hybrd1(fcn, n, x, fvec, tol, wa, lwa, 0.0);
    }

    /**
     * subroutine hybrd1
     * <p>
     * the purpose of hybrd1 is to find a zero of a system of
     * n nonlinear functions in n variables by a modification
     * of the powell hybrid method. this is done by using the
     * more general nonlinear equation solver hybrd. the user
     * must provide a subroutine which calculates the functions.
     * the jacobian is then calculated by a forward-difference
     * approximation.
     * <p>
     * the subroutine statement is
     * <p>
     * subroutine hybrd1(fcn,n,x,fvec,tol,info,wa,lwa)
     * <p>
     * where
     *
     * @param fcn  is the name of the user-supplied subroutine which
     *             calculates the functions. fcn must be declared
     *             in an external statement in the user calling
     *             program, and should be written as follows.
     *             <p>
     *             subroutine fcn(n,x,fvec,iflag)
     *             integer n,iflag
     *             double precision x(n),fvec(n)
     *             ----------
     *             calculate the functions at x and
     *             return this vector in fvec.
     *             ---------
     *             return
     *             end
     *             <p>
     *             the value of iflag should not be changed by fcn unless
     *             the user wants to terminate execution of hybrd1.
     *             in this case set iflag to a negative integer.
     * @param n    is a positive integer input variable set to the number
     *             of functions and variables.
     * @param x    is an array of length n. on input x must contain
     *             an initial estimate of the solution vector. on output x
     *             contains the final estimate of the solution vector.
     * @param fvec is an output array of length n which contains
     *             the functions evaluated at the output x.
     * @param tol  is a nonnegative input variable. termination occurs
     *             when the algorithm estimates that the relative error
     *             between x and the solution is at most tol.
     * @param wa   is a work array of length lwa.
     * @param lwa  is a positive integer input variable not less than
     *             (n*(3*n+13))/2.
     * @return info is an integer output variable. if the user has
     * terminated execution, info is set to the (negative)
     * value of iflag. see description of fcn. otherwise,
     * info is set as follows.
     * <p>
     * info = 0   improper input parameters.
     * <p>
     * info = 1   algorithm estimates that the relative error
     * between x and the solution is at most tol.
     * <p>
     * info = 2   number of calls to fcn has reached or exceeded
     * 200*(n+1).
     * <p>
     * info = 3   tol is too small. no further improvement in
     * the approximate solution x is possible.
     * <p>
     * info = 4   iteration is not making good progress.
     * @author argonne national laboratory. minpack project. march 1980.
     * @author burton s. garbow, kenneth e. hillstrom, jorge j. more
     */
    public static int hybrd1 (Hybrd_Function fcn, int n,
                              double x[], double fvec[], double tol, double wa[], int lwa, double epsfcn )
    {
        int info,j,lr,maxfev,ml,mode,mu,nprint;
        int[] nfev = new int[1];
        double factor,xtol;

        info = 0;
/*
  Check the input.
*/
        if ( n <= 0 || tol <= 0.0 || lwa < ( n * ( 3 * n + 13 ) ) / 2 )
        {
            return info;
        }
/*
  Call HYBRD.
*/
        maxfev = 200 * ( n + 1 );
        xtol = tol;
        ml = n - 1;
        mu = n - 1;
        mode = 2;
        for ( j = 0; j < n; j++ )
        {
            wa[j] = 1.0;
        }
        factor = 100.0;
        nprint = 0;
        lr = ( n * ( n + 1 ) ) / 2;

        double[] fjac = new double[n*n];
        double[] r = new double[lr];
        double[] qtf = new double[n];
        double[] wa1 = new double[n];
        double[] wa2 = new double[n];
        double[] wa3 = new double[n];
        double[] wa4 = new double[n];
        info = hybrd ( fcn, n, x, fvec, xtol, maxfev, ml, mu, epsfcn, wa, mode,
                factor, nprint, nfev, fjac, n, r, lr,
                qtf, wa1, wa2, wa3, wa4 );

        if ( info == 5 )
        {
            info = 4;
        }
        return info;
    }


    /**
     * subroutine chkder
     * <p>
     * this subroutine checks the gradients of m nonlinear functions
     * in n variables, evaluated at a point x, for consistency with
     * the functions themselves. the user must call chkder twice,
     * first with mode = 1 and then with mode = 2.
     * <p>
     * mode = 1. on input, x must contain the point of evaluation.
     * on output, xp is set to a neighboring point.
     * <p>
     * mode = 2. on input, fvec must contain the functions and the
     * rows of fjac must contain the gradients
     * of the respective functions each evaluated
     * at x, and fvecp must contain the functions
     * evaluated at xp.
     * on output, err contains measures of correctness of
     * the respective gradients.
     * <p>
     * the subroutine does not perform reliably if cancellation or
     * rounding errors cause a severe loss of significance in the
     * evaluation of a function. therefore, none of the components
     * of x should be unusually small (in particular, zero) or any
     * other value which may cause loss of significance.
     * <p>
     * the subroutine statement is
     * <p>
     * subroutine chkder(m,n,x,fvec,fjac,ldfjac,xp,fvecp,mode,err)
     * <p>
     * where
     *
     * @param m      is a positive integer input variable set to the number
     *               of functions.
     * @param n      is a positive integer input variable set to the number
     *               of variables.
     * @param x      is an input array of length n.
     * @param fvec   is an array of length m. on input when mode = 2,
     *               fvec must contain the functions evaluated at x.
     * @param fjac   is an m by n array. on input when mode = 2,
     *               the rows of fjac must contain the gradients of
     *               the respective functions evaluated at x.
     * @param ldfjac is a positive integer input parameter not less than m
     *               which specifies the leading dimension of the array fjac.
     * @param xp     is an array of length n. on output when mode = 1,
     *               xp is set to a neighboring point of x.
     * @param fvecp  is an array of length m. on input when mode = 2,
     *               fvecp must contain the functions evaluated at xp.
     * @param mode   is an integer input variable set to 1 on the first call
     *               and 2 on the second. other values of mode are equivalent
     *               to mode = 1.
     * @param err    is an array of length m. on output when mode = 2,
     *               err contains measures of correctness of the respective
     *               gradients. if there is no severe loss of significance,
     *               then if err(i) is 1.0 the i-th gradient is correct,
     *               while if err(i) is 0.0 the i-th gradient is incorrect.
     *               for values of err between 0.0 and 1.0, the categorization
     *               is less certain. in general, a value of err(i) greater
     *               than 0.5 indicates that the i-th gradient is probably
     *               correct, while a value of err(i) less than 0.5 indicates
     *               that the i-th gradient is probably incorrect.

     * @author argonne national laboratory. minpack project. march 1980.
     * @author burton s. garbow, kenneth e. hillstrom, jorge j. more
     */
    public static void chkder(int m, int n, double x[], double fvec[], double fjac[],
                int ldfjac, double xp[], double fvecp[], int mode, double err[]) {
        /* Local variables */
        int i, j;
        double eps, epsf, temp, epsmch;
        double epslog;

        final double factor = 100.0;


        /* epsmch is the machine precision. */
        epsmch = dpmpar(1);

        eps = Math.sqrt(epsmch);

        /*        mode = 1. */
        if (mode == 1) {
            for (j = 0; j < n; j++) {
                if (x[j] == 0.0) {
                    temp = eps;
                } else {
                    temp = eps * Math.abs(x[j]);
                }
                xp[j] = x[j] + temp;
            }
            return;
        }
        /*        mode = 2. */

        epsf = factor * epsmch;
        epslog = Math.log10(eps);
        for (i = 0; i < m; i++) {
            err[i] = 0.;
        }

        for (j = 0; j < n; j++) {
            if (x[j] == 0.0) {
                temp = 1.;
            } else {
                temp = Math.abs(x[j]);
            }
            for (i = 0; i < m; i++) {
                err[i] = err[i] + temp * fjac[i + j * ldfjac];
            }
        }

        for (i = 0; i < m; i++) {
            temp = 1.;
            if (fvec[i] != 0.0 &&
                    fvecp[i] != 0.0 &&
                    epsf * Math.abs(fvec[i]) <= Math.abs(fvecp[i] - fvec[i])) {
                temp = eps * Math.abs((fvecp[i] - fvec[i]) / eps - err[i])
                        / (Math.abs(fvec[i]) + Math.abs(fvecp[i]));

                if (temp <= epsmch) {
                    err[i] = 1.;
                } else if (temp < eps) {
                    err[i] = (Math.log10(temp) - epslog) / epslog;
                } else {
                    err[i] = 0.0;
                }
            }
        }
    }
}
