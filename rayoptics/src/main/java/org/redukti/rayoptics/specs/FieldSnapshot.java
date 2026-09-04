package org.redukti.rayoptics.specs;

/** Analysis metadata captured at result creation, with no model dependencies. */
public final class FieldSnapshot {
    public final double x, y, vux, vuy, vlx, vly, wt;
    private final String label;

    public FieldSnapshot(Field field) {
        x = field.x;
        y = field.y;
        vux = field.vux;
        vuy = field.vuy;
        vlx = field.vlx;
        vly = field.vly;
        wt = field.wt;
        label = field.toString();
    }

    @Override
    public String toString() { return label; }
}
