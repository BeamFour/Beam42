package org.redukti.jfotoptix.examples;

import org.redukti.jfotoptix.analysis.AnalysisSpot;
import org.redukti.jfotoptix.curve.Flat;
import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.math.Matrix3;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.medium.Abbe;
import org.redukti.jfotoptix.medium.GlassMap;
import org.redukti.jfotoptix.model.Image;
import org.redukti.jfotoptix.model.Lens;
import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.model.PointSource;
import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.jfotoptix.shape.Rectangle;

import java.util.*;
import java.util.stream.Collectors;

// Takes too long to run for more than about 22 glasses (that takes 2 hrs as well)
public class Noctilux50Optim {

    static final class GlassType {
        final String name;
        final double nd;
        final double vd;

        public GlassType(String name, double nd, double vd) {
            this.name = name;
            this.nd = nd;
            this.vd = vd;
        }

        @Override
        public String toString() {
            return "(" + name + "," + nd + "," + vd + ')';
        }
    }

    static final class SurfaceType {
        double radius;
        double thickness;
        double apertureRadius;
        boolean isStop;
        double nd;
        double vd;
        String glassName;

        public SurfaceType(boolean isStop, double radius, double thickness, double nd, double apertureRadius, double vd, String glassName) {
            this.radius = radius;
            this.thickness = thickness;
            this.apertureRadius = apertureRadius;
            this.isStop = isStop;
            this.nd = nd;
            this.vd = vd;
            this.glassName = glassName;
        }
    }

    private static double add_surface(Lens.Builder lens, double radius, double thickness, double diameter, double nd, double vd, String glassName, boolean stop) {
        double apertureRadius = diameter / 2.0;
        if (stop) {
            lens.add_stop(apertureRadius, thickness, true);
            return thickness;
        }
        if (nd != 0.0) {
            var glass = GlassMap.glassByName(glassName);
            if (glass == null) {
                lens.add_surface(radius, apertureRadius, thickness, new Abbe(Abbe.AbbeFormula.AbbeVd, nd, vd, 0.0));
            }
            else {
                lens.add_surface(radius, apertureRadius, thickness, glass);
            }
        } else {
            lens.add_surface(radius, apertureRadius, thickness);
        }
        return thickness;
    }

    // Baseline
    private static List<SurfaceType> getSurfaces() {
        List<SurfaceType> list = new ArrayList<>();
        list.add(new SurfaceType(false, 60.99547581765, 8.071, 1.6779, 54.57, 55.2,	"N-LAK12"));
        list.add(new SurfaceType(false, 1758.18207663098,	0.1,	0,	54.57, 0, null));
        list.add(new SurfaceType(false, 30.1407788557876,	8.0,	1.883,	46.571,	40.8,	"S-LAH58"));
        list.add(new SurfaceType(false, 68.82815600250589, 1.7857, 0,44.644, 0, null));
        list.add(new SurfaceType(false, 121.28440784534101,	4.0714,	1.7847,	45.214,	26.08,	"SF56A"));
        list.add(new SurfaceType(false, 19.534735219, 9.35, 0,31.6,0,null));
        list.add(new SurfaceType(true, 0, 7.1,	0,	30.6,0,null	));
        list.add(new SurfaceType(false, -23.80812804,	1.357,	1.72825,	31.0,	28.41,	"SF10"));
        list.add(new SurfaceType(false, 91.78595326889,	8.7143,	1.883,	37.643,	40.8,	"S-LAH58"));
        list.add(new SurfaceType(false, -32.0671949598,	0.1,		0, 37.714, 0, null));
        list.add(new SurfaceType(false, 92.46787956,	4.0,	1.788,	35.286,	47.49,	"N-LAF21"));
        list.add(new SurfaceType(false, 549.86675633889,	0.1, 0,		35.286, 0, null	));
        list.add(new SurfaceType(false, 83.0795202171,	4,	1.788,	33.429,	47.49,	"N-LAF21"));
        list.add(new SurfaceType(false, -197.873443,	27.365, 0,		33.429, 0, null));
        return list;
    }

    private static OpticalSystem.Builder buildSystem(List<SurfaceType> surfaces, boolean addPointSource, boolean skew) {
        OpticalSystem.Builder sys = new OpticalSystem.Builder();
        double imageHeight = 43.2;
        double angleOfView = 47.0 / 2.0;
        double fNum = 1.0;
        if (addPointSource) {
            Vector3 direction = Vector3.vector3_001;
            if (skew) {
                // Construct unit vector at an angle
                //      double z1 = cos (angleOfView);
                //      double y1 = sin (angleOfView);
                //      unit_vector = math::Vector3 (0, y1, z1);
                double aov = Math.toRadians(47.0*0.7) / 2.0;
                Matrix3 r = Matrix3.get_rotation_matrix(0, aov);
                direction = r.times(direction);
            }
            PointSource.Builder ps = new PointSource.Builder(PointSource.SourceInfinityMode.SourceAtInfinity, direction)
                    .add_spectral_line(SpectralLine.d)
                    .add_spectral_line(SpectralLine.C)
                    .add_spectral_line(SpectralLine.F);
            sys.add(ps);
        }
        /* anchor lens */
        Lens.Builder lens = new Lens.Builder().position(Vector3Pair.position_000_001);
        double image_pos = 0.0;
        for (int i = 0; i < surfaces.size(); i++) {
            SurfaceType s = surfaces.get(i);
            double nd = s.nd;
            double vd = s.vd;
            String glassName = s.glassName;
            double thickness = add_surface(lens, s.radius, s.thickness, s.apertureRadius, nd, vd, glassName, s.isStop);
            image_pos += thickness;
        }
        sys.add(lens);
        Image.Builder image = new Image.Builder().position(new Vector3Pair(new Vector3(0, 0, image_pos), Vector3.vector3_001)).curve(Flat.flat).shape(new Rectangle(imageHeight * 2.));
        sys.add(image);
        sys.angle_of_view(angleOfView);
        sys.f_number(fNum);
        return sys;
    }

    private static void setup(int surface, double[] radii) {
        if (surface == radii.length)
            return;
        double[] example = Arrays.copyOf(radii,radii.length);
        if (radii[surface] != 0.0) {
            double unit = radii[surface] * 0.001;
            example[surface] += unit;
            analyse( example);
            setup( surface + 1, example);
            example = Arrays.copyOf(radii, radii.length);
            example[surface] -= unit;
            analyse( example);
            setup( surface + 1, example);
        }
        else {
            setup( surface + 1, example);
        }
    }

    public static void main(String[] args) throws Exception {
        var surfaces = getSurfaces();
        double[] radii = new double[surfaces.size()];
        for (int i = 0; i < radii.length; i++)
            radii[i] = surfaces.get(i).radius;
        // Now try a combination
        setup(0,radii);
    }

    private static void analyse(double[] radii) {
        var surfaces = getSurfaces();
        assert surfaces.size() == radii.length;
        for (int i = 0; i < radii.length; i++) {
            surfaces.get(i).radius = radii[i];
        }
        var sys = buildSystem(surfaces, true, false).build();
        var parax = ParaxialFirstOrderInfo.compute(sys);
        if (parax.effective_focal_length > 52.39 && parax.effective_focal_length < 52.41
                && parax.enp_dist > 42.88 && parax.enp_dist < 42.91
            ) {
            var spotAnalysis = new AnalysisSpot(sys, 10);
            spotAnalysis.process_analysis();
            var nonskew = spotAnalysis.get_rms_radius();
            if (nonskew > 16)
               return;
            sys = buildSystem(surfaces,  true, true).build();
            spotAnalysis = new AnalysisSpot(sys, 10);
            spotAnalysis.process_analysis();
            var skewed = spotAnalysis.get_rms_radius();
            if (skewed > 93)
                return;
            StringBuilder sb = new StringBuilder();
            sb.append(nonskew).append("\t");
            sb.append(skewed).append("\t");
            sb.append(parax.effective_focal_length).append("\t")
                    .append(parax.back_focal_length).append("\t")
                    .append(parax.fno).append("\t")
                    .append(parax.enp_dist).append("\t");
            for (int i = 0; i < radii.length; i++)
                sb.append(radii[i]).append("\t");
            System.out.println(sb.toString());
        }
    }
}
