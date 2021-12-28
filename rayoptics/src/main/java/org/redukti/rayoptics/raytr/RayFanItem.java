package org.redukti.rayoptics.raytr;

public class RayFanItem {
    public double x;
    public double y;
    public RayPkg ray_pkg;

    public RayFanItem(double x, double y, RayPkg ray_pkg) {
        this.x = x;
        this.y = y;
        this.ray_pkg = ray_pkg;
    }
}
