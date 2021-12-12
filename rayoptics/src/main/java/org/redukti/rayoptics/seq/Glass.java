package org.redukti.rayoptics.seq;

public class Glass extends Medium {

    public double v;

    public Glass(double nd, double vd, String label, String catalog_name) {
        super(label, nd, catalog_name);
        this.v = vd;
        // TODO model
    }

    public Glass() {
        this(1.5168, 64.17, "", "");
    }

    public Glass(double nd, double vd) {
        this(nd, vd, "", "");
    }


}
