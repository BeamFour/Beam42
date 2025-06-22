package org.redukti.jfotoptix.spec;

import org.redukti.jfotoptix.analysis.AnalysisSpot;
import org.redukti.jfotoptix.curve.Asphere;
import org.redukti.jfotoptix.curve.Flat;
import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.math.Matrix3;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.medium.Abbe;
import org.redukti.jfotoptix.medium.Air;
import org.redukti.jfotoptix.medium.GlassMap;
import org.redukti.jfotoptix.model.Image;
import org.redukti.jfotoptix.model.Lens;
import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.model.PointSource;
import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.jfotoptix.patterns.Distribution;
import org.redukti.jfotoptix.patterns.Pattern;
import org.redukti.jfotoptix.shape.Disk;
import org.redukti.jfotoptix.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

// A format for prescription that is easier to work with when
// trying to optimize
public class Prescription {

    public double focalLength;
    public double fno;
    // The quoted angle of view - e.g. 47 degrees for 50mm
    public double angleOfViewDegrees;
    // For 35mm this is sqrt(36^2 + 24^2) = 43.27
    public double diameterImageCircle;
    public boolean d_line;
    public Distribution distribution;   // FIXME rename, used for ray finding only

    // Used to build
    public List<SurfaceType> surfaceList = new ArrayList<SurfaceType>();
    public SurfaceType[] surfaces;

    public Prescription(double focalLength, double fno, double angleOfViewDegrees, double diameterImageCircle, boolean d_line) {
        this.focalLength = focalLength;
        this.fno = fno;
        this.angleOfViewDegrees = angleOfViewDegrees;
        this.diameterImageCircle = diameterImageCircle;
        this.d_line = d_line;
        this.distribution = new Distribution(Pattern.UserDefined,10, 0.999);
    }

    public Prescription surf(double radius, double thickness, double diameter, double nd, double vd, String glassName) {
        surfaceList.add(new SurfaceType(Integer.toString(surfaceList.size()+1), false, radius, thickness, diameter, nd, vd, glassName));
        return this;
    }
    public Prescription surf(double radius, double thickness, double diameter, double nd, double vd) {
        surfaceList.add(new SurfaceType(Integer.toString(surfaceList.size()+1),false, radius, thickness, diameter, nd, vd, null));
        return this;
    }
    public Prescription surf(double radius, double thickness, double diameter) {
        surfaceList.add(new SurfaceType(Integer.toString(surfaceList.size()+1),false, radius, thickness, diameter, 0, 0, null));
        return this;
    }
    public Prescription stop(double thickness, double diameter) {
        surfaceList.add(new SurfaceType(Integer.toString(surfaceList.size()+1),true,0,thickness,diameter,0,0,null));
        return this;
    }
    public Prescription asph(double k, double[] coeffs) {
        var lastSurface = surfaceList.get(surfaceList.size()-1);
        lastSurface.k = k;
        lastSurface.coeffs = coeffs;
        return this;
    }

    /**
     * The diameter for given field
     */
    public double imageDiameterForField(double field) {
        assert field >= 0 && field <= 1.0;
        return diameterImageCircle*field;
    }

    /**
     * Full angle of view in degrees for given field
     */
//    public double fullAngleOfViewDegrees(double field) {
//        assert field > 0 && field <= 1.0;
//        var radius = imageDiameterForField(field)/2.0;
//        var radians = Math.atan(radius/focalLength);
//        return 2.0*Math.toDegrees(radians);
//    }
    public double fullAngleOfViewDegrees(double field) {
        assert field > 0 && field <= 1.0;
        return Math.toDegrees(Math.atan(Math.tan(Math.toRadians(angleOfViewDegrees/2.0))*field))*2.0;
    }

    public Prescription build() {
        this.surfaces = surfaceList.toArray(new SurfaceType[surfaceList.size()]);
        return this;
    }
    public OpticalSystem.Builder buildSystem(boolean addPointSource, double field) {
        OpticalSystem.Builder sys = new OpticalSystem.Builder();
        if (addPointSource) {
            Vector3 direction = Vector3.vector3_001;
            if (field != 0.0 || angleOfViewDegrees != 0.0) {
                // Construct unit vector at an angle
                //      double z1 = cos (angleOfView);
                //      double y1 = sin (angleOfView);
                //      unit_vector = math::Vector3 (0, y1, z1);
                double effectiveAngle = field != 0? angleOfViewDegrees*field : angleOfViewDegrees;
                double aov = Math.toRadians(effectiveAngle) / 2.0;
                Matrix3 r = Matrix3.get_rotation_matrix(0, aov);
                direction = r.times(direction);
            }
            PointSource.Builder ps = new PointSource.Builder(PointSource.SourceInfinityMode.SourceAtInfinity, direction)
                    .add_spectral_line(SpectralLine.d);
            if (!d_line)
                ps.add_spectral_line(SpectralLine.C)
                    .add_spectral_line(SpectralLine.F);
            sys.add(ps);
        }
        /* anchor lens */
        Lens.Builder lens = new Lens.Builder().position(Vector3Pair.position_000_001);
        double image_pos = 0.0;
        for (int i = 0; i < surfaces.length; i++) {
            var s = surfaces[i];
            double thickness = add_surface(lens, s);
            image_pos += thickness;
        }
        sys.add(lens);
        Image.Builder image = new Image.Builder().position(new Vector3Pair(new Vector3(0, 0, image_pos), Vector3.vector3_001)).curve(Flat.flat).shape(new Rectangle(diameterImageCircle * 2.));
        sys.add(image);
        sys.angle_of_view(this.angleOfViewDegrees);
        sys.f_number(this.fno);
        return sys;
    }
    private static double add_surface(Lens.Builder lens, SurfaceType s) {
        double apertureRadius = s.diameter / 2.0;
        if (s.isStop) {
            lens.add_stop(apertureRadius, s.thickness, true);
            return s.thickness;
        }
        if (s.k != 0 || (s.coeffs != null && s.coeffs.length > 0)) {
            var curve = getAsphere(s);
            var shape = new Disk(apertureRadius);
            if (s.nd != 0.0) {
                var glass = GlassMap.glassByName(s.glassName);
                if (glass != null) {
                    lens.add_surface(curve, shape, s.thickness, glass);
                } else {
                    lens.add_surface(curve, shape, s.thickness, new Abbe(Abbe.AbbeFormula.AbbeVd, s.nd, s.vd, 0.0));
                }
            } else {
                lens.add_surface(curve, shape, s.thickness, Air.air);
            }
        }
        else {
            // Non aspherical
            if (s.nd != 0.0) {
                var glass = GlassMap.glassByName(s.glassName);
                if (glass == null) {
                    lens.add_surface(s.radius, apertureRadius, s.thickness, new Abbe(Abbe.AbbeFormula.AbbeVd, s.nd, s.vd, 0.0));
                } else {
                    lens.add_surface(s.radius, apertureRadius, s.thickness, glass);
                }
            } else {
                lens.add_surface(s.radius, apertureRadius, s.thickness);
            }
        }
        return s.thickness;
    }

    private static Asphere getAsphere(SurfaceType s) {
        double k = s.k + 1.0;
        double a4 = s.coeffs.length > 0 ? s.coeffs[0] : 0.0;
        double a6 = s.coeffs.length > 1 ? s.coeffs[1] : 0.0;
        double a8 = s.coeffs.length > 2 ? s.coeffs[2] : 0.0;
        double a10 = s.coeffs.length > 3 ? s.coeffs[3] : 0.0;
        double a12 = s.coeffs.length > 4 ? s.coeffs[4] : 0.0;
        double a14 = s.coeffs.length > 5 ? s.coeffs[5] : 0.0;
        double a16 = s.coeffs.length > 6 ? s.coeffs[6] : 0.0;
        double a18 = s.coeffs.length > 7 ? s.coeffs[7] : 0.0;
        double a20 = s.coeffs.length > 8 ? s.coeffs[8] : 0.0;
        return new Asphere(s.radius, k, a4, a6, a8, a10, a12, a14, a16, a18, a20);
    }

    public StringBuilder toOptBenchStr(StringBuilder sb) {
        for (SurfaceType surface : surfaceList) {
            surface.toOptBenchStr(sb);
        }
        return sb;
    }
}
