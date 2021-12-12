package org.redukti.rayoptics.integration;

import org.junit.jupiter.api.Test;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.specs.FieldSpec;
import org.redukti.rayoptics.specs.OpticalSpecs;
import org.redukti.rayoptics.specs.PupilSpec;
import org.redukti.rayoptics.specs.SpecKey;
import org.redukti.rayoptics.util.Pair;

public class NoctNikkorTest {

    @Test
    public void test() {
        OpticalModel opm = new OpticalModel();
        SequentialModel sm = opm.sequential_model;
        OpticalSpecs osp = opm.optical_spec;
        osp.pupil = new PupilSpec(osp, new Pair<>("image", "f/#"), 0.98);
        osp.field_of_view = new FieldSpec(osp, new Pair<>("object", "angle"), new double [] {0., 19.98});
    }
}
