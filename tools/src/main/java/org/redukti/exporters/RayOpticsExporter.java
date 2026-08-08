package org.redukti.exporters;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.rayoptics.seq.Glass;
import org.redukti.util.Args;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class RayOpticsExporter {
    void generate_preamble(OpticalBenchDataImporter.LensSpecifications system, int scenario, StringBuilder fp) {
        OpticalBenchDataImporter.VarSet descriptive_data = system.get_descriptive_data();
        String title = descriptive_data.get_value("title");
        OpticalBenchDataImporter.Variable f_number = system.find_variable("F-Number");
        var half_angle = system.get_half_angle_of_view_in_degrees(scenario);
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
                .append("sm  = opm['seq_model']\n")
                .append("osp = opm['optical_spec']\n")
                .append("pm = opm['parax_model']\n")
                .append("em = opm['ele_model']\n")
                .append("pt = opm['part_tree']\n")
                .append("ar = opm['analysis_results']\n")
                .append("osp.pupil = PupilSpec(osp, key=['image', 'f/#'], value=").append(f_number.get_value_as_double(scenario)).append(")\n")
                .append("osp.field_of_view = FieldSpec(osp, key=('object', 'angle'), value=").append(half_angle)
                .append(", flds=[0.0,0.7,1.0]")
                .append(", is_relative=True, is_wide_angle=").append(half_angle > 45. ? "True": "False").append(")\n")
                .append("osp.spectral_region = WvlSpec([(486.1327, 0.5), (587.5618, 1.0), (656.2725, 0.5)], ref_wl=1)\n")
                .append("opm.system_spec.title = \"").append(title).append("\"\n")
                .append("opm.system_spec.dimensions = 'mm'\n")
                .append("opm.radius_mode = True\n");
    }

    void generate_aspherics(OpticalBenchDataImporter.AsphericalData asphere, StringBuilder fp) {
        if (asphere.get_asphere_type() != OpticalBenchDataImporter.AsphereType.Odd)
            fp.append("sm.ifcs[sm.cur_surface].profile = EvenPolynomial(r=").append(asphere.get_r()).append(", cc=").append(asphere.get_cc()).append(",\n");
        else
            fp.append("sm.ifcs[sm.cur_surface].profile = RadialPolynomial(r=").append(asphere.get_r()).append(", cc=").append(asphere.get_cc()).append(",\n");
        double[] coeffs = asphere.get_coeffs();
        fp.append("\tcoefs=[");
        for (int i = 0; i < coeffs.length; i++) {
            if (i > 0) fp.append(",");
            fp.append(coeffs[i]);
        }
        fp.append("])\n");
    }

    /* handling of Field Stop surface is problematic because it messes up the
     * numbering of surfaces and therefore we need to adjust the surface id
     * when we see a field stop. Currently we cannot handle more than 1 field stop.
     */
    void generate_lens_data(OpticalBenchDataImporter.LensSpecifications system, int scenario, StringBuilder fp, boolean use_glass_types) {
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
        for (int i = 0; i < surfaces.size(); i++) {
            double thickness = 0.0;
            OpticalBenchDataImporter.LensSurface s = surfaces.get(i);
            double diameter = s.get_diameter(scenario);
            if (s.get_surface_type() == OpticalBenchDataImporter.SurfaceType.aperture_stop && aperture_diameters != null) {
                diameter = aperture_diameters.get_value_as_double(scenario);
            }
            thickness += s.get_thickness(scenario);
            if (s.get_surface_type() == OpticalBenchDataImporter.SurfaceType.surface) {
                if (s.get_refractive_index() != 0.0) {
                    String glassName = s.get_glass_name();
                    String catalogName = Glass.get_catalog_name(s.get_catalog_name());
                    Glass glass = glassName != null ? Glass.glass_by_catalog_name(catalogName, glassName) : null;
                    if (glass != null && glass.catalog_name != null && use_glass_types) {
                        fp.append("sm.add_surface([")
                                .append(s.get_radius()).append(",")
                                .append(thickness).append(",'")
                                .append(glass.label).append("','")
                                .append(glass.catalog_name).append("']");
                    }
                    else {
                        fp.append("sm.add_surface([")
                                .append(s.get_radius()).append(",")
                                .append(thickness).append(",")
                                .append(s.get_refractive_index()).append(",")
                                .append(s.get_abbe_vd()).append("]");
                    }
                } else {
                    fp.append("sm.add_surface([")
                            .append(s.get_radius()).append(",")
                            .append(thickness).append("]");
                }
                fp.append(",sd=").append(diameter / 2.0).append(")\n");
                OpticalBenchDataImporter.AsphericalData aspherics = s.get_aspherical_data();
                if (aspherics != null) {
                    generate_aspherics(aspherics, fp);
                }
            } else if (s.get_surface_type() == OpticalBenchDataImporter.SurfaceType.aperture_stop) {
                fp.append("sm.add_surface([")
                        .append(s.get_radius()).append(",")
                        .append(thickness).append("]")
                        .append(",sd=").append(diameter / 2.0).append(")\n")
                        .append("sm.set_stop()\n");
            } else if (s.get_surface_type() == OpticalBenchDataImporter.SurfaceType.field_stop) {
                fp.append("sm.add_surface([")
                        .append(s.get_radius()).append(",")
                        .append(thickness).append("]")
                        .append(",sd=").append(diameter / 2.0).append(")\n");
            }
        }
    }
    void generate_rest(StringBuilder fp) {
        fp.append("sm.list_surfaces()\n")
                .append("sm.list_gaps()\n")
                .append("sm.do_apertures = False\n")
                .append("opm.update_model()\n")
                .append("set_vignetting(opm)\n")
                .append("print('')\n")
                .append("listobj(osp)\n")
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
    String generate(OpticalBenchDataImporter.LensSpecifications system, int scenario, boolean use_glass_types) {
        StringBuilder sb = new StringBuilder();
        generate_preamble(system, scenario, sb);
        generate_lens_data(system, scenario, sb, use_glass_types);
        generate_rest(sb);
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        Args arguments = Args.parseArguments(args);
        if (arguments.specfile == null) {
            System.err.println("Usage: --specfile inputfile [--scenario num]");
            System.exit(1);
        }
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(arguments.specfile);
        RayOpticsExporter exporter = new RayOpticsExporter();
        if (arguments.outputFile != null)
            Files.writeString(new File(arguments.outputFile).toPath(),exporter.generate(specs, arguments.scenario, arguments.use_glass_types), StandardOpenOption.CREATE);
        System.out.println(exporter.generate(specs, arguments.scenario, arguments.use_glass_types));
    }

}
