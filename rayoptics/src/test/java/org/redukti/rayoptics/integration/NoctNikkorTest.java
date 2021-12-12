package org.redukti.rayoptics.integration;

import org.junit.jupiter.api.Test;
import org.redukti.rayoptics.math.Vector2;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.specs.*;
import org.redukti.rayoptics.util.Pair;

public class NoctNikkorTest {

    @Test
    public void test() {
        OpticalModel opm = new OpticalModel();
        SequentialModel sm = opm.sequential_model;
        OpticalSpecs osp = opm.optical_spec;
        osp.pupil = new PupilSpec(osp, new Pair<>("image", "f/#"), 0.98);
        osp.field_of_view = new FieldSpec(osp, new Pair<>("object", "angle"), new double [] {0., 19.98});
        osp.spectral_region = new WvlSpec(new Vector2[]{ new Vector2(486.1327, 0.5),
                new Vector2(587.5618, 1.0),
                new Vector2(656.2725, 0.5)}, 1);
        opm.system_spec.title = "WO2019-229849 Example 1 (Nikkor Z 58mm f/0.95 S)";
        opm.system_spec.dimensions = "MM";
        opm.radius_mode = true;
        sm.gaps.get(0).thi=1e10;
        // sm.add_surface([108.488,7.65,'J-LASFH9A', 'Hikari'])
    }
}
