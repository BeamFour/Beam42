package org.redukti.rayoptics.specs;

import org.junit.jupiter.api.Test;
import org.redukti.util.Args;
import org.redukti.spec.VigType;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VignettingMappingTest {

    @Test
    void defaultMappingRetainsPiecewiseUpstreamBehaviour() {
        Field field = asymmetricField();

        assertArrayEquals(field.apply_vignetting(new double[]{0.4, -0.5}),
                field.apply_vignetting(
                        new double[]{0.4, -0.5}, VignettingMapping.Piecewise), 0.0);
        assertArrayEquals(new double[]{0.28, -0.5},
                field.apply_vignetting(new double[]{0.4, -0.5}), 1.0e-15);
    }

    @Test
    void affineEllipseMatchesAllFourMeasuredExtremaAndMovesItsCentre() {
        Field field = asymmetricField();

        assertArrayEquals(new double[]{0.7, -0.2}, field.apply_vignetting(
                new double[]{1.0, 0.0}, VignettingMapping.AffineEllipse), 1.0e-15);
        assertArrayEquals(new double[]{-0.5, -0.2}, field.apply_vignetting(
                new double[]{-1.0, 0.0}, VignettingMapping.AffineEllipse), 1.0e-15);
        assertArrayEquals(new double[]{0.1, 0.6}, field.apply_vignetting(
                new double[]{0.0, 1.0}, VignettingMapping.AffineEllipse), 1.0e-15);
        assertArrayEquals(new double[]{0.1, -1.0}, field.apply_vignetting(
                new double[]{0.0, -1.0}, VignettingMapping.AffineEllipse), 1.0e-15);
        assertArrayEquals(new double[]{0.1, -0.2}, field.apply_vignetting(
                new double[]{0.0, 0.0}, VignettingMapping.AffineEllipse), 1.0e-15);

        assertEquals(0.6, Field.affine_vignetting_scale(field.vlx, field.vux), 1.0e-15);
        assertEquals(0.8, Field.affine_vignetting_scale(field.vly, field.vuy), 1.0e-15);
    }

    @Test
    void lensToolArgumentSelectsAffineEllipse() {
        Args arguments = Args.parseArguments(new String[] {
                "--vignetting-mapping", "affine-ellipse"
        });

        assertEquals(VignettingMapping.AffineEllipse, arguments.vignetting_mapping);
        assertEquals(VignettingMapping.Piecewise,
                Args.parseArguments(new String[0]).vignetting_mapping);
    }

    @Test
    void lensToolCanDisableRemappingWithoutDisablingFactorCalculation() {
        Args arguments = Args.parseArguments(new String[] {
                "--no-vignetting-remap", "--spot-grid-size", "128"
        });

        assertEquals(false, arguments.apply_vignetting);
        assertEquals(VigType.SetPupil, arguments.vig_type);
        assertEquals(128, arguments.spot_grid_size);
    }

    private static Field asymmetricField() {
        Field field = new Field(null);
        field.vux = 0.3; // +x extent 0.7
        field.vlx = 0.5; // -x extent -0.5
        field.vuy = 0.4; // +y extent 0.6
        field.vly = 0.0; // -y extent -1.0
        return field;
    }
}
