// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
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

    @Override
    public StringBuilder toString(StringBuilder sb) {
        sb.append("Glass(").append("nd=").append(n)
                .append(", vd=").append(v)
                .append(", mat=''").append(", cat=''").append(")");
        return sb;
    }
}
