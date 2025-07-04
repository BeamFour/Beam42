package org.redukti.rayoptics.elem.surface;

import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.elem.profiles.Spherical;
import org.redukti.rayoptics.elem.profiles.SurfaceProfile;
import org.redukti.rayoptics.seq.Interface;
import org.redukti.rayoptics.util.ZDir;

import java.util.ArrayList;
import java.util.List;

/**
 * Container of profile, extent, position and orientation
 */
public class Surface extends Interface {

    public String label;
    public List<Aperture> clear_apertures = new ArrayList<>();
    public List<Aperture> edge_apertures = new ArrayList<>();

    public Surface() {
        this("", "transmit");
    }

    public Surface(String label, String interact_mode) {
        this(interact_mode, 0.0, 1.0, null, label, null);
    }

    public Surface(String interact_mode, double delta_n,
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
}
