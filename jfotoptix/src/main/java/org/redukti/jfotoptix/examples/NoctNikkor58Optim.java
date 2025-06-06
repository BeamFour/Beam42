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

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

// Takes too long to run for more than about 22 glasses (that takes 2 hrs as well)
public class NoctNikkor58Optim {

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

    // Measured by DM - off 1001 tale 16
    private static List<SurfaceType> getSurfaces() {
        List<SurfaceType> list = new ArrayList<>();

        list.add(new SurfaceType(false, 79.9975, 6.885, 1.8485, 50.4875, 43.8));
        list.add(new SurfaceType(false, 0, 0.1, 0, 50.4875, 0));
        list.add(new SurfaceType(false, 33.737, 9.75, 1.69, 44.832, 54.7));
        list.add(new SurfaceType(false, 70.18675, 1.56, 0, 44.832, 0));
        list.add(new SurfaceType(false, 134.505, 2.87, 1.7783, 42.169, 23.9));
        list.add(new SurfaceType(false, 22.3687, 8.44, 0, 32.128, 0));
        list.add(new SurfaceType(true, 0, 7.95, 0, 31.227, 0));
        list.add(new SurfaceType(false, -23.02418, 1.64, 1.58148, 31.45, 40.9));
        list.add(new SurfaceType(false, 306.553, 8.196, 1.6934, 40.2, 53.3));
        list.add(new SurfaceType(false, -37.555, 0.15, 0, 40.2, 0));
        list.add(new SurfaceType(false, -396.94, 6.147, 1.6516, 39.5, 58.4));
        list.add(new SurfaceType(false, -52.56789, 0.000, 0, 39.5, 0));
        list.add(new SurfaceType(false, 223.8426, 4.016, 1.6217, 38.275, 58.54));
        list.add(new SurfaceType(false, -94.08052, 37.780, 0, 38.275, 0));
        return list;
    }

    // Latest contrib 01 June
    private static List<SurfaceType> getSurfacesContrib() {
        List<SurfaceType> list = new ArrayList<>();

        list.add(new SurfaceType(false, 80.344, 7.042, 1.8485, 50.4875, 43.8));
        list.add(new SurfaceType(false, 0, 0.189, 0, 50.4875, 0));
        list.add(new SurfaceType(false, 33.61, 9.715, 1.69, 44.832, 54.7));
        list.add(new SurfaceType(false, 69.686, 1.476, 0, 44.832, 0));
        list.add(new SurfaceType(false, 129.534, 3.02, 1.7783, 42.169, 23.9));
        list.add(new SurfaceType(false, 22.55, 8.47, 0, 32.128, 0));
        list.add(new SurfaceType(true, 0, 7.9, 0, 31.227, 0));
        list.add(new SurfaceType(false, -23.365, 1.71, 1.58148, 31.45, 40.9));
        list.add(new SurfaceType(false, 311.537, 8.141, 1.6934, 40.2, 53.3));
        list.add(new SurfaceType(false, -38.532, 0.074, 0, 40.2, 0));
        list.add(new SurfaceType(false, -483.703, 6.009, 1.6516, 39.5, 58.4));
        list.add(new SurfaceType(false, -55.749, 0.1, 0, 39.5, 0));
        list.add(new SurfaceType(false, 200.86, 3.894, 1.6217, 38.275, 58.54));
        list.add(new SurfaceType(false, -106.579, 37.780, 0, 38.275, 0));
        return list;
    }


    // Contrib - service manual based
    private static List<SurfaceType> getSurfaces2() {
        List<SurfaceType> list = new ArrayList<>();

        list.add(new SurfaceType(false, 73.77, 7.17, 1.8485, 50.4875, 43.8));
        list.add(new SurfaceType(false, -20000, 0.34, 0, 50.4875, 0));
        list.add(new SurfaceType(false, 33.44, 9.63, 1.69, 44.832, 54.7));
        list.add(new SurfaceType(false, 68.854, 1.6, 0, 44.832, 0));
        list.add(new SurfaceType(false, 137.71, 3.43, 1.7783, 42.169, 23.9));
        list.add(new SurfaceType(false, 25.57, 7.702, 0, 32.128, 0));
        list.add(new SurfaceType(true, 0, 7.771, 0, 31.227, 0));
        list.add(new SurfaceType(false, -24.59, 1.87, 1.58148, 31.45, 40.9));
        list.add(new SurfaceType(false, 304.93, 7.95, 1.6934, 40.2, 53.3));
        list.add(new SurfaceType(false, -39.345, 0.4, 0, 40.2, 0));
        list.add(new SurfaceType(false, -295.1, 5.63, 1.6516, 39.5, 58.4));
        list.add(new SurfaceType(false, -56.6, 0.1, 0, 39.5, 0));
        list.add(new SurfaceType(false, 125.91, 4.09, 1.6217, 38.275, 58.54));
        list.add(new SurfaceType(false, -98.36, 37.78, 0, 38.275, 0));
        return list;
    }

    // modified manually based on Zemax sliders
    private static List<SurfaceType> getSurfaces1() {
        List<SurfaceType> list = new ArrayList<>();

        list.add(new SurfaceType(false, 79.946, 6.885, 1.8485, 50.4875, 43.8));
        list.add(new SurfaceType(false, 0, 0.1, 0, 50.4875, 0));
        list.add(new SurfaceType(false, 33.792, 9.75, 1.69, 44.832, 54.7));
        list.add(new SurfaceType(false, 70.187, 1.56, 0, 44.832, 0));
        list.add(new SurfaceType(false, 134.614, 2.87, 1.7783, 42.169, 23.9));
        list.add(new SurfaceType(false, 22.369, 8.5, 0, 32.128, 0));
        list.add(new SurfaceType(true, 0, 7.9, 0, 31.227, 0));
        list.add(new SurfaceType(false, -23.035, 1.64, 1.58148, 31.45, 40.9));
        list.add(new SurfaceType(false, 306.553, 8.196, 1.6934, 40.2, 53.3));
        list.add(new SurfaceType(false, -37.566, 0.15, 0, 40.2, 0));
        list.add(new SurfaceType(false, -396.814, 6.147, 1.6516, 39.5, 58.4));
        list.add(new SurfaceType(false, -52.568, 0.000, 0, 39.5, 0));
        list.add(new SurfaceType(false, 223.843, 4.016, 1.6217, 38.275, 58.54));
        list.add(new SurfaceType(false, -94.081, 37.780, 0, 38.275, 0));
        return list;
    }


    private static OpticalSystem.Builder buildSystem(List<SurfaceType> surfaces, GlassType[] glassTypes, boolean addPointSource, boolean skew) {
        OpticalSystem.Builder sys = new OpticalSystem.Builder();
        double imageHeight = 43.28;
        double angleOfView = 40.9 / 2.0;
        double fNum = 1.2;
        if (addPointSource) {
            Vector3 direction = Vector3.vector3_001;
            if (skew) {
                // Construct unit vector at an angle
                //      double z1 = cos (angleOfView);
                //      double y1 = sin (angleOfView);
                //      unit_vector = math::Vector3 (0, y1, z1);
                double aov = Math.toRadians(40.9) / 2.0;
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
        sys.angle_of_view(angleOfView);
        sys.f_number(fNum);
        return sys;
    }

    static GlassType[] getGlassTypes() {
        return new GlassType[]{
                new GlassType("J-SF5", 1.6727, 32.19),    // wakamiya J-SF5
                new GlassType("S-TIM22", 1.64769, 33.79),   // US 4,234,242 50mm f1.8   S-TIM22
                new GlassType("S-TIM28", 1.68893, 31.07),  // shimuzu - f1.2 US 3,738,736 S-TIM28
                new GlassType("S-TIM35", 1.69895, 30.13),   // wakamiya S-TIM35
                new GlassType("S-LAL8", 1.713, 53.87),     // shimuzu - f1.2 US 3,738,736 S-LAL8
                new GlassType("J-SF1", 1.71736, 29.57),   // wakamiya J-SF1
                new GlassType("J-SF10", 1.72825, 28.38),   // shimuzu - f1.2 US 3,738,736 J-SF10
                new GlassType("LAC10", 1.72, 50.4),      // shimuzu - f1.2 US 3,738,736  LAC10
                new GlassType("S-TIH3", 1.74, 28.3),      // wakamiya S-TIH3
                new GlassType("S-TIH13", 1.74077, 27.79),   // wakamiya S-TIH13
                new GlassType("S-LAM2", 1.744, 44.78),     // shimuzu - f1.2 US 3,738,736 S-LAM2
                new GlassType("J-SF4", 1.75520, 27.57),   // shimuzu - f1.2 US 3,738,736  J-SF4
                new GlassType("J-LASFH2", 1.76684, 46.78),   // shimuzu - f1.2 US 3,738,736, US 4,234,242 J-LASFH2
                new GlassType("J-LASF016", 1.7725, 46.62),   // wakamiya J-LASF016
                new GlassType("J-SFS3", 1.78470, 26.27),   // shimuzu - f1.2 US 3,738,736 J-SFS3
                new GlassType("TAF4", 1.788, 47.37),   // wakamiya  S-LAH64 TAF4
                new GlassType("J-LASF017", 1.795, 45.31),   // shimuzu - f1.2 US 3,738,736 J-LASF017?
                new GlassType("TAF2", 1.7945, 45.4),  // 50mm f1.8s US 4234242 TAF2
                new GlassType("J-LASF015", 1.8042, 46.52),   // wakamiya TAF3D    LASF015
                new GlassType("J-LASFH22", 1.8485, 43.79),   // wakamiya  J-LASFH22
                new GlassType("J-LASF08A", 1.883, 40.69)
                };
    }

    static GlassType[] getGlassTypesNew() {
        var glasses = GlassMap.glasses.values().stream()
                .filter(e ->e.get_manufacturer().equals("Hikari"))
                .filter(e -> e.get_name().startsWith("E-"))
                .filter(e->e.get_refractive_index(SpectralLine.d) >= 1.64 && e.get_refractive_index(SpectralLine.d) < 1.91)
                .map(e->new GlassType(e.get_name(), e.nd, e.vd))
                .collect(Collectors.toList())
                .toArray(new GlassType[0]);
        return glasses;
    }

    static class SampleData {
        double eff;
        double bf;
        double fno;
        double ppk;
        double pp1;
        GlassType[] glasses;

        SampleData(String line, Map<Double, GlassType> glassTypes) {
            String[] fields = line.split("\t");
            glasses = new GlassType[7];
            eff = Double.parseDouble(fields[0]);
            bf = Double.parseDouble(fields[1]);
            fno = Double.parseDouble(fields[2]);
            ppk = Double.parseDouble(fields[3]);
            pp1 = Double.parseDouble(fields[4]);
            // 5 is air
            for (int i = 6; i < fields.length; i++) {
                Double nd = Double.parseDouble(fields[i]);
                glasses[i - 6] = glassTypes.get(nd);
                if (glasses[i - 6] == null)
                    throw new RuntimeException("No definition found for " + nd);
            }
        }
    }

    private static void setup(GlassType[] glassTypes, int surface, double[] radii) {
        if (surface == radii.length)
            return;
        double[] example = Arrays.copyOf(radii,radii.length);
        if (radii[surface] != 0.0) {
            double unit = radii[surface] * 0.01;
            example[surface] += unit;
            analyse(glassTypes, example);
            setup(glassTypes, surface + 1, example);
            example = Arrays.copyOf(radii, radii.length);
            example[surface] -= unit;
            analyse(glassTypes, example);
            setup(glassTypes, surface + 1, example);
        }
        else {
            setup(glassTypes, surface + 1, example);
        }
    }

    public static void main(String[] args) throws Exception {
        var glasses = getGlassTypes();
        var glassmap = new HashMap<Double, GlassType>();
        for (var g: glasses) {
            glassmap.put(g.nd,g);
        }

        SampleData data = new SampleData("57.99113525147889\t37.77299281169799\t1.2000000000000002\t20.218142439780898\t51.784538421968996\t1.0\t1.795\t1.8485\t1.74\t1.74077\t1.788\t1.7725\t1.795\t",glassmap);
        GlassType[] glassTypes = data.glasses;
        var surfaces = getSurfaces();
        double[] radii = new double[surfaces.size()];
        for (int i = 0; i < radii.length; i++)
            radii[i] = surfaces.get(i).radius;
        // Now try a combination
        setup(glassTypes,0,radii);
    }

    private static void analyse(GlassType[] glassTypes, double[] radii) {
        var surfaces = getSurfaces();
        assert surfaces.size() == radii.length;
        for (int i = 0; i < radii.length; i++) {
            surfaces.get(i).radius = radii[i];
        }
        var sys = buildSystem(surfaces, glassTypes, true, false).build();
        var parax = ParaxialFirstOrderInfo.compute(sys);
        var spotAnalysis = new AnalysisSpot(sys, 10);
        spotAnalysis.process_analysis();
        var nonskew = spotAnalysis.get_rms_radius();
        if (nonskew > 25)
            return;
        sys = buildSystem(surfaces, glassTypes, true, true).build();
        spotAnalysis = new AnalysisSpot(sys, 10);
        spotAnalysis.process_analysis();
        var skewed = spotAnalysis.get_rms_radius();
        if (skewed > 125)
            return;
//        if (parax.effective_focal_length > 57.7 && parax.effective_focal_length < 58.2
//                && parax.back_focal_length > 37.77 && parax.back_focal_length < 37.79
//                && parax.pp1 > 51.75 && parax.pp1 < 51.85 // 51.8 from first surface
//                && parax.ppk > 20.15 && parax.ppk < 20.25) {  // 20.2 from last surface
            StringBuilder sb = new StringBuilder();
            sb.append(nonskew).append("\t");
            sb.append(skewed).append("\t");
            sb.append(parax.effective_focal_length).append("\t")
                    .append(parax.back_focal_length).append("\t")
                    .append(parax.fno).append("\t")
                    .append(parax.ppk).append("\t")
                    .append(parax.pp1).append("\t");
            for (int i = 0; i < glassTypes.length; i++)
                sb.append(glassTypes[i]).append("\t");
            for (int i = 0; i < radii.length; i++)
                sb.append(radii[i]).append("\t");
            System.out.println(sb.toString());
//        }
    }
}
