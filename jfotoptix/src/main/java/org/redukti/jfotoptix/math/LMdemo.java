package org.redukti.jfotoptix.math;

/* https://www.ssl.berkeley.edu/~mlampton/LMdemo.java */

/** LMdemo Levenberg Marquardt demonstrator
 *
 * Demonstrates fitting a list of data to a simple math function.
 * No graphics; does not create a window; text output to screen.
 * Data are internal, needs no other files.
 *
 * This one file contains several classes:
 *     LMdemo        contains only main(); creates a FitHost.
 *     FitHost       defines the fitting problem.
 *     LMhost        defines interface methods required by LM.
 *     LM            The Levenberg-Marquardt routine.
 *
 *  M.Lampton UCB SSL (c) 1996; Java edition 2007, 2011
 *  see M.Lampton, 1997 Computers In Physics v.11 #10 110-115.
 */
public class LMdemo
{
    public static void main(String args[])
    {
        new FitHost();
    }
}



/**  FitHost
 * Fits a function of x to a list of given {x,y} data.
 *
 *
 * Triggers LM by instantiating an object of class LM
 * Supplies four callback methods mandated by interface LMhost
 */
class FitHost implements LMhost
{
    //--------constants for LM---------------
    protected final double DELTAP = 1e-6; // parm step
    protected final double BIGVAL = 9e99; // fault flag

    //---constants for fitting: pixels & parameters-----

    private double data[][] =   // {x, y} data pairs [row][col]
            {{0.00,	0.6793},
                    {0.03,	0.6787},
                    {0.06,	0.6768},
                    {0.09,	0.6736},
                    {0.12,	0.6691},
                    {0.15,	0.6634},
                    {0.18,	0.6565},
                    {0.21,	0.6482},
                    {0.24,	0.6388},
                    {0.27,	0.6280},
                    {0.30,	0.6161},
                    {0.33,	0.6030},
                    {0.36,	0.5887},
                    {0.39,	0.5733},
                    {0.42,	0.5568},
                    {0.45,	0.5394},
                    {0.48,	0.5210},
                    {0.51,	0.5019},
                    {0.54,	0.4820},
                    {0.57,	0.4614},
                    {0.60,	0.4404},
                    {0.63,	0.4191},
                    {0.66,	0.3975},
                    {0.69,	0.3758},
                    {0.72,	0.3542},
                    {0.75,	0.3328},
                    {0.78,	0.3117},
                    {0.81,	0.2910},
                    {0.84,	0.2710},
                    {0.87, 0.2515},
                    {0.90,	0.2328},
                    {0.93,	0.2149},
                    {0.96,	0.1979},
                    {0.99,	0.1818},
                    {1.02,	0.1667},
                    {1.05,	0.1524},
                    {1.08,	0.1392},
                    {1.11,	0.1268},
                    {1.14,	0.1154},
                    {1.17,	0.1048},
                    {1.20,	0.0951},
                    {1.23,	0.0862},
                    {1.26,	0.0781},
                    {1.29,	0.0706},
                    {1.32,	0.0639},
                    {1.35,	0.0577},
                    {1.38,	0.0521},
                    {1.41,	0.0471},
                    {1.44,	0.0425},
                    {1.47,	0.0384},
                    {1.50,	0.0347},
                    {1.53,	0.0313},
                    {1.56,	0.0283},
                    {1.59,	0.0255},
                    {1.62,	0.0231},
                    {1.65,	0.0209},
                    {1.68,	0.0189},
                    {1.71,	0.0171},
                    {1.74,	0.0155},
                    {1.77,	0.0140},
                    {1.80,	0.0127},
                    {1.83,	0.0115},
                    {1.86,	0.0105},
                    {1.89,	0.0095},
                    {1.92,	0.0087},
                    {1.95,	0.0079},
                    {1.98,	0.0072}};

    protected int    NPTS     = data.length;
    protected double WEIGHT   = 1.0;
    private double   parms[]  = {1.0, 1.0, 1.0, 1.0}; // initial guess
    protected int    NPARMS   = parms.length;

    private double   resid[]  = new double[NPTS];
    private double   jac[][]  = new double[NPTS][NPARMS];

    public FitHost()
    {
        listParms("Start Parms", parms);
        LM myLM = new LM(this, NPARMS, NPTS);
        listParms("Fitted Parms", parms);
    }

    //------------mathematical helpers for FitHost--------------


    void listParms(String title, double p[])
    {
        System.out.print(title + "----");
        for (int i=0; i<NPARMS; i++)
            System.out.print(String.format("%12.6f", p[i]));
        System.out.println("");
    }


    private double func(int i, double data[][], double p[])
    // inverse even polynomial.
    // Called only by dComputeResiduals().
    {
        double x = data[i][0];
        double x2 = x*x;
        double x4 = x2*x2;
        double x6 = x4*x2;
        // double x8 = x6*x2;
        double denom = 1 + p[1]*x2 + p[2]*x4 + p[3]*x6;  // + p[4]*x8;
        return p[0]/denom;
    }


    private double dComputeResiduals()
    // Called by mandated dNudge().
    // Uses current parms[] vector;
    // Evaluates resid[i] = (model[i] - data[i])*WEIGHT.
    // Returns sum-of-squares.
    {
        double sumsq = 0.0;
        for (int i=0; i<NPTS; i++)
        {
            double y = data[i][1];  // row i, col 1
            resid[i] = (func(i,data,parms) - y) * WEIGHT;
            sumsq += resid[i]*resid[i];
        }
        return sumsq;
    }

    //------the four mandated interface methods------------

    public double dNudge(double dp[])
    // Allows LM to modify parms[] and reevaluate its fit.
    // Returns sum-of-squares for nudged params.
    // This is the only place that parms[] are modified.
    // If NADJ<NPARMS, this is the place for your LUT.
    {
        for (int j=0; j<NPARMS; j++)
            parms[j] += dp[j];
        return dComputeResiduals();
    }

    public boolean bBuildJacobian()
    // Allows LM to compute a new Jacobian.
    // Uses current parms[] and two-sided finite difference.
    // If current parms[] is bad, returns false.
    {
        double delta[] = new double[NPARMS];
        double FACTOR = 0.5 / DELTAP;
        double d=0;

        for (int j=0; j<NPARMS; j++)
        {
            for (int k=0; k<NPARMS; k++)
                delta[k] = (k==j) ? DELTAP : 0.0;

            d = dNudge(delta); // resid at pplus
            if (d==BIGVAL)
            {
                System.out.println("Bad dBuildJacobian() exit 2");
                return false;
            }
            for (int i=0; i<NPTS; i++)
                jac[i][j] = dGetResid(i);

            for (int k=0; k<NPARMS; k++)
                delta[k] = (k==j) ? -2*DELTAP : 0.0;

            d = dNudge(delta); // resid at pminus
            if (d==BIGVAL)
            {
                System.out.println("Bad dBuildJacobian() exit 3");
                return false;
            }

            for (int i=0; i<NPTS; i++)
                jac[i][j] -= dGetResid(i);  // fetches resid[]

            for (int i=0; i<NPTS; i++)
                jac[i][j] *= FACTOR;

            for (int k=0; k<NPARMS; k++)
                delta[k] = (k==j) ? DELTAP : 0.0;

            d = dNudge(delta);
            if (d==BIGVAL)
            {
                System.out.println("Bad dBuildJacobian() exit 4");
                return false;
            }
        }
        return true;
    }

    public double dGetResid(int i)
    // Allows LM to see one element of the resid[] vector.
    {
        return resid[i];
    }

    public double dGetJac(int i, int j)
    // Allows LM to see one element of the Jacobian matrix.
    {
        return jac[i][j];
    }
}





/** LMhost interface class declares four abstract methods
 * These allow LM to request numerical work
 * M.Lampton UCB SSL 2005
 */
interface LMhost
{
    double dNudge(double dp[]);
    // Allows myLM.bLMiter to modify parms[] and reevaluate.
    // This is the only modifier of parms[].
    // So, if NADJ<NPARMS, put your LUT here.

    boolean bBuildJacobian();
    // Allows LM to request a new Jacobian.

    double dGetResid(int i);
    // Allows LM to see one element of the resid[] vector.

    double dGetJac(int i, int j);
    // Allows LM to see one element of the Jacobian matrix.
}





/**
 *  class LM   Levenberg Marquardt w/ Lampton improvements
 *  M.Lampton, 1997 Computers In Physics v.11 #10 110-115.
 *
 *  Constructor is used to set up all parms including host for callback.
 *  bLMiter() performs one iteration.
 *  Host arrays parms[], resid[], jac[][] are unknown here.
 *  Callback method uses CallerID to access four host methods:
 *
 *    double dNudge(dp);         Moves parms, builds resid[], returns sos.
 *    boolean bBuildJacobian();  Builds Jacobian, returns false if parms NG.
 *    double dGetJac(i,j);       Fetches one value of host Jacobian.
 *    double dGetResid(i);       Fetches one value of host residual.
 *
 *  Exit leaves host with optimized parms[].
 *
 *  @author: M.Lampton UCB SSL (c) 2005
 */
class LM
{
    private final int    LMITER     =  100;     // max number of L-M iterations
    private final double LMBOOST    =  2.0;     // damping increase per failed step
    private final double LMSHRINK   = 0.10;     // damping decrease per successful step
    private final double LAMBDAZERO = 0.001;    // initial damping
    private final double LAMBDAMAX  =  1E9;     // max damping
    private final double LMTOL      = 1E-12;    // exit tolerance
    private final double BIGVAL     = 9e99;     // trouble flag

    private double sos, sosprev, lambda;

    private LMhost myH = null;    // overwritten by constructor
    private int nadj = 0;         // overwritten by constructor
    private int npts = 0;         // overwritten by constructor

    private double[] delta;       // local parm change
    private double[] beta;        // local
    private double[][] alpha;     // local
    private double[][] amatrix;   // local

    public LM(LMhost gH, int gnadj, int gnpts)
    // Constructor sets up fields and drives iterations.
    {
        myH = gH;
        nadj = gnadj;
        npts = gnpts;
        delta = new double[nadj];
        beta = new double[nadj];
        alpha = new double[nadj][nadj];
        amatrix = new double[nadj][nadj];
        lambda = LAMBDAZERO;
        int niter = 0;
        boolean done = false;
        do
        {
            done = bLMiter();
            niter++;
        }
        while (!done && (niter<LMITER));
    }

    private boolean bLMiter( )
    // Each call performs one LM iteration.
    // Returns true if done with iterations; false=wants more.
    // Global nadj, npts; needs nadj, myH to be preset.
    // Ref: M.Lampton, Computers in Physics v.11 pp.110-115 1997.
    {
        for (int k=0; k<nadj; k++)
            delta[k] = 0.0;
        sos = myH.dNudge(delta);
        if (sos==BIGVAL)
        {
            System.out.println("  bLMiter finds faulty initial dNudge()");
            return false;
        }
        sosprev = sos;

        System.out.println("  bLMiter..SumOfSquares= "+sos);
        if (!myH.bBuildJacobian())
        {
            System.out.println("  bLMiter finds bBuildJacobian()=false");
            return false;
        }
        for (int k=0; k<nadj; k++)      // get downhill gradient beta
        {
            beta[k] = 0.0;
            for (int i=0; i<npts; i++)
                beta[k] -= myH.dGetResid(i)*myH.dGetJac(i,k);
        }
        for (int k=0; k<nadj; k++)      // get curvature matrix alpha
            for (int j=0; j<nadj; j++)
            {
                alpha[j][k] = 0.0;
                for (int i=0; i<npts; i++)
                    alpha[j][k] += myH.dGetJac(i,j)*myH.dGetJac(i,k);
            }
        double rrise = 0;
        do  // inner damping loop searches for one downhill step
        {
            for (int k=0; k<nadj; k++)       // copy and damp it
                for (int j=0; j<nadj; j++)
                    amatrix[j][k] = alpha[j][k] + ((j==k) ? lambda : 0.0);

            gaussj(amatrix, nadj);           // invert

            for (int k=0; k<nadj; k++)       // compute delta[]
            {
                delta[k] = 0.0;
                for (int j=0; j<nadj; j++)
                    delta[k] += amatrix[j][k]*beta[j];
            }
            sos = myH.dNudge(delta);         // try it out.
            if (sos==BIGVAL)
            {
                System.out.println("  LMinner failed SOS step");
                return false;
            }
            rrise = (sos-sosprev)/(1+sos);
            if (rrise <= 0.0)                // good step!
            {
                lambda *= LMSHRINK;          // shrink lambda
                break;                       // leave lmInner.
            }
            for (int q=0; q<nadj; q++)       // reverse course!
                delta[q] *= -1.0;
            myH.dNudge(delta);               // sosprev should still be OK
            if (rrise < LMTOL)               // finished but keep prev parms
                break;                         // leave inner loop
            lambda *= LMBOOST;               // else try more damping.
        } while (lambda<LAMBDAMAX);
        boolean done = (rrise>-LMTOL) || (lambda>LAMBDAMAX);
        return done;
    }

    private double gaussj( double[][] a, int N )
    // Inverts the double array a[N][N] by Gauss-Jordan method
    // M.Lampton UCB SSL (c)2003, 2005
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
} //-----------end of class LM--------------------
