package org.redukti.jfotoptix.examples;

import org.redukti.jfotoptix.analysis.AnalysisSpot;
import org.redukti.jfotoptix.curve.Flat;
import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.medium.Abbe;
import org.redukti.jfotoptix.medium.GlassMap;
import org.redukti.jfotoptix.model.Image;
import org.redukti.jfotoptix.model.Lens;
import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.model.PointSource;
import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.jfotoptix.shape.Rectangle;
import org.redukti.mathlib.Matrix3;
import org.redukti.mathlib.Vector3;
import org.redukti.mathlib.Vector3Pair;
import org.redukti.rayoptics.seq.Glass;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

// Takes too long to run for more than about 22 glasses (that takes 2 hrs as well)
public class LeicaSummilux50mmSelect2 {

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

        public SurfaceType(boolean isStop, double radius, double thickness, double nd, double apertureRadius, double vd) {
            this.radius = radius;
            this.thickness = thickness;
            this.apertureRadius = apertureRadius;
            this.isStop = isStop;
            this.nd = nd;
            this.vd = vd;
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

    // Measured by DM - off patent
    private static List<SurfaceType> getSurfaces() {
        List<SurfaceType> list = new ArrayList<>();

        list.add(new SurfaceType(false, 40.376, 5.725, 1.73430, 37.93, 28.19));
        list.add(new SurfaceType(false, 90.57, 0.0, 0, 35.49, 0));
        list.add(new SurfaceType(false, 26.437, 8.7, 1.67133, 33.05, 41.64));
        list.add(new SurfaceType(false, -532.104, 2.748, 1.7919, 31.217, 25.55));
        list.add(new SurfaceType(false, 22.866, 5.88, 0, 26.4, 0));
        list.add(new SurfaceType(true, 0, 2.06, 0, 25.798, 0));
        list.add(new SurfaceType(false, -68.33, 2.5158, 1.78831, 25.722, 47.7));
        list.add(new SurfaceType(false, -155.056, 3.05, 0, 25.722, 0));
        list.add(new SurfaceType(false, -50.09, 3.282, 1.64769, 25.722, 33.78));
        list.add(new SurfaceType(false, 70.145, 6.869, 1.816, 27.63, 46.62));
        list.add(new SurfaceType(false, -33.546, 0.534, 0, 27.63, 0));
        list.add(new SurfaceType(false, 47.987, 6.716, 1.816, 25.722, 46.62));
        list.add(new SurfaceType(false, -42.666, 5.037, 1.62004, 25.722, 36.26));
        list.add(new SurfaceType(false, 30.645, 26.256, 0, 26.026, 0));
        return list;
    }

    // Measured by DM - off website
    private static List<SurfaceType> getSurfaces2() {
        List<SurfaceType> list = new ArrayList<>();

        list.add(new SurfaceType(false, 45.527, 5.419, 1.73430, 37.86, 28.19));
        list.add(new SurfaceType(false, 94.469, 0.229, 0, 36.25, 0));
        list.add(new SurfaceType(false, 25.619, 7.86, 1.67133, 33.2, 41.64));
        list.add(new SurfaceType(false, -451.4844, 3.13, 1.7919, 33.2, 25.55));
        list.add(new SurfaceType(false, 22.769, 5.724, 0, 26.56, 0));
        list.add(new SurfaceType(true, 0, 2.06, 0, 26.1, 0));
        list.add(new SurfaceType(false, -83.844, 2.67, 1.78831, 25.722, 47.7));
        list.add(new SurfaceType(false, -180.816, 3.05, 0, 25.722, 0));
        list.add(new SurfaceType(false, -46.063, 3.13, 1.64769, 25.722, 33.78));
        list.add(new SurfaceType(false, 79.183, 6.793, 1.816, 27.7, 46.62));
        list.add(new SurfaceType(false, -37.255, 0.382, 0, 27.7, 0));
        list.add(new SurfaceType(false, 45.795, 6.564, 1.816, 25.645, 46.62));
        list.add(new SurfaceType(false, -44.038, 4.961, 1.62004, 25.645, 36.26));
        list.add(new SurfaceType(false, 30.437, 25.722, 0, 26.256, 0));
        return list;
    }


    private static OpticalSystem.Builder buildSystem(GlassType[] glassTypes, boolean addPointSource, double field) {
        OpticalSystem.Builder sys = new OpticalSystem.Builder();
        double imageHeight = 43.28;
        double angleOfView = 45.0 / 2.0;
        double fNum = 1.4;
        if (addPointSource) {
            Vector3 direction = Vector3.vector3_001;
            if (field != 0.0) {
                // Construct unit vector at an angle
                //      double z1 = cos (angleOfView);
                //      double y1 = sin (angleOfView);
                //      unit_vector = math::Vector3 (0, y1, z1);
                double effectiveAngle = 45.0 * field;
                double aov = Math.toRadians(effectiveAngle) / 2.0;
                Matrix3 r = Matrix3.get_rotation_matrix(0, aov);
                direction = r.multiply(direction);
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
        List<SurfaceType> surfaces = getSurfaces2();
        int index = 0;
        for (int i = 0; i < surfaces.size(); i++) {
            SurfaceType s = surfaces.get(i);
            double nd = s.nd;
            double vd = s.vd;
            String glassName = null;
            if (nd != 0) {
                nd = glassTypes[index].nd;
                vd = glassTypes[index].vd;
                glassName = glassTypes[index].name;
                index++;
            }
            double thickness = add_surface(lens, s.radius, s.thickness, s.apertureRadius, nd, vd, glassName, s.isStop);
            image_pos += thickness;
        }
        sys.add(lens);
        Image.Builder image = new Image.Builder().position(new Vector3Pair(new Vector3(0, 0, image_pos), Vector3.vector3_001)).curve(Flat.flat).shape(new Rectangle(imageHeight * 2.));
        sys.add(image);
        sys.half_angle_of_view_in_degrees(angleOfView);
        sys.f_number(fNum);
        return sys;
    }

    static GlassType[] getGlassTypes(int surface) {
        switch (surface) {
            case 0:
                return new GlassType[]{
                        new GlassType("S-LAH58", 1.883, 40.77),
                        new GlassType("S-LAH99", 2.001, 29.14),
                        new GlassType("N-LASF55",1.9538,30.56),
                        new GlassType("J-LASFH9A",1.90265,35.77),
                        new GlassType("N-LASF46B",1.90366,31.32),
                };
            case 5:
            case 6:
                return new GlassType[]{
                        new GlassType("S-LAH99", 2.001, 29.14),
                        new GlassType("S-LAH58", 1.883, 40.77),
//                        new GlassType("S-LAH79", 2.0033, 28.27),
                        new GlassType("S-LAH59", 1.816, 46.62),
                        new GlassType("L-LAH85", 1.854, 40.39),
                        new GlassType("S-TIH6", 1.80518, 25.43),
                        new GlassType("S-LAH64",1.788,47.37),
                        new GlassType("S-LAH66", 1.7725, 49.6),
                        new GlassType("S-LAH65", 1.804, 46.57),
                        new GlassType("S-LAH60", 1.834, 37.16),
                        new GlassType("S-TIH53", 1.84666, 23.78),
                        new GlassType("J-LASFH9A",1.90265,35.77),
                        new GlassType("N-LAF33",1.78582,44.05),
                };
            case 1:
                return new GlassType[]{
                        new GlassType("S-FPL51", 1.497, 81.55),
                        new GlassType("N-FK51", 1.48656, 84.47),
                        new GlassType("FCD515", 1.59282, 68.62),
                        new GlassType("FCD705", 1.55032, 75.5),
                        new GlassType("FCD100", 1.437, 95.1),
                        new GlassType("N-FK58",1.456,90.9),
                };
            case 2:
                return new GlassType[]{
                        new GlassType("S-LAH97",1.755,52.32),
                        new GlassType("S-LAH64",1.788,47.37),
                        new GlassType("S-LAH66", 1.7725, 49.6),
                        new GlassType("S-TIM22", 1.64769, 33.78),
                        new GlassType("S-TIM2", 1.62004, 29.57),
                        new GlassType("S-TIM1", 1.62588, 35.7),
                        new GlassType("J-SF10", 1.72825, 36.26),
                        new GlassType("N-SF11", 1.7919, 25.55),
                        new GlassType("N-SF10", 1.7343, 28.19),
                        new GlassType("J-LAK9",1.691,54.93),
                        new GlassType("S-BAL42", 1.58313, 59.37),
                        new GlassType("S-TIH6", 1.80518, 25.43),
                        new GlassType("S-PHM53", 1.603, 65.44),
                        new GlassType("N-KZFS2",1.55836,54.01),
                        new GlassType("N-KZFS4",1.61336,44.49),
                        new GlassType("N-KZFS5",1.65412,39.7),
                        new GlassType("N-KZFS8",1.72047,34.7),
                        new GlassType("J-SF2",1.64769,33.72),
                };
            case 3:
                return new GlassType[]{
                        new GlassType("M-BACD12",1.58313,59.46),
                        new GlassType("M-PCD4",1.61881,63.86),
                        new GlassType("M-PCD51",1.59201,67.02),
                        new GlassType("M-LAC14",1.6968,55.46),
                        new GlassType("M-TAC60",1.75501,51.16),
                        new GlassType("M-TAF401",1.77377,47.17),
                        new GlassType("M-TAF105",1.7725,49.5),
                        new GlassType("M-LAC8",1.713,53.94),
                        new GlassType("M-NBF1",1.7433,49.33),
                };
            case 4:
            case 7:
                return new GlassType[]{
                        new GlassType("S-TIM22", 1.64769, 33.78),
                        new GlassType("S-TIM2", 1.62004, 29.57),
                        new GlassType("S-TIM1", 1.62588, 35.7),
                        new GlassType("J-LAK9",1.691,54.93),
                        new GlassType("S-NBH5", 1.65412, 39.68),
                        new GlassType("N-KZFS2",1.55836,54.01),
                        new GlassType("N-KZFS4",1.61336,44.49),
                        new GlassType("N-KZFS5",1.65412,39.7),
                        new GlassType("N-KZFS8",1.72047,34.7),
                        new GlassType("S-TIM5",1.60342,38.03),
                        new GlassType("N-SK4",1.61272,58.63),
                };
        }
        throw new IllegalArgumentException();
    }

    static GlassType[] getGlassTypes() {
        return new GlassType[]{
                //new GlassType("J-BASF6", 1.67133, 41.64),
                //new GlassType("S-NPH2", 1.92286, 18.9),
                new GlassType("S-LAH99", 2.001, 29.14),
                new GlassType("S-LAH97",1.755,52.32),
                new GlassType("S-LAH64",1.788,47.37),
                new GlassType("S-LAH66", 1.7725, 49.6),
                new GlassType("S-LAH59", 1.816, 46.62),
                new GlassType("L-LAH85", 1.854, 40.39),
                new GlassType("S-LAH58", 1.883, 40.77),
                new GlassType("S-TIM22", 1.64769, 33.78),
                new GlassType("S-TIM2", 1.62004, 29.57),
                new GlassType("S-TIM1", 1.62588, 35.7),
                new GlassType("J-SF10", 1.72825, 36.26),
                new GlassType("N-SF11", 1.7919, 25.55),
                new GlassType("N-SF10", 1.7343, 28.19),
                new GlassType("S-FPL51", 1.497, 81.55),
                new GlassType("N-FK51", 1.48656, 84.47),
                new GlassType("J-LAK9",1.691,54.93),
                //new GlassType("S-NBH5", 1.65412, 39.68),
                //new GlassType("S-BAL42", 1.58313, 59.37),
                //new GlassType("S-TIH53", 1.84666, 23.78),
                new GlassType("S-TIH6", 1.80518, 25.43),
                //new GlassType("S-PHM53", 1.603, 65.44),
                //new GlassType("S-NBH8", 1.72047, 34.71),
                new GlassType("M-TAF401",1.77377,47.17),
                new GlassType("M-PCD51",1.59201,67.02),
                new GlassType("N-KZFS2",1.55836,54.01),

                //new GlassType("L-BSL7", 1.51633, 64.07),
                };
    }

    static final class ProcessSystems implements Runnable {

        GlassType[][] glassTypes;

        public ProcessSystems(GlassType[][] glassTypes) {
            this.glassTypes = glassTypes;
        }

        public void run() {
            var glasses = new GlassType[8];
            double bestRMS = 999.00;
            String bestData = null;
            for (int a = 0; a < glassTypes[0].length; a++) {
                glasses[0] = glassTypes[0][a];
                for (int b = 0; b < glassTypes[1].length; b++) {
                    glasses[1] = glassTypes[1][b];
                    for (int c = 0; c < glassTypes[2].length; c++) {
                        glasses[2] = glassTypes[2][c];
                        for (int d = 0; d < glassTypes[3].length; d++) {
                            glasses[3] = glassTypes[3][d];
                            for (int e = 0; e < glassTypes[4].length; e++) {
                                glasses[4] = glassTypes[4][e];
                                for (int f = 0; f < glassTypes[5].length; f++) {
                                    glasses[5] = glassTypes[5][f];
                                    for (int g = 0; g < glassTypes[6].length; g++) {
                                        glasses[6] = glassTypes[6][g];
                                        for (int h = 0; h < glassTypes[7].length; h++) {
                                            glasses[7] = glassTypes[7][h];
                                            var system = buildSystem(glasses, false, 0.0).build();
                                            //System.out.println(system);
                                            try {
                                                var parax = ParaxialFirstOrderInfo.compute(system);
                                                // Expected H' ppk = 37.5 from front-surface, 20.2 from last surface
                                                // Expected H pp1 = 51.8
                                                // expected H - H1 = 14.3

                                                if (parax.effective_focal_length > 51.0 && parax.effective_focal_length < 52.0
                                                        && parax.back_focal_length > 24.0 && parax.back_focal_length < 27.0) {

//                                                    var sb1 = new StringBuilder();
//                                                    sb1.append(parax.effective_focal_length).append("\t")
//                                                            .append(parax.back_focal_length).append("\t")
//                                                            .append(parax.fno).append("\t");
                                                    //System.out.println(sb1);
                                                    var system2 = buildSystem(glasses, true, 0.0).build();
                                                    var spotAnalysis = new AnalysisSpot(system2, 30);
                                                    try {
                                                        spotAnalysis.process_analysis();
                                                    }
                                                    catch (Exception ign) {
                                                        continue;
                                                    }
                                                    if (spotAnalysis.get_rms_radius() < 850.0) {
                                                        StringBuilder sb = new StringBuilder();
                                                        sb.append(spotAnalysis.get_rms_radius()).append("\t");
                                                        sb.append(parax.effective_focal_length).append("\t")
                                                                .append(parax.back_focal_length).append("\t")
                                                                .append(parax.fno).append("\t");
                                                        for (int i = 0; i < glasses.length; i++)
                                                            sb.append(glasses[i]).append("\t");
                                                        System.out.println(sb.toString());
                                                        if (spotAnalysis.get_rms_radius() < bestRMS) {
                                                            bestRMS = spotAnalysis.get_rms_radius();
                                                            bestData = sb.toString();
                                                        }
                                                    }
                                                }
                                            } catch (Exception ex) {
                                                ex.printStackTrace();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            System.out.println("Best Data");
            System.out.println(bestData);
        }
    }

    public static void main(String[] args) throws Exception {

        GlassType[][] glassTypes = new GlassType[][]{
                getGlassTypes(0),
                getGlassTypes(1),
                getGlassTypes(2),
                getGlassTypes(3),
                getGlassTypes(4),
                getGlassTypes(5),
                getGlassTypes(6),
                getGlassTypes(7),
        };
        var p = new ProcessSystems(glassTypes);
        p.run();
    }
}
