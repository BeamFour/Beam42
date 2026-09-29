// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
// See LICENSE-GPL-3.0.txt
package org.redukti.cppport;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.spec.Prescription;
import org.redukti.spec.RayOpticsModelBuilder;
import org.redukti.spec.VigType;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Java side of the C++ port's {@code tests/SpecTest.cpp}: writes the lines that
 * {@code tests/SpecExpected.h} asserts.
 *
 * <p>This module is the port's ground truth, so a C++ expectation is regenerated from
 * here rather than edited by hand. Run it, then feed the output to the port's
 * {@code tools/regen_expected.py}:</p>
 *
 * <pre>
 * java -cp rayoptics/target/classes;rayoptics/target/test-classes \
 *     org.redukti.cppport.DumpSpec spec-dump.txt
 * python ../rayoptics-cpp/tools/regen_expected.py \
 *     ../rayoptics-cpp/tests/SpecExpected.h EXPECTED_SPEC_LINES spec-dump.txt
 * </pre>
 *
 * <p>The prescriptions are read from the C++ checkout, since they are the copies that
 * test asserts against. {@code FILES} must match the list in {@code SpecTest.cpp}.</p>
 */
public class DumpSpec {
    /** Relative to the C++ checkout's Examples directory; keep in step with SpecTest.cpp. */
    static final String[] FILES = {
            "canon-ef11-24mm-f4L/US20150146085_Example01P.txt",
            "canon-ef35mm-f1.4L/JP1999-211978_Example01P.txt",
            "angenieux-180mm-f2.3/US004726669_Example01P.txt",
    };

    static void report(StringBuilder sb, String examples, String path) throws Exception {
        sb.append("--- FILE ").append(path).append(" ---\n");
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(examples + path);
        var surfaces = specs.get_surfaces();
        sb.append("surfaces=").append(surfaces.size()).append("\n");
        for (int i = 0; i < surfaces.size(); i++) {
            var s = surfaces.get(i);
            sb.append("  surf ").append(i)
                    .append(" type=").append(s.get_surface_type().name())
                    .append(" r=").append(s.get_radius())
                    .append(" t0=").append(s.get_thickness(0))
                    .append(" dia0=").append(s.get_diameter(0))
                    .append(" nd=").append(s.get_refractive_index())
                    .append(" vd=").append(s.get_abbe_vd())
                    .append(" glass=").append(s.get_glass_name())
                    .append(" cat=").append(s.get_catalog_name())
                    .append(" asph=").append(s.get_aspherical_data() != null).append("\n");
        }
        sb.append("image_height=").append(specs.get_image_height()).append("\n");
        sb.append("focal_length=").append(specs.get_focal_length()).append("\n");
        sb.append("aov0=").append(specs.get_angle_of_view_in_degrees(0)).append("\n");
        sb.append("fno0=").append(specs.get_f_number(0)).append("\n");
        sb.append("half_aov_rad0=").append(specs.get_half_angle_of_view_in_radians(0)).append("\n");

        var p = Prescription.build_prescription(specs, true, false, false);
        sb.append("num_configurations=").append(p.get_num_configurations()).append("\n");
        sb.append("title=").append(p.get_title()).append("\n");
        sb.append("--- OPT BENCH ---\n");
        p.to_opt_bench_str(sb);
        sb.append("--- MARKDOWN ---\n");
        p.to_markdown_str(sb);

        for (var vt : new VigType[]{VigType.None, VigType.Paraxial, VigType.SetVig}) {
            var model = new RayOpticsModelBuilder(p).build_optical_model(
                    true, new double[]{0.0, 0.707, 1.0}, false, vt, false, 0);
            var fod = model.optical_spec.parax_data.fod;
            sb.append("--- MODEL ").append(vt.name()).append(" ---\n");
            sb.append("efl=").append(fod.efl).append("\n");
            sb.append("bfl=").append(fod.bfl).append("\n");
            sb.append("ffl=").append(fod.ffl).append("\n");
            sb.append("fno=").append(fod.fno).append("\n");
            sb.append("img_ht=").append(fod.img_ht).append("\n");
            sb.append("enp_dist=").append(fod.enp_dist).append("\n");
            sb.append("enp_radius=").append(fod.enp_radius).append("\n");
            sb.append("exp_dist=").append(fod.exp_dist).append("\n");
            sb.append("exp_radius=").append(fod.exp_radius).append("\n");
            sb.append("opt_inv=").append(fod.opt_inv).append("\n");
            sb.append("obj_ang=").append(fod.obj_ang).append("\n");
            sb.append("num_ifcs=").append(model.seq_model.ifcs.size()).append("\n");
            var fields = model.optical_spec.fov.fields;
            for (int fi = 0; fi < fields.length; fi++) {
                var f = fields[fi];
                sb.append("  fld ").append(fi).append(" y=").append(f.y)
                        .append(" vlx=").append(f.vlx).append(" vly=").append(f.vly)
                        .append(" vux=").append(f.vux).append(" vuy=").append(f.vuy).append("\n");
            }
        }
        var model = new RayOpticsModelBuilder(p).build_optical_model(
                true, new double[]{0.0, 0.707, 1.0}, true, VigType.SetVig, false, 0);
        int changed = p.update_apertures_from(model, 0);
        sb.append("apertures_changed=").append(changed).append("\n");
        for (var s : p.get_surfaces())
            sb.append("  dia ").append(s._id).append("=").append(s.get_diameter()).append("\n");
    }

    /**
     * @param args output file, then optionally the C++ checkout (default
     *             {@code ../rayoptics-cpp}, relative to the working directory)
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: DumpSpec <output file> [rayoptics-cpp checkout]");
            System.exit(2);
        }
        String examples = CppPort.examples(args, 1);
        var sb = new StringBuilder();
        for (var f : FILES)
            report(sb, examples, f);
        Files.writeString(Path.of(args[0]), sb.toString());
        System.out.println("wrote " + args[0]);
    }
}
