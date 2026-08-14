package org.redukti.rayoptics.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrientationTest {

    @Test
    void checkedReturnsTheTwoMeridiansAndRejectsEverythingElse() {
        assertEquals(Orientation.SAGITTAL, Orientation.checked(Orientation.SAGITTAL));
        assertEquals(Orientation.TANGENTIAL, Orientation.checked(Orientation.TANGENTIAL));

        IllegalArgumentException tooLarge = assertThrows(IllegalArgumentException.class,
                () -> Orientation.checked(2));
        assertTrue(tooLarge.getMessage().contains("SAGITTAL"), tooLarge.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Orientation.checked(-1));
    }

    /**
     * The ray-coordinate aliases and the MTF orientations index the same analysis arrays
     * from different callers - a spot deviation goal takes the x intercepts for
     * {@link Orientation#X} while a contrast goal takes the sagittal residual for
     * {@link Orientation#SAGITTAL}. They have to stay equal or the two would disagree
     * about which meridian index 0 is.
     */
    @Test
    void rayCoordinateAliasesMatchTheMeridiansTheyName() {
        assertEquals(Orientation.SAGITTAL, Orientation.X);
        assertEquals(Orientation.TANGENTIAL, Orientation.Y);
    }

    /** {@code for (xy = 0; xy < COUNT; xy++)} must cover both meridians and nothing else. */
    @Test
    void countBoundsALoopOverBothMeridians() {
        assertEquals(2, Orientation.COUNT);
        assertTrue(Orientation.SAGITTAL < Orientation.COUNT);
        assertTrue(Orientation.TANGENTIAL < Orientation.COUNT);
    }

    @Test
    void namesTheMeridianForLabelsAndDescriptions() {
        assertEquals("sag", Orientation.name(Orientation.SAGITTAL));
        assertEquals("tan", Orientation.name(Orientation.TANGENTIAL));
    }
}
