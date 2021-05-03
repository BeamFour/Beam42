package org.redukti.jfotoptix.math;

/**
 *  @author: M.Lampton (c) 2005 Stellar Software
 *  Original License: GPL v2
 */
public interface LMLFunction {
    /** Returns sos, or BIGVAL if parms failed. */
    double computeResiduals();

    /** false if parms failed. */
    boolean buildJacobian();

    /** Get residual at i */
    double getResidual(int i);

    /** Get Jacobian at i,j */
    double getJacobian(int i, int j);

    /** Moves parms, builds resid[], returns sos or BIGVAL */
    double nudge(double[] delta);
}
