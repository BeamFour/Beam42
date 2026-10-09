package org.redukti.tools;

import org.junit.jupiter.api.Test;
import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.spec.Prescription;

import static org.junit.jupiter.api.Assertions.*;

/** Which airspaces the routine optimization picks out of a prescription. */
class DefaultOptimizationsTest {

    /**
     * A two configuration zoom shaped like the patents that model the sensor stack:
     * two airspaces that move with the zoom, then a cover glass, then the fixed gap
     * between cover glass and image. 'Bf' is a multi valued row holding one value, as
     * these files write it.
     */
    private static final String ZOOM_WITH_COVER_GLASS = """
            [descriptive data]
            title	Test zoom
            [variable distances]
            Focal Length	50	60
            F-Number	4	5
            Angle of View	40	35
            d2	10	6
            d4	20	26
            Bf	1	1
            [lens data]
            1	50	4	1.5	20	60
            2	-50	d2		20
            3	80	3	1.6	20	50
            4	-80	d4		20
            5	CG	1.6	1.5168	44	64	BK7
            6	CG	Bf		44
            [report data]
            lens name	Test zoom
            scenarios	0	1
            names	Wide	Long
            """;

    private static Prescription prescription(String text) throws Exception {
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_buffer(text);
        return LensTool2.createPrescription(specs, true, false);
    }

    @Test
    void zoomVariesTheAirspacesThatMoveWithTheZoomAndNotTheFixedSensorGap() throws Exception {
        var prescription = prescription(ZOOM_WITH_COVER_GLASS);
        assertEquals(2, prescription.get_num_configurations());
        // The airspace in front of the cover glass is the back focus by position, but on
        // this zoom it is also one of the airspaces that moves, so it has to be varied.
        assertEquals(3, DefaultOptimizations.findBackFocusSurface(prescription));
        assertArrayEquals(new int[]{1, 3}, DefaultOptimizations.findVariableThicknesses(prescription));
    }

    @Test
    void anAirspaceHoldingOneValueInEveryConfigurationIsNotAZoomAirspace() throws Exception {
        // Bf is referenced from a multi valued row, so it carries a thickness per
        // configuration; holding the same value in both is what keeps it out.
        var prescription = prescription(ZOOM_WITH_COVER_GLASS);
        var surfaces = prescription.get_surfaces();
        assertArrayEquals(new double[]{1.0, 1.0}, surfaces[5]._thickness_by_scenario);
        for (int surface : DefaultOptimizations.findVariableThicknesses(prescription))
            assertNotEquals(5, surface, "the fixed cover glass to image gap must not be varied");
    }

    @Test
    void aZoomWithoutACoverGlassVariesItsRearAirspace() throws Exception {
        // With no cover glass the rear airspace is both the back focus and the airspace
        // that moves with the zoom; leaving it out would drop the strongest variable.
        var prescription = prescription(ZOOM_WITH_COVER_GLASS
                .replace("""
                        5	CG	1.6	1.5168	44	64	BK7
                        6	CG	Bf		44
                        """, "")
                .replace("Bf\t1\t1\n", ""));
        assertEquals(4, prescription.get_surfaces().length);
        assertEquals(3, DefaultOptimizations.findBackFocusSurface(prescription));
        assertArrayEquals(new int[]{1, 3}, DefaultOptimizations.findVariableThicknesses(prescription));
    }

    /**
     * A three scenario zoom whose optical rows are all filled in, so that only the
     * thickness rows decide anything. 'Bf' holds the same value at the outer two
     * scenarios and a placeholder at the middle one, which is how these files write a
     * scenario the patent never tabulated.
     */
    private static final String ZOOM_WITH_THIRD_SCENARIO = """
            [descriptive data]
            title	Test zoom
            [variable distances]
            Focal Length	50	55	60
            F-Number	4	4.5	5
            Angle of View	40	37	35
            d2	10	8	6
            d4	20	23	26
            Bf	1	MIDDLE	1
            [lens data]
            1	50	4	1.5	20	60
            2	-50	d2		20
            3	80	3	1.6	20	50
            4	-80	d4		20
            5	CG	1.6	1.5168	44	64	BK7
            6	CG	Bf		44
            [report data]
            lens name	Test zoom
            scenarios	SELECTED
            names	Wide	Long
            """;

    private static String zoomSelecting(String middleValue, String selected) {
        return ZOOM_WITH_THIRD_SCENARIO
                .replace("MIDDLE", middleValue)
                .replace("SELECTED", selected);
    }

    @Test
    void onlyTheScenariosTheReportSelectsDecideWhetherAnAirspaceVaries() throws Exception {
        // The middle scenario is not one of the two being reported, so whatever stands in
        // it has no say. Bf reads as one value across the selected pair either way.
        for (String middle : new String[]{"undefined", "", "17"}) {
            var prescription = prescription(zoomSelecting(middle, "0\t2"));
            assertEquals(2, prescription.get_num_configurations());
            var surfaces = prescription.get_surfaces();
            assertArrayEquals(new double[]{10.0, 6.0}, surfaces[1]._thickness_by_scenario);
            assertArrayEquals(new double[]{20.0, 26.0}, surfaces[3]._thickness_by_scenario);
            assertArrayEquals(new double[]{1.0, 1.0}, surfaces[5]._thickness_by_scenario,
                    "middle scenario '" + middle + "' leaked into the selected pair");
            assertArrayEquals(new int[]{1, 3}, DefaultOptimizations.findVariableThicknesses(prescription));
        }
    }

    @Test
    void aMissingValueInASelectedScenarioIsRejectedBeforeAnyAirspaceIsPicked() throws Exception {
        // Selecting the scenario the patent left blank is an error in the input, caught
        // while the prescription is built - so a missing value can never reach the
        // selection above as the zero that a lenient parse would produce, where it would
        // read as a thickness that varies.
        for (String middle : new String[]{"undefined", "", "nonsense", "NaN"}) {
            var error = assertThrows(IllegalArgumentException.class,
                    () -> prescription(zoomSelecting(middle, "0\t1")),
                    "middle scenario '" + middle + "' was accepted");
            assertTrue(error.getMessage().contains("'Bf'"), error.getMessage());
            assertTrue(error.getMessage().contains("scenario 1"), error.getMessage());
        }
    }

    @Test
    void aPrescriptionWithOneConfigurationHasNoZoomAirspaces() throws Exception {
        var prescription = prescription(ZOOM_WITH_COVER_GLASS
                .replace("scenarios\t0\t1\nnames\tWide\tLong", "scenarios\t0\nnames\tWide"));
        assertEquals(1, prescription.get_num_configurations());
        assertEquals(0, DefaultOptimizations.findVariableThicknesses(prescription).length);
        // A prime is optimized on its back focus instead, which is still located.
        assertEquals(3, DefaultOptimizations.findBackFocusSurface(prescription));
    }
}
