package org.redukti.exporters;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.spec.Prescription;
import org.redukti.spec.SurfaceType;
import org.redukti.util.Args;
import org.redukti.util.Helper;

public class ZemaxExporter {


    public static void main(String[] args) throws Exception {
        Args arguments = Args.parseArguments(args);
        if (arguments.specfile == null) {
            System.err.println("Usage: --specfile inputfile [--scenario num]");
            System.exit(1);
        }
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(arguments.specfile);
        Prescription prescription = Prescription.build_prescription(specs,true);
        String output = new ZemaxExporter().generate(prescription,arguments.only_d_line);
        Helper.createOutputFile(Helper.getOutputPathChangeExt(arguments.specfile, ".zmx"), output);
    }

    public String generate(Prescription prescription, boolean d_line_only) {
        StringBuilder sb = new StringBuilder();
        outputHeading(prescription, d_line_only, sb);
        output_object(prescription, sb);
        output_surfaces(prescription, sb);
        output_image_plane(prescription, sb);
        output_configurations(prescription, sb);
        return sb.toString();
    }

    private void outputHeading(Prescription prescription, boolean d_line_only, StringBuilder sb) {
        sb.append("VERS 161019 507 33785\n");
        sb.append("MODE SEQ\n");
        sb.append("NAME ").append(prescription.get_title()).append("\n");
        sb.append("PFIL 0 0 0\n").append("LANG 0\n").append("UNIT MM X W X CM MR CPMM\n");
        if (prescription.get_num_configurations() == 0)
            sb.append("FNUM ").append(prescription.get_f_number()).append("\n");
        else
            sb.append("FLOA\n");
        var img_ht = prescription._diameter_image_circle/2.0;
        sb.append(String.format("""
                ENVD 20 1 0
                GFAC 0 0
                GCAT SCHOTT HOYA OHARA NIKON-HIKARI NIKON HIKARI SUMITA CDGM LACROIX
                RAIM 0 2 1 1 0 1 0 0 0
                SDMA 0 1 0
                FTYP 3 0 9 3 0 0 0 9
                ROPD 2
                HYPR 0
                PICB 1
                XFLN 0 0 0 0 0 0 0 0 0 0 0 0
                YFLN 0 2.16 4.33 6.5 8.65 10.8 12.98 15.14 %f 0 0 0 0 0 0 0 0
                FWGN 10 9 9 8 8 7 7 7 3 1 1 1
                VDXN 0 0 0 0 0 0 0 0 0 0 0 0
                VDYN 0 0 0 0 0 0 0 0 0 0 0 0
                VCXN 0 0 0 0 0 0 0 0 0 0 0 0
                VCYN 0 0 0 0 0 0 0 0 0 0 0 0
                VANN 0 0 0 0 0 0 0 0 0 0 0 0
                """, img_ht));
        if (d_line_only) {
            sb.append("""
                    WAVM 1 0.5875618 1
                    WAVM 2 0.550 0
                    WAVM 3 0.550 0
                    """);
        } else {
            sb.append("""
                WAVM 1 0.4861327 1
                WAVM 2 0.5875618 1
                WAVM 3 0.6562725 1
                """);
        }

        sb.append("""
                WAVM 4 0.550 0
                WAVM 5 0.550 0
                WAVM 6 0.550 0
                WAVM 7 0.550 0
                WAVM 8 0.550 0
                WAVM 9 0.550 0
                WAVM 10 0.550 0
                WAVM 11 0.550 0
                WAVM 12 0.550 0
                WAVM 13 0.550 0
                WAVM 14 0.550 0
                WAVM 15 0.550 0
                WAVM 16 0.550 0
                WAVM 17 0.550 0
                WAVM 18 0.550 0
                WAVM 19 0.550 0
                WAVM 20 0.550 0
                WAVM 21 0.550 0
                WAVM 22 0.550 0
                WAVM 23 0.550 0
                WAVM 24 0.550 0
                PWAV 2
                POLS 1 0 1 0 0 1 0
                GSTD 0 100 100 100 100 100 100 0 1 1 0 0 1 1 1 1 1 1
                NSCD 100 500 0 1.0E-3 5 1.0E-6 0 0 0 0 0 0 1000000 0 2
                """);
    }

    private void output_object(Prescription prescription, StringBuilder sb) {
        sb.append("""
                SURF 0
                  TYPE STANDARD
                  CURV 0.0 0 0 0 0 ""
                  HIDE 0 0 0 0 0 0 0 0 0 0
                  MIRR 2 0
                  DISZ INFINITY
                  DIAM 0 1 0 0 1 ""
                  POPS 0 0 0 0 0 0 0 0 1 1 1 1 0 0 0 0
                """);
    }

    private void output_surfaces(Prescription prescription, StringBuilder sb) {
        SurfaceType[] surfaces = prescription.get_surfaces();
        for (int i = 0; i < surfaces.length; i++) {
            SurfaceType s = surfaces[i];
            double thickness = 0.0;
            double diameter = s.get_diameter();
            diameter /= 2.0;
            thickness += s.get_thickness();
            sb.append("SURF ").append(i+1).append("\n");
            if (s.is_aperture_stop()) {
                sb.append("  STOP\n");
            }
            if (s.is_aspheric()) {
                if (s.is_odd_asphere())
                    sb.append("  TYPE ODDASPHE\n");
                else
                    sb.append("  TYPE EVENASPH\n");
            }
            else sb.append("  TYPE STANDARD\n");
            double curvature = s.get_radius_of_curvature() == 0.0 ? 0 : 1.0 / s.get_radius_of_curvature();
            sb.append("  CURV ").append(curvature).append(" 0 0 0 0\n");
            sb.append("  HIDE 0 0 0 0 0 0 0 0 0 0\n");
            sb.append("  MIRR 2 0\n");
            if (s.is_aspheric()) {
                double[] aspherics = s.get_aspheric_coeffs();
                for (int a = 1; a <= aspherics.length; a++) {
                    sb.append("  PARM ").append(a).append(" ");
                    sb.append(aspherics[a-1]).append("\n");
                }
            }
            sb.append("  DISZ ").append(thickness).append("\n");
            if (s.is_aspheric()) {
                // For Odd aspheres we have to supply ec, for Even aspheres cc
                double k = s.is_odd_asphere() ? s.get_cc()+1 : s.get_cc();
                sb.append("  CONI ").append(k).append("\n");
            }
            if (s.get_refractive_index() != 0.0) {
                sb.append("  GLAS ");
                String glassName = s.get_glass_name();
                if (glassName != null) {
                    sb.append(glassName).append(" 0 0 ");
                } else {
                    sb.append("___BLANK 1 0 ");
                }
                sb.append(s.get_refractive_index()).append(" ").append(s.get_abbe_vd()).append(" 0 0 0 0 0 0\n");
            }
            sb.append("  DIAM ").append(diameter).append(" 1 0 0 1 \"\"\n");
            if (s.is_field_stop()) {
                sb.append("  CLAP 0 ").append(diameter).append(" 0\n");
            }
            sb.append("  POPS 0 0 0 0 0 0 0 0 1 1 1 1 0 0 0 0\n");
        }
    }

    private void output_image_plane(Prescription prescription, StringBuilder sb) {
        int sid = prescription.get_surfaces().length + 1;
        sb.append("SURF ").append(sid).append("\n");
        var img_ht = prescription._diameter_image_circle/2.0;
        sb.append(String.format("""
                  TYPE STANDARD
                  CURV 0.0 0 0 0 0 ""
                  HIDE 0 0 0 0 0 0 0 0 0 0
                  MIRR 2 0
                  DISZ 0
                  DIAM %f 1 0 0 1 ""
                  POPS 0 0 0 0 0 0 0 0 1 1 1 1 0 0 0 0
                TOL TOFF   0   0              0              0   0 0 0 0
                """, img_ht));
    }

    private void output_configurations(Prescription prescription, StringBuilder sb) {
        if (prescription.get_num_configurations() <= 1)
            return;
        sb.append("MNUM ").append(prescription.get_num_configurations()).append(" 1\n");
        for (int i = 0; i < prescription._surfaces.length; i++) {
            var surface = prescription._surfaces[i];
            if (surface._diameter_by_scenario != null) {
                for (int j = 0; j < surface._diameter_by_scenario.length; j++) {
                    sb.append("SDIA    ").append(i+1).append("   ").append(j+1).append(" ").append(surface._diameter_by_scenario[j]/2.0).append("  0 0 0 1 1 1 0 0\n");
                }
            }
            if (surface._thickness_by_scenario != null) {
                for (int j = 0; j < surface._thickness_by_scenario.length; j++) {
                    sb.append("THIC    ").append(i+1).append("   ").append(j+1).append(" ").append(surface._thickness_by_scenario[j]).append("  0 0 0 1 1 1 0 0\n");
                }
            }
        }
    }
}
