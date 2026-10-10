package org.redukti.spec;

import org.junit.jupiter.api.Test;
import org.redukti.rayoptics.seq.Glass;
import org.redukti.rayoptics.specs.WvlSpec;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WavelengthSetTest {

    @Test
    void theStandardSetsAreTheirDocumentedLinesAndWeights() {
        assertEquals("d", WavelengthSet.dLine().letters());
        assertArrayEquals(new double[]{1.0}, WavelengthSet.dLine().weights());

        assertEquals("d F C", WavelengthSet.standardTriColor().letters());
        assertTrue(WavelengthSet.standardTriColor().hasEqualWeights());

        var five = WavelengthSet.standardFiveColor();
        assertEquals("d C e F g", five.letters());
        assertArrayEquals(new double[]{1.0, 0.475, 0.98, 0.49, 0.15}, five.weights());
        assertTrue(five.hasStandardWeights());

        // The measurement weighting is the same five lines, weighted differently: it is
        // greener than the design set, so e carries more than d.
        var k9 = WavelengthSet.k9Tester();
        assertEquals("d C e F g", k9.letters());
        assertArrayEquals(new double[]{1.0, 0.330, 1.046, 0.654, 0.235}, k9.weights());
        assertFalse(k9.hasStandardWeights());
        assertFalse(k9.hasEqualWeights());
    }

    /** Wavelengths come from the ported spectral line table, not from a copy of it. */
    @Test
    void linesResolveThroughWvlSpec() {
        assertArrayEquals(
                new double[]{WvlSpec.get_wavelength("d"), WvlSpec.get_wavelength("C"),
                        WvlSpec.get_wavelength("e"), WvlSpec.get_wavelength("F"),
                        WvlSpec.get_wavelength("g")},
                WavelengthSet.standardFiveColor().wavelengths());
        assertEquals(WvlSpec.get_wavelength("e"), WavelengthSet.eLine().wavelengths()[0]);
    }

    @Test
    void lettersRoundTripAndUnknownOnesAreRejected() {
        assertEquals("e", WavelengthSet.letterOf(WvlSpec.get_wavelength("e")));
        assertNull(WavelengthSet.letterOf(500.0));

        // Case matters: C and c are not the same line, and only one of them is a line.
        assertEquals(WvlSpec.get_wavelength("C"), WavelengthSet.wavelengthOf("C"));
        var unknown = assertThrows(IllegalArgumentException.class,
                () -> WavelengthSet.wavelengthOf("c"));
        assertTrue(unknown.getMessage().contains("unknown wavelength 'c'"), unknown.getMessage());

        // Resolvable in the ported table, but not a Fraunhofer line a trial may name.
        assertThrows(IllegalArgumentException.class, () -> WavelengthSet.wavelengthOf("He-Ne"));
    }

    @Test
    void rejectsMismatchedOrNegativeWeights() {
        assertThrows(IllegalArgumentException.class,
                () -> WavelengthSet.ofLetters(List.of("d", "F"), new double[]{1.0}));
        assertThrows(IllegalArgumentException.class,
                () -> WavelengthSet.ofLetters(List.of("d"), new double[]{-1.0}));
        assertThrows(IllegalArgumentException.class,
                () -> WavelengthSet.of(new double[]{0.0}, new double[]{1.0}));
        assertThrows(IllegalArgumentException.class, () -> WavelengthSet.ofLetters(List.of(), null));
    }

    /**
     * A catalog glass has measured indices at seven lines and throws for anything else, so
     * every set offered here has to stay within them or a trial using it cannot be traced.
     */
    @Test
    void everyBuiltInSetIsTraceableWithACatalogGlass() {
        Glass bk7 = Glass.glass_by_catalog_name("Schott", "N-BK7");
        assertNotNull(bk7);
        for (var set : List.of(WavelengthSet.dLine(), WavelengthSet.eLine(),
                WavelengthSet.standardTriColor(), WavelengthSet.standardFiveColor(),
                WavelengthSet.k9Tester()))
            for (double wavelength : set.wavelengths())
                assertTrue(bk7.rindex(wavelength) > 1.0,
                        set.letters() + " needs an index at " + wavelength + " nm");

        // The lines a trial may name are wider than that on purpose: they serve a glass
        // given as index and Abbe number, whose index comes from a dispersion model.
        assertThrows(IllegalArgumentException.class,
                () -> bk7.rindex(WavelengthSet.wavelengthOf("i")));
        Glass modelled = new Glass(1.5168, 64.17, -0.0009);
        assertTrue(modelled.rindex(WavelengthSet.wavelengthOf("i")) > 1.0);
    }

    @Test
    void aSetIsEqualToAnotherBuiltTheSameWay() {
        assertEquals(WavelengthSet.standardFiveColor(), WavelengthSet.standardFiveColor());
        assertEquals(WavelengthSet.standardFiveColor().hashCode(),
                WavelengthSet.standardFiveColor().hashCode());
        assertFalse(WavelengthSet.standardFiveColor().equals(WavelengthSet.k9Tester()));
    }
}
