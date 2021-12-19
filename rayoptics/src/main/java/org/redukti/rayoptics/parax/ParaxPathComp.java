package org.redukti.rayoptics.parax;

public class ParaxPathComp {
    public double pwr;
    public double tau;
    public double indx;
    public String rmd;

    public ParaxPathComp(double power, double tau, double indx, String imode) {
        this.pwr = power;
        this.tau = tau;
        this.indx = indx;
        this.rmd = imode;
    }
}
