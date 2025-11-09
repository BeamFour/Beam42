// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.elem.surface;

import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.elem.profiles.Spherical;
import org.redukti.rayoptics.elem.profiles.SurfaceProfile;
import org.redukti.rayoptics.seq.InteractMode;
import org.redukti.rayoptics.seq.Interface;
import org.redukti.rayoptics.util.ZDir;

import java.util.ArrayList;
import java.util.List;

/**
 * Container of profile, extent, position and orientation.
 *
 *     Attributes:
 *         label: optional label
 *         profile: :class:`~.elem.profiles.SurfaceProfile`
 *         clear_apertures: list of :class:`Aperture`
 *         edge_apertures: list of :class:`Aperture`
 */
public class Surface extends Interface {

    public String label;
    public List<Aperture> clear_apertures = new ArrayList<>();
    public List<Aperture> edge_apertures = new ArrayList<>();

    public Surface() {
        this("", InteractMode.TRANSMIT);
    }

    public Surface(String label, InteractMode interact_mode) {
        this(interact_mode, 0.0, 1.0, null, label, null);
    }

    public Surface(InteractMode interact_mode, double delta_n,
                   double max_ap, DecenterData decenter, String label, SurfaceProfile profile) {
        super(interact_mode, delta_n, max_ap, decenter);
        this.label = label;
        if (profile != null)
            this.profile = profile;
        else
            this.profile = new Spherical();
    }

    @Override
    public void update() {
        super.update();
        profile.update();
    }

    @Override
    public IntersectionResult intersect(Vector3 p0, Vector3 d, double eps, ZDir z_dir) {
        return profile.intersect(p0, d, eps, z_dir);
    }

    @Override
    public Vector3 normal(Vector3 p) {
        return profile.normal(p);
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        sb.append(getClass().getSimpleName()).append("(");
        if (label != null && !label.isEmpty())
            sb.append("lbl=").append(label).append(", ");
        sb.append("profile=");
        profile.toString(sb);
        sb.append(", ").append("interact_mode='")
                .append(interact_mode).append("'");
        sb.append(")");
        return sb;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    @Override
    public double optical_power() {
        return delta_n * profile.cv;
    }

    @Override
    public double surface_od() {
        double od = 0.0;
        if (!edge_apertures.isEmpty()) {
            for (Aperture e : edge_apertures) {
                double edg = e.max_dimension();
                if (edg > od)
                    od = edg;
            }
        } else if (!clear_apertures.isEmpty()) {
            for (Aperture ca : clear_apertures) {
                double ap = ca.max_dimension();
                if (ap > od)
                    od = ap;
            }
        } else {
            od = max_aperture;
        }
        return od;
    }

    @Override
    public boolean point_inside(double x, double y, Double fuzz) {
        if (fuzz == null) fuzz = 1e-5;
        boolean is_inside = true;
        if (clear_apertures.size() > 0) {
            for (var ca: clear_apertures) {
                is_inside = is_inside && ca.point_inside(x,y,fuzz);
                if (!is_inside)
                    return is_inside;
            }
        }
        else return super.point_inside(x,y,fuzz);
        return is_inside;
    }

    @Override
    public Vector2 edge_pt_target(Vector2 rel_dir) {
        if (clear_apertures.size() > 0)
            return clear_apertures.get(0).edge_pt_target(rel_dir);
        else
            return super.edge_pt_target(rel_dir);
    }

    @Override
    public void set_max_aperture(double max_ap) {
        super.set_max_aperture(max_ap);
        for (var ap: clear_apertures) {
            ap.set_dimension(max_ap,max_ap);
        }
    }
}
