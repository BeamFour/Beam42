package org.redukti.optim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The goals that come in sagittal/tangential pairs all validate their orientation
 * against {@link org.redukti.rayoptics.util.Orientation}. Three of these used to accept
 * any int and read the sagittal fan for it, so a typo produced a plausible but wrong
 * residual rather than an error.
 */
class DirectionalGoalTest {

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
