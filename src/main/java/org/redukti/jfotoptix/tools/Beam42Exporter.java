package org.redukti.jfotoptix.tools;

import org.redukti.jfotoptix.importers.OpticalBenchDataImporter;
import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.model.*;

import java.util.List;
import java.util.stream.Collectors;

public class Beam42Exporter {
    double get_angle_of_view(OpticalBenchDataImporter.LensSpecifications system, int scenario) {
        OpticalBenchDataImporter.Variable view_angles = system.find_variable("Angle of View");
        return view_angles.get_value_as_double(scenario) / 2.0;
    }

    String generate(OpticalBenchDataImporter.LensSpecifications specs, OpticalSystem system, int scenario) {
        StringBuilder sb = new StringBuilder();
        List<Element> seq = system.get_sequence().stream()
                .filter(e -> !(e instanceof RaySource))
                .collect(Collectors.toList());
        double wvln = SpectralLine.d;
        sb.append(seq.size()).append(" surfaces").append(System.lineSeparator());
        sb.append("Type   Index   Z    C    Dia   S    A2   A4   A6    A8    A10   A12   A14  ").append(System.lineSeparator());
        sb.append("-----:-------:----:----:-----:----:----:-----:-----:-----:-----:-----:-----").append(System.lineSeparator());
        for (Element e: seq) {
            if (e instanceof Stop) {
                Stop stop = (Stop)e;
                sb.append("iris : ")
                        .append(stop.get_material(0).get_refractive_index(wvln))
                        .append(" : ")
                        .append(stop.get_position().z())
                        .append(" : ")
                        .append(stop.get_curve().get_curvature())
                        .append(" : ")
                        .append(stop.get_shape().max_radius()*2.0)
                        .append(" :  :  :  :  :  :  :  : ");
            }
            else if (e instanceof OpticalSurface) {
                OpticalSurface surface = (OpticalSurface) e;
                sb.append("lens : ")
                        .append(surface.get_material(0).get_refractive_index(wvln))
                        .append(" : ")
                        .append(surface.get_position().z())
                        .append(" : ")
                        .append(surface.get_curve().get_curvature())
                        .append(" : ")
                        .append(surface.get_shape().max_radius()*2.0)
                        .append(" :  :  :  :  :  :  :  : ");
            }
            else if (e instanceof Image) {
                Image image = (Image) e;
                sb.append("lens : ")
                        .append(1.0)
                        .append(" : ")
                        .append(image.get_position().z())
                        .append(" : ")
                        .append(image.get_curve().get_curvature())
                        .append(" : ")
                        .append(image.get_shape().max_radius()*2.0)
                        .append(" :  :  :  :  :  :  :  : ");
            }
            sb.append(System.lineSeparator());
        }
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
                arguments.specfile = arg2;
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
        if (arguments.specfile == null) {
            System.err.println("Usage: --specfile inputfile [--scenario num]");
            System.exit(1);
        }
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(arguments.specfile);
        OpticalSystem system = OpticalBenchDataImporter.build_system(specs, arguments.scenario, arguments.use_glass_types).build();
        Beam42Exporter exporter = new Beam42Exporter();
        System.out.println(exporter.generate(specs, system, arguments.scenario));
    }

}
