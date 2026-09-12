package org.redukti.rayoptics.analysis;

import org.junit.jupiter.api.Test;
import org.redukti.examples.ExampleFinder;
import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.plotter.PupilMapPlot;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.spec.Prescription;
import org.redukti.spec.RayOpticsModelBuilder;
import org.redukti.spec.VigType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The pupil map on an ultra wide angle lens, where the vignetting factors expand the pupil
 * rather than shrink it: the map is what shows that the bundle off axis reaches well
 * outside the nominal pupil, which a sampling pattern confined to the unit circle misses.
 */
class PupilMapAnalysisTest {

    /** 114 degrees; the factors reach 1.42 in x at full field, and 0.65 in -y. */
    private static final String EF14 = "Examples/jfotoptix/canon-ef14mm-f2.8L/JP1993-034592_Example02P.txt";

    private static final double[] FIELDS = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
    /** Coarse enough to be quick; a boundary is then placed to about a 20th of the pupil. */
    private static final int SAMPLES = 41;

    private static OpticalModel model() throws Exception {
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(ExampleFinder.geoPathToExample(EF14));
        Prescription prescription = Prescription.build_prescription(specs, true, false, false);
        return new RayOpticsModelBuilder(prescription)
                .build_optical_model(true, FIELDS, false, VigType.SetPupil, true, 0);
    }

    @Test
    void measuresTheBundleTheLensActuallyPasses() throws Exception {
        var opm = model();
        var result = PupilMapAnalysis.eval(opm, SAMPLES, new int[]{0, 10});
        assertEquals(2, result.maps.size());
        var axial = result.maps.get(0);
        var full = result.maps.get(1);

        // On axis nothing is vignetted: the whole nominal pupil passes, and nothing outside
        // it does.
        assertEquals(1.0, axial.nominal_passed, 1e-9);
        double step = 2 * axial.reach / (SAMPLES - 1);
        assertEquals(1.0, axial.passed_x, step);
        assertEquals(1.0, axial.passed_y, step);

        // At full field the factors expand the pupil in x and shrink it below the axis, and
        // the traced bundle agrees with them: this is the measurement that says the nominal
        // pupil is the wrong bundle on a lens like this.
        assertEquals(1.418, PupilMapAnalysis.scale(full.fld.vlx), 1e-3);
        assertEquals(0.654, PupilMapAnalysis.scale(full.fld.vly), 1e-3);
        assertEquals(PupilMapAnalysis.scale(full.fld.vlx), full.passed_x, step);
        assertTrue(full.nominal_passed < 0.95,
                "part of the nominal pupil is blocked at full field: " + full.nominal_passed);
        assertTrue(full.passed_x > 1.0 + step,
                "the bundle reaches outside the nominal pupil: " + full.passed_x);
    }

    @Test
    void bothMappingsDescribeTheMeasuredRegion() throws Exception {
        var opm = model();
        for (var map : PupilMapAnalysis.eval(opm, SAMPLES, new int[]{0, 6, 10}).maps) {
            for (var quality : new PupilMapAnalysis.MappingQuality[]{
                    map.piecewise_quality, map.ellipse_quality}) {
                // Neither mapping is a fit to the traced boundary, so this is a sanity
                // bound rather than a measure of quality: the region is in the right place
                // and mostly usable.
                assertTrue(quality.sampled() > 0.85,
                        "field " + map.fld.yv() + " samples mostly passable pupil: " + quality);
                assertTrue(quality.covered() > 0.85,
                        "field " + map.fld.yv() + " covers most of the bundle: " + quality);
            }
        }
    }

    @Test
    void expandsToMeasureTheBundleWithClearedFactors() throws Exception {
        var opm = model();
        int n = PupilMapAnalysis.DEFAULT_NUM_SAMPLES;
        var reference = PupilMapAnalysis.eval(opm, n, new int[]{10}).maps.get(0);
        long passed = reference.samples.stream().filter(s -> s.passed()).count();
        long nominalPassed = reference.samples.stream()
                .filter(s -> s.passed() && s.x() * s.x() + s.y() * s.y() <= 1.0).count();
        double expectedCoverage = (double) nominalPassed / passed;

        var fld = opm.optical_spec.fov.fields[10];
        fld.clear_vignetting();
        double initialReach = PupilMapAnalysis.reach_for(fld);
        var map = PupilMapAnalysis.eval(opm, n, new int[]{10}).maps.get(0);

        assertTrue(map.reach > initialReach);
        assertTrue(map.passed_x > 1.3, "transmitted light outside the initial square is measured");
        assertEquals(reference.passed_x, map.passed_x, 2 * map.reach / (n - 1));
        assertEquals(expectedCoverage, map.piecewise_quality.covered(), 0.01);
        assertEquals(expectedCoverage, map.ellipse_quality.covered(), 0.01);
        assertEquals(n * n, map.samples.size());
        for (int k = 0; k < n; k++) {
            assertFalse(map.sample(0, k).passed());
            assertFalse(map.sample(n - 1, k).passed());
            assertFalse(map.sample(k, 0).passed());
            assertFalse(map.sample(k, n - 1).passed());
        }
        assertEquals(0.0, fld.vlx);
        assertEquals(0.0, fld.vux);
        assertEquals(0.0, fld.vly);
        assertEquals(0.0, fld.vuy);
    }

    @Test
    void plotsTheMap() throws Exception {
        var opm = model();
        var map = PupilMapAnalysis.eval(opm, SAMPLES, new int[]{10}).maps.get(0);
        String svg = new PupilMapPlot(map).plot(320);
        assertTrue(svg.startsWith("<?xml"), svg.substring(0, Math.min(40, svg.length())));
        assertTrue(svg.contains("<svg"));
        // The passed region, the two candidate outlines and the labels all made it in.
        assertTrue(svg.contains("polygon"), "the measured region is drawn");
        assertTrue(svg.contains("field 1.00"), "the field is labelled");
        assertTrue(svg.contains("piecewise"), "the mapping quality is reported");
    }
}
