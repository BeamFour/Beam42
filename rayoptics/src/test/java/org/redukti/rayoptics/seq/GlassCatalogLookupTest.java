package org.redukti.rayoptics.seq;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlassCatalogLookupTest {

    @Test
    void catalogQualifiedLookupDisambiguatesDuplicateGlassNames() {
        Glass hikari = Glass.glass_by_catalog_name("Hikari", "BAF3");
        Glass schott = Glass.glass_by_catalog_name("Schott", "BAF3");

        assertNotNull(hikari);
        assertNotNull(schott);
        assertEquals("Hikari", hikari.catalog_name);
        assertEquals("Schott", schott.catalog_name);
        assertNotSame(hikari, schott);
    }

    @Test
    void catalogNamesAreMatchedWithoutCaseSensitivity() {
        Glass glass = Glass.glass_by_catalog_name("sChOtT", "BAF3");

        assertNotNull(glass);
        assertEquals("Schott", glass.catalog_name);
    }

    @Test
    void nameOnlyLookupRetainsLegacyCatalogPriority() {
        Glass glass = Glass.glass_by_name("BAF3");

        assertNotNull(glass);
        assertEquals("Hoya", glass.catalog_name);
    }

    @Test
    void unknownCatalogDoesNotFallBackToAnotherManufacturer() {
        assertNull(Glass.glass_by_catalog_name("Unknown catalog", "BAF3"));
    }

    @Test
    void findsAndRanksGlassesByNdAndVd() {
        var matches = Glass.find_glasses(1.58267, 46.48);

        assertFalse(matches.isEmpty());
        assertEquals("Hoya", matches.get(0).glass().catalog_name);
        assertEquals("BAF3", matches.get(0).glass().label);
        assertTrue(matches.get(0).exact());
        assertTrue(matches.get(0).score() < 0.001);
    }

    @Test
    void rejectsGlassesOutsideConfiguredTolerances() {
        assertTrue(Glass.find_glasses(1.10, 10.0).isEmpty());
    }

    @Test
    void limitsNearestNeighbourResults() {
        var matches = Glass.find_glasses(1.5827, 46.5,
                Glass.DEFAULT_ND_TOLERANCE, Glass.DEFAULT_VD_TOLERANCE, 2);

        assertEquals(2, matches.size());
        assertTrue(matches.get(0).score() <= matches.get(1).score());
    }
}
