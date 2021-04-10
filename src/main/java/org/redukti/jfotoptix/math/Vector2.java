/*
The software is ported from Goptical, hence is licensed under the GPL.
Copyright (c) 2021 Dibyendu Majumdar

Original GNU Optical License and Authors are as follows:

      The Goptical library is free software; you can redistribute it
      and/or modify it under the terms of the GNU General Public
      License as published by the Free Software Foundation; either
      version 3 of the License, or (at your option) any later version.

      The Goptical library is distributed in the hope that it will be
      useful, but WITHOUT ANY WARRANTY; without even the implied
      warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
      See the GNU General Public License for more details.

      You should have received a copy of the GNU General Public
      License along with the Goptical library; if not, write to the
      Free Software Foundation, Inc., 59 Temple Place, Suite 330,
      Boston, MA 02111-1307 USA

      Copyright (C) 2010-2011 Free Software Foundation, Inc
      Author: Alexandre Becoulet
 */


package org.redukti.jfotoptix.math;

import java.util.Arrays;

import static org.redukti.jfotoptix.math.MathUtils.square;

/**
 * Vector with 2 components named x,y.
 */
public class Vector2 {

    public static final Vector2 vector2_0 = new Vector2 (0.0, 0.0);
    public static final Vector2 vector2_1 = new Vector2 (1.0, 1.0);
    public static final  Vector2 vector2_10 = new Vector2 (1.0, 0.0);
    public static final  Vector2 vector2_01 = new Vector2 (0.0, 1.0);

    private static final int N = 2;

    final double[] _values;

    public Vector2(double x, double y) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            throw new IllegalArgumentException("NaN");
        }
        this._values = new double[N];
        this._values[0] = x;
        this._values[1] = y;
    }

    public Vector2(double v) {
        this(v,v);
    }

    private Vector2(double[] values) {
        this._values = values;
    }

    public final double x() {
        return this._values[0];
    }
    public final double y() {
        return this._values[1];
    }

    public final Vector2 x(double value) { return new Vector2(value, y()); }
    public final Vector2 y(double value) { return new Vector2(x(), value); }

    public double v(int i) {return this._values[i];}
    public Vector2 set(int i, double v) {
        double[] values = Arrays.copyOf(_values, _values.length);
        values[i] = v;
        return new Vector2(values);
    }

    public Vector2 plus(Vector2 v)
    {
        double[] r = new double[N];
        for (int i = 0; i < N; i++)
            r[i] = _values[i] + v._values[i];
        return new Vector2(r);
    }

    public Vector2 minus(Vector2 v)
    {
        double[] r = new double[N];
        for (int i = 0; i < N; i++)
            r[i] = _values[i] - v._values[i];
        return new Vector2(r);
    }

    public Vector2 divide(double scale) {
        double[] r = new double[N];
        for (int i = 0; i < N; i++)
            r[i] = _values[i] / scale;
        return new Vector2(r);
    }

    public Vector2 times(double scale) {
        double[] r = new double[N];
        for (int i = 0; i < N; i++)
            r[i] = _values[i] * scale;
        return new Vector2(r);
    }

    /**
     * element by element divide
     */
    public Vector2 ebeDivide(Vector2 v)
    {
        double[] r = new double[N];
        for (int i = 0; i < N; i++)
            r[i] = _values[i] / v._values[i];
        return new Vector2(r);
    }

    /** element by element multiply */
    public Vector2 ebeTimes(Vector2 v)
    {
        double[] r = new double[N];
        for (int i = 0; i < N; i++)
            r[i] = _values[i] * v._values[i];
        return new Vector2(r);
    }

    public Vector2 negate()
    {
        double[] r = new double[N];
        for ( int i = 0; i < N; i++)
            r[i] = -_values[i];
        return new Vector2(r);
    }

    public double len ()
    {
        double r = 0;
        for (int i = 0; i < N; i++)
            r += square (_values[i]);
        return Math.sqrt (r);
    }

    public static Vector2 from(Vector3 v3, int a, int b)
    {
        double[] r = new double[2];
        r[0] = v3.v(a);
        r[1] = v3.v(b);
        return new Vector2(r);
    }

    @Override
    public String toString() {
        return "[" + x() + ',' + y() + ']';
    }

    public final boolean isEqual(Vector2 other, double tolerance) {
        return Math.abs(this.x() - other.x()) < tolerance &&
                Math.abs(this.y() - other.y()) < tolerance;
    }
}
