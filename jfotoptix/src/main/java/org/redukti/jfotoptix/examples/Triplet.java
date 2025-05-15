package org.redukti.jfotoptix.examples;

import org.redukti.jfotoptix.analysis.AnalysisSpot;
import org.redukti.jfotoptix.curve.Flat;
import org.redukti.jfotoptix.layout.SystemLayout2D;
import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.math.Matrix3;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.medium.Abbe;
import org.redukti.jfotoptix.model.Image;
import org.redukti.jfotoptix.model.Lens;
import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.model.PointSource;
import org.redukti.jfotoptix.patterns.Distribution;
import org.redukti.jfotoptix.patterns.Pattern;
import org.redukti.jfotoptix.rendering.RendererSvg;
import org.redukti.jfotoptix.rendering.Rgb;
import org.redukti.jfotoptix.shape.Rectangle;
import org.redukti.jfotoptix.tracing.RayTraceParameters;
import org.redukti.jfotoptix.tracing.RayTraceRenderer;
import org.redukti.jfotoptix.tracing.RayTraceResults;
import org.redukti.jfotoptix.tracing.RayTracer;

public class Triplet {

    public static void main(String[] args) throws Exception {

        /* US1987878 - Modern Optical Design p 219 fig 8.12 */
        OpticalSystem.Builder systemBuilder = new OpticalSystem.Builder();
        Lens.Builder lensBuilder = new Lens.Builder()
                .position(Vector3Pair.position_000_001)
                .add_surface(26.16,  11.7, 4.916,
                        new Abbe(Abbe.AbbeFormula.AbbeVd, 1.678, 55.2, 0.0))
                .add_surface(1201.7,              11.7, 3.988)
                .add_surface(-83.46, 10.2, 1.038,
                        new Abbe(Abbe.AbbeFormula.AbbeVd, 1.648, 33.8, 0.0))
                .add_surface(25.67,  10.2, 4.0)
                .add_stop(9.2, 6.925,true)
                .add_surface(302.61,              10.3, 2.567,
                        new Abbe(Abbe.AbbeFormula.AbbeVd, 1.651, 55.9, 0.0))
                .add_surface(-54.79, 10.3, 81.433);
        systemBuilder.add(lensBuilder);
        Image.Builder imagePlaneBuilder = new Image.Builder()
                .position(new Vector3Pair(new Vector3(0, 0, 4.916+3.988+1.038+4.0+6.925+2.567+81.433), Vector3.vector3_001))
                .curve(Flat.flat)
                .shape(new Rectangle(46.33));
        systemBuilder.add(imagePlaneBuilder);

        Vector3 direction = Vector3.vector3_001;
        boolean skew = true;
        if (skew)
        {
            // Construct unit vector at an angle
            //      double z1 = cos (angleOfView);
            //      double y1 = sin (angleOfView);
            //      unit_vector = math::Vector3 (0, y1, z1);

            Matrix3 r = Matrix3.get_rotation_matrix(0, 25.17*2);
            direction = r.times(direction);
        }
        PointSource.Builder ps = new PointSource.Builder(PointSource.SourceInfinityMode.SourceAtInfinity, direction)
                .add_spectral_line(SpectralLine.d)
                .add_spectral_line(SpectralLine.C)
                .add_spectral_line(SpectralLine.F);
        systemBuilder.add(ps);

        RendererSvg renderer = new RendererSvg( 800, 400);
        OpticalSystem system = systemBuilder.build();
        System.out.println(system);
        // draw 2d system layout
//        system.draw_2d_fit(renderer);
//        system.draw_2d(renderer);
        SystemLayout2D systemLayout2D = new SystemLayout2D();
        systemLayout2D.layout2d(renderer, system);

        RayTraceParameters parameters = new RayTraceParameters(system);

        RayTracer rayTracer = new RayTracer();
        parameters.set_default_distribution (
                new Distribution(Pattern.MeridionalDist, 10, 0.999));
        // TODO set save generated state on point source
        System.out.println(parameters.sequenceToString(new StringBuilder()).toString());

        RayTraceResults result = rayTracer.trace(system, parameters);
        RayTraceRenderer.draw_2d(renderer, result, false, null);
        System.out.println(renderer.write(new StringBuilder()).toString());

        renderer =  new RendererSvg (300, 300, Rgb.rgb_black);
        AnalysisSpot spot = new AnalysisSpot(system, 20);
        spot.draw_diagram(renderer, true);
        System.out.println(renderer.write(new StringBuilder()).toString());

    }
}
