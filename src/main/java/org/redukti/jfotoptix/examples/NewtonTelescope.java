package org.redukti.jfotoptix.examples;

import org.redukti.jfotoptix.curve.Conic;
import org.redukti.jfotoptix.curve.Flat;
import org.redukti.jfotoptix.math.MathUtils;
import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.shape.Disk;
import org.redukti.jfotoptix.shape.Ellipse;
import org.redukti.jfotoptix.sys.Element;
import org.redukti.jfotoptix.sys.Group;
import org.redukti.jfotoptix.sys.MirrorSurface;

import java.util.ArrayList;
import java.util.List;

public class NewtonTelescope {

    static class Newton extends Group
    {
        final double _focal;
        final double _diameter;
        final double _bwd;
        final double _field_angle;
        final double _unvignetted_image_size;
        final double _offset;
        final double _minor_axis;
        final double _major_axis;

        final Disk _primary_shape;
        final Conic _primary_curve;
        final MirrorSurface _primary;
        final Ellipse _secondary_shape;
        final MirrorSurface _secondary;
        final Vector3Pair _focal_plane;

        public Newton(int id, Vector3Pair p, Transform3 transform3, List<? extends Element> elements,
                      double _focal, double _diameter, double _bwd, double _field_angle, double _unvignetted_image_size,
                      double _offset, double _minor_axis, double _major_axis, Disk _primary_shape,
                      Conic _primary_curve, MirrorSurface _primary, Ellipse _secondary_shape,
                      MirrorSurface _secondary, Vector3Pair _focal_plane) {
            super(id, p, transform3, elements);
            this._focal = _focal;
            this._diameter = _diameter;
            this._bwd = _bwd;
            this._field_angle = _field_angle;
            this._unvignetted_image_size = _unvignetted_image_size;
            this._offset = _offset;
            this._minor_axis = _minor_axis;
            this._major_axis = _major_axis;
            this._primary_shape = _primary_shape;
            this._primary_curve = _primary_curve;
            this._primary = _primary;
            this._secondary_shape = _secondary_shape;
            this._secondary = _secondary;
            this._focal_plane = _focal_plane;
        }

        public static class Builder extends Group.Builder {

            double _focal;
            double _diameter;
            double _bwd;
            double _field_angle;
            double _unvignetted_image_size;
            double _offset;
            double _minor_axis;
            double _major_axis;

            Disk _primary_shape;
            Conic _primary_curve;
            MirrorSurface.Builder _primary;
            Ellipse _secondary_shape;
            MirrorSurface.Builder _secondary;
            Vector3Pair _focal_plane;


            @Override
            public Newton build() {
                return null;
            }

            public Newton.Builder create(Vector3Pair p, double focal, double diameter,
                                double bwd, double field_angle) {
                List<? extends Element> elements = new ArrayList<>();
                position(p);
                _focal = focal;
                _diameter = diameter;
                _bwd = (_diameter / 2.0 + bwd);
                _field_angle = (field_angle);
                _unvignetted_image_size = calc_unvignetted_image_size ();
                _offset = calc_secondary ();
                _focal_plane = new Vector3Pair(new Vector3 (0, _bwd, 0), new Vector3 (0, 1.0, 0));
                _primary_shape = new Disk (diameter / 2.0);
                _primary_curve = new Conic (_focal * 2.0, -1.0);
                _primary = new MirrorSurface.Builder (false)
                        .position(new Vector3Pair (new Vector3(0, 0, _focal - _bwd),
                                new Vector3(0, 0, -1.0)))
                        .curve(_primary_curve)
                        .shape(_primary_shape);
                _secondary_shape = new Ellipse (_minor_axis / 2.0,
                        _major_axis / 2.0);
                _secondary = new MirrorSurface.Builder (true)
                        .position(new Vector3Pair(new Vector3 (0, -_offset, _offset), Vector3.vector3_001))
                        .curve(Flat.flat)
                        .shape(_secondary_shape);
                _secondary.transform(_secondary.transform().linearRotation(new Vector3(-135, 0, 0)));
                add (_primary);
                add (_secondary);
                return this;
            }

            double calc_unvignetted_image_size ()
            {
                return _unvignetted_image_size
                        = Math.tan (Math.toRadians (_field_angle / 2.0)) * _focal * 2;
            }

            double calc_secondary ()
            {
                // formula from http://www.astro-electronic.de/faq2.html

                double e = MathUtils.square (_diameter) / (16.0 * _focal);
                double c = _focal - e;
                double b = _diameter - _unvignetted_image_size;
                double l = _unvignetted_image_size * c + _bwd * b;
                double m = 2.0 * c - b;
                double n = 2.0 * c + b;
                double a = l / m + l / n;
                double o = (l / m - l / n) / 2.0;

                _offset = o;
                _minor_axis = Math.sqrt (MathUtils.square (a) - 4.0 * MathUtils.square (_offset));
                _major_axis = Math.sqrt (2.0) * a;

                return _offset;
            }
        }
    };

}
