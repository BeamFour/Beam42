package org.redukti.rayoptics.raytr;

public class GridItem {
    public final double x;
    public final double y;
    public final RayPkg ray_pkg;

    public GridItem(double x, double y, RayPkg ray_pkg) {
        this.x = x;
        this.y = y;
        this.ray_pkg = ray_pkg;
    }
}
