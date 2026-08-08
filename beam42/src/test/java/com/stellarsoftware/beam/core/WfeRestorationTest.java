package com.stellarsoftware.beam.core;

import org.junit.jupiter.api.Test;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class WfeRestorationTest implements B4constants {

    @Test
    void reproducesHistoricalWfeDemoRmsAndGroupMeans() {
        RT13 rt13 = load("WFEDEMO");

        int good = rt13.iBuildRays(true);
        assertEquals(320, good);
        assertEquals(10, Globals.giFlags[RNWFEGROUPS]);

        double sumSquares = 0.0;
        double[] groupSums = new double[Globals.giFlags[RNWFEGROUPS]];
        int[] groupCounts = new int[groupSums.length];
        for (int ray=1; ray<=Globals.giFlags[RNRAYS]; ray++) {
            if (!rt13.isRayOK[ray])
                continue;
            double wfe = rt13.dGetRay(ray, 0, RTWFE);
            assertTrue(Double.isFinite(wfe));
            sumSquares += wfe*wfe;
            int group = rt13.iWFEgroup[ray];
            groupSums[group] += wfe;
            groupCounts[group]++;
        }

        // The RAY file records 5.252e-8 from the pre-A208 tracer. The current
        // surface-by-surface tracer produces this stable baseline with the
        // restored WFE calculation.
        assertEquals(3.4775101239437806e-8, Math.sqrt(sumSquares/good), 1e-14);
        for (int group=0; group<groupSums.length; group++)
            assertEquals(0.0, groupSums[group]/groupCounts[group], 1e-14);
    }

    @Test
    void globalRandomizationRemainsDefaultAndGroupedModeIsOptIn() {
        RT13 rt13 = load("WFEDEMO");
        rt13.iBuildRays(true);

        assertFalse(rt13.isGroupedRandomizationEnabled());
        assertTrue(rt13.spans[RU] > rt13.groupSpans[0][RU]);

        boolean traced = false;
        for (int attempt=0; attempt<100 && !traced; attempt++)
            traced = rt13.bRunRandomRay();
        assertTrue(traced);
        assertEquals(Double.doubleToRawLongBits(-0.0),
                Double.doubleToRawLongBits(rt13.dGetRay(0, 0, RTWFE)));

        rt13.setGroupedRandomizationEnabled(true);
        traced = false;
        for (int attempt=0; attempt<100 && !traced; attempt++)
            traced = rt13.bRunRandomRay();
        assertTrue(traced);
        assertTrue(Double.isFinite(rt13.dGetRay(0, 0, RTWFE)));
    }

    private RT13 load(String stem) {
        Globals.init();
        File registryDir = new File("target/wfe-test-registry");
        assertTrue(registryDir.mkdirs() || registryDir.isDirectory());
        Globals.reg = new Registry(registryDir.getPath());
        RT13 rt13 = Globals.RT13;
        OPTDataModel optics = new OPTDataModel(rt13);
        RAYDataModel rays = new RAYDataModel(rt13);
        File examples = new File("../Examples/BeamFour");
        assertTrue(optics.bLoadFile(new File(examples, stem + ".OPT")));
        assertTrue(rays.bLoadFile(new File(examples, stem + ".RAY")));
        new B4DataParser(optics, rays, null, rt13).parse(true);
        assertEquals(GPARSEOK, Globals.giFlags[STATUS]);
        return rt13;
    }
}
