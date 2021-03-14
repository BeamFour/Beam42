package org.redukti.jfotoptix.examples;

import org.redukti.jfotoptix.material.Abbe;
import org.redukti.jfotoptix.sys.Lens;
import org.redukti.jfotoptix.sys.OpticalSystem;

public class Tessar {

    public static void main() {

        OpticalSystem.Builder sys = new OpticalSystem.Builder();
        Lens.Builder lens = new Lens.Builder()
                .add_surface(1/0.031186861,  14.934638, 4.627804137,
                    new Abbe(Abbe.AbbeFormula.AbbeVd, 1.607170, 59.5002, 0.0))
                .add_surface(0,              14.934638, 5.417429465)
                .add_surface(1/-0.014065441, 12.766446, 3.728230979,
                        new Abbe(Abbe.AbbeFormula.AbbeVd, 1.575960, 41.2999, 0.0))
                .add_surface(1/0.034678487,  11.918098, 4.417903733)
                .add_stop(12.066273, 2.288913925)
                .add_surface(0,              12.372318, 1.499288597,
                        new Abbe(Abbe.AbbeFormula.AbbeVd, 1.526480, 51.4000, 0.0))
                .add_surface(1/0.035104369,  14.642815, 7.996205852,
                        new Abbe(Abbe.AbbeFormula.AbbeVd, 1.623770, 56.8998, 0.0))
                .add_surface(1/-0.021187519, 14.642815, 85.243965130);
        sys.add(lens);

    }


}
