// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.importers.obench;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.elem.profiles.RadialPolynomial;
import org.redukti.spec.Prescription;
import org.redukti.spec.SurfaceType;
import org.redukti.tools.LensTool2;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class OpticalBenchAsphereTest {
    @TempDir Path tempDir;

    private OpticalBenchDataImporter.LensSpecifications parse(String text) throws Exception {
        Path file = tempDir.resolve("lens.txt");
        Files.writeString(file, text);
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(file.toString());
        return specs;
    }

    private OpticalBenchDataImporter.LensSpecifications lens(String constants, String coefficients) throws Exception {
        return parse("[constants]\n" + constants + "\n[variable distances]\n"
                + "Focal Length\t36\nF-Number\t1.45\nAngle of View\t63.06\n"
                + "[lens data]\n1\t-224.2964\t2.5\t1.7433\t29.46\t49.32\n"
                + "[aspherical data]\n1\t-224.2964\t195\t" + coefficients + "\n");
    }

    @Test void nikkor35mmCoefficientsAndSag() throws Exception {
        // Haruo Sato, US7663816B2, Table 1: https://patents.google.com/patent/US7663816B2/en
        var specs = lens("AsphericalOddCount\t1",
                "-2.0873E-07\t-1.2426E-05\t2.7998E-09\t-5.1736E-11\t1.7973E-13\t-8.9748E-17");
        double[] expected = {0, 0, -2.0873e-7, -1.2426e-5, 0, 2.7998e-9,
                0, -5.1736e-11, 0, 1.7973e-13, 0, -8.9748e-17};
        var prescription = Prescription.build_prescription(specs, false);
        assertEquals(1, prescription._aspherical_odd_count);
        var surface = prescription.get_surfaces()[0];
        assertArrayEquals(expected, surface.get_aspheric_coeffs());
        String readme = LensTool2.startREADME(specs).toString();
        assertTrue(readme.contains("| ID  | Type | k   | P1 | P2 | P3 | P4 | P5 | P6 | P7 | P8 | P9 | P10 | P11 | P12 |\n"));
        assertTrue(readme.contains("| 1| RADIAL | 195.0 | 0.0 | 0.0 | -2.0873E-7 | -1.2426E-5"
                + " | 0.0 | 2.7998E-9 | 0.0 | -5.1736E-11 | 0.0 | 1.7973E-13 | 0.0 | -8.9748E-17 |\n"));
        var profile = new RadialPolynomial().r(surface._radius).cc(surface._k).coefs(surface._coeffs);
        double r = 5;
        double c = 1 / surface._radius;
        double conic = c * r * r / (1 + Math.sqrt(1 - 196 * c * c * r * r));
        double sag = conic - 2.0873e-7 * Math.pow(r, 3) - 1.2426e-5 * Math.pow(r, 4)
                + 2.7998e-9 * Math.pow(r, 6) - 5.1736e-11 * Math.pow(r, 8)
                + 1.7973e-13 * Math.pow(r, 10) - 8.9748e-17 * Math.pow(r, 12);
        assertEquals(sag, profile.sag(3, 4), 1e-14);
        double h = 1e-5;
        assertEquals(-(profile.sag(3 + h, 4) - profile.sag(3 - h, 4)) / (2 * h),
                profile.df(new Vector3(3, 4, sag)).x, 1e-10);
        var restored = Prescription.build_prescription(parse(prescription.to_opt_bench_str(new StringBuilder()).toString()), false);
        assertEquals(1, restored._aspherical_odd_count);
        assertArrayEquals(expected, restored.get_surfaces()[0]._coeffs);
    }

    @Test void twoOddTermsAreInterleavedBeforeEvenOnlyTail() throws Exception {
        var specs = lens("AsphericalOddCount\t2", "3\t4\t5\t6\t8\t10");
        assertArrayEquals(new double[]{0, 0, 3, 4, 5, 6, 0, 8, 0, 10},
                specs.get_surfaces().get(0).get_aspherical_data().get_coeffs());
        var p = Prescription.build_prescription(specs, false);
        var restored = Prescription.build_prescription(parse(p.to_opt_bench_str(new StringBuilder()).toString()), false);
        assertEquals(2, restored._aspherical_odd_count);
        assertArrayEquals(p.get_surfaces()[0]._coeffs, restored.get_surfaces()[0]._coeffs);
    }

    @Test void preservesDeclaredCountEvenWhenOddTermsAreZero() throws Exception {
        var p = Prescription.build_prescription(lens("AsphericalOddCount\t7", "0\t4\t0\t6"), false);
        var restored = Prescription.build_prescription(parse(p.to_opt_bench_str(new StringBuilder()).toString()), false);
        assertEquals(7, restored._aspherical_odd_count);
        assertArrayEquals(p.get_surfaces()[0]._coeffs, restored.get_surfaces()[0]._coeffs);
    }

    @Test void zeroCountAndEvenFormatsKeepEvenPowers() throws Exception {
        for (String constants : new String[]{"", "AsphericalOddCount\t0"}) {
            var asphere = lens(constants, "4\t6\t8").get_surfaces().get(0).get_aspherical_data();
            assertEquals(OpticalBenchDataImporter.AsphereType.Even, asphere.get_asphere_type());
            assertArrayEquals(new double[]{0, 4, 6, 8}, asphere.get_coeffs());
        }
        var a2 = lens("AsphericalA2", "2\t4\t6").get_surfaces().get(0).get_aspherical_data();
        assertArrayEquals(new double[]{2, 4, 6}, a2.get_coeffs());
    }

    @Test void exportRetainsNewOddTermsAndPadsEvenSurfaces() throws Exception {
        var p = Prescription.build_prescription(lens("AsphericalOddCount\t1", "3\t4\t6"), false);
        p.get_surfaces()[0]._coeffs[4] = 5; // newly introduced A5
        p.surf(100, 1, 20).asph(SurfaceType.ASPH_EVEN, 0, new double[]{0, 4, 6}).build();
        var restored = Prescription.build_prescription(parse(p.to_opt_bench_str(new StringBuilder()).toString()), false);
        assertEquals(2, restored._aspherical_odd_count);
        assertArrayEquals(new double[]{0, 0, 3, 4, 5, 6}, restored.get_surfaces()[0]._coeffs);
        assertArrayEquals(new double[]{0, 0, 0, 4, 0, 6}, restored.get_surfaces()[1]._coeffs);
    }

    @Test void invalidCountsAreRejected() {
        for (String count : new String[]{"-1", "1.5", "bad", ""})
            assertThrows(IllegalArgumentException.class, () -> lens("AsphericalOddCount\t" + count, "3\t4"));
    }
}
