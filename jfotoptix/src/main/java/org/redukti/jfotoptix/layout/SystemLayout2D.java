package org.redukti.jfotoptix.layout;

import org.redukti.jfotoptix.medium.Solid;
import org.redukti.jfotoptix.math.*;
import org.redukti.jfotoptix.rendering.Renderer;
import org.redukti.jfotoptix.rendering.RendererSvg;
import org.redukti.jfotoptix.rendering.RendererViewport;
import org.redukti.jfotoptix.rendering.Rgb;
import org.redukti.jfotoptix.model.*;

import java.util.List;

public class SystemLayout2D {

    public void layout2d(RendererSvg r, OpticalSystem system) {
        draw_2d_fit(r, system);
        draw_2d(r, system);
    }

    void draw_2d_fit(RendererViewport r, OpticalSystem system, boolean keep_aspect) {
        Vector3Pair b = system.get_bounding_box();

        r.set_window(Vector2Pair.from(b, 2, 1), keep_aspect);
        r.set_camera_direction(Vector3.vector3_100);
        r.set_camera_position(Vector3.vector3_0);

        r.set_feature_size(b.v1.y() - b.v0.y() / 20.);
    }

    void draw_2d_fit(RendererSvg r, OpticalSystem system) {
        draw_2d_fit(r, system, true);
    }

    void draw_2d(RendererSvg r, OpticalSystem system) {
        // optical axis
        Vector3Pair b = system.get_bounding_box();
        r.draw_segment(new Vector2Pair(new Vector2(b.v0.z(), 0.), new Vector2(b.v1.z(), 0.)), Rgb.rgb_gray);

        for (Element e : system.elements()) {
            draw_element_2d(r, e, null);
        }
    }

    void draw_element_2d(Renderer r, Element self, Element ref) {
        // Order is important here as Lens extends Group
        // and Stop extends Surface
        if (self instanceof Lens) {
            r.group_begin("element");
            draw_2d_e(r, (Lens) self, ref);
            r.group_end();
        } else if (self instanceof Group) {
            r.group_begin("element");
            draw_2d_e(r, (Group) self, ref);
            r.group_end();
        } else if (self instanceof Stop) {
            r.group_begin("element");
            draw_2d_e(r, (Stop) self, ref);
            r.group_end();
        } else if (self instanceof Surface) {
            r.group_begin("element");
            draw_2d_e(r, (Surface) self, ref);
            r.group_end();
        } else {
        }
    }

    void draw_2d_e(Renderer r, Group g, Element ref) {
        for (Element e : g.elements()) {
            draw_element_2d(r, e, ref);
        }
    }

    void draw_2d_e(Renderer r, Lens lens, Element ref) {
        boolean grp = false;

        if (lens.stop() != null)
            draw_2d_e(r, lens.stop(), ref);

        if (lens.elements().isEmpty())
            return;

        List<OpticalSurface> surfaces = lens.surfaces();
        OpticalSurface first = surfaces.get(0);
        if (first.get_material(1) != first.get_material(0)) {
            if (!grp) {
                r.group_begin("");
                grp = true;
            }
            draw_2d_e(r, first, ref);
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
                        Lens.LensEdge.StraightEdge, ref);
                draw_2d_edge(r, left, left_bot_edge, right, right_bot_edge,
                        Lens.LensEdge.StraightEdge, ref);

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
                            Lens.LensEdge.SlopeEdge, ref);
                    draw_2d_edge(r, left, left_bot_hole, right, right_bot_hole,
                            Lens.LensEdge.SlopeEdge, ref);
                }
            }

            if (right.get_material(1) != right.get_material(0)) {
                if (!grp) {
                    r.group_begin("");
                    grp = true;
                }
                draw_2d_e(r, right, ref);
            }
        }

        if (grp) {
            r.group_end();
        }
    }

    void draw_2d_edge(Renderer r, Surface left, double l_y,
                      Surface right, double r_y, Lens.LensEdge type,
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

    void draw_2d_e(Renderer r, Stop stop, Element ref) {
        Vector3 mr = new Vector3(0, stop.get_external_radius(), 0);
        Vector3 top = new Vector3(0, stop.get_shape().get_outter_radius(Vector2.vector2_01), 0);
        Vector3 bot = new Vector3(0, -stop.get_shape().get_outter_radius(Vector2.vector2_01.negate()),
                0);

        Transform3 t = stop.get_transform_to(ref);
        Rgb color = r.get_style_color(stop.get_style());
        r.group_begin("");
        r.draw_segment(t.transform(top), t.transform(mr), color);
        r.draw_segment(t.transform(bot), t.transform(mr.negate()), color);
        r.group_end();
    }

    void draw_2d_e(Renderer r, Surface surface, Element ref) {
        double top_edge = surface.get_shape().get_outter_radius(Vector2.vector2_01);
        double top_hole = surface.get_shape().get_hole_radius(Vector2.vector2_01);

        double bot_edge = -surface.get_shape().get_outter_radius(Vector2.vector2_01.negate());
        double bot_hole = -surface.get_shape().get_hole_radius(Vector2.vector2_01.negate());

        int res = Math.max(
                100,
                Math.min(4, (int) (Math.abs(top_edge - bot_edge) / r.get_feature_size())));

        Rgb color = r.get_style_color(surface.get_style());

        if (Math.abs(bot_hole - top_hole) > 1e-6) {
            Vector2[] p = new Vector2[res / 2];

            get_2d_points(surface, p, top_edge, top_hole, ref);
            r.draw_polygon(p, color, false, false);
            get_2d_points(surface, p, bot_hole, bot_edge, ref);
            r.draw_polygon(p, color, false, false);
        } else {
            Vector2[] p = new Vector2[res];

            get_2d_points(surface, p, top_edge, bot_edge, ref);
            r.draw_polygon(p, color, false, false);
        }
    }

    void get_2d_points(Surface surface, Vector2[] array, double start,
                       double end, Element ref) {
        int count = array.length;
        assert (count > 1);

        double y1 = start;
        double step = (end - start) / (count - 1);
        int i;

        Transform3 t = surface.get_transform_to(ref);

        for (i = 0; i < (int) count; i++) {
            Vector3 v = new Vector3(0., y1, 0.);
            v = v.z(surface.get_curve().sagitta(v.project_xy()));

            array[i] = t.transform(v).project_zy();
            y1 += step;
        }
    }
}
