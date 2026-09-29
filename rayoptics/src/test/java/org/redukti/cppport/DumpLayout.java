// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
// See LICENSE-GPL-3.0.txt
package org.redukti.cppport;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.plotter.GeoMTFByFieldPlot;
import org.redukti.plotter.GeoMTFPlot;
import org.redukti.plotter.RayAberrationPlot;
import org.redukti.plotter.SpotDiagram;
import org.redukti.rayoptics.analysis.MonochromaticGeometricMTF;
import org.redukti.rayoptics.analysis.SpotAnalysis;
import org.redukti.rayoptics.analysis.SpotOptions;
import org.redukti.rayoptics.analysis.TransverseRayAberrationAnalysis;
import org.redukti.rayoptics.analysis.WavefrontAberrationAnalysis;
import org.redukti.rayoptics.layout.Layout2D;
import org.redukti.rayoptics.layout.LayoutOptions;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.spec.Prescription;
import org.redukti.spec.RayOpticsModelBuilder;
import org.redukti.spec.VigType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Java side of the C++ port's {@code tests/LayoutPlotterTest.cpp}: writes one file per
 * {@code EXPECTED_*} array in {@code tests/LayoutPlotterExpected.h}, named after it.
 *
 * <pre>
 * java -cp rayoptics/target/classes;rayoptics/target/test-classes \
 *     org.redukti.cppport.DumpLayout dumps
 * for %f in (dumps\EXPECTED_*.txt) do python ../rayoptics-cpp/tools/regen_expected.py \
 *     ../rayoptics-cpp/tests/LayoutPlotterExpected.h %~nf dumps\%~nxf
 * </pre>
 *
 * <p>The options here must match the C++ test's; a difference in sampling or vignetting
 * shows up as a whole-file diff rather than as a number moving.</p>
 */
public class DumpLayout {
    /** The lens LayoutPlotterTest.cpp uses, read from the C++ checkout. */
    static final String SPEC = "canon-rf70-200mm-f2.8LZ/US20250155694_Example01P.txt";

    static Prescription buildPrescription(String examples) throws Exception {
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(examples + SPEC);
        return Prescription.build_prescription(specs, true, false, false);
    }

    static OpticalModel layoutSystem(Prescription p, int config) {
        return new RayOpticsModelBuilder(p).build_optical_model(
                true, new double[]{0.0, 1.0}, false, VigType.SetPupil, true, config);
    }

    static OpticalModel analysisSystem(Prescription p) {
        return new RayOpticsModelBuilder(p).build_optical_model(
                true, new double[]{0.0, 0.3, 0.5, 0.707, 0.85, 1.0}, false, VigType.SetVig, false, 0);
    }

    /**
     * @param args output directory, then optionally the C++ checkout (default
     *             {@code ../rayoptics-cpp}, relative to the working directory)
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: DumpLayout <output directory> [rayoptics-cpp checkout]");
            System.exit(2);
        }
        String examples = CppPort.examples(args, 1);
        var out = Path.of(args[0]);
        Files.createDirectories(out);
        {
            var p = buildPrescription(examples);
            var opm = layoutSystem(p, 0);
            var lay = new Layout2D();
            var fanOpts = new LayoutOptions().drawReferenceRays(false).fanRayCount(9).clipRays(true).useTraceFan(true);
            Files.writeString(out.resolve("EXPECTED_LAYOUT_FAN.txt"), lay.renderSvg(opm, 1000, 500, fanOpts));
            var elementsOpts = new LayoutOptions().drawReferenceRays(false);
            Files.writeString(out.resolve("EXPECTED_LAYOUTONLY.txt"), lay.renderSvg(opm, 1000, 500, elementsOpts));
            Files.writeString(out.resolve("EXPECTED_LAYOUT.txt"), lay.renderSvg(opm, 1000, 500, new LayoutOptions()));
        }
        {
            var p = buildPrescription(examples);
            var model = analysisSystem(p);
            var options = new SpotOptions().use_hexapolar().num_rings(6);
            var spot = SpotAnalysis.eval(model, options);
            Files.writeString(out.resolve("EXPECTED_SPOT_DIAGRAM.txt"), new SpotDiagram(spot.spot_results.get(0)).plot(null));

            var spotFld = spot.spot_results.get(1);
            var cfg = spotFld.mtfHistogramConfig();
            var mono = new MonochromaticGeometricMTF(spotFld.intercepts.get(0), cfg);
            Files.writeString(out.resolve("EXPECTED_GEO_MTF.txt"), new GeoMTFPlot(spotFld.fld, mono).plot());

            int[] freqs = {10, 20, 40};
            var byField = new GeoMTFByFieldPlot(Arrays.asList(spot.computeMTFs(freqs)), spot.fields());
            Files.writeString(out.resolve("EXPECTED_MTF_BY_FIELD.txt"), byField.plot());
            Files.writeString(out.resolve("EXPECTED_MTF_BY_FIELD_CSV.txt"), byField.toString());
            Files.writeString(out.resolve("EXPECTED_FREQ_LEGEND.txt"), GeoMTFByFieldPlot.freq_legend(freqs));
        }
        {
            var p = buildPrescription(examples);
            var model = analysisSystem(p);
            var traceOptions = new TraceOptions();
            var tra = TransverseRayAberrationAnalysis.eval(model, 11, false, traceOptions);
            Files.writeString(out.resolve("EXPECTED_RAY_ABERRATION.txt"), new RayAberrationPlot(tra).plot(tra.results.get(4), 0));
            var opd = WavefrontAberrationAnalysis.eval(model, 11, false, traceOptions);
            Files.writeString(out.resolve("EXPECTED_OPD_ABERRATION.txt"), new RayAberrationPlot(opd).plot(opd.results.get(7), 0));
        }
        System.out.println("wrote " + out.toAbsolutePath());
    }
}
