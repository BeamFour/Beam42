package org.redukti.jfotoptix.examples;

import org.redukti.jfotoptix.analysis.AnalysisSpot;
import org.redukti.jfotoptix.analysis.ChiefRayFinder;
import org.redukti.jfotoptix.importers.OpticalBenchDataImporter;
import org.redukti.jfotoptix.layout.SystemLayout2D;
import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.math.Matrix3;
import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.model.PointSource;
import org.redukti.jfotoptix.patterns.Distribution;
import org.redukti.jfotoptix.patterns.Pattern;
import org.redukti.jfotoptix.rendering.RendererSvg;
import org.redukti.jfotoptix.rendering.Rgb;
import org.redukti.jfotoptix.tracing.RayTraceParameters;
import org.redukti.jfotoptix.tracing.RayTraceRenderer;
import org.redukti.jfotoptix.tracing.RayTraceResults;
import org.redukti.jfotoptix.tracing.SequentialRayTracer;

import java.util.List;

public class Canon50mm {

    public static void main(String[] args) throws Exception {

        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file("C:\\work\\github\\goptical\\data\\canon-rf-50mmf1.2\\canon-rf-50mmf1.2.txt");
        OpticalSystem.Builder systemBuilder = OpticalBenchDataImporter.build_system(specs, 0, false);
        double angleOfView = OpticalBenchDataImporter.get_angle_of_view_in_radians(specs, 0);
        Vector3 direction = Vector3.vector3_001;
        boolean skew = true;
        if (skew)
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
        System.out.println(system);

        // draw 2d system layout
        RendererSvg renderer = new RendererSvg( 800, 400);
        SystemLayout2D systemLayout2D = new SystemLayout2D();
        systemLayout2D.layout2d(renderer, system);

        RayTraceParameters parameters = new RayTraceParameters(system);

        SequentialRayTracer rayTracer = new SequentialRayTracer();
        Distribution d = new Distribution(Pattern.UserDefined, 10, 0.999);
        d.set_user_defined_points(List.of(new Vector2(0,-20.2)));
        parameters.set_default_distribution (d);
        // TODO set save generated state on point source
        System.out.println(parameters.sequenceToString(new StringBuilder()).toString());

        RayTraceResults result = rayTracer.trace(system, parameters);
        RayTraceRenderer.draw_2d(renderer, result, false, null);
        System.out.println(renderer.write(new StringBuilder()).toString());

        result = ChiefRayFinder.findChiefRay(system);
        RayTraceRenderer.draw_2d(renderer, result, false, null);
        System.out.println(renderer.write(new StringBuilder()).toString());

        renderer =  new RendererSvg (300, 300, Rgb.rgb_black);
        AnalysisSpot spot = new AnalysisSpot(system, 20);
        spot.draw_diagram(renderer, true);
        System.out.println(renderer.write(new StringBuilder()).toString());
    }
}
