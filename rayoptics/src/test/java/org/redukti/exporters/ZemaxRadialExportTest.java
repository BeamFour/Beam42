// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.exporters;

import org.junit.jupiter.api.Test;
import org.redukti.examples.ExampleFinder;
import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.spec.Prescription;
import org.redukti.spec.SurfaceType;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class ZemaxRadialExportTest {
    private static String surface(String zmx, int number) {
        String marker = "SURF " + number + "\n";
        int start = zmx.indexOf(marker);
        assertTrue(start >= 0);
        int end = zmx.indexOf("\nSURF ", start);
        return zmx.substring(start, end < 0 ? zmx.length() : end);
    }

    private static Map<Integer, Double> extraData(String block) {
        var result = new TreeMap<Integer, Double>();
        for (String line : block.lines().toList()) {
            String[] fields = line.trim().split("\\s+");
            if (fields[0].equals("XDAT"))
                assertNull(result.put(Integer.parseInt(fields[1]), Double.parseDouble(fields[2])));
        }
        return result;
    }

    @Test void sigmaPatentHasNineOddPowersAndTwentyRadialSlots() throws Exception {
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(ExampleFinder.geoPathToExample(
                "Examples/jfotoptix/sigma-14-24mm-f2.8-ml/JP2020-042221_Example01P.txt"));
        var prescription = Prescription.build_prescription(specs, false);
        assertEquals(9, prescription._aspherical_odd_count);
        // JP2020-042221, Example 1, supplied patent table: A3 through A20.
        int[] ids = {1, 5, 6, 31, 32};
        double[][] patent = {
                {0, 8.58209e-6, 0, -1.40764e-8, 0, 3.05748e-11, 0, -5.97803e-14,
                        0, 9.08590e-17, 0, -9.58737e-20, 0, 6.40051e-23, 0, -2.39147e-26, 0, 3.78519e-30},
                {-5.23111e-5, -1.26716e-5, -1.13040e-5, 1.95245e-6, -9.38134e-8, -7.82976e-10,
                        1.22496e-10, 1.97968e-12, -1.33295e-14, -1.10265e-14, 5.32582e-17, 7.28532e-18,
                        1.89244e-19, -1.81192e-20, 1.13899e-21, -2.99255e-23, 1.48595e-25, -3.31214e-27},
                {-3.48559e-5, -1.50563e-5, -1.10043e-5, 1.72222e-6, -4.71099e-8, -3.15483e-9,
                        -5.86163e-11, 1.76453e-11, -1.10783e-13, -5.28181e-15, -8.35047e-16, 5.89441e-17,
                        -9.54814e-18, 3.21284e-19, 6.43253e-21, -4.91029e-23, 1.37261e-24, -5.09803e-25},
                {0, -2.45667e-5, 0, -8.17092e-8, 0, 2.81370e-9, 0, -7.26008e-11, 0, 1.11778e-12,
                        0, -9.83681e-15, 0, 4.86452e-17, 0, -1.24975e-19, 0, 1.29336e-22},
                {0, 5.66654e-6, 0, 5.42201e-8, 0, -2.06458e-9, 0, 3.25090e-11, 0, -2.56410e-13,
                        0, 1.03356e-15, 0, -1.78037e-18, 0, -8.07610e-22, 0, 5.22718e-24}
        };
        String exported = new ZemaxExporter().generate(prescription, true);
        assertFalse(exported.contains("ODDASPHE"));
        for (int i = 0; i < ids.length; i++) {
            var s = prescription._surfaces[ids[i] - 1];
            double[] expected = new double[20];
            System.arraycopy(patent[i], 0, expected, 2, 18);
            assertEquals(SurfaceType.ASPH_RADIAL, s._asph_type);
            assertArrayEquals(expected, s._coeffs);
            String block = surface(exported, ids[i]);
            assertTrue(block.contains("TYPE XOSPHERE\n"));
            assertFalse(block.contains("PARM "));
            double conic = ids[i] == 6 ? -0.0364842 : 0;
            assertTrue(block.contains("CONI " + conic + "\n"));
            Map<Integer, Double> xd = extraData(block);
            assertEquals(22, xd.size());
            assertEquals(20.0, xd.get(1));
            assertEquals(1.0, xd.get(2));
            for (int power = 1; power <= 20; power++)
                assertEquals(expected[power - 1], xd.get(power + 2));
        }
        var restored = new OpticalBenchDataImporter.LensSpecifications();
        restored.parse_buffer(prescription.to_opt_bench_str(new StringBuilder()).toString());
        assertEquals(9, restored.get_aspherical_odd_count());
        var readme = prescription.to_markdown_str(new StringBuilder()).toString();
        assertTrue(readme.contains("| RADIAL |"));
        assertFalse(readme.contains("| ODD |"));
    }

    @Test void sparseRadialExportRetainsZerosAndConicWhileEvenExportKeepsParameters() {
        var p = new Prescription(50, 4, 20, 30, true)
                .surf(50, 5, 20).asph(SurfaceType.ASPH_RADIAL, -0.25,
                        new double[]{0, 0, 1e-6, 2e-7, 0, 3e-9, 0, 4e-11, 0, 5e-13, 0, 6e-15})
                .surf(-50, 40, 20).asph(SurfaceType.ASPH_EVEN, -0.5, new double[]{0, 1e-6})
                .build();
        String zmx = new ZemaxExporter().generate(p, true);
        String radial = surface(zmx, 1);
        assertTrue(radial.contains("CONI -0.25\n"));
        var xd = extraData(radial);
        assertEquals(12.0, xd.get(1));
        for (int power : new int[]{1, 2, 5, 7, 9, 11})
            assertEquals(0.0, xd.get(power + 2));
        assertEquals(6e-15, xd.get(14));
        String even = surface(zmx, 2);
        assertTrue(even.contains("TYPE EVENASPH\n"));
        assertTrue(even.contains("CONI -0.5\n"));
        assertTrue(even.contains("PARM 1 0.0\n"));
        assertTrue(even.contains("PARM 2 1.0E-6\n"));
        assertFalse(even.contains("XDAT"));
    }

    @Test void rejectsRadialOrdersBeyondZemaxLimit() {
        var p = new Prescription(50, 4, 20, 30, true)
                .surf(50, 5, 20).asph(SurfaceType.ASPH_RADIAL, 0, new double[241]).build();
        assertThrows(IllegalArgumentException.class, () -> new ZemaxExporter().generate(p, true));
    }
}
