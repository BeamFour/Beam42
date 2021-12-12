package org.redukti.rayoptics.elem;

import org.redukti.rayoptics.seq.Interface;

import java.util.ArrayList;
import java.util.List;

/**
 * Container of profile, extent, position and orientation
 */
public class Surface extends Interface {

    public String label;
    public SurfaceProfile profile;
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

}
