package org.redukti.rayoptics.parax;

import org.redukti.rayoptics.math.Vector2;
import org.redukti.rayoptics.math.Vector3;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.seq.Gap;
import org.redukti.rayoptics.seq.Interface;
import org.redukti.rayoptics.seq.SeqPathComponent;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.util.ZDir;

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
//        p_ray, q_ray = paraxial_trace(seq_model.path(wl=wvl), start,
//                [1., 0.], [0., uq0])
    }

    public void paraxial_trace(List<SeqPathComponent> path, int start, Vector3 start_yu, Vector3 start_yu_bar) {

        // Vector2.x = ht
        // Vector2.y = slp

        Iterator<SeqPathComponent> iter = path.iterator();
        SeqPathComponent pc = iter.next();

        Interface b4_ifc = pc.ifc;
        Gap b4_gap = pc.gap;
        double b4_rndx = pc.rndx;
        ZDir z_dir_before = pc.z_dir;

        double n_before = z_dir_before.value > 0 ? b4_rndx : -b4_rndx;

        Vector3 b4_yui = start_yu;
        Vector3 b4_yui_bar = start_yu_bar;

        if (start == 1) {
            // compute object coords from 1st surface data
            double t0 = b4_gap.thi;
            double obj_ht = start_yu.x - t0 * start_yu.y;
            double obj_htb = start_yu_bar.x - t0 * start_yu_bar.y;
            b4_yui = new Vector3(obj_ht, start_yu.y, 0);
            b4_yui_bar = new Vector3(obj_htb, start_yu_bar.y, 0);
        }

        double cv = b4_ifc.profile_cv();
        // calculate angle of incidence (aoi)
        double aoi = b4_yui.y + b4_yui.x * cv;
        double aoi_bar = b4_yui_bar.y + b4_yui_bar.x * cv;

        b4_yui = new Vector3(b4_yui.x, b4_yui.y, aoi);
        b4_yui_bar = new Vector3(b4_yui_bar.x, b4_yui_bar.y, aoi_bar);

        // loop over remaining surfaces in path
        while (iter.hasNext()) {

        }
    }
}
