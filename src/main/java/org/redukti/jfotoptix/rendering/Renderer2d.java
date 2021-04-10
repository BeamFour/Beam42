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

package org.redukti.jfotoptix.rendering;

import org.redukti.jfotoptix.math.*;

import java.util.EnumSet;

/**
 Base class for 2d rendering drivers

 This class provide default implementations for 3d projection
 and 3d drawing primitives. It's designed to be used as a base
 class for 2d only renderers so that they can perform 3d
 rendering too.
*/
public abstract class Renderer2d extends RendererViewport {
    enum ProjectionType {
        Ortho,
        Perspective;
    }

    ProjectionType _projection_type = ProjectionType.Ortho;
    Transform3 _cam_transform = new Transform3();
    double _eye_dist;

    public Renderer2d() {
    }

    @Override
    public void set_perspective() {
        double out_ratio
                = (_2d_output_res.y() / _rows) / (_2d_output_res.x() / _cols);

        if (out_ratio < 1.)
            _window2d = new Vector2Pair(new Vector2(-1. / out_ratio, -1.), new Vector2(1. / out_ratio, 1.));
        else
            _window2d = new Vector2Pair(new Vector2(-1, -out_ratio), new Vector2(1., out_ratio));
        _window2d_fit = _window2d;
        update_2d_window();
        set_page(_pageid);
        _projection_type = ProjectionType.Perspective;
        _eye_dist = 1. / Math.tan(Math.toRadians(_fov) / 2.);
    }

    @Override
    public void set_orthographic() {
        this._projection_type = ProjectionType.Ortho;
    }

    /** project in 2d space */
    public Vector2 project(Vector3 v) {
        switch (_projection_type) {
            case Perspective:
                return projection_perspective(v);
            default:
                return projection_ortho(v);
        }
    }

    /** project in 2d space and scale for ploting to 2d output */
    public Vector2 project_scale(Vector3 v) {
        Vector2 v2d = project(v);
        return new Vector2(x_trans_pos(v2d.x()), y_trans_pos(v2d.y()));
    }

    public Vector2 projection_ortho(Vector3 v) {
        return _cam_transform.transform(v).project_xy();
    }

    public Vector2 projection_perspective(Vector3 v) {
        Vector3 t = _cam_transform.transform(v);
        return new Vector2(t.x() * _eye_dist / -t.z(), t.y() * _eye_dist / -t.z());
    }

    public void draw_point(Vector3 p, Rgb rgb,
                           PointStyle s) {
        draw_point(project(p), rgb, s);
    }

    public void draw_segment(Vector3Pair l, Rgb rgb) {
        draw_segment(new Vector2Pair(project(l.point()), project(l.direction())), rgb);
    }

    public void draw_text(Vector3 pos, Vector3 dir,
                          String str, TextAlignMask a, int size,
                          Rgb rgb) {
        draw_text(project(pos), project(dir), str, EnumSet.of(a), size, rgb);
    }

    /** Get reference to 3d camera transform */
    @Override
    public Transform3 get_camera_transform () {
        return _cam_transform;
    }
    /** Get modifiable reference to 3d camera transform */
    @Override
    public void set_camera_transform (Transform3 t) {
        this._cam_transform = t;
    }
}
