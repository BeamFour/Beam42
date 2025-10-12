// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.parax.firstorder;

import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.seq.*;
import org.redukti.rayoptics.util.Lists;
import org.redukti.rayoptics.util.ZDir;

import java.util.ArrayList;
import java.util.List;

public class ParaxialModel {
    public OpticalModel opt_model;
    public SequentialModel seq_model;
    public double opt_inv;
    // the reduced forms of the paraxial axial and chief rays
    public List<ParaxComponent> ax;
    public List<ParaxComponent> pr;
    public ParaxData parax_data;
    public List<ParaxPathComp> sys;

    public ParaxialModel(OpticalModel opt_model, SequentialModel seq_model) {
        this.opt_model = opt_model;
        this.seq_model = seq_model;
        this.opt_inv = 1.0;
        this.ax = null;
        this.pr = null;
        this.parax_data = null;
    }

    public void update_model() {
        parax_data = opt_model.optical_spec.parax_data;
        build_lens();
    }

    /**
     * rebuild the `sys` description from the seq_model path
     */
    private void build_lens() {
        sys = seq_path_to_paraxial_lens(seq_model.path(null, null, null, 1));
        // precalculate the reduced forms of the paraxial axial and chief rays
        if (parax_data != null) {
            List<ParaxComponent> ax_ray = parax_data.ax_ray;
            List<ParaxComponent> pr_ray = parax_data.pr_ray;
            FirstOrderData fod = parax_data.fod;
            opt_inv = fod.opt_inv;
            ax = new ArrayList<>();
            pr = new ArrayList<>();
            for (int i = 0; i < sys.size(); i++) {
                double n = sys.get(i).indx;
                ax.add(new ParaxComponent(ax_ray.get(i).ht, n*ax_ray.get(i).slp, n*ax_ray.get(i).aoi));
                pr.add(new ParaxComponent(pr_ray.get(i).ht, n*pr_ray.get(i).slp, n*pr_ray.get(i).aoi));
            }
        }
    }

    /**
     * returns lists of power, reduced thickness, signed index and refract
     *             mode
     * @param path
     */
    public List<ParaxPathComp> seq_path_to_paraxial_lens(List<PathSeg> path) {
        List<ParaxPathComp> sys = new ArrayList<>();
        for (int i = 0; i < path.size(); i++) {
            PathSeg sg = path.get(i);
            Interface ifc = sg.ifc;
            Gap gap = sg.gap;
            Double rndx = sg.Indx;
            ZDir z_dir = sg.Zdir;
            InteractMode imode = ifc.interact_mode;
            double power = ifc.optical_power();
            if (gap != null) {
                double n_after = (z_dir.value > 0) ? rndx : -rndx;
                double tau = gap.thi/n_after;
                sys.add(new ParaxPathComp(power, tau, n_after, imode));
            }
            else {
                sys.add(new ParaxPathComp(power, 0.0, Lists.get(sys,-1).indx, imode));
            }
        }
        return sys;
    }

    public void first_order_data() {
        // List out the first order imaging properties of the model.
        System.out.println(opt_model.optical_spec.parax_data.fod.toString(new StringBuilder()).toString());
    }
}
