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

package org.redukti.jfotoptix.plotting;

import org.redukti.jfotoptix.data.Range;
import org.redukti.jfotoptix.math.Vector3;

public class PlotAxes {
    /**
     * Specify axes
     */
    public enum AxisMask {
        X(1),
        Y(2),
        Z(4),
        XY(3),
        YZ(6),
        XZ(5),
        XYZ(7);

        AxisMask(int value) {
            this._value = value;
        }

        final int _value;

        public int get_value() {
            return _value;
        }
    }

    enum step_mode_e {
        step_interval,
        step_count,
        step_base
    }

    static final class Axis {
        Axis() {
            _axis = true;
            _tics = true;
            _values = true;
            _step_mode = step_mode_e.step_base;
            _count = 5;
            _step_base = 10.0;
            _si_prefix = false;
            _pow10_scale = true;
            _pow10 = 0;
            _unit = "";
            _label = "";
            _range = new Range(0, 0);
        }

        boolean _axis;
        boolean _tics;
        boolean _values;

        step_mode_e _step_mode;
        int _count;
        double _step_base;
        boolean _si_prefix;
        boolean _pow10_scale;
        int _pow10;

        String _unit;
        String _label;
        Range _range;
    }

    Axis[] _axes = new Axis[]{new Axis(), new Axis(), new Axis()};
    boolean _grid;
    boolean _frame;
    Vector3 _pos;
    Vector3 _origin;

    public PlotAxes() {
        _grid = false;
        _frame = true;
        _pos = Vector3.vector3_0;
        _origin = Vector3.vector3_0;
    }

    static final int _axes_bits[] = {AxisMask.X.get_value(), AxisMask.Y.get_value(), AxisMask.Z.get_value()};

    /**
     * This sets distance between axis tics to specified value.
     * see set_tics_count, set_tics_base
     */
    void set_tics_step(double step, AxisMask a) {
        for (int i = 0; i < _axes_bits.length; i++) {
            if ((a.get_value() & _axes_bits[i]) != 0) {
                _axes[i]._step_base = step;
                _axes[i]._step_mode = step_mode_e.step_interval;
            }
        }
    }

    void set_tics_step(double step) {
        set_tics_step(step, AxisMask.XYZ);
    }

    /**
     * @This sets tics count. @see {set_tics_step, set_tics_base}
     */
    public void set_tics_count(int count, AxisMask a) {
        for (int i = 0; i < _axes_bits.length; i++) {
            if ((a.get_value() & _axes_bits[i]) != 0) {
                _axes[i]._count = count;
                _axes[i]._step_mode = step_mode_e.step_count;
            }
        }
    }

    void set_tics_count(int count) {
        set_tics_count(count, AxisMask.XYZ);
    }

    /**
     * @This sets distance between axis tics to best fit power of
     * specified base divided by sufficient factor of 2 and 5 to
     * have at least @tt min_count tics. @see {set_tics_step,
     * set_tics_count}
     */
    void set_tics_base(int min_count, double base, AxisMask a) {
        for (int i = 0; i < _axes_bits.length; i++) {
            if ((a.get_value() & _axes_bits[i]) != 0) {
                _axes[i]._count = min_count;
                _axes[i]._step_base = base;
                _axes[i]._step_mode = step_mode_e.step_base;
            }
        }
    }

    void set_tics_base() {
        set_tics_base(5, 10.0, AxisMask.XYZ);
    }

    /**
     * This sets axis tics values origin.
     */
    public void set_origin(Vector3 origin) {
        _origin = origin;
    }

    /**
     * This returns axes tics values origin.
     */
    Vector3 get_origin() {
        return _origin;
    }

    /**
     * This returns axis position
     */
    public void set_position(Vector3 position) {
        _pos = position;
    }

    /**
     * This returns axis position
     */
    Vector3 get_position() {
        return _pos;
    }

    /**
     * This sets grid visibility. Grid points use tic
     * step.
     */
    void set_show_grid(boolean show) {
        _grid = show;
    }

    /**
     * see set_show_grid
     */
    boolean get_show_grid() {
        return _grid;
    }

    /**
     * @This sets frame visibility.
     */
    void set_show_frame(boolean show) {
        _frame = show;
    }

    /**
     * see set_show_frame
     */
    boolean get_show_frame() {
        return _frame;
    }

    /**
     * @This sets axes visibility.
     */
    public void set_show_axes(boolean show, AxisMask a) {
        for (int i = 0; i < _axes_bits.length; i++) {
            if ((a.get_value() & _axes_bits[i]) != 0) {
                _axes[i]._axis = show;
            }
        }
    }

    /**
     * see set_show_axes
     */
    boolean get_show_axes(int axis) {
        return _axes[axis]._axis;
    }

    /**
     * This sets tics visibility. Tics are located on axes and
     * frame. see {set_show_axes, set_show_frame}
     */
    void set_show_tics(boolean show, AxisMask a) {
        for (int i = 0; i < _axes_bits.length; i++) {
            if ((a.get_value() & _axes_bits[i]) != 0) {
                _axes[i]._tics = show;
                _axes[i]._axis |= show;
            }
        }
    }

    /**
     * see set_show_tics
     */
    boolean get_show_tics(int axis) {
        return _axes[axis]._tics;
    }

    /**
     * @This sets tics value visibility. When frame is visible,
     * tics value is located on frame tics instead of axes tics.
     * @see {set_show_axes, set_show_frame}
     */
    void set_show_values(boolean show, AxisMask a) {
        for (int i = 0; i < _axes_bits.length; i++) {
            if ((a.get_value() & _axes_bits[i]) != 0) {
                _axes[i]._values = show;
                _axes[i]._tics |= show;
                _axes[i]._axis |= show;
            }
        }
    }

    /**
     * see set_show_values
     */
    boolean get_show_values(int axis) {
        return _axes[axis]._values;
    }

    /**
     * This set axis label
     */
    public void set_label(String label, AxisMask a) {
        for (int i = 0; i < _axes_bits.length; i++) {
            if ((a.get_value() & _axes_bits[i]) != 0) {
                _axes[i]._label = label;
            }
        }
    }

    /**
     * This sets axis unit.
     * <p>
     * When @tt pow10_scale is set, value will be scaled to shorten
     * their length and appropriate power of 10 factor will be
     * displayed in axis label.
     * <p>
     * If @tt si_prefix is set, SI letter decimal prefix is used
     * and the @tt pow10 parameter can be used to scale base unit
     * by power of ten (useful when input data use scaled SI base unit).
     */
    public void set_unit(String unit, boolean pow10_scale,
                  boolean si_prefix, int pow10, AxisMask a) {
        for (int i = 0; i < _axes_bits.length; i++) {
            if ((a.get_value() & _axes_bits[i]) != 0) {
                _axes[i]._si_prefix = si_prefix;
                _axes[i]._unit = unit;
                _axes[i]._pow10_scale = pow10_scale;
                _axes[i]._pow10 = pow10;
            }
        }
    }

    /**
     * Get axis label
     */
    String get_label(int axis) {
        return _axes[axis]._label;
    }

    /**
     * Set value range for given axis. Default range is [0,0] which
     * means automatic range.
     */
    void set_range(Range r, AxisMask a) {
        for (int i = 0; i < _axes_bits.length; i++) {
            if ((a.get_value() & _axes_bits[i]) != 0) {
                _axes[i]._range = r;
            }
        }
    }

    /**
     * get distance between axis tics
     */
    double get_tics_step(int index, Range r) {
        assert (index < 3);

        Axis a = _axes[index];
        double d = r.second - r.first;

        switch (a._step_mode) {
            case step_interval:
                return d > 0 ? a._step_base : -a._step_base;

            case step_count:
                return d / (double) a._count;

            case step_base: {
                if (d == 0.0)
                    return 1;

                double da = Math.abs(d);
                double p = Math.floor(Math.log(da) / Math.log(a._step_base));
                double n = Math.pow(a._step_base, p);
                int f = 1;

                while ((int) (da / n * f) < a._count) {
                    if ((int) (da / n * f * 2) >= a._count) {
                        f *= 2;
                        break;
                    } else if ((int) (da / n * f * 5) >= a._count) {
                        f *= 5;
                        break;
                    } else {
                        f *= 10;
                    }
                }

                n /= f;

                return d > 0 ? n : -n;
            }

            default:
                throw new IllegalArgumentException();
        }
    }

}
