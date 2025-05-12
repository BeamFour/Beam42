package org.redukti.jfotoptix.tools;

import org.redukti.jfotoptix.importers.OpticalBenchDataImporter;

import java.util.List;

public class ZemaxExporter {


    public static void main(String[] args) throws Exception {
        Args arguments = Args.parseArguments(args);
        if (arguments.specfile == null) {
            System.err.println("Usage: --specfile inputfile [--scenario num]");
            System.exit(1);
        }
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(arguments.specfile);
        ZemaxExporter exporter = new ZemaxExporter();
        System.out.println(exporter.generate(specs, arguments.scenario, arguments.only_d_line));
    }

    private String generate(OpticalBenchDataImporter.LensSpecifications specs, int scenario, boolean dlineOnly) {
        StringBuilder sb = new StringBuilder();

        outputHeading(specs, scenario, dlineOnly, sb);
        outputObject(specs, scenario, sb);
        outputSurfaces(specs, scenario, sb);
        outputImagePlane(specs, scenario, sb);
        return sb.toString();
    }

    private void outputImagePlane(OpticalBenchDataImporter.LensSpecifications specs, int scenario, StringBuilder sb) {
        int sid = specs.get_surfaces().size() + 1;
        sb.append("SURF ").append(sid).append("\n");
        sb.append("""
                  TYPE STANDARD
                  CURV 0.0 0 0 0 0 ""
                  HIDE 0 0 0 0 0 0 0 0 0 0
                  MIRR 2 0
                  DISZ 0
                  DIAM 21.63 1 0 0 1 ""
                  POPS 0 0 0 0 0 0 0 0 1 1 1 1 0 0 0 0
                TOL TOFF   0   0              0              0   0 0 0 0
                """);
    }

    private void outputSurfaces(OpticalBenchDataImporter.LensSpecifications system, int scenario, StringBuilder sb) {
        List<OpticalBenchDataImporter.LensSurface> surfaces = system.get_surfaces();
        OpticalBenchDataImporter.Variable view_angles = system.find_variable("Angle of View");
        OpticalBenchDataImporter.Variable image_heights = system.find_variable("Image Height");
        OpticalBenchDataImporter.Variable back_focus = system.find_variable("Bf");
        if (back_focus == null) back_focus = system.find_variable("Bf(m)");
        OpticalBenchDataImporter.Variable aperture_diameters = system.find_variable("Aperture Diameter");
        if (scenario >= view_angles.num_scenarios() || scenario >= image_heights.num_scenarios() || scenario >= back_focus.num_scenarios() || (aperture_diameters != null && scenario >= aperture_diameters.num_scenarios())) {
            System.err.println("Scenario %u has missing data " + scenario);
            System.exit(1);
        }
        int surfaceNum = 1;
        for (int i = 0; i < surfaces.size(); i++) {
            OpticalBenchDataImporter.LensSurface s = surfaces.get(i);
            double thickness = 0.0;
            if (s.get_surface_type() == OpticalBenchDataImporter.SurfaceType.field_stop) {
                continue;
            }
            if (i < surfaces.size() - 1 && surfaces.get(i + 1).get_surface_type() == OpticalBenchDataImporter.SurfaceType.field_stop) {
                // Next surface is field stop
                // we will add the thickess of field stop to the current surface
                // FS will get 0 thickness as for now we skip it
                // TODO allow option to retain field stop
                thickness = surfaces.get(i + 1).get_thickness(scenario);
            }
            double diameter = s.get_diameter();
            if (s.get_surface_type() == OpticalBenchDataImporter.SurfaceType.aperture_stop && aperture_diameters != null) {
                diameter = aperture_diameters.get_value_as_double(scenario);
            }
            diameter /= 2.0;
            thickness += s.get_thickness(scenario);
            sb.append("SURF ").append(surfaceNum++).append("\n");
            if (s.get_surface_type() == OpticalBenchDataImporter.SurfaceType.aperture_stop) {
                sb.append("  STOP\n");
            }
            OpticalBenchDataImporter.AsphericalData aspherics = s.get_aspherical_data();
            if (aspherics != null) sb.append("  TYPE EVENASPH\n");
            else sb.append("  TYPE STANDARD\n");
            double curvature = s.get_radius() == 0.0 ? 0 : 1.0 / s.get_radius();
            sb.append("  CURV ").append(curvature).append(" 0 0 0 0\n");
            sb.append("  HIDE 0 0 0 0 0 0 0 0 0 0\n");
            sb.append("  MIRR 2 0\n");
            if (aspherics != null) {
                sb.append("  PARM 1 0\n");
                for (int a = 2; a < 9; a++) {
                    sb.append("  PARM ").append(a).append(" ");
                    sb.append(aspherics.data(a)).append("\n");
                }
            }
            sb.append("  DISZ ").append(thickness).append("\n");
            if (aspherics != null) {
                sb.append("  CONI ").append(aspherics.data(1)).append("\n");
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
            sb.append("  POPS 0 0 0 0 0 0 0 0 1 1 1 1 0 0 0 0\n");
        }
    }

    private void outputObject(OpticalBenchDataImporter.LensSpecifications specs, int scenario, StringBuilder sb) {
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

    private void outputHeading(OpticalBenchDataImporter.LensSpecifications specs, int scenario, boolean dLineOnly, StringBuilder sb) {
        sb.append("VERS 161019 507 33785\n");
        sb.append("MODE SEQ\n");
        sb.append("NAME ").append(specs.get_descriptive_data().get_title()).append("\n");
        sb.append("PFIL 0 0 0\n").append("LANG 0\n").append("UNIT MM X W X CM MR CPMM\n");
        sb.append("FNUM ").append(specs.find_variable("F-Number").get_value(scenario)).append("\n");
        sb.append("""
                ENVD 20 1 0
                GFAC 0 0
                GCAT SCHOTT LACROIX HOYA HIKARI OHARA NIKON-HIKARI NIKON
                RAIM 0 2 1 1 0 1 0 0 0
                SDMA 0 1 0
                FTYP 3 0 3 3 0 0 0
                ROPD 2
                HYPR 0
                PICB 1
                XFLN 0 0 0 0 0 0 0 0 0 0 0 0
                YFLN 0 15.141 21.63 0 0 0 0 0 0 0 0 0 0
                FWGN 1 1 1 1 1 1 1 1 1 1 1 1
                VDXN 0 0 0 0 0 0 0 0 0 0 0 0
                VDYN 0 0 0 0 0 0 0 0 0 0 0 0
                VCXN 0 0 0 0 0 0 0 0 0 0 0 0
                VCYN 0 0 0 0 0 0 0 0 0 0 0 0
                VANN 0 0 0 0 0 0 0 0 0 0 0 0
                """);
        if (dLineOnly) {
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
}
