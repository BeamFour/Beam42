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
}
