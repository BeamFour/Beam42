package org.redukti.jfotoptix.math;

public class Triangle2 {
    final int N = 3;
    public final Vector2[] _v;

    public Triangle2 (Vector2 a, Vector2 b, Vector2 c)
    {
        _v = new Vector2[N];
        _v[0] = a;
        _v[1] = b;
        _v[2] = c;
    }

    public Vector2 get_centroid ()
    {
        return _v[0].plus(_v[1]).plus(_v[2]).divide(3.);
    }
}
