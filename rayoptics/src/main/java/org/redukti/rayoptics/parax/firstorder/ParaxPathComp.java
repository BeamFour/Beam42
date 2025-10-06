package org.redukti.rayoptics.parax.firstorder;

import org.redukti.rayoptics.seq.InteractMode;

public class ParaxPathComp {
    public double pwr;
    public double tau;
    public double indx;
    public InteractMode rmd;

    public ParaxPathComp(double power, double tau, double indx, InteractMode imode) {
        this.pwr = power;
        this.tau = tau;
        this.indx = indx;
        this.rmd = imode;
    }
}
