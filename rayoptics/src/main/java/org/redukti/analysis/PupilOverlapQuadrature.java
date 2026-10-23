// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.analysis;

import org.redukti.mathlib.GaussQ;
import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.raytr.GaussianQuadraturePoint;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.util.Orientation;

import java.util.ArrayList;
import java.util.List;

/** Fixed-count area quadrature over P intersect (P - shift), for an axis-aligned shear.
 * P is the unit disk mapped by the field's piecewise x and y vignetting scales.
 */
public class PupilOverlapQuadrature {
    public record Rule(List<GaussianQuadraturePoint> points, double area, double pupilArea) {
        public double overlapFraction() { return area / pupilArea; }
    }

    public static Rule generate(Field field, int orientation, double shift, int rings, Integer spokes) {
        Orientation.checked(orientation);
        int across = spokes == null ? 4 * (rings + 1) : spokes;
        if (rings < 1 || across < 3 || !Double.isFinite(shift))
            throw new IllegalArgumentException("Overlap quadrature needs finite shear, rings >= 1 and spokes >= 3");
        boolean x = orientation == Orientation.SAGITTAL;
        double aLow = x ? field.vignetting_scale_x(-1) : field.vignetting_scale_y(-1);
        double aHigh = x ? field.vignetting_scale_x(1) : field.vignetting_scale_y(1);
        double bLow = x ? field.vignetting_scale_y(-1) : field.vignetting_scale_x(-1);
        double bHigh = x ? field.vignetting_scale_y(1) : field.vignetting_scale_x(1);
        for (double scale : new double[]{aLow, aHigh, bLow, bHigh})
            if (!(scale > 0) || !Double.isFinite(scale))
                throw new IllegalArgumentException("Vignetted pupil scales must be finite and positive");
        double width = aLow + aHigh;
        if (Math.abs(shift) >= width)
            throw new IllegalArgumentException("The requested contrast shear has no pupil overlap");

        // At transverse coordinate b*sin(theta), a pupil slice is
        // [-aLow*cos(theta), aHigh*cos(theta)]. Intersect it with its shifted copy.
        double thetaMax = Math.acos(Math.abs(shift) / width);
        var alongRule = GaussQ.gauss_legendre_nodes_and_weights(rings);
        var points = new ArrayList<GaussianQuadraturePoint>(rings * across);
        double area = 0;
        for (int half = 0; half < 2; half++) {
            int count = half == 0 ? across / 2 : across - across / 2;
            var transverseRule = GaussQ.gauss_legendre_nodes_and_weights(count);
            double b = half == 0 ? bLow : bHigh;
            double sign = half == 0 ? -1 : 1;
            for (int j = 0; j < count; j++) {
                double theta = thetaMax * (1 + transverseRule[0][j]) / 2;
                double cos = Math.cos(theta);
                double transverse = sign * b * Math.sin(theta);
                double low = -aLow * cos + Math.max(0, -shift);
                double high = aHigh * cos - Math.max(0, shift);
                double length = high - low;
                for (int i = 0; i < rings; i++) {
                    double along = (low + high + length * alongRule[0][i]) / 2;
                    double weight = length / 2 * alongRule[1][i]
                            * b * cos * thetaMax / 2 * transverseRule[1][j];
                    points.add(new GaussianQuadraturePoint(
                            x ? new Vector2(along, transverse) : new Vector2(transverse, along), weight));
                    area += weight;
                }
            }
        }
        if (!(area > 0) || !Double.isFinite(area))
            throw new IllegalArgumentException("The pupil overlap has no finite positive area");
        var normalized = new ArrayList<GaussianQuadraturePoint>(points.size());
        for (var point : points)
            normalized.add(new GaussianQuadraturePoint(point.pupil(), point.weight() / area));
        return new Rule(List.copyOf(normalized), area, Math.PI * width * (bLow + bHigh) / 4);
    }
}
