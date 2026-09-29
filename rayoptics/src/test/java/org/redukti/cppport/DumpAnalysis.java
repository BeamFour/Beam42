// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
// See LICENSE-GPL-3.0.txt
package org.redukti.cppport;

import org.redukti.rayoptics.analysis.ContrastAnalysis;
import org.redukti.rayoptics.analysis.ContrastOptions;
import org.redukti.rayoptics.analysis.SpotAnalysis;
import org.redukti.rayoptics.analysis.SpotAnalysisResult;
import org.redukti.rayoptics.analysis.SpotOptions;
import org.redukti.rayoptics.analysis.TransverseRayAberrationAnalysis;
import org.redukti.rayoptics.analysis.WavefrontAberrationAnalysis;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.rayoptics.raytr.VigCalc;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.seq.SurfaceData;
import org.redukti.rayoptics.specs.FieldSpec;
import org.redukti.rayoptics.specs.ImageKey;
import org.redukti.rayoptics.specs.OpticalSpecs;
import org.redukti.rayoptics.specs.PupilSpec;
import org.redukti.rayoptics.specs.ValueKey;
import org.redukti.rayoptics.specs.WvlSpec;
import org.redukti.rayoptics.specs.WvlWt;
import org.redukti.rayoptics.util.Pair;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Java side of the C++ port's {@code tests/AnalysisTest.cpp}: writes one file per
 * {@code EXPECTED_*} block in {@code tests/AnalysisExpected.h}, named after it.
 *
 * <pre>
 * java -cp rayoptics/target/classes;rayoptics/target/test-classes;mathlib/target/classes \
 *     org.redukti.cppport.DumpAnalysis dumps
 * python ../rayoptics-cpp/tools/regen_raw.py ../rayoptics-cpp/tests/AnalysisExpected.h \
 *     EXPECTED_SPOT dumps/EXPECTED_SPOT.txt
 * </pre>
 *
 * <p>Those blocks are raw string literals rather than line arrays, so they are updated
 * with {@code regen_raw.py} rather than {@code regen_expected.py}.</p>
 *
 * <p>The model is the Leica Summicron R 50mm f/2 of
 * {@code org.redukti.rayoptics.integration.Leica50mmSummironRTest}, built here rather
 * than shared because that test keeps it inside its test method. The rendering below
 * must stay character for character with the C++ test, which is what is compared.</p>
 */
public class DumpAnalysis {

    static OpticalModel buildSummicron() {
        OpticalModel opm = new OpticalModel();
        SequentialModel sm = opm.seq_model;
        OpticalSpecs osp = opm.optical_spec;
        osp.pupil = new PupilSpec(osp, new Pair<>(ImageKey.Image, ValueKey.Fnum), 2.0);
        osp.fov = new FieldSpec(osp, new Pair<>(ImageKey.Object, ValueKey.Angle), 22.5,
                new double[]{0., 1.}, true, true);
        osp.wvls = new WvlSpec(new WvlWt[]{new WvlWt(587.5618, 1.0)}, 0);
        opm.system_spec.title = "Leica Summicron R 50mm f/2)";
        opm.system_spec.dimensions = "mm";
        opm.radius_mode = true;
        sm.gaps.get(0).thi = 1e10;
        sm.add_surface(new SurfaceData(42.71, 3.99).rindex(1.73430, 28.19).max_aperture(14.47));
        sm.add_surface(new SurfaceData(195.38, 0.2).max_aperture(13.53));
        sm.add_surface(new SurfaceData(20.5, 7.18).rindex(1.67133, 41.64).max_aperture(12.01));
        sm.add_surface(new SurfaceData(0.0, 1.29).rindex(1.79190, 25.55).max_aperture(10.745));
        sm.add_surface(new SurfaceData(14.94, 5.35).max_aperture(9.195));
        sm.add_surface(new SurfaceData(0.0, 7.61).max_aperture(9.0295));
        sm.set_stop();
        sm.add_surface(new SurfaceData(-14.94, 1.0).rindex(1.65222, 33.60).max_aperture(8.75));
        sm.add_surface(new SurfaceData(0.0, 5.22).rindex(1.79227, 47.15).max_aperture(9.635));
        sm.add_surface(new SurfaceData(-20.5, 0.2).max_aperture(10.19));
        sm.add_surface(new SurfaceData(0.0, 3.69).rindex(1.79227, 47.15).max_aperture(11.48));
        sm.add_surface(new SurfaceData(-42.71, 37.32).max_aperture(11.985));
        sm.do_apertures = false;
        opm.update_model();
        VigCalc.set_vig(opm);
        opm.update_model();
        return opm;
    }

    static SpotAnalysisResult spotResult(OpticalModel opm) {
        return SpotAnalysis.eval(opm, new SpotOptions().use_hexapolar().num_rings(8));
    }

    static String spotBlock(OpticalModel opm) {
        var spot = spotResult(opm);
        var sb = new StringBuilder(spot.toString());
        var flds = spot.fields();
        for (int i = 0; i < flds.length; i++)
            sb.append("field[").append(i).append("]=").append(flds[i]).append("\n");
        for (int i = 0; i < spot.spot_results.size(); i++) {
            var r = spot.spot_results.get(i);
            sb.append("spot[").append(i).append("] max=").append(r.max_radius)
                    .append(" mean=").append(r.mean_radius)
                    .append(" ngrids=").append(r.trace_results.size()).append("\n");
            for (var ic : r.intercepts) {
                sb.append("  wvl=").append(ic.wvl).append(" n=").append(ic.x.length).append("\n");
                for (int j = 0; j < ic.x.length; j += 17)
                    sb.append("   [").append(j).append("] ").append(ic.x[j]).append(" ")
                            .append(ic.y[j]).append(" w=").append(ic.weights[j])
                            .append(" v=").append(ic.valid[j]).append("\n");
                var c = ic.compute_centroid();
                sb.append("  centroid=").append(c.x).append(",").append(c.y).append("\n");
            }
            var cfg = r.mtfHistogramConfig();
            sb.append("  cfg bins=").append(cfg.num_bins).append(" px=").append(cfg.pixel_size)
                    .append("\n");
        }
        return sb.toString();
    }

    static String mtfBlock(OpticalModel opm) {
        var spot = spotResult(opm);
        var sb = new StringBuilder();
        for (var m : spot.computeMTFs(new int[]{5, 10, 20, 40})) {
            sb.append("freq=").append(m.freq).append("\n");
            for (int i = 0; i < m.sag_mtf_by_field.length; i++)
                sb.append("  sag[").append(i).append("]=").append(m.sag_mtf_by_field[i])
                        .append(" tan[").append(i).append("]=").append(m.tan_mtf_by_field[i])
                        .append("\n");
        }
        return sb.toString();
    }

    static String contrastBlock(OpticalModel opm) {
        var options = new ContrastOptions(30.0).num_rings(2).num_spokes(4).center_residuals(true);
        var ca = ContrastAnalysis.eval(opm, options);
        var sb = new StringBuilder("spatialFrequency=").append(ca.spatialFrequency).append("\n");
        for (int fi = 0; fi < ca.fields.size(); fi++) {
            var f = ca.fields.get(fi);
            sb.append("field ").append(fi).append(" ").append(f.field()).append("\n");
            for (var w : f.wavelengths()) {
                sb.append("  wvl=").append(w.wavelength())
                        .append(" shift=").append(w.normalizedPupilShift())
                        .append(" sagOff=").append(w.sagittalOffset())
                        .append(" tanOff=").append(w.tangentialOffset())
                        .append(" n=").append(w.samples().size()).append("\n");
                for (int si = 0; si < w.samples().size(); si++) {
                    var s = w.samples().get(si);
                    sb.append("   [").append(si).append("] pupil=").append(s.pupil().x)
                            .append(",").append(s.pupil().y)
                            .append(" sag=").append(s.sagittalDifference())
                            .append(" tan=").append(s.tangentialDifference())
                            .append(" w=").append(s.weight())
                            .append(" valid=").append(s.valid())
                            .append(" fail=")
                            .append(s.failure() == null ? "null"
                                    : s.failure().ray() + "/" + s.failure().exceptionType()
                                            + "/" + s.failure().surface())
                            .append(" sres=").append(w.sagittalResidual(si))
                            .append(" tres=").append(w.tangentialResidual(si)).append("\n");
                }
            }
        }
        return sb.toString();
    }

    /** @param args output directory for the EXPECTED_*.txt files */
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: DumpAnalysis <output directory>");
            System.exit(2);
        }
        var out = Path.of(args[0]);
        Files.createDirectories(out);

        Files.writeString(out.resolve("EXPECTED_SPOT.txt"), spotBlock(buildSummicron()));
        Files.writeString(out.resolve("EXPECTED_MTF.txt"), mtfBlock(buildSummicron()));

        var traceOptions = new TraceOptions();
        var tra = TransverseRayAberrationAnalysis.eval(buildSummicron(), 11, false, traceOptions);
        Files.writeString(out.resolve("EXPECTED_TRA.txt"), tra.list_ray_fans());
        var opd = WavefrontAberrationAnalysis.eval(buildSummicron(), 11, false, traceOptions);
        Files.writeString(out.resolve("EXPECTED_OPD.txt"), opd.list_ray_fans());

        Files.writeString(out.resolve("EXPECTED_CONTRAST.txt"), contrastBlock(buildSummicron()));
        System.out.println("wrote " + out.toAbsolutePath());
    }
}
