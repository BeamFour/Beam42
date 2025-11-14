package org.redukti.rayoptics.parax;

import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.seq.InteractMode;
import org.redukti.rayoptics.seq.PathSeg;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.util.Lists;
import org.redukti.rayoptics.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class ParaxModel {
    public final OpticalModel opt_model;
    public Double opt_inv;
    public final SequentialModel seq_model;
    public List<ParaxItem> sys;
    public List<ParaxComponent> ax;
    public List<ParaxComponent> pr;

    public ParaxModel(OpticalModel opt_model, Double opt_inv) {
        this.opt_model = opt_model;
        this.opt_inv = opt_inv == null ? 1.0 : opt_inv;
        this.seq_model = opt_model.seq_model;
    }

    public static class ParaxItem {
        double power;
        double reduced_thickness;
        double index;
        InteractMode refract_mode;

        public ParaxItem(double power, double reduced_thickness, double index, InteractMode refract_mode) {
            this.power = power;
            this.reduced_thickness = reduced_thickness;
            this.index = index;
            this.refract_mode = refract_mode;
        }
    }

    public static List<ParaxItem> seq_path_to_paraxial_lens(List<PathSeg> path) {
        List<ParaxItem> sys = new ArrayList<>();
        for (int i = 0; i < path.size(); i++) {
            var sg = path.get(i);
            var ifc = sg.ifc;
            var gap = sg.gap;
            var rndx = sg.Indx;
            var z_dir = sg.Zdir;
            var imode = ifc.interact_mode;
            var power = ifc.optical_power();
            if (gap != null) {
                var n_after = z_dir.value > 0 ? rndx : -rndx;
                var tau = gap.thi/n_after;
                sys.add(new ParaxItem(power, tau, n_after, imode));
            }
            else
                sys.add(new ParaxItem(power, 0.0, Lists.get(sys,-1).index, imode));
        }
        return sys;
    }

    public void update_model() {
        var num_ifcs = seq_model.ifcs.size();
        if (num_ifcs > 2)
            build_lens();
    }

    // rebuild the `sys` description from the seq_model path
    void build_lens() {
        sys = seq_path_to_paraxial_lens(seq_model.path());

        // precalculate the reduced forms of the paraxial axial and chief rays
        var parax_data = opt_model.optical_spec.parax_data;
        if (parax_data != null) {
            var ax_ray = parax_data.ax_ray;
            var pr_ray = parax_data.pr_ray;
            var fod = parax_data.fod;
            opt_inv = fod.opt_inv;

            ax = new ArrayList<>();
            pr = new ArrayList<>();

            for (int i = 0; i < sys.size(); i++) {
                var n = sys.get(i).index;
                ax.add(new ParaxComponent(ax_ray.get(i).ht, n*ax_ray.get(i).slp, 0.0));
                pr.add(new ParaxComponent(pr_ray.get(i).ht, n*pr_ray.get(i).slp, 0.0));
            }
        }
    }

    // Calculate the vignetting factors using paraxial optics.
    public Pair<Pair<Double,Integer>,Pair<Double,Integer>> paraxial_vignetting(Double rel_fov) {
        if (rel_fov == null)
            rel_fov = 1.0;
        var sm = seq_model;
        var min_vly = new Pair<Double,Integer>(1.0, null);
        var min_vuy = new Pair<Double,Integer>(1.0, null);
        for (var i = 0; i < sm.ifcs.size()-1; i++) {
            var ifc  = sm.ifcs.get(i);
            if (ax.get(i).ht != 0) {
                var max_ap = ifc.surface_od();
                var y = ax.get(i).ht;
                var ybar = rel_fov * pr.get(i).ht;
                var ratio = (max_ap - Math.abs(ybar))/Math.abs(y);
                if (ratio > 0.0) {
                    if (ybar < 0.0) {
                        if (ratio < min_vly.first)
                            min_vly = new Pair<>(ratio, i);
                    }
                    else if (ybar > 0.0) {
                        if (ratio < min_vuy.first)
                            min_vuy = new Pair<>(ratio, i);
                    }
                    else { //  ybar == 0
                        if (ratio < min_vly.first)
                            min_vly = new Pair<>(ratio, i);
                        if (ratio < min_vuy.first)
                            min_vuy = new Pair<>(ratio, i);
                    }
                }
            }

        }
        return new Pair<>(min_vly, min_vuy);
    }

}
