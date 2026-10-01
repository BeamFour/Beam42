package org.redukti.rayoptics.analysis;

import org.junit.jupiter.api.Test;
import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.raytr.GridItem;
import org.redukti.rayoptics.raytr.ReferenceSphere;
import org.redukti.rayoptics.raytr.TraceGridByWvl;
import org.redukti.rayoptics.specs.Field;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeightedSpotAnalysisTest {

    @Test
    void reportsAndPlotsPhysicalSpotSizesInMicrometresForEverySupportedUnit() {
        String[] units = {"m", "cm", "mm", "in", "ft", "MM"};
        double[] factors = {1e6, 1e4, 1e3, 25400.0, 304800.0, 1e3};
        String referencePlot = null;
        for (int i = 0; i < units.length; i++) {
            var model = new org.redukti.rayoptics.optical.OpticalModel();
            model.system_spec.dimensions = units[i];
            var field = model.optical_spec.fov.fields[0];
            field.ref_sphere = new ReferenceSphere(Vector3.ZERO, Vector3.ZERO, 1.0, null);
            var trace = new TraceGridByWvl(550.0, List.of(
                    new GridItem(new Vector2(3.0 / factors[i], 4.0 / factors[i]), null)));
            var result = new SpotAnalysisResult.SpotResultsForField(field, List.of(trace), 550.0, false);
            assertEquals(5.0, result.get_mean_radius(), 1e-12, units[i]);
            assertEquals(5.0, result.get_max_radius(), 1e-12, units[i]);
            assertEquals(5.0 / factors[i], result.max_radius, 1e-15, units[i]);

            var analysis = new org.redukti.optim.Analysis(null, new double[]{0.0}, new int[0]);
            analysis._spots = new SpotAnalysisResult.SpotResultsForField[]{result};
            var x = new org.redukti.optim.GoalSpotDeviation(analysis, 1, 0, 0,
                    org.redukti.rayoptics.util.Orientation.X, 1.0);
            var y = new org.redukti.optim.GoalSpotDeviation(analysis, 1, 0, 0,
                    org.redukti.rayoptics.util.Orientation.Y, 1.0);
            assertEquals(5.0, Math.hypot(x.value(), y.value()), 1e-12, units[i]);
            String plot = new org.redukti.plotter.SpotDiagram(result).plot(null);
            if (referencePlot == null) referencePlot = plot;
            else assertEquals(referencePlot, plot, units[i]);
            assertTrue(org.redukti.tools.LensTool2.spotResultsMarkdownTable(
                    new SpotAnalysisResult(false), new StringBuilder()).toString().contains("(µm)"));

            // Existing results retain the units of their stored intercepts.
            model.system_spec.dimensions = "ft";
            assertEquals(5.0, result.get_mean_radius(), 1e-12, units[i]);
        }
    }

    @Test
    void rejectsUnsupportedModelUnits() {
        var model = new org.redukti.rayoptics.optical.OpticalModel();
        model.system_spec.dimensions = "unknown";
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SpotAnalysisResult.SpotResultsForField(
                        model.optical_spec.fov.fields[0], List.of(), 550.0, false));
    }

    @Test
    void resultsRetainMetadataAndInterceptsAfterInputsChange() {
        var field = new Field(null);
        field.y = 0.7;
        field.ref_sphere = new ReferenceSphere(Vector3.ZERO, Vector3.ZERO, 1.0, null);
        var items = new java.util.ArrayList<GridItem>();
        items.add(new GridItem(new Vector2(2.0, 4.0), null));
        var trace = new TraceGridByWvl(550.0, items);
        var spot = new SpotAnalysisResult.SpotResultsForField(field, List.of(trace), 550.0, false);
        var contrast = new ContrastAnalysisResult.FieldResult(field, List.of());
        var fan = new org.redukti.rayoptics.raytr.TraceFanResult(field, 0, 0, List.of(), 0, 0);
        String label = field.toString();
        var mono = new MonochromaticGeometricMTF(spot.intercepts.get(0));
        var plot = new org.redukti.plotter.GeoMTFPlot(field, mono);
        field.y = 9.0;
        items.clear();
        assertEquals(0.7, spot.fld.y);
        assertEquals(0.7, contrast.field().y);
        assertEquals(0.7, fan.fld.y);
        assertEquals(label, plot.fld.toString());
        assertEquals(2.0, spot.intercepts.get(0).compute_centroid().x);
        spot.intercepts.get(0).adjust_to_centroid(new Vector2(2.0, 4.0));
        assertEquals(0.0, spot.intercepts.get(0).compute_centroid().x);
    }

    @Test
    void computesWeightedCentroidAndRmsRadius() {
        var items = List.of(
                new GridItem(new Vector2(0.0, 0.0), null).withWeight(0.75),
                new GridItem(new Vector2(2.0, 0.0), null).withWeight(0.25));
        var trace = new TraceGridByWvl(550.0, items);
        var intercepts = new SpotIntercepts(trace);

        assertEquals(0.5, intercepts.compute_centroid().x, 1.0e-15);

        var field = new Field(null);
        field.ref_sphere = new ReferenceSphere(Vector3.ZERO, Vector3.ZERO, 1.0, null);
        var result = new SpotAnalysisResult.SpotResultsForField(
                field, List.of(trace), 550.0, true);

        assertEquals(Math.sqrt(0.75) * 1000.0, result.get_mean_radius(), 1.0e-12);
    }

    @Test
    void selectingGridAndQuadraturePatternsIsUnambiguous() {
        var options = new SpotOptions(true);
        assertTrue(options.is_gauss_quadrature());
        assertFalse(options.is_grid());
        assertFalse(options.is_hexapolar());

        options.use_grid();
        assertTrue(options.is_grid());
        assertFalse(options.is_gauss_quadrature());
        assertFalse(options.is_hexapolar());
    }
}
