package org.redukti.jfotoptix.examples;

import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.medium.Abbe;
import org.redukti.jfotoptix.model.Lens;
import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.parax.YNUTrace;

public class ParaxialTrace {

    public static void main(String[] args) throws Exception {

        /*
           Modern Optical Enginerring, W.J.Smith.
           Section 2.6, Example D.
         */
        OpticalSystem.Builder systemBuilder = new OpticalSystem.Builder();
        Lens.Builder lensBuilder = new Lens.Builder()
                .position(Vector3Pair.position_000_001)
                .add_surface(50.0,  10.0, 10,
                        new Abbe(Abbe.AbbeFormula.AbbeVd, 1.5, 55.2, 0.0))
                .add_surface(-50.0, 10.0, 2.0,
                        new Abbe(Abbe.AbbeFormula.AbbeVd, 1.6, 33.8, 0.0))
                .add_surface(0.0,  10.0, 30.0);
        systemBuilder.add(lensBuilder);
        OpticalSystem system = systemBuilder.build();

        YNUTrace ynuTrace = new YNUTrace();
        ynuTrace.trace(system, -300.0, 10);


    }
}
