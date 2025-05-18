package org.redukti.rayoptics.elem.layout;

import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.specs.FieldSpec;

import java.util.ArrayList;
import java.util.List;

public class LensLayout {
    OpticalModel opt_model;

    public LensLayout(OpticalModel opt_model) {
        this.opt_model = opt_model;
    }

    public List<RayBundle> create_ray_entities(double start_offset) {
        List<RayBundle> ray_bundles = new ArrayList<>();
        FieldSpec fov = opt_model.optical_spec.field_of_view;
        double wvl = opt_model.seq_model.central_wavelength();
        for (int i = 0; i < fov.fields.length; i++) {
            Field fld = fov.fields[i];
            String fld_label = fov.index_labels[i];
            RayBundle rb = new RayBundle(opt_model, fld, fld_label, wvl, start_offset);
            ray_bundles.add(rb);
        }
        return ray_bundles;
    }


}
