package org.redukti.jfotoptix.material;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.redukti.jfotoptix.material.AirFormula.AirKohlrausch68Formula;

public class TestMaterials {

    @Test
    public void testVacuum() {
        Vacuum vac = new Vacuum();
        Assertions.assertEquals( 1.0, vac.get_refractive_index(550), 1e-8);
    }

    @Test
    public void testAirKohlrausch68() {
        Air airk = new Air(AirKohlrausch68Formula);
        Assertions.assertEquals( 1.00027308, airk.get_refractive_index(550),  1e-7);

        airk = new Air(AirKohlrausch68Formula, 2 * airk.std_pressure, 30.);
        Assertions.assertEquals( 1.00052810, airk.get_refractive_index(550) , 1e-7);

        airk = new Air(AirKohlrausch68Formula, 0, 30.);
        Assertions.assertEquals( 1.0, airk.get_refractive_index(550),  1e-7);
    }
}
