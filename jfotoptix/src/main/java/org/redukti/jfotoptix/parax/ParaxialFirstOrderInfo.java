// Large sections of the following implementation are ported from
// https://github.com/mjhoptics/ray-optics
// SD 3-Clause License
//
//Copyright (c) 2017-2021, Michael J. Hayford
//All rights reserved.

package org.redukti.jfotoptix.parax;

import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.model.OpticalSurface;
import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.model.Stop;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ParaxialFirstOrderInfo {

    // efl: effective focal length
    public double effective_focal_length;
    // bfl: back focal length
    public double back_focal_length;
    // opt_inv: optical invariant
    public double optical_invariant;
    // obj_dist: object distance
    public double object_distance;
    // img_dist: paraxial image distance
    public double image_distance;
    public double power;
    // pp1: distance of front principle plane from 1st surface
    public double pp1;
    // ppk: distance of rear principle plane from last surface
    public double ppk;
    // ffl: front focal length
    public double ffl;
    // fno: focal ratio at working conjugates, f/#
    public double fno;
    // enp_dist: entrance pupil distance from 1st surface
    public double enp_dist;
    // enp_radius: entrance pupil radius
    public double enp_radius;
    // exp_dist: exit pupil distance from last interface
    public double exp_dist;
    // exp_radius: exit pupil radius
    public double exp_radius;
    public double m;
    // red: reduction ratio
    public double red;
    // n_obj: refractive index at central wavelength in object space
    public double n_obj;
    // n_img: refractive index at central wavelength in image space
    public double n_img;
    // img_ht: image height
    public double img_ht;
    // obj_ang: maximum object angle (degrees)
    public double obj_ang;
    // obj_na: numerical aperture in object space
    public double obj_na;
    // img_na: numerical aperture in image space
    public double img_na;

    static final double DISTANCE = 1e10;

    public static ParaxialFirstOrderInfo compute(OpticalSystem system) {

        YNUTrace tracer = new YNUTrace();
        List<OpticalSurface> seq = system.get_sequence().stream().filter(e -> e instanceof OpticalSurface).map(e -> (OpticalSurface)e).collect(Collectors.toList());
        // Trace a ray parallel to axis at height 1.0 from infinity
        Map<Integer, YNUTraceData> p_ray = tracer.trace(seq, 1.0, 0.0, -DISTANCE);
        // Trace a ray that has unit angle from infinity
        Map<Integer, YNUTraceData> q_ray = tracer.trace(seq, DISTANCE, 1.0, -DISTANCE);

        int first = seq.get(0).id();
        int last = seq.get(seq.size()-1).id();
        ParaxialFirstOrderInfo pfo = new ParaxialFirstOrderInfo();
        double u_k = p_ray.get(last).slope;
        pfo.effective_focal_length = -p_ray.get(first).height/u_k;
        pfo.back_focal_length = -p_ray.get(last).height/u_k;

        double n = seq.get(0).get_material(0).get_refractive_index(SpectralLine.d);
        double n_k = 1.0*seq.get(seq.size()-1).get_material(1).get_refractive_index(SpectralLine.d); // FIXME 1.0 is z_dir
        double ak1 = p_ray.get(last).height;
        double bk1 = q_ray.get(last).height;
        double ck1 = n_k * p_ray.get(last).slope;
        double dk1 = n_k * q_ray.get(last).slope;

        Stop stop = seq.stream().filter(e-> e instanceof Stop.ApertureStop).map(e -> (Stop)e).findFirst().orElse(null);
        double n_s = 1.0*stop.get_material(0).get_refractive_index(SpectralLine.d); // FIXME 1.0 is z_dir
        double as1 = p_ray.get(stop.id()).height;
        double bs1 = q_ray.get(stop.id()).height;
        double cs1 = n_s*p_ray.get(stop.id()).slope;
        double ds1 = n_s*q_ray.get(stop.id()).slope;

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
        double pupil_value = system.get_f_number();

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
        double max_fld = system.get_angle_of_view();
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

        // Get height at first surface from object height
        yu_ht = yu_ht + DISTANCE * yu_slp;
        yu_bar_ht = yu_bar_ht + DISTANCE * yu_bar_slp;

        // We have the starting coordinates, now trace the rays
        Map<Integer, YNUTraceData> ax_ray = tracer.trace(seq, yu_ht, yu_slp, 0.0);
        Map<Integer, YNUTraceData> pr_ray = tracer.trace(seq, yu_bar_ht, yu_bar_slp, 0.0);

        pfo.optical_invariant = n * (ax_ray.get(first).height * yu_bar_slp
                - pr_ray.get(first).height * yu_slp);

        // Fill in the contents of the FirstOrderData struct
        pfo.object_distance = thi0;
        pfo.image_distance = seq.get(seq.size()-1).get_thickness();
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
            pfo.ppk = (p_ray.get(last).height - 1.0) * (n_k / ck1);
            pfo.ffl = pfo.pp1 - pfo.effective_focal_length;
            pfo.back_focal_length = pfo.effective_focal_length - pfo.ppk;
            pfo.fno = -1.0 / (2.0 * n_k * ax_ray.get(last).slope);
        }
        pfo.m = ak1 + ck1*pfo.image_distance/n_k;
        pfo.red = dk1 + ck1*pfo.object_distance;
        pfo.n_obj = n_0;
        pfo.n_img = n_k;
        pfo.img_ht = -pfo.optical_invariant/(n_k*ax_ray.get(last).slope);
        pfo.obj_ang = Math.toDegrees(Math.atan(yu_bar_slp));
        if (yu_bar_slp != 0) {
            double nu_pr0 = n_0 * yu_bar_slp;
            pfo.enp_dist = -pr_ray.get(first).height / nu_pr0;
            pfo.enp_radius = Math.abs(pfo.optical_invariant / nu_pr0);
        }
        else {
            pfo.enp_dist = -DISTANCE;
            pfo.enp_radius = DISTANCE;
        }

        if (pr_ray.get(last).slope != 0) {
            pfo.exp_dist = -(pr_ray.get(0).height / pr_ray.get(0).slope - pfo.image_distance);
            pfo.exp_radius = Math.abs(pfo.optical_invariant / (n_k * pr_ray.get(0).slope));
        }
        else {
            pfo.exp_dist = -DISTANCE;
            pfo.exp_radius = DISTANCE;
        }

        // compute object and image space numerical apertures
        pfo.obj_na = n_0*Math.sin(Math.atan(1.0*yu_slp)); // FIXME 1.0 is z_dir
        pfo.img_na = n_k*Math.sin(Math.atan(1.0*ax_ray.get(0).slope)); // FIXME 1.0 is z_dir

        return pfo;
    }

    @Override
    public String toString() {
        return    "effective_focal_length " + effective_focal_length +
                "\nback_focal_length      " + back_focal_length +
                "\noptical_invariant      " + optical_invariant +
                "\nobject_distance        " + object_distance +
                "\nimage_distance         " + image_distance +
                "\npower                  " + power +
                "\npp1_H                  " + pp1 +
                "\nppk_H'                 " + ppk +
                "\nffl_F                  " + ffl +
                "\nfno                    " + fno +
                "\nenp_dist_P             " + enp_dist +
                "\nenp_radius             " + enp_radius +
                "\nexp_dist_P'            " + exp_dist +
                "\nexp_radius             " + exp_radius +
                "\nm                      " + m +
                "\nred                    " + red +
                "\nn_obj                  " + n_obj +
                "\nn_img                  " + n_img +
                "\nimg_ht                 " + img_ht +
                "\nobj_ang                " + obj_ang +
                "\nobj_na                 " + obj_na +
                "\nimg_na                 " + img_na +
                "\n";
    }
}
