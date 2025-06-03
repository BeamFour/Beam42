package org.redukti.mathlib;

/**
 *  @author: M.Lampton (c) 2005 Stellar Software
 *  Original License: GPL v2
 */
public interface LMLFunction {
    /** Returns sos, or BIGVAL if parms failed. */
    double computeResiduals();

    /** Allows LM to request a new Jacobian. false if parms failed. */
    boolean buildJacobian();

    /** Get residual at i */
    double getResidual(int i);

    /** Get Jacobian at i,j */
    double getJacobian(int i, int j);

    /**
     * Allows LM to modify parms[] and reevaluate its fit.
     * Returns sum-of-squares for nudged params.
     * This is the only place that parms[] are modified.
     * If NADJ<NPARMS, this is the place for your LUT. (?)
     * Moves parms, builds resid[], returns sos or BIGVAL
     */
    double nudge(double[] delta);
}
