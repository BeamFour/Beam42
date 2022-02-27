package org.redukti.rayoptics.parax;

import org.redukti.mathlib.M;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.seq.Gap;
import org.redukti.rayoptics.seq.Interface;
import org.redukti.rayoptics.seq.SeqPathComponent;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.specs.FieldSpec;
import org.redukti.rayoptics.specs.PupilSpec;
import org.redukti.rayoptics.specs.SpecKey;
import org.redukti.rayoptics.util.Lists;
import org.redukti.rayoptics.util.Pair;
import org.redukti.rayoptics.util.ZDir;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FirstOrder {

    /**
     * Returns paraxial axial and chief rays, plus first order data.
     *
     * @param opt_model
     * @param stop
     * @param wvl
     * @return
     */
    public static ParaxData compute_first_order(OpticalModel opt_model, Integer stop, double wvl) {
        SequentialModel seq_model = opt_model.seq_model;
        int start = 1;
        double n_0 = seq_model.z_dir.get(start - 1).value * seq_model.central_rndx(start - 1);
        double uq0 = 1.0 / n_0;
        Pair<List<ParaxComponent>, List<ParaxComponent>> paraxcomps =
                paraxial_trace(seq_model.path(wvl, null, null, 1), start,
                        new ParaxComponent(1., 0., 0), new ParaxComponent(0., uq0, 0));
        List<ParaxComponent> p_ray = paraxcomps.first;
        List<ParaxComponent> q_ray = paraxcomps.second;
        // -1 is Pythonic way to get last element
        double n_k = Lists.get(seq_model.z_dir, -1).value * seq_model.central_rndx(-1);
        int img = seq_model.get_num_surfaces() > 2 ? -2 : -1;
        double ak1 = Lists.get(p_ray, img).ht;
        double bk1 = Lists.get(q_ray, img).ht;
        double ck1 = n_k * Lists.get(p_ray, img).slp;
        double dk1 = n_k * Lists.get(q_ray, img).slp;

        // The code below computes the object yu and yu_bar values
        Integer orig_stop = stop;
        ParaxComponent yu = null;
        ParaxComponent yu_bar = null;
        if (stop == null) {
            if (opt_model.parax_model.ax != null) {
                //floating stop surface - use parax_model for starting data
                List<ParaxComponent> ax = opt_model.parax_model.ax;
                List<ParaxComponent> pr = opt_model.parax_model.pr;
                yu = new ParaxComponent(0., Lists.get(ax, 0).slp / n_0, 0.0);
                yu_bar = new ParaxComponent(Lists.get(pr, 0).ht, Lists.get(pr, 0).slp / n_0, 0.0);
            } else {
                // temporarily set stop to surface 1
                stop = 1;
            }
        }
        if (stop != null) {
            double n_s = Lists.get(seq_model.z_dir, stop).value * seq_model.central_rndx(stop);
            double as1 = Lists.get(p_ray, stop).ht;
            double bs1 = Lists.get(q_ray, stop).ht;
            double cs1 = n_s * Lists.get(p_ray, stop).slp;
            double ds1 = n_s * Lists.get(q_ray, stop).slp;

            // find entrance pupil location w.r.t. first surface
            double ybar1 = -bs1;
            double ubar1 = as1;
            n_0 = seq_model.gaps.get(0).medium.rindex(wvl);
            double enp_dist = -ybar1 / (n_0 * ubar1);

            double thi0 = seq_model.gaps.get(0).thi;

            // calculate reduction ratio for given object distance
            double red = dk1 + thi0 * ck1;
            double obj2enp_dist = thi0 + enp_dist;

            PupilSpec pupil = opt_model.optical_spec.pupil;
            double slp0;
            SpecKey key = pupil.key;
            if (key.imageKey.equals("object")) {
                if (key.valueKey.equals("pupil")) {
                    slp0 = 0.5 * pupil.value / obj2enp_dist;
                } else if (key.valueKey.equals("f/#")) {
                    slp0 = -1. / (2.0 * pupil.value);
                } else if (key.valueKey.equals("NA")) {
                    slp0 = n_0 * Math.tan(Math.asin(pupil.value / n_0));
                } else {
                    throw new IllegalArgumentException();
                }
            } else if (key.imageKey.equals("image")) {
                if (key.valueKey.equals("f/#")) {
                    double slpk = -1. / (2.0 * pupil.value);
                    slp0 = slpk / red;
                } else if (key.valueKey.equals("NA")) {
                    double slpk = n_k * Math.tan(Math.asin(pupil.value / n_k));
                    slp0 = slpk / red;
                } else {
                    throw new IllegalArgumentException();
                }
            } else {
                throw new IllegalArgumentException();
            }
            yu = new ParaxComponent(0., slp0, 0.);

            FieldSpec fov = opt_model.optical_spec.field_of_view;
            key = fov.key;
            Pair<Double, Integer> maxfield = fov.max_field();
            double max_fld = maxfield.first;
            int fn = maxfield.second;
            if (M.isZero(max_fld))
                max_fld = 1.0;
            double slpbar0;
            double ybar0;
            if (key.imageKey.equals("object")) {
                if (key.valueKey.equals("angle")) {
                    double ang = Math.toRadians(max_fld);
                    slpbar0 = Math.tan(ang);
                    ybar0 = -slpbar0 * obj2enp_dist;
                } else if (key.valueKey.equals("height")) {
                    ybar0 = -max_fld;
                    slpbar0 = -ybar0 / obj2enp_dist;
                } else {
                    throw new IllegalArgumentException();
                }
            } else if (key.imageKey.equals("image")) {
                if (key.valueKey.equals("height")) {
                    ybar0 = red * max_fld;
                    slpbar0 = -ybar0 / obj2enp_dist;
                } else {
                    throw new IllegalArgumentException();
                }
            } else {
                throw new IllegalArgumentException();
            }
            yu_bar = new ParaxComponent(ybar0, slpbar0, 0.0);
        }
        stop = orig_stop;

        // We have the starting coordinates, now trace the rays
        Pair<List<ParaxComponent>, List<ParaxComponent>> rays = paraxial_trace(seq_model.path(wvl, null, null, 1), 0, yu, yu_bar);
        List<ParaxComponent> ax_ray = rays.first;
        List<ParaxComponent> pr_ray = rays.second;

        // Calculate the optical invariant
        n_0 = seq_model.central_rndx(0);
        double opt_inv = n_0 * (Lists.get(ax_ray, 1).ht * Lists.get(pr_ray, 0).slp - Lists.get(pr_ray, 1).ht * Lists.get(ax_ray, 0).slp);

        //Fill in the contents of the FirstOrderData struct
        FirstOrderData fod = new FirstOrderData();
        fod.opt_inv = opt_inv;
        double obj_dist = fod.obj_dist = seq_model.gaps.get(0).thi;
        double img_dist = 0;
        if (M.isZero(ck1)) {
            img_dist = fod.img_dist = 1e10;
            fod.power = 0.0;
            fod.efl = 0.0;
            fod.pp1 = 0.0;
            fod.ppk = 0.0;
        } else {
            fod.img_dist = img_dist = -Lists.get(ax_ray, img).ht / Lists.get(ax_ray, img).slp;
            fod.power = -ck1;
            fod.efl = -1.0 / ck1;
            fod.pp1 = (dk1 - 1.0) * (n_0 / ck1);
            fod.ppk = (Lists.get(p_ray, -2).ht - 1.0) * (n_k / ck1);
        }
        fod.ffl = fod.pp1 - fod.efl;
        fod.bfl = fod.efl - fod.ppk;
        fod.fno = -1.0 / (2.0 * n_k * Lists.get(ax_ray, -1).slp);

        fod.m = ak1 + ck1 * img_dist / n_k;
        fod.red = dk1 + ck1 * obj_dist;
        fod.n_obj = n_0;
        fod.n_img = n_k;
        fod.img_ht = -fod.opt_inv / (n_k * Lists.get(ax_ray, -1).slp);
        fod.obj_ang = Math.toDegrees(Math.atan(Lists.get(pr_ray, 0).slp));
        if (!M.isZero(Lists.get(pr_ray, 0).slp)) {
            double nu_pr0 = n_0 * Lists.get(pr_ray, 0).slp;
            fod.enp_dist = -Lists.get(pr_ray, 1).ht / nu_pr0;
            fod.enp_radius = Math.abs(fod.opt_inv / nu_pr0);
        } else {
            fod.enp_dist = -1e10;
            fod.enp_radius = 1e10;
        }
        if (!M.isZero(Lists.get(pr_ray, -1).slp)) {
            fod.exp_dist = -(Lists.get(pr_ray, -1).ht / Lists.get(pr_ray, -1).slp - fod.img_dist);
            fod.exp_radius = Math.abs(fod.opt_inv / (n_k * Lists.get(pr_ray, -1).slp));
        } else {
            fod.exp_dist = -1e10;
            fod.exp_radius = 1e10;
        }
        // compute object and image space numerical apertures
        fod.obj_na = n_0 * Math.sin(Math.atan(Lists.get(seq_model.z_dir, 0).value * Lists.get(ax_ray, 0).slp));
        fod.img_na = n_k * Math.sin(Math.atan(Lists.get(seq_model.z_dir, -1).value * Lists.get(ax_ray, -1).slp));

        return new ParaxData(ax_ray, pr_ray, fod);
    }

    public static Pair<List<ParaxComponent>, List<ParaxComponent>> paraxial_trace(List<SeqPathComponent> path, int start, ParaxComponent start_yu, ParaxComponent start_yu_bar) {

        List<ParaxComponent> p_ray = new ArrayList<>();
        List<ParaxComponent> p_ray_bar = new ArrayList<>();

        Iterator<SeqPathComponent> iter = path.iterator();
        SeqPathComponent before = iter.next();

        Interface b4_ifc = before.ifc;
        Gap b4_gap = before.gap;
        double b4_rndx = before.rndx;
        ZDir z_dir_before = before.z_dir;

        double n_before = z_dir_before.value > 0 ? b4_rndx : -b4_rndx;

        ParaxComponent b4_yui = start_yu;
        ParaxComponent b4_yui_bar = start_yu_bar;

        if (start == 1) {
            // compute object coords from 1st surface data
            double t0 = b4_gap.thi;
            double obj_ht = start_yu.ht - t0 * start_yu.slp;
            double obj_htb = start_yu_bar.ht - t0 * start_yu_bar.slp;
            b4_yui = new ParaxComponent(obj_ht, start_yu.slp, 0);
            b4_yui_bar = new ParaxComponent(obj_htb, start_yu_bar.slp, 0);
        }

        double cv = b4_ifc.profile_cv();
        // calculate angle of incidence (aoi)
        double aoi = b4_yui.slp + b4_yui.ht * cv;
        double aoi_bar = b4_yui_bar.slp + b4_yui_bar.ht * cv;

        b4_yui = new ParaxComponent(b4_yui.ht, b4_yui.slp, aoi);
        b4_yui_bar = new ParaxComponent(b4_yui_bar.ht, b4_yui_bar.slp, aoi_bar);

        p_ray.add(b4_yui);
        p_ray_bar.add(b4_yui_bar);

        // loop over remaining surfaces in path
        while (iter.hasNext()) {
            SeqPathComponent after = iter.next();
            Interface ifc = after.ifc;
            Gap gap = after.gap;
            Double rndx = after.rndx;
            ZDir z_dir_after = after.z_dir;

            // Transfer
            double t = b4_gap.thi;
            double cur_ht = b4_yui.ht + t * b4_yui.slp;
            double cur_htb = b4_yui_bar.ht + t * b4_yui_bar.slp;

            double cur_slp;
            double cur_slpb;
            // Refraction/Reflection
            if (ifc.interact_mode.equals("dummy")) { // Object or Image
                cur_slp = b4_yui.slp;
                cur_slpb = b4_yui_bar.slp;
            } else {
                double n_after = z_dir_after.value > 0 ? rndx : -rndx;

                double k = n_before / n_after;

                // calculate slope after refraction/reflection
                double pwr = ifc.optical_power();
                cur_slp = k * b4_yui.slp - cur_ht * pwr / n_after;
                cur_slpb = k * b4_yui_bar.slp - cur_htb * pwr / n_after;

                n_before = n_after;
                z_dir_before = z_dir_after;
            }
            // calculate angle of incidence (aoi)
            cv = ifc.profile_cv();
            aoi = cur_slp + cur_ht * cv;
            aoi_bar = cur_slpb + cur_htb * cv;

            ParaxComponent yu = new ParaxComponent(cur_ht, cur_slp, aoi);
            ParaxComponent yu_bar = new ParaxComponent(cur_htb, cur_slpb, aoi_bar);

            p_ray.add(yu);
            p_ray_bar.add(yu_bar);

            b4_yui = yu;
            b4_yui_bar = yu_bar;

            b4_gap = gap;
        }
        return new Pair<>(p_ray, p_ray_bar);
    }

}
