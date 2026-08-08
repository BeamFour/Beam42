package org.redukti.importers.obench;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpticalBenchCatalogTest {

    @TempDir
    Path tempDir;

    @Test
    void importsAndNormalizesCatalogName() throws Exception {
        OpticalBenchDataImporter.LensSpecifications specs = parseLensData(
                "1\t50.0\t4.0\t1.58267\t20.0\t46.19\tBAF3\tschott\n");

        OpticalBenchDataImporter.LensSurface surface = specs.get_surfaces().get(0);
        assertEquals("BAF3", surface.get_glass_name());
        assertEquals("Schott", surface.get_catalog_name());
    }

    @Test
    void legacyLensDataLeavesCatalogUnspecified() throws Exception {
        OpticalBenchDataImporter.LensSpecifications specs = parseLensData(
                "1\t50.0\t4.0\t1.58267\t20.0\t46.19\tBAF3\n");

        OpticalBenchDataImporter.LensSurface surface = specs.get_surfaces().get(0);
        assertEquals("BAF3", surface.get_glass_name());
        assertNull(surface.get_catalog_name());
    }

    private OpticalBenchDataImporter.LensSpecifications parseLensData(String row) throws Exception {
        Path input = tempDir.resolve("prescription.txt");
        Files.writeString(input, "[lens data]\n" + row);
        OpticalBenchDataImporter.LensSpecifications specs =
                new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(input.toString());
        return specs;
    }
}
