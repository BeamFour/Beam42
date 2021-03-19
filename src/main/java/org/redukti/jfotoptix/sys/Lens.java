package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.curve.Curve;
import org.redukti.jfotoptix.curve.Flat;
import org.redukti.jfotoptix.curve.Sphere;
import org.redukti.jfotoptix.io.Renderer;
import org.redukti.jfotoptix.material.Abbe;
import org.redukti.jfotoptix.material.MaterialBase;
import org.redukti.jfotoptix.material.Solid;
import org.redukti.jfotoptix.math.*;
import org.redukti.jfotoptix.shape.Disk;
import org.redukti.jfotoptix.shape.Shape;

import java.util.ArrayList;
import java.util.List;

public class Lens extends Group {

    enum LensEdge {
        StraightEdge,
        SlopeEdge,
    }

    OpticalSystem opticalSystem;
    List<OpticalSurface> surfaces;
    final Stop _stop;

    public Lens(int id, Vector3Pair position, Transform3 transform, List<OpticalSurface> elementList, Stop stop) {
        super(id, position, transform, elementList);
        this._stop = stop;
        this.surfaces = elementList;
    }

    @Override
    public void draw_2d_e(Renderer r, Element ref) {
        boolean grp = false;

        if (_stop != null)
            _stop.draw_2d_e(r, ref);

        if (elements().isEmpty())
            return;

        OpticalSurface first = surfaces.get(0);
        if (first.get_material(1) != first.get_material(0)) {
            if (!grp) {
                r.group_begin("");
                grp = true;
            }
            first.draw_2d_e(r, ref);
        }

        for (int i = 0; i < surfaces.size() - 1; i++) {
            OpticalSurface left = surfaces.get(i);
            OpticalSurface right = surfaces.get(i + 1);

            if (left.get_material(1) == null || !(left.get_material(1) instanceof Solid)) {
                if (grp) {
                    r.group_end();
                    grp = false;
                }
            } else {
                // draw outter edges
                double left_top_edge
                        = left.get_shape().get_outter_radius(Vector2.vector2_01);
                double left_bot_edge
                        = -left.get_shape().get_outter_radius(Vector2.vector2_01.negate());
                double right_top_edge
                        = right.get_shape().get_outter_radius(Vector2.vector2_01);
                double right_bot_edge
                        = -right.get_shape().get_outter_radius(Vector2.vector2_01.negate());

                draw_2d_edge(r, left, left_top_edge, right, right_top_edge,
                        LensEdge.StraightEdge, ref);
                draw_2d_edge(r, left, left_bot_edge, right, right_bot_edge,
                        LensEdge.StraightEdge, ref);

                // draw hole edges if not coincident
                double left_top_hole
                        = left.get_shape().get_hole_radius(Vector2.vector2_01);
                double left_bot_hole
                        = -left.get_shape().get_hole_radius(Vector2.vector2_01.negate());
                double right_top_hole
                        = right.get_shape().get_hole_radius(Vector2.vector2_01);
                double right_bot_hole
                        = -right.get_shape().get_hole_radius(Vector2.vector2_01.negate());

                if (Math.abs(left_bot_hole - left_top_hole) > 1e-6
                        || Math.abs(right_bot_hole - right_top_hole) > 1e-6) {
                    draw_2d_edge(r, left, left_top_hole, right, right_top_hole,
                            LensEdge.SlopeEdge, ref);
                    draw_2d_edge(r, left, left_bot_hole, right, right_bot_hole,
                            LensEdge.SlopeEdge, ref);
                }
            }

            if (right.get_material(1) != right.get_material(0)) {
                if (!grp) {
                    r.group_begin("");
                    grp = true;
                }
                right.draw_2d_e(r, ref);
            }
        }

        if (grp) {
            r.group_end();
        }
    }

    void draw_2d_edge(Renderer r, Surface left, double l_y,
                      Surface right, double r_y, LensEdge type,
                      Element ref) {
        Vector3 l3 = new Vector3(0., l_y,
                left.get_curve().sagitta(new Vector2(0., l_y)));
        Vector2 l2 = left.get_transform_to(ref).transform(l3).project_zy();
        Vector3 r3 = new Vector3(0., r_y, right.get_curve().sagitta(new Vector2(0., r_y)));
        Vector2 r2 = right.get_transform_to(ref).transform(r3).project_zy();

        switch (type) {
            case StraightEdge: {
                if (Math.abs(l2.y() - r2.y()) > 1e-6) {
                    double m;

                    if (Math.abs(l2.y()) > Math.abs(r2.y())) {
                        m = l2.y();
                        r.draw_segment(new Vector2Pair(new Vector2(r2.x(), m), new Vector2(r2.x(), r2.y())),
                                r.get_style_color(left.get_style()));
                    } else {
                        m = r2.y();
                        r.draw_segment(new Vector2Pair(new Vector2(l2.x(), m), new Vector2(l2.x(), l2.y())),
                                r.get_style_color(left.get_style()));
                    }

                    r.draw_segment(new Vector2Pair(new Vector2(l2.x(), m), new Vector2(r2.x(), m)),
                            r.get_style_color(left.get_style()));

                    break;
                }
            }

            case SlopeEdge:
                r.draw_segment(l2, r2, r.get_style_color(left.get_style()));
                break;
        }
    }

    @Override
    void set_system(OpticalSystem system) {
        super.set_system(system);
        _stop.set_system(system);
    }

    @Override
    public String toString() {
        return "Lens{"+ super.toString() + "}";
    }

    public static class Builder extends Group.Builder {
        List<OpticalSurface.Builder> opticalSurfaces = new ArrayList<>();
        double _last_pos = 0;
        MaterialBase _next_mat = null;
        Stop.Builder _stop = null;

        @Override
        public Element build() {
            Stop stop = null;
            ArrayList<OpticalSurface> myels = new ArrayList<>();
            ArrayList<Element> els = new ArrayList<>();
            for (OpticalSurface.Builder e : opticalSurfaces) {
                OpticalSurface s = e.build();
                myels.add(s);
                els.add(s);
            }
            return new Lens(id, position, transform, myels, _stop.build());
        }

        /**
         * Add an optical surface
         *
         * @param curvature curvature of the surface, 1/r
         * @param radius    the radius of the disk
         * @param thickness the thickness after this surface
         * @param glass     the material after this surface
         */
        public Lens.Builder add_surface(double curvature, double radius, double thickness, Abbe glass) {
            Curve curve;
            if (curvature == 0.)
                curve = Flat.flat;
            else
                curve = new Sphere(curvature);
            return add_surface(curve, new Disk(radius), thickness,
                    glass);
        }

        public Lens.Builder add_surface(double curvature, double radius, double thickness) {
            return add_surface(curvature, radius, thickness, null);
        }

        public Lens.Builder add_surface(Curve curve, Shape shape, double thickness, MaterialBase glass) {
            assert (thickness >= 0.);
            OpticalSurface.Builder surface = new OpticalSurface.Builder()
                    .position(new Vector3Pair(new Vector3(0, 0, _last_pos), Vector3.vector3_001))
                    .curve(curve)
                    .shape(shape)
                    .leftMaterial(_next_mat)
                    .rightMaterial(glass);
            opticalSurfaces.add(surface);
            _next_mat = glass;
            _last_pos += thickness;
            add(surface);
            return this;
        }

        public Lens.Builder add_stop(double radius, double thickness) {
            return add_stop(new Disk(radius), thickness);
        }

        public Lens.Builder add_stop(Shape shape, double thickness) {
            if (_stop != null)
                throw new IllegalArgumentException("Can not add more than one stop per Lens");
            _stop = new Stop.Builder()
                    .position(new Vector3Pair(new Vector3(0, 0, _last_pos), Vector3.vector3_001))
                    .shape(shape);
            _last_pos += thickness;
            add(_stop);
            return this;
        }

        @Override
        public void computeGlobalTransform(Transform3Cache tcache) {
            super.computeGlobalTransform(tcache);
        }

        @Override
        public Lens.Builder position(Vector3Pair position) {
            return (Lens.Builder) super.position(position);
        }
    }
}
