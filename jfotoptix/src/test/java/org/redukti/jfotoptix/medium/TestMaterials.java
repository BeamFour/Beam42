package org.redukti.jfotoptix.medium;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redukti.jfotoptix.light.SpectralLine;

import static org.redukti.jfotoptix.medium.Abbe.AbbeFormula.AbbeVd;
import static org.redukti.jfotoptix.medium.Abbe.AbbeFormula.AbbeVe;
import static org.redukti.jfotoptix.medium.AirFormula.AirBirch94Formula;
import static org.redukti.jfotoptix.medium.AirFormula.AirKohlrausch68Formula;

public class TestMaterials {

    @Test
    public void testVacuum() {
        Vacuum vac = new Vacuum();
        Assertions.assertEquals(1.0, vac.get_refractive_index(550), 1e-8);
    }

    @Test
    public void testAirKohlrausch68() {
        Air airk = new Air(AirKohlrausch68Formula);
        Assertions.assertEquals(1.00027308, airk.get_refractive_index(550), 1e-7);

        airk = new Air(AirKohlrausch68Formula, 2 * Air.std_pressure, 30.);
        Assertions.assertEquals(1.00052810, airk.get_refractive_index(550), 1e-7);

        airk = new Air(AirKohlrausch68Formula, 0, 30.);
        Assertions.assertEquals(1.0, airk.get_refractive_index(550), 1e-7);

        airk = new Air(AirKohlrausch68Formula);
        System.out.println("d " + airk.get_refractive_index(SpectralLine.d));
        System.out.println("C " + airk.get_refractive_index(SpectralLine.C));
        System.out.println("F " + airk.get_refractive_index(SpectralLine.F));
    }

    @Test
    public void testAirBirch94() {
        Air airb = new Air(AirBirch94Formula);
        Assertions.assertEquals(1.00027308, airb.get_refractive_index(550), 1e-7);

        airb = new Air(AirBirch94Formula, 2 * Air.std_pressure, 30.0);
        Assertions.assertEquals(1.00052826, airb.get_refractive_index(550), 1e-7);

        airb = new Air(AirBirch94Formula, 0, 30.0);
        Assertions.assertEquals(1., airb.get_refractive_index(550), 1e-7);

        airb = new Air(AirBirch94Formula);
        System.out.println("d " + airb.get_refractive_index(SpectralLine.d));
        System.out.println("C " + airb.get_refractive_index(SpectralLine.C));
        System.out.println("F " + airb.get_refractive_index(SpectralLine.F));
    }

    @Test
    public void testSellmeier() {
        // measurment material
        Air airm = new Air(AirKohlrausch68Formula);
        // environment material
        Air airk = new Air(AirKohlrausch68Formula);

        // BAF3 Sellmeier
        Sellmeier sellm = new Sellmeier(1.32064267E+000, 8.87798715E-003, 1.33572683E-001,
                4.20290346E-002, 8.85521821E-001, 1.11729167E+002);

        sellm.set_temperature_schott(1.4100E-006, 1.7300E-008, -1.5100E-011,
                5.7600E-007, 4.6800E-010, 2.6700E-001);

        sellm.set_measurement_medium(airm);

        Assertions.assertEquals(1.6056515, sellm.get_refractive_index(400., airk), 1e-7);
        Assertions.assertEquals(1.5738740, sellm.get_refractive_index(800., airk), 1e-7);

        airk = new Air(AirKohlrausch68Formula, Air.std_pressure, 100.);
        sellm.set_temperature(100.);

        Assertions.assertEquals(1.6061251, sellm.get_refractive_index(400., airk), 1e-7);
        Assertions.assertEquals(1.5741071, sellm.get_refractive_index(800., airk), 1e-7);

        airk = new Air(AirKohlrausch68Formula, 10 * Air.std_pressure, 100.);

        Assertions.assertEquals(1.6029774, sellm.get_refractive_index(400., airk), 1e-7);
        Assertions.assertEquals(1.5711062, sellm.get_refractive_index(800., airk), 1e-7);
    }

    @Test
    public void testAbbe() {
        Air airm = new Air(AirKohlrausch68Formula);

        // BAF3
        Sellmeier sellm = new Sellmeier(1.32064267E+000, 8.87798715E-003, 1.33572683E-001,
                4.20290346E-002, 8.85521821E-001, 1.11729167E+002);

        Assertions.assertEquals(46.47, sellm.get_abbe_vd(), 0.01);

        Abbe abbevd = new Abbe(AbbeVd, 1.582670, 46.47, .0001);
        abbevd.set_measurement_medium(airm);

        Assertions.assertEquals(1.605655, abbevd.get_refractive_index(400., airm), 1e-6);
        Assertions.assertEquals(1.573845, abbevd.get_refractive_index(800., airm), 1e-6);

        Assertions.assertEquals(46.18, sellm.get_abbe_ve(), 0.01);

        Abbe abbeve = new Abbe(AbbeVe, 1.585648, 46.18, .0001);
        abbeve.set_measurement_medium(airm);

        Assertions.assertEquals(1.605655, abbeve.get_refractive_index(400., airm), 1e-6);
        Assertions.assertEquals(1.573844, abbeve.get_refractive_index(800., airm), 1e-6);

        GlassMap Hikari_FKH1 = new GlassMap("J-FKH1",1.49782,1.49598,1.502009,0,0);
        Assertions.assertEquals(1.49782, Hikari_FKH1.get_refractive_index(SpectralLine.d), 1e-6);
        Assertions.assertEquals(1.49598, Hikari_FKH1.get_refractive_index(SpectralLine.C), 1e-6);
        Assertions.assertEquals(1.502009, Hikari_FKH1.get_refractive_index(SpectralLine.F), 1e-6);
    }
}
