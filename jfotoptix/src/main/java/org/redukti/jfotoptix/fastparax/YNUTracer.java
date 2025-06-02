package org.redukti.jfotoptix.fastparax;

import org.redukti.jfotoptix.model.*;
import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;

import java.util.*;

public class YNUTracer {

    private final double f_number;
    private final double max_fld;
    int first_surface_position;
    int last_surface_position;
    int stop_position;
    int image_position;
    int[] surface_id_positions;
    Element[] elements;
    double[] curvatures;
    double[] thickness;
    int[] left_medium;
    int[] right_medium;

    String[] glass_names;
    double[] glass_nd;

    Rays p_ray;
    Rays q_ray;
    Rays ax_ray;
    Rays pr_ray;
    static final double DISTANCE = 1e10;
    public final ParaxialFirstOrderInfo pfo = new ParaxialFirstOrderInfo();

    public static class Rays {
        // Outputs
        public final double[] heights;
        public final double[] slopes;
        public final double[] angles;    // angle of incidence

        public Rays(int numElements) {
            heights = new double[numElements];
            slopes = new double[numElements];
            angles = new double[numElements];
        }
    }

    private int glassIndex(String[] glassNames, String name) {
        for (int i = 0; i < glassNames.length; i++) {
            if (glassNames[i].equals(name))
                return i;
        }
        throw new RuntimeException("Glass " + name + " not registered");
    }

    public YNUTracer(OpticalSystem system,
                     String[] glassNames) {
        List<Element> elements = system.get_sequence()
                .stream()
                .filter(e->e instanceof OpticalSurface || e instanceof Image)
                .toList();
        int numElements = elements.size();
        this.f_number = system.get_f_number();
        this.max_fld = system.get_angle_of_view();
        this.elements = elements.toArray(new Element[0]);
        glass_names = glassNames;
        glass_nd = new double[glassNames.length];
        curvatures = new double[numElements];
        thickness = new double[numElements];
        left_medium = new int[numElements];
        right_medium = new int[numElements];
        int max_id = elements.stream().map(e->e.id()).max(Comparator.naturalOrder()).orElseThrow();
        surface_id_positions = new int[max_id+1];
        first_surface_position = -1;
        for (int i = 0; i < elements.size(); i++) {
            Element e = elements.get(i);
            surface_id_positions[e.id()] = i;
            if (e instanceof OpticalSurface opticalSurface) {
                if (first_surface_position < 0)
                    first_surface_position = i;
                last_surface_position = i;
                var glassName1 = opticalSurface.get_material(0).get_name();
                left_medium[i] = glassIndex(glassNames, glassName1);
                var glassName2 = opticalSurface.get_material(1).get_name();
                right_medium[i] = glassIndex(glassNames, glassName2);
                thickness[i] = opticalSurface.get_thickness();
                curvatures[i] = opticalSurface.get_curve().get_curvature();
            }
            if (e instanceof Stop.ApertureStop) {
                stop_position = i;
            }
            if (e instanceof Image) {
                image_position = i;
            }
        }
        p_ray = new Rays(numElements);
        q_ray = new Rays(numElements);
        ax_ray = new Rays(numElements);
        pr_ray = new Rays(numElements);
    }

    public void setGlasses(String[] names, double[] nd) {
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            int idx = glassIndex(glass_names,name);
            glass_nd[idx] = nd[i];
        }
    }

    /*
       The implementation below is based on description in
       Modern Optical Engineering, W.J.Smith.
       Section 2.6, Example D.
       Also see section 5.9 in MIL-HDBK-141
     */
    public void trace(double object_height, double initial_slope_angle, double object_distance, Rays rays) {
        double y1 = object_distance != 0.0 ?
                object_height + object_distance*initial_slope_angle: // Note object_distance will usually be negative
                object_height; // y = height
        double u1 = initial_slope_angle;  // angle
        double y2 = y1;
        double aoi = 0.0;
        for (int i = 0; i < elements.length; i++) {
            if (elements[i] instanceof OpticalSurface surface) {
                y1 = y2; // height on this surface
                //Medium leftMedium = surface.get_material(0);
                //double t1 = surface.get_thickness();
                double t1 = thickness[i];
                //Medium rightMedium = surface.get_material(1);
                //double C1 = surface.get_curve().get_curvature();
                double C1 = curvatures[i];
                //double n1 = leftMedium.get_refractive_index(SpectralLine.d);
                double n1 = glass_nd[left_medium[i]];
                //double n1_ = rightMedium.get_refractive_index(SpectralLine.d);
                double n1_ = glass_nd[right_medium[i]];
                double n1_u1_ = -y1 * C1 * (n1_-n1)  + n1*u1; // Eq 57 in MIL-HDBK-141,, Eq 2.31 in MOE
                // Calculate y for next surface
                y2 = y1 + t1 * (n1_u1_)/n1_;    // Eq 56 in MIL-HDBK-141, Eq 2.32 in MOE
                u1 = n1_u1_/n1_; // ray angle
                aoi = u1 + y1 * C1; // Eq 1.51 in handbook of Optical Dsign
                //double power = surface.power(SpectralLine.d);
                rays.heights[i] = y1;
                rays.slopes[i] = u1;
                rays.angles[i] = aoi;
            }
        }
        rays.heights[image_position] = y2;
        rays.slopes[image_position] = u1;
        rays.angles[image_position] = aoi;
    }

    public ParaxialFirstOrderInfo compute() {

        // Trace a ray parallel to axis at height 1.0 from infinity
        trace(1.0, 0.0, -DISTANCE, p_ray);
        // Trace a ray that has unit angle from infinity
        trace(DISTANCE, 1.0, -DISTANCE, q_ray);

        double u_k = p_ray.slopes[last_surface_position];
        pfo.effective_focal_length = -p_ray.heights[first_surface_position]/u_k;
        pfo.back_focal_length = -p_ray.heights[last_surface_position]/u_k;

        double n = glass_nd[left_medium[0]];
        double n_k = 1.0 * glass_nd[right_medium[last_surface_position]]; // FIXME 1.0 is z_dir
        double ak1 = p_ray.heights[last_surface_position];
        double bk1 = q_ray.heights[last_surface_position];
        double ck1 = n_k * p_ray.slopes[last_surface_position];
        double dk1 = n_k * q_ray.slopes[last_surface_position];

        double n_s = 1.0* glass_nd[left_medium[stop_position]]; // FIXME 1.0 is z_dir
        double as1 = p_ray.heights[stop_position];
        double bs1 = q_ray.heights[stop_position];
        double cs1 = n_s*p_ray.slopes[stop_position];
        double ds1 = n_s*q_ray.slopes[stop_position];

        // find entrance pupil location w.r.t. first surface
        double ybar1 = -bs1;
        double ubar1 = as1;
        double n_0 = n;
        double enp_dist = -ybar1/(n_0*ubar1);

        double thi0 = DISTANCE;
        // calculate reduction ratio for given object distance
        double red = dk1 + thi0*ck1;
        double obj2enp_dist = thi0 + enp_dist;

        String pupil_aperture = "aperture";
        String obj_img_key = "image";
        String value_key = "f/#";
        double pupil_value = f_number;

        double slp0 = 0.0;
        if (obj_img_key.equals("object")) {
            if (value_key.equals("pupil")) {
                slp0 = 0.5 * pupil_value / obj2enp_dist;
            } else if (value_key.equals("NA")) {
                slp0 = n_0 * Math.tan(Math.asin(pupil_value / n_0));
            }
        }
        else if(obj_img_key.equals("image")) {
            if (value_key.equals("f/#")) {
                double slpk = -1.0 / (2.0 * pupil_value);
                slp0 = slpk / red;
            } else if (value_key.equals("NA")) {
                double slpk = n_k * Math.tan(Math.asin(pupil_value / n_k));
                slp0 = slpk / red;
            }
        }
        double yu_ht = 0.0;
        double yu_slp = slp0;

        String field = "field";
        obj_img_key = "object";
        value_key = "angle";
        int fn = 1;
        double ang = 0.0;
        double slpbar0 = 0.0;
        double ybar0 = 0.0;

        double max_fld = this.max_fld;
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

        // Get height at first surface from object height
        yu_ht = yu_ht + DISTANCE * yu_slp;
        yu_bar_ht = yu_bar_ht + DISTANCE * yu_bar_slp;

        // We have the starting coordinates, now trace the rays
        trace(yu_ht, yu_slp, 0.0, ax_ray);
        trace(yu_bar_ht, yu_bar_slp, 0.0, pr_ray);

        pfo.optical_invariant = n * (ax_ray.heights[first_surface_position] * yu_bar_slp
                - pr_ray.heights[first_surface_position] * yu_slp);

        // Fill in the contents of the FirstOrderData struct
        pfo.object_distance = thi0;
        pfo.image_distance = thickness[last_surface_position];
        if (ck1 == 0.0) {
            pfo.power = 0.0;
            pfo.effective_focal_length = 0.0;
            pfo.pp1 = 0.0;
            pfo.ppk = 0.0;
        }
        else {
            pfo.power = -ck1;
            pfo.effective_focal_length = -1.0 / ck1;
            pfo.pp1 = (dk1 - 1.0) * (n_0 / ck1);
            pfo.ppk = (p_ray.heights[last_surface_position] - 1.0) * (n_k / ck1);
            pfo.ffl = pfo.pp1 - pfo.effective_focal_length;
            pfo.back_focal_length = pfo.effective_focal_length - pfo.ppk;
            pfo.fno = -1.0 / (2.0 * n_k * ax_ray.slopes[last_surface_position]);
        }
        pfo.m = ak1 + ck1*pfo.image_distance/n_k;
        pfo.red = dk1 + ck1*pfo.object_distance;
        pfo.n_obj = n_0;
        pfo.n_img = n_k;
        pfo.img_ht = -pfo.optical_invariant/(n_k*ax_ray.slopes[last_surface_position]);
        pfo.obj_ang = Math.toDegrees(Math.atan(yu_bar_slp));
        if (yu_bar_slp != 0) {
            double nu_pr0 = n_0 * yu_bar_slp;
            pfo.enp_dist = -pr_ray.heights[first_surface_position] / nu_pr0;
            pfo.enp_radius = Math.abs(pfo.optical_invariant / nu_pr0);
        }
        else {
            pfo.enp_dist = -DISTANCE;
            pfo.enp_radius = DISTANCE;
        }

        if (pr_ray.slopes[last_surface_position] != 0) {
            pfo.exp_dist = -(pr_ray.heights[image_position] / pr_ray.slopes[image_position] - pfo.image_distance);
            pfo.exp_radius = Math.abs(pfo.optical_invariant / (n_k * pr_ray.slopes[image_position]));
        }
        else {
            pfo.exp_dist = -DISTANCE;
            pfo.exp_radius = DISTANCE;
        }

        // compute object and image space numerical apertures
        pfo.obj_na = n_0*Math.sin(Math.atan(1.0*yu_slp)); // FIXME 1.0 is z_dir
        pfo.img_na = n_k*Math.sin(Math.atan(1.0*ax_ray.slopes[image_position])); // FIXME 1.0 is z_dir

        return pfo;
    }

}
