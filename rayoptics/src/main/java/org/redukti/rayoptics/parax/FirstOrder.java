package org.redukti.rayoptics.parax;

import org.redukti.rayoptics.math.Vector2;
import org.redukti.rayoptics.math.Vector3;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.seq.Gap;
import org.redukti.rayoptics.seq.Interface;
import org.redukti.rayoptics.seq.SeqPathComponent;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.util.Lists;
import org.redukti.rayoptics.util.Pair;
import org.redukti.rayoptics.util.ZDir;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FirstOrder {

    final int ht = 0;
    final int slp = 1;

    /**
     * Returns paraxial axial and chief rays, plus first order data.
     *
     * @param opt_model
     * @param stop
     * @param wvl
     */
    public void compute_first_order(OpticalModel opt_model, int stop, double wvl) {
        SequentialModel seq_model = opt_model.seq_model;
        int start = 1;
        double n_0 = seq_model.z_dir.get(start - 1).value * seq_model.central_rndx(start - 1);
        double uq0 = 1.0/n_0;
        Pair<List<ParaxComponent>, List<ParaxComponent>> paraxcomps =
                paraxial_trace(seq_model.path(wvl, null, null, 1), start,
                new ParaxComponent(1., 0., 0), new ParaxComponent(0., uq0, 0));
        // -1 is Pythonic way to get last element
        double n_k = Lists.get(seq_model.z_dir,-1).value * seq_model.central_rndx(-1);
    }

    public Pair<List<ParaxComponent>,List<ParaxComponent>> paraxial_trace(List<SeqPathComponent> path, int start, ParaxComponent start_yu, ParaxComponent start_yu_bar) {

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
            }
            else {
                double n_after = z_dir_after.value > 0 ? rndx : -rndx;

                double k = n_before/n_after;

                // calculate slope after refraction/reflection
                double pwr = ifc.optical_power();
                cur_slp = k * b4_yui.slp - cur_ht * pwr/n_after;
                cur_slpb = k * b4_yui_bar.slp - cur_htb * pwr/n_after;

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
