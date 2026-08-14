package org.redukti.optim;

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
     * The ray-coordinate aliases and the MTF orientations are read by different goals
     * against the same analysis arrays - {@link GoalSpotDeviation} indexes the x
     * intercepts with {@link Orientation#X} while {@link GoalContrast} takes the
     * sagittal residual for {@link Orientation#SAGITTAL}. They have to stay equal or
     * the two goal families would disagree about which meridian index 0 is.
     */
    @Test
    void rayCoordinateAliasesMatchTheMeridiansTheyName() {
        assertEquals(Orientation.SAGITTAL, Orientation.X);
        assertEquals(Orientation.TANGENTIAL, Orientation.Y);
    }

    @Test
    void namesTheMeridianForGoalDescriptions() {
        assertEquals("sag", Orientation.name(Orientation.SAGITTAL));
        assertEquals("tan", Orientation.name(Orientation.TANGENTIAL));
    }

    /**
     * Every directional goal validates. Three of these used to accept any int and read
     * the sagittal fan for it, so a typo produced a plausible but wrong residual rather
     * than an error.
     */
    @Test
    void directionalGoalsRejectAnOrientationThatIsNeitherMeridian() {
        assertThrows(IllegalArgumentException.class,
                () -> new GoalGeoMTF(null, 1, 2, 10, 0.5, 1.0));
        assertThrows(IllegalArgumentException.class,
                () -> new GoalRayAberration(null, 1, 2, 0, 0.5876, 0.0, 1.0));
        assertThrows(IllegalArgumentException.class,
                () -> new GoalMTFProxy(null, 1, 2, 0, 0.5876, 10, 0.0, 1.0));
        assertThrows(IllegalArgumentException.class,
                () -> new GoalContrast(null, 0, 10, 1, 0, 0, 2, 1.0));
        assertThrows(IllegalArgumentException.class,
                () -> new GoalSpotDeviation(null, 1, 0, 0, 2, 1.0));
    }
}
