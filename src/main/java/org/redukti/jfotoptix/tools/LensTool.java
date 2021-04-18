package org.redukti.jfotoptix.tools;

import org.redukti.jfotoptix.analysis.AnalysisSpot;
import org.redukti.jfotoptix.importers.OpticalBenchDataImporter;
import org.redukti.jfotoptix.layout.SystemLayout2D;
import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.math.Matrix3;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.patterns.Distribution;
import org.redukti.jfotoptix.patterns.Pattern;
import org.redukti.jfotoptix.rendering.RendererSvg;
import org.redukti.jfotoptix.rendering.Rgb;
import org.redukti.jfotoptix.sys.OpticalSystem;
import org.redukti.jfotoptix.sys.PointSource;
import org.redukti.jfotoptix.tracing.RayTraceParameters;
import org.redukti.jfotoptix.tracing.RayTraceRenderer;
import org.redukti.jfotoptix.tracing.RayTraceResults;
import org.redukti.jfotoptix.tracing.RayTracer;

public class LensTool {

    static final class Args {
        int scenario = 0;
        String filename = null;
        String outputType = "layout";
        boolean skewRays = false;
        boolean dumpSystem = false;
    }

    static Args parseArguments(String[] args) {
        Args arguments = new Args();
        for (int i = 0; i < args.length; i++) {
            String arg1 = args[i];
            String arg2 = i+1 < args.length ? args[i+1] : null;
            if (arg1.equals("--specfile")) {
                arguments.filename = arg2;
                i++;
            }
            else if (arg1.equals("--scenario")) {
                arguments.scenario = Integer.parseInt(arg2);
                i++;
            }
            else if (arg1.equals("--output")) {
                arguments.outputType = arg2;
                i++;
            }
            else if (arg1.equals("--skew")) {
                arguments.skewRays = true;
            }
            else if (arg1.equals("--dump-system")) {
                arguments.dumpSystem = true;
            }
        }
        return arguments;
    }


    public static void main(String[] args) throws Exception {
        Args arguments = parseArguments(args);
        if (arguments.filename == null) {
            System.err.println("Usage: --specfile inputfile [--scenario num] [--skew] [--output layout|spot] [--dump-system]");
            System.exit(1);
        }
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(arguments.filename);
        OpticalSystem.Builder systemBuilder = OpticalBenchDataImporter.buildSystem(specs, arguments.scenario);
        double angleOfView = OpticalBenchDataImporter.getAngleOfViewInRadians (specs, arguments.scenario);
        Vector3 direction = Vector3.vector3_001;
        if (arguments.skewRays)
        {
            // Construct unit vector at an angle
            //      double z1 = cos (angleOfView);
            //      double y1 = sin (angleOfView);
            //      unit_vector = math::Vector3 (0, y1, z1);
            Matrix3 r = Matrix3.get_rotation_matrix(0, angleOfView);
            direction = r.times(direction);
        }
        PointSource.Builder ps = new PointSource.Builder(PointSource.SourceInfinityMode.SourceAtInfinity, direction)
                .add_spectral_line(SpectralLine.d)
                .add_spectral_line(SpectralLine.C)
                .add_spectral_line(SpectralLine.F);
        systemBuilder.add(ps);

        OpticalSystem system = systemBuilder.build();
        if (arguments.dumpSystem) {
            System.out.println(system);
        }
        if (arguments.outputType.equals("layout")) {
            // draw 2d system layout
            RendererSvg renderer = new RendererSvg( 800, 400);
            SystemLayout2D systemLayout2D = new SystemLayout2D();
            systemLayout2D.layout2d(renderer, system);
            RayTraceParameters parameters = new RayTraceParameters(system);
            RayTracer rayTracer = new RayTracer();
            parameters.set_default_distribution(
                    new Distribution(Pattern.MeridionalDist, 20, 0.999));
            if (arguments.dumpSystem) {
                System.out.println(parameters.sequenceToString(new StringBuilder()).toString());
            }
            RayTraceResults result = rayTracer.trace(system, parameters);
            RayTraceRenderer.draw_2d(renderer, result, false, null);
            System.out.println(renderer.write(new StringBuilder()).toString());
        }
        if (arguments.outputType.equals("spot")) {
            RendererSvg renderer = new RendererSvg(300, 300, Rgb.rgb_black);
            AnalysisSpot spot = new AnalysisSpot(system);
            spot.draw_diagram(renderer, true);
            System.out.println(renderer.write(new StringBuilder()).toString());
        }
    }
}
