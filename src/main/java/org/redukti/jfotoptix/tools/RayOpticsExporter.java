package org.redukti.jfotoptix.tools;

import org.redukti.jfotoptix.importers.OpticalBenchDataImporter;

import java.util.List;

public class RayOpticsExporter {
    double get_angle_of_view(OpticalBenchDataImporter.LensSpecifications system, int scenario) {
        OpticalBenchDataImporter.Variable view_angles = system.find_variable("Angle of View");
        return view_angles.get_value_as_double(scenario) / 2.0;
    }

    void generate_preamble(OpticalBenchDataImporter.LensSpecifications system, int scenario, StringBuilder fp) {
        OpticalBenchDataImporter.DescriptiveData descriptive_data = system.get_descriptive_data();
        String title = descriptive_data.get_title();
        OpticalBenchDataImporter.Variable f_number = system.find_variable("F-Number");
        fp.append("%matplotlib inline\n")
                .append("isdark = False\n")
                .append("from rayoptics.environment import *\n")
                .append("from rayoptics.elem.elements import Element\n")
                .append("from rayoptics.raytr.trace import apply_paraxial_vignetting\n")
                .append("\n")
                .append("# ").append(title).append("\n")
                .append("# Obtained via https://www.photonstophotos.net/GeneralTopics/Lenses/OpticalBench/OpticalBenchHub.htm\n")
                .append("\n")
                .append("opm = OpticalModel()\n")
                .append("sm  = opm.seq_model\n")
                .append("osp = opm.optical_spec\n")
                .append("pm = opm.parax_model\n")
                .append("osp.pupil = PupilSpec(osp, key=['image', 'f/#'], value=").append(f_number.get_value_as_double(scenario)).append(")\n")
                .append("osp.field_of_view = FieldSpec(osp, key=['object', 'angle'], flds=[0., ").append(get_angle_of_view(system, scenario)).append("])\n")
                .append("osp.spectral_region = WvlSpec([(486.1327, 0.5), (587.5618, 1.0), (656.2725, 0.5)], ref_wl=1)\n")
                .append("opm.system_spec.title = \"").append(title).append("\"\n")
                .append("opm.system_spec.dimensions = 'MM'\n")
                .append("opm.radius_mode = True\n");
    }

    void generate_aspherics(OpticalBenchDataImporter.AsphericalData asphere, StringBuilder fp) {
        fp.append("sm.ifcs[sm.cur_surface].profile = EvenPolynomial(r=").append(asphere.data(0)).append(", cc=").append(asphere.data(1)).append(",\n");
        fp.append("\tcoefs=[0.0,")
                .append(asphere.data(2)).append(",")
                .append(asphere.data(3)).append(",")
                .append(asphere.data(4)).append(",")
                .append(asphere.data(5)).append(",")
                .append(asphere.data(6)).append(",")
                .append(asphere.data(7)).append("])\n");
    }

    /* handling of Field Stop surface is problematic because it messes up the
     * numbering of surfaces and therefore we need to adjust the surface id
     * when we see a field stop. Currently we cannot handle more than 1 field stop.
     */
    void generate_lens_data(OpticalBenchDataImporter.LensSpecifications system, int scenario, StringBuilder fp) {
        List<OpticalBenchDataImporter.LensSurface> surfaces = system.get_surfaces();
        OpticalBenchDataImporter.Variable view_angles = system.find_variable("Angle of View");
        OpticalBenchDataImporter.Variable image_heights = system.find_variable("Image Height");
        OpticalBenchDataImporter.Variable back_focus = system.find_variable("Bf");
        if (back_focus == null)
            back_focus = system.find_variable("Bf(m)");
        OpticalBenchDataImporter.Variable aperture_diameters = system.find_variable("Aperture Diameter");
        if (scenario >= view_angles.num_scenarios() ||
                scenario >= image_heights.num_scenarios() ||
                scenario >= back_focus.num_scenarios() ||
                (aperture_diameters != null && scenario >= aperture_diameters.num_scenarios())) {
            System.err.println("Scenario %u has missing data " + scenario);
            System.exit(1);
        }
        fp.append("sm.gaps[0].thi=1e10\n");
//        double Bf = back_focus.get_value_as_double(scenario);
        double thickness = 0.0;
        for (int i = 0; i < surfaces.size(); i++) {
            OpticalBenchDataImporter.LensSurface s = surfaces.get(i);
            if (s.get_surface_type() == OpticalBenchDataImporter.SurfaceType.field_stop) {
                thickness += s.get_thickness(scenario);
                continue;
            }
//            if (i + 1 == surfaces.size() && s.is_cover_glass()) {
//                // Oddity - override the Bf
//                Bf = s.get_thickness(scenario);
//            }
            double diameter = s.get_diameter();
            if (s.get_surface_type() == OpticalBenchDataImporter.SurfaceType.aperture_stop && aperture_diameters != null) {
                diameter = aperture_diameters.get_value_as_double(scenario);
            }
            thickness += s.get_thickness(scenario);
            if (s.get_surface_type() == OpticalBenchDataImporter.SurfaceType.surface) {
                if (s.get_refractive_index() != 0.0) {
                    fp.append("sm.add_surface([")
                            .append(s.get_radius()).append(",")
                            .append(thickness).append(",")
                            .append(s.get_refractive_index()).append(",")
                            .append(s.get_abbe_vd()).append("])\n");
                } else {
                    fp.append("sm.add_surface([")
                            .append(s.get_radius()).append(",")
                            .append(thickness).append("])\n");
                }
                OpticalBenchDataImporter.AsphericalData aspherics = s.get_aspherical_data();
                if (aspherics != null) {
                    generate_aspherics(aspherics, fp);
                }
            } else if (s.get_surface_type() == OpticalBenchDataImporter.SurfaceType.aperture_stop) {
                fp.append("sm.add_surface([")
                        .append(s.get_radius()).append(",")
                        .append(thickness).append("])\n")
                        .append("sm.set_stop()\n");
            }
            fp.append("sm.ifcs[sm.cur_surface].max_aperture = ").append(diameter / 2.0).append("\n");
            thickness = 0.0;
        }
    }
    void generate_rest(StringBuilder fp) {
        fp.append("sm.list_surfaces()\n")
                .append("sm.list_gaps()\n")
                .append("sm.do_apertures = False\n")
                .append("opm.update_model()\n")
                .append("apply_paraxial_vignetting(opm)\n")
                .append("layout_plt = plt.figure(FigureClass=InteractiveLayout, opt_model=opm, do_draw_rays=True, do_paraxial_layout=False,\n")
                .append("                        is_dark=isdark).plot()\n")
                .append("sm.list_model()\n")
                .append("# List the optical specifications\n")
                .append("pm.first_order_data()\n")
                .append("# List the paraxial model\n")
                .append("pm.list_lens()\n")
                .append("# Plot the transverse ray aberrations\n")
                .append("abr_plt = plt.figure(FigureClass=RayFanFigure, opt_model=opm,\n")
                .append("          data_type='Ray', scale_type=Fit.All_Same, is_dark=isdark).plot()\n")
                .append("# Plot the wavefront aberration\n")
                .append("wav_plt = plt.figure(FigureClass=RayFanFigure, opt_model=opm,\n")
                .append("          data_type='OPD', scale_type=Fit.All_Same, is_dark=isdark).plot()\n")
                .append("# Plot spot diagrams\n")
                .append("spot_plt = plt.figure(FigureClass=SpotDiagramFigure, opt_model=opm, \n")
                .append("                      scale_type=Fit.User_Scale, user_scale_value=0.1, is_dark=isdark).plot()\n");
    }
    String generate(OpticalBenchDataImporter.LensSpecifications system, int scenario) {
        StringBuilder sb = new StringBuilder();
        generate_preamble(system, scenario, sb);
        generate_lens_data(system, scenario, sb);
        generate_rest(sb);
        return sb.toString();
    }

    static final class Args {
        int scenario = 0;
        String filename = null;
    }

    static LensTool.Args parseArguments(String[] args) {
        LensTool.Args arguments = new LensTool.Args();
        for (int i = 0; i < args.length; i++) {
            String arg1 = args[i];
            String arg2 = i + 1 < args.length ? args[i + 1] : null;
            if (arg1.equals("--specfile")) {
                arguments.filename = arg2;
                i++;
            } else if (arg1.equals("--scenario")) {
                arguments.scenario = Integer.parseInt(arg2);
                i++;
            }
        }
        return arguments;
    }

    public static void main(String[] args) throws Exception {
        LensTool.Args arguments = parseArguments(args);
        if (arguments.filename == null) {
            System.err.println("Usage: --specfile inputfile [--scenario num]");
            System.exit(1);
        }
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(arguments.filename);
        RayOpticsExporter exporter = new RayOpticsExporter();
        System.out.println(exporter.generate(specs, arguments.scenario));
    }

}
