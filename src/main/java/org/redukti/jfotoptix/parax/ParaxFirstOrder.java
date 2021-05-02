package org.redukti.jfotoptix.parax;

import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.model.Element;
import org.redukti.jfotoptix.model.OpticalSurface;
import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.model.Stop;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ParaxFirstOrder {

    double effective_focal_length;
    double back_focal_length;
    double optical_invariant;

    public static ParaxFirstOrder compute(OpticalSystem system) {

        YNUTrace tracer = new YNUTrace();
        List<OpticalSurface> seq = system.get_sequence().stream().filter(e -> e instanceof OpticalSurface).map(e -> (OpticalSurface)e).collect(Collectors.toList());
        // Trace a ray parallel to axis at height 1.0 from infinity
        Map<Integer, Vector3> p_ray = tracer.trace(seq, 1.0, 0.0, -1e10);
        // Trace a ray that has unit angle from infinity
        Map<Integer, Vector3> q_ray = tracer.trace(seq, 1e10, 1.0, -1e10);

        int first = seq.get(0).id();
        int last = seq.get(seq.size()-1).id();
        ParaxFirstOrder pfo = new ParaxFirstOrder();
        double u_k = p_ray.get(last).v(1);
        pfo.effective_focal_length = -p_ray.get(first).v(0)/u_k;
        pfo.back_focal_length = -p_ray.get(last).v(0)/u_k;

        double n = seq.get(0).get_material(0).get_refractive_index(SpectralLine.d);


        double phi = -(1.0*u_k)/1.0;
        double fE = 1/phi;

        double n_k = seq.get(seq.size()-1).get_material(1).get_refractive_index(SpectralLine.d);
        double ak1 = p_ray.get(last).v(0);
        double bk1 = q_ray.get(last).v(0);
        double ck1 = n_k * p_ray.get(last).v(1);
        double dk1 = n_k * q_ray.get(last).v(1);

        Stop stop = seq.stream().filter(e-> e instanceof Stop).map(e -> (Stop)e).findFirst().orElse(null);
        double n_s = 1.0*stop.get_material(0).get_refractive_index(SpectralLine.d);
        double as1 = p_ray.get(stop.id()).v(0);
        double bs1 = q_ray.get(stop.id()).v(0);
        double cs1 = n_s*p_ray.get(stop.id()).v(1);
        double ds1 = n_s*q_ray.get(stop.id()).v(1);

        // find entrance pupil location w.r.t. first surface
        double ybar1 = -bs1;
        double ubar1 = as1;
        double n_0 = n;
        double enp_dist = -ybar1/(n_0*ubar1);

        double thi0 = 1e10;
        // calculate reduction ratio for given object distance
        double red = dk1 + thi0*ck1;
        double obj2enp_dist = thi0 + enp_dist;

        String pupil_aperture = "aperture";
        String obj_img_key = "image";
        String value_key = "f/#";
        double pupil_value = 0.98;

        double slp0 = 0.0;
        double slpk = 0.0;
        if (obj_img_key.equals("object")) {
            if (value_key.equals("pupil")) {
                slp0 = 0.5 * pupil_value / obj2enp_dist;
            } else if (value_key.equals("NA")) {
                slp0 = n_0 * Math.tan(Math.asin(pupil_value / n_0));
            }
        }
        else if(obj_img_key.equals("image")) {
            if (value_key.equals("f/#")) {
                slpk = -1.0 / (2.0 * pupil_value);
                slp0 = slpk / red;
            } else if (value_key.equals("NA")) {
                slpk = n_k * Math.tan(Math.asin(pupil_value / n_k));
                slp0 = slpk / red;
            }
        }
        double yu_ht = 0.0;
        double yu_slp = slp0;

        String field = "field";
        obj_img_key = "object";
        value_key = "angle";
        double max_fld = 19.98;
        int fn = 1;
        double ang = 0.0;
        double slpbar0 = 0.0;
        double ybar0 = 0.0;

        if (max_fld == 0.0)
            max_fld = 1.0;
        if (obj_img_key.equals("object")) {
            if (value_key.equals("angle")) {
                ang = Math.toRadians(max_fld);
                slpbar0 = Math.tan(ang);
                ybar0 = -slpbar0 * obj2enp_dist;
            } else if (value_key.equals("height")) {
                ybar0 = -max_fld;
                slpbar0 = -ybar0 / obj2enp_dist;
            }
        }
        else if (obj_img_key.equals("image")) {
            if (value_key.equals("height")) {
                ybar0 = red * max_fld;
                slpbar0 = -ybar0 / obj2enp_dist;
            }
        }
        double yu_bar_ht = ybar0;
        double yu_bar_slp = slpbar0;

        yu_ht = yu_ht + 1e10 * yu_slp;
        yu_bar_ht = yu_bar_ht + 1e10 * yu_bar_slp;

        // We have the starting coordinates, now trace the rays
        Map<Integer, Vector3> ax_ray = tracer.trace(seq, yu_ht, yu_slp, 0.0);
        Map<Integer, Vector3> pr_ray = tracer.trace(seq, yu_bar_ht, yu_bar_slp, 0.0);

        pfo.optical_invariant = n * (ax_ray.get(first).v(0) * yu_bar_slp
                - pr_ray.get(first).v(0) * yu_slp);

        return pfo;
    }

    @Override
    public String toString() {
        return "ParaxFirstOrder{" +
                "effective_focal_length=" + effective_focal_length +
                ", back_focal_length=" + back_focal_length +
                '}';
    }
}
