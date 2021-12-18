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
