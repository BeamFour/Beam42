// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.parax;

import org.redukti.mathlib.M;
import org.redukti.mathlib.Matrix2;
import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.seq.*;
import org.redukti.rayoptics.specs.*;
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
        var sm = opt_model.seq_model;
        var osp = opt_model.optical_spec;
        int start = 1;
        double oal = sm.overall_length();
        double n_0 = sm.z_dir.get(start-1).value * sm.central_rndx(start-1);
        double n_k = Lists.get(sm.z_dir,-1).value * sm.central_rndx(-1);
        var ppinfo = compute_principle_points(sm.path(wvl, null, null, 1),
                        oal, n_0, n_k, null, null);
        var p_ray = ppinfo.p_ray;
        var q_ray = ppinfo.q_ray;
        int img = sm.get_num_surfaces() > 2 ? -2 : -1;
        double ak1 = Lists.get(p_ray, img).ht;
        double bk1 = Lists.get(q_ray, img).ht;
        double ck1 = n_k * Lists.get(p_ray, img).slp;
        double dk1 = n_k * Lists.get(q_ray, img).slp;
        var Mk1 = new Matrix2(ak1, bk1, ck1, dk1);
        var M1k = new Matrix2(dk1, -bk1, -ck1, ak1);


        // The code below computes the object yu and yu_bar values
        Integer orig_stop = stop;
        ParaxComponent yu = null;
        ParaxComponent yu_bar = null;
        double ybar1 = 0;
        double ubar1 = 0;
        double enp_dist = 0;

        if (stop == null) {
            // check for previously computed paraxial data and
            // use that to float the stop
            if (osp.parax_data != null) {
                //floating stop surface - use parax_model for starting data
                var pr = osp.parax_data.pr_ray;
                enp_dist = -pr.get(1).ht/(n_0*pr.get(0).slp);
            } else {
                // temporarily set stop to surface 1
                stop = 1;
                ybar1 = 0;
                ubar1 = 1.0;
                enp_dist = 0.0;
            }
        }

        if (stop != null) {
            double n_s = Lists.get(sm.z_dir, stop).value * sm.central_rndx(stop);
            double as1 = Lists.get(p_ray, stop).ht;
            double bs1 = Lists.get(q_ray, stop).ht;
            double cs1 = n_s * Lists.get(p_ray, stop).slp;
            double ds1 = n_s * Lists.get(q_ray, stop).slp;

            // find entrance pupil location w.r.t. first surface
            ybar1 = -bs1;
            ubar1 = as1;
            n_0 = sm.gaps.get(0).medium.rindex(wvl);
            enp_dist = -ybar1 / (n_0 * ubar1);
        }
        else {
            double as1 = Lists.get(p_ray, 1).ht;
            double bs1 = Lists.get(q_ray, 1).ht;
            ybar1 = -bs1;
            ubar1 = as1;
        }

        double thi0 = sm.gaps.get(0).thi;

        // calculate reduction ratio for given object distance
        double red = dk1 + thi0 * ck1;
        double obj2enp_dist = thi0 + enp_dist;

        PupilSpec pupil = osp.pupil;
        var aperture_spec = pupil.derive_parax_params();
        var pupil_oi_key = aperture_spec.first;
        var pupil_key =  aperture_spec.second;
        var pupil_value =  aperture_spec.third;

        double slp0;
        if (pupil_oi_key == ImageKey.Object) {
            if (pupil_key == ValueKey.Height) {
                slp0 = pupil_value / obj2enp_dist;
            }
            else if (pupil_key == ValueKey.Slope) {
                slp0 = pupil_value;
            }
            else if (pupil_key == ValueKey.EPD) {
                slp0 = 0.5 * pupil.value / obj2enp_dist;
            }
            else if (pupil_key == ValueKey.Fnum) {
                slp0 = -1.0 / (2.0 * pupil.value);
            }
            else if (pupil_key == ValueKey.NA) {
                slp0 = pupil.value / n_0;
            }
            else {
                throw new IllegalArgumentException();
            }
        }
        else if (pupil_oi_key == ImageKey.Image) {
            double slpk;
            if (pupil_key == ValueKey.Height) {
                slpk = pupil_value / obj2enp_dist;
            }
            else if (pupil_key == ValueKey.Slope) {
                slpk = pupil_value;
            }
            else if (pupil_key == ValueKey.Fnum) {
                slpk = -1.0 / (2.0 * pupil.value);
            }
            else if (pupil_key == ValueKey.NA) {
                slpk = pupil.value / n_k;
            }
            else {
                throw new IllegalArgumentException();
            }
            slp0 = slpk/red;
        } else {
            throw new IllegalArgumentException();
        }

        yu = new ParaxComponent(0., slp0, 0.);
        var field_spec = osp.fov.derive_parax_params();
        var fov_oi_key = field_spec.first;
        var field_key =  field_spec.second;
        var field_value = field_spec.third;

        double slpbar0 = 0;
        double ybar0 = 0;
        if (fov_oi_key == ImageKey.Object) {
            if (field_key == ValueKey.Slope) {
                slpbar0 = field_value;
                ybar0 = -slpbar0 * obj2enp_dist;
            }
            else if (field_key == ValueKey.Height) {
                ybar0 = field_value;
                slpbar0 = -ybar0 / obj2enp_dist;
            }
            else {
                throw new IllegalArgumentException();
            }
        }
        else if (fov_oi_key == ImageKey.Image) {
            var parax_matrix = get_parax_matrix(p_ray,q_ray,-1,n_k);
            var Mi1 = parax_matrix.first;
            var M1i = parax_matrix.second;
            var q_ray_1 = new Vector2(ybar1, ubar1);
            var q_ray_k = Mk1.multiply(q_ray_1);
            var exp_dist = -q_ray_k.x / (n_k * q_ray_k.y); // x=ht, y=slp
            var img2exp_dist = exp_dist - Lists.get(sm.gaps,-1).thi;

            if (field_key == ValueKey.Height) {
                var ht_i = field_value;
                var slp_i = -ht_i / img2exp_dist;
                var pr_ray_i = new Vector2(ht_i, slp_i);
                var pr_ray_1 = M1i.multiply(pr_ray_i);
                slpbar0 = pr_ray_1.y; // y=slp
                ybar0 = -slpbar0*obj2enp_dist;
            }
            else if (field_key == ValueKey.Slope) {
                var slp_k = field_value;
                var ht_k = -slp_k * exp_dist;
                var pr_ray_k = new Vector2(ht_k, slp_k);
                var pr_ray_1 = M1k.multiply(pr_ray_k);
                slpbar0 = pr_ray_1.y; // y=slp
                ybar0 = -slpbar0*obj2enp_dist;
            }
            else {
                throw new IllegalArgumentException();
            }
        }

        yu_bar = new ParaxComponent(ybar0, slpbar0, 0.0);
        stop = orig_stop;
        var idx = 0;

        // We have the starting coordinates, now trace the rays
        Pair<List<ParaxComponent>, List<ParaxComponent>> rays = paraxial_trace(sm.path(wvl, null, null, 1), idx, yu, yu_bar);
        List<ParaxComponent> ax_ray = rays.first;
        List<ParaxComponent> pr_ray = rays.second;

        // Calculate the optical invariant
        double opt_inv = n_0 * (Lists.get(ax_ray, 1).ht * Lists.get(pr_ray, 0).slp - Lists.get(pr_ray, 1).ht * Lists.get(ax_ray, 0).slp);

        //Fill in the contents of the FirstOrderData struct
        FirstOrderData fod = new FirstOrderData();
        fod.opt_inv = opt_inv;
        double obj_dist = fod.obj_dist = sm.gaps.get(0).thi;
        double img_dist = 0;
        if (M.isZero(ck1)) {
            img_dist = fod.img_dist = 1e10;
            fod.power = 0.0;
            fod.fl_obj = 0.0;
            fod.fl_img = 0.0;
            fod.efl = 0.0;
            fod.pp1 = 0.0;
            fod.ppk = 0.0;
        } else {
            if (!M.isZero(Lists.get(ax_ray,img).slp))
                fod.img_dist = img_dist = -Lists.get(ax_ray, img).ht / Lists.get(ax_ray, img).slp;
            else
                fod.img_dist = img_dist = Math.copySign(1e10, Lists.get(sm.gaps,-1).thi);
            fod.power = -ck1;
            fod.fl_obj = n_0/fod.power;
            fod.fl_img = n_k/fod.power;
            fod.efl = fod.fl_img;
            fod.pp1 = (1.0 - dk1) * (fod.fl_obj);
            fod.ppk = (ak1 - 1.0) * (fod.fl_img);
        }
        fod.ffl = fod.pp1 - fod.fl_obj;
        fod.bfl = fod.ppk + fod.fl_img;
        fod.pp_sep = oal - fod.pp1 + fod.ppk;

        if (!M.isZero(Lists.get(ax_ray,img).slp)) {
            fod.fno = -1.0 / (2.0 * n_k * Lists.get(ax_ray, -1).slp);
            fod.img_ht = -fod.opt_inv / (n_k * Lists.get(ax_ray, -1).slp);
        }
        else {
            fod.fno = 1e10;
            fod.img_ht = 1e10;
        }
        fod.m = ak1 + ck1 * img_dist / n_k;
        fod.red = dk1 + ck1 * obj_dist;
        fod.n_obj = n_0;
        fod.n_img = n_k;
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
        fod.obj_na = n_0 * Math.sin(Math.atan(Lists.get(sm.z_dir, 0).value * Lists.get(ax_ray, 0).slp));
        fod.img_na = n_k * Math.sin(Math.atan(Lists.get(sm.z_dir, -1).value * Lists.get(ax_ray, -1).slp));

        return new ParaxData(ax_ray, pr_ray, fod);
    }

    /**
     * Calculate transfer matrix and inverse from 1st to kth surface.
     */
    public static Pair<Matrix2,Matrix2> get_parax_matrix(
            List<ParaxComponent> p_ray,
            List<ParaxComponent> q_ray,
            int kth,
            double n_k) {
        var ak1 = p_ray.get(kth).ht;
        var bk1 = q_ray.get(kth).ht;
        var ck1 = n_k*p_ray.get(kth).slp;
        var dk1 = n_k*q_ray.get(kth).slp;
        var Mk1 = new Matrix2(ak1, bk1, ck1, dk1);
        var M1k = new Matrix2(dk1, -bk1, -ck1, ak1);
        return new Pair<>(Mk1, M1k);
    }

    /**
     * Returns paraxial p and q rays, plus partial first order data.
     *
     *     Args:
     *         path: an iterator containing interfaces and gaps to be traced.
     *               for each iteration, the sequence or generator should return a
     *               list containing: **Intfc, Gap, Trfm, Index, Z_Dir**
     *         oal: overall geometric length of the gaps in `path`
     *         n_0: refractive index preceding the first interface
     *         n_k: refractive index following last interface
     *
     *     Returns:
     *         (p_ray, q_ray, (efl, fl_obj, fl_img, pp1, ppk, pp_sep, ffl, bfl))
     *
     *         - p_ray: [ht, slp, aoi], [1, 0, -]
     *         - q_ray: [ht, slp, aoi], [0, 1, -]
     *         - power: optical power of system
     *         - efl: effective focal length
     *         - fl_obj: object space focal length, f
     *         - fl_img: image space focal length, f'
     *         - pp1: distance from the 1st interface to the front principle plane
     *         - ppk: distance from the last interface to the rear principle plane
     *         - pp_sep: distance from the front principle plane to the rear
     *                   principle plane
     *         - ffl: front focal length, distance from the 1st interface to the
     *                front focal point
     *         - bfl: back focal length, distance from the last interface to the back
     *                focal point
     */
    public static PrincipalPointsInfo compute_principle_points(
            List<PathSeg> path,
            double oal,
            Double n_0,
            Double n_k,
            Integer os_idx,
            Integer is_idx) {
        if (n_0 == null) n_0 = 1.0;
        if (n_k == null) n_k = 1.0;
        if (os_idx == null) os_idx = 1;

        double uq0 = 1.0 / n_0;
        var paraxcomps = paraxial_trace(path, os_idx,
                        new ParaxComponent(1.0, 0.0, 0),
                        new ParaxComponent(0.0, uq0, 0));
        var p_ray = paraxcomps.first;
        var q_ray = paraxcomps.second;

        int img;
        if (is_idx == null)
            img = p_ray.size() > 2 ? -2 : -1;
        else
            img = is_idx;
        // -1 is Pythonic way to get last element
        double ak1 = Lists.get(p_ray, img).ht;
        double bk1 = Lists.get(q_ray, img).ht;
        double ck1 = n_k * Lists.get(p_ray, img).slp;
        double dk1 = n_k * Lists.get(q_ray, img).slp;

        double power = 0.0;
        double fl_obj = 0.0;
        double fl_img = 0.0;
        double efl = 0.0;
        double pp1 = 0.0;
        double ppk = 0.0;
        if (ck1 != 0.0) {
            power = -ck1;
            fl_obj = n_0/power;
            fl_img = n_k/power;
            efl = fl_img;
            pp1 = (1.0 - dk1)*(fl_obj);
            ppk = (ak1 - 1.0)*(fl_img);
        }

        double ffl = pp1 + (-fl_obj);
        double bfl = ppk + fl_img;
        double pp_sep = oal - pp1 + ppk;

        return new PrincipalPointsInfo(p_ray, q_ray, power, efl, fl_obj, fl_img,
                          pp1, ppk, pp_sep, ffl, bfl);
    }


    /**
     * Perform a paraxial raytrace of 2 linearly independent rays
     */
    public static Pair<List<ParaxComponent>, List<ParaxComponent>> paraxial_trace(List<PathSeg> path, int start, ParaxComponent start_yu, ParaxComponent start_yu_bar) {

        List<ParaxComponent> p_ray = new ArrayList<>();
        List<ParaxComponent> p_ray_bar = new ArrayList<>();

        Iterator<PathSeg> iter = path.iterator();
        PathSeg before = iter.next();

        Interface b4_ifc = before.ifc;
        Gap b4_gap = before.gap;
        double b4_rndx = before.Indx;
        ZDir z_dir_before = before.Zdir;

        double n_before = z_dir_before.value > 0 ? b4_rndx : -b4_rndx;

        ParaxComponent b4_yui = start_yu;
        ParaxComponent b4_yui_bar = start_yu_bar;

        if (start == 1) {
            // compute object coords from 1st surface data
            double t0 = b4_gap.thi;
            double obj_ht;
            double obj_htb;
            if (Double.isInfinite(t0)) {
                obj_ht = 0;
                obj_htb = Double.NEGATIVE_INFINITY;
            }
            else {
                obj_ht = start_yu.ht - t0 * start_yu.slp;
                obj_htb = start_yu_bar.ht - t0 * start_yu_bar.slp;
            }
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
            PathSeg after = iter.next();
            Interface ifc = after.ifc;
            Gap gap = after.gap;
            Double rndx = after.Indx;
            ZDir z_dir_after = after.Zdir;

            if (rndx == null)
                rndx = Math.abs(n_before);
            if (z_dir_after == null)
                z_dir_after = z_dir_before;

            // Transfer
            double t = b4_gap.thi;
            double cur_ht = b4_yui.ht + t * b4_yui.slp;
            double cur_htb = b4_yui_bar.ht + t * b4_yui_bar.slp;

            double cur_slp;
            double cur_slpb;
            // Refraction/Reflection
            if (ifc.interact_mode == InteractMode.DUMMY ||
                ifc.interact_mode == InteractMode.PHANTOM) {
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
