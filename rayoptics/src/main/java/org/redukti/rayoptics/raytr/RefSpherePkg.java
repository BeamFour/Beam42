package org.redukti.rayoptics.raytr;

import org.redukti.rayoptics.parax.firstorder.ParaxData;
import org.redukti.rayoptics.util.ZDir;

public class RefSpherePkg {
    public RefSphere ref_sphere;
    public ParaxData parax_data;
    public double n_obj;
    public double n_img;
    public ZDir z_dir;

    public RefSpherePkg(RefSphere ref_sphere, ParaxData parax_data, double n_obj, double n_img, ZDir z_dir) {
        this.ref_sphere = ref_sphere;
        this.parax_data = parax_data;
        this.n_obj = n_obj;
        this.n_img = n_img;
        this.z_dir = z_dir;
    }
}
