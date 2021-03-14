package org.redukti.jfotoptix.examples;

import org.redukti.jfotoptix.material.Abbe;
import org.redukti.jfotoptix.sys.Lens;
import org.redukti.jfotoptix.sys.OpticalSystem;

public class Tessar {

    public static void main() {

        OpticalSystem.Builder sys = new OpticalSystem.Builder();
        Lens.Builder lens = new Lens.Builder();
        lens.add_surface(1/0.031186861,  14.934638, 4.627804137,
                new Abbe(Abbe.AbbeFormula.AbbeVd, 1.607170, 59.5002, 0.0));
    }

}
