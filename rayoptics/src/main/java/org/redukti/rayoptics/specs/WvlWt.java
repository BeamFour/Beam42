package org.redukti.rayoptics.specs;

public class WvlWt {

    public final double wvl;
    public final double wt;

    public WvlWt(double wvl, double wt) {
        this.wvl = wvl;
        this.wt = wt;
    }

    public WvlWt(String wvl_name, double wt) {
        this(WvlSpec.get_wavelength(wvl_name), wt);
    }
}
