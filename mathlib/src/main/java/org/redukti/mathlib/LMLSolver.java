package org.redukti.mathlib;

/**
 *  Levenberg Marquardt Lampton solver
 *  From BeamFour project - AutoRay.
 *  M.Lampton, 1997 Computers In Physics v.11 #10 110-115.
 *
 *  Constructor is used to set up all parms including host for callback.
 *  Sole public method is iLMiter() performs one iteration.
 *  Arrays parms[], resid[], jac[][] are unknown here.
 *  Instead, the host must fake these, and provide results....
 *  Callback method uses CallerID to access five host methods:
 *
 *    double dPerformResid();    Returns sos, or BIGVAL if parms failed.
 *    double dNudge(dp);         Moves parms, builds resid[], returns sos or BIGVAL
 *    boolean bBuildJacobian();  false if parms failed.
 *    double dFetchJac(i,j);     cannot fail.
 *    double dFetchResid(i);     cannot fail.
 *
 *  Exit leaves host with parms[] optimized through its sequence of nudges.
 *
 *  @author: M.Lampton (c) 2005 Stellar Software
 *  Original License: GPL v2
 */
public class LMLSolver {
    public static final double BIGVAL      = 9.876543e+99;
    public static final int DOWNITER  = 0;    // iteration ok, want more
    public static final int LEVELITER = 1;    // iteration ok, all done.
    public static final int MAXITER   = 2;    // did enough iterations.
    public static final int BADITER   = 3;    // ray killed bail out

    private final double LMBOOST    =  2.0;     // damping increase per bad step
    private final double LMSHRINK   = 0.10;     // damping decrease per good step
    private final double LAMBDAZERO = 0.001;    // initial damping
    private final double LAMBDAMAX  =  1E3;     // max damping

    private int niter = 0;                      // local diagnostic only
    private double sos, sosinit, lambda;        // local diagnostic only

    private LMLFunction myH = null;    // overwritten by constructor
    private double  lmtol = 1E-6;  // overwritten by constructor
    private int     lmiter = 100;  // overwritten by constructor
    private int     nparms = 0;    // overwritten by constructor
    private int     npts = 0;      // overwritten by constructor

    private double[] delta;        // local
    private double[] beta;         // local
    private double[][] alpha;      // local
    private double[][] amatrix;    // local

    /**
     * Builds an instance of the solver.
     *
     * @param gH User defined function
     * @param gtol Tolerance
     * @param gnparms # of parameters to adjust
     * @param gnpts # of points to fit to
     */
    public LMLSolver(LMLFunction gH, double gtol, int gnparms, int gnpts)
    // Constructor sets up private fields, including host for callbacks.
    {
        myH = gH;
        lmtol = gtol;
        nparms = gnparms;
        npts = gnpts;
        niter = 0;
        delta = new double[nparms];
        beta = new double[nparms];
        alpha = new double[nparms][nparms];
        amatrix = new double[nparms][nparms];
        lambda = LAMBDAZERO;
    }

    public int iLMiter( )
    // Called repeatedly by LMhost to perform each LM iteration.
    // Returns BADITER to shut down ray failed;
    // Returns DOWNITER if iteration went OK, more needed;
    // Returns LEVELITER if iteration went OK, all done.
    // Globals: npts, nparms, myH.
    // Ref: M.Lampton, Computers in Physics v.11 pp.110-115 1997.
    {
        sosinit = myH.computeResiduals();
        if (sosinit==BIGVAL)              // failed ray?
            return BADITER;                 // cannot proceed, request host OUTER LOOP exit.

        if (!myH.buildJacobian())        // ask host for new Jacobian.
            return BADITER;                 // cannot proceed, request host OUTER LOOP exit.

        for (int k=0; k<nparms; k++)      // get downhill gradient beta
        {
            beta[k] = 0.0;
            for (int i=0; i<npts; i++)
                beta[k] -= myH.getResidual(i)*myH.getJacobian(i,k);
        }
        for (int k=0; k<nparms; k++)      // get undamped curvature matrix alpha
            for (int j=0; j<nparms; j++)
            {
                alpha[j][k] = 0.0;
                for (int i=0; i<npts; i++)
                    alpha[j][k] += myH.getJacobian(i,j)*myH.getJacobian(i,k);
            }

        double rise = 0;
        do  /// LMinner damping loop searches for one downhill step
        {
            niter++;                         // local diagnostic only
            for (int k=0; k<nparms; k++)     // copy and damp it
                for (int j=0; j<nparms; j++)
                    amatrix[j][k] = alpha[j][k] + ((j==k) ? lambda : 0.0);
            gaussj(amatrix, nparms);         // invert

            for (int k=0; k<nparms; k++)     // compute delta[]
            {
                delta[k] = 0.0;
                for (int j=0; j<nparms; j++)
                    delta[k] += amatrix[j][k]*beta[j];
            }

            sos = myH.nudge(delta);         // try it out.
            rise = (sos-sosinit)/(1+sosinit);

            //---four possibilities and three exits---------

            if (rise <= -lmtol)              // good downhill step!
            {
                lambda *= LMSHRINK;           // shrink lambda
                return DOWNITER;              // return to host: request another OUTER LOOP iteration.
            }

            if (rise <= 0.0)                 // good step but level; all done.
            {
                lambda *= LMSHRINK;           // no need to shrink lambda?
                return LEVELITER;             // return to host: OUTER LOOP exit.
            }

            for (int k=0; k<nparms; k++)     // reverse course!
                delta[k] *= -1.0;
            myH.nudge(delta);               // sosprev is still OK

            if (rise < lmtol)                // finished but keep prev parms
            {
                return LEVELITER;             // return to host: OUTER LOOP exit.
            }

            lambda *= LMBOOST;               // UPITER:  apply more damping.
        } while (lambda<LAMBDAMAX);          // and stay in this INNER LOOP.

        return BADITER; // exceeded LAMBDAMAX, so request host OUTER LOOP exit.
        // usual cause is ray damage during minimization.
    }

    private double gaussj( double[][] a, int N )
    // inverts the double array a[N][N] by Gauss-Jordan method
    {
        double det = 1.0, big, save;
        int i,j,k,L;
        int[] ik = new int[100];
        int[] jk = new int[100];
        for (k=0; k<N; k++)
        {
            big = 0.0;
            for (i=k; i<N; i++)
                for (j=k; j<N; j++)          // find biggest element
                    if (Math.abs(big) <= Math.abs(a[i][j]))
                    {
                        big = a[i][j];
                        ik[k] = i;
                        jk[k] = j;
                    }
            if (big == 0.0) return 0.0;
            i = ik[k];
            if (i>k)
                for (j=0; j<N; j++)          // exchange rows
                {
                    save = a[k][j];
                    a[k][j] = a[i][j];
                    a[i][j] = -save;
                }
            j = jk[k];
            if (j>k)
                for (i=0; i<N; i++)
                {
                    save = a[i][k];
                    a[i][k] = a[i][j];
                    a[i][j] = -save;
                }
            for (i=0; i<N; i++)            // build the inverse
                if (i != k)
                    a[i][k] = -a[i][k]/big;
            for (i=0; i<N; i++)
                for (j=0; j<N; j++)
                    if ((i != k) && (j != k))
                        a[i][j] += a[i][k]*a[k][j];
            for (j=0; j<N; j++)
                if (j != k)
                    a[k][j] /= big;
            a[k][k] = 1.0/big;
            det *= big;                    // bomb point
        }                                  // end k loop
        for (L=0; L<N; L++)
        {
            k = N-L-1;
            j = ik[k];
            if (j>k)
                for (i=0; i<N; i++)
                {
                    save = a[i][k];
                    a[i][k] = -a[i][j];
                    a[i][j] = save;
                }
            i = jk[k];
            if (i>k)
                for (j=0; j<N; j++)
                {
                    save = a[k][j];
                    a[k][j] = -a[i][j];
                    a[i][j] = save;
                }
        }
        return det;
    }
}

