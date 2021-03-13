package org.redukti.jfotoptix.math;

public class Quaternion {
    final double x, y, z, w;

    public Quaternion(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    Quaternion (Vector3 a, Vector3 b)
    {
        Vector3 cp = a.crossProduct (b);
        double _x = cp.x ();
        double _y = cp.y ();
        double _z = cp.z ();
        double _w = a.dotProduct(b) + 1.0;
        double n = norm(_x, _y, _z, _w);
        x = _x/n;
        y = _y/n;
        z = _z/n;
        w = _w/n;
    }

    static final double norm (double x, double y, double z, double w)
    {
        return Math.sqrt (x * x + y * y + z * z + w * w);
    }


}
