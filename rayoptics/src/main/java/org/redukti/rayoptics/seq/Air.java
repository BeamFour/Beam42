// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.seq;

public class Air extends Medium {

    public static final Air INSTANCE = new Air();

    public Air() {
        super("air", 1.0);
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        sb.append("Air()");
        return sb;
    }

}
