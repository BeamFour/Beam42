package org.redukti.optim;

import org.redukti.mathlib.Vector2;
import org.redukti.spec.Prescription;

import java.util.List;

public class VarRayDist extends Var {
    public int index;
    public VarRayDist(Prescription prescription, int index, double originalValue, double dDelta) {
        super(prescription, originalValue, dDelta);
        this.index = index;
        var points = prescription._distribution.get_user_defined_points();
        Vector2 point;
        if (points == null || points.size() == 0) {
            point = new Vector2(0, 0);
        }
        else {
            point = points.get(0);
        }
        var newPoint = point.set(index,originalValue);
        prescription._distribution.set_user_defined_points(List.of(newPoint));
    }
    @Override
    public void shift(double delta, boolean scale) {
        var points = prescription._distribution.get_user_defined_points();
        if (points == null || points.size() == 0) {
            throw new RuntimeException("No user defined points");
        }
        var point = points.get(0);
        //System.out.println("Shifting Dist Point[" + index + "] from " + originalValue + " to " + (originalValue+delta));
        var newPoint = point.set(index, originalValue + delta);
        prescription._distribution.set_user_defined_points(List.of(newPoint));
    }

    @Override
    public double get_value() {
        var points = prescription._distribution.get_user_defined_points();
        if (points == null || points.size() == 0) {
            throw new RuntimeException("No user defined points");
        }
        var point = points.get(0);
        //System.out.println("Shifting Dist Point[" + index + "] from " + originalValue + " to " + (originalValue+delta));
        return point.v(index);
    }

    @Override
    public void set_value(double value) {
        var points = prescription._distribution.get_user_defined_points();
        if (points == null || points.size() == 0) {
            throw new RuntimeException("No user defined points");
        }
        var point = points.get(0);
        //System.out.println("Shifting Dist Point[" + index + "] from " + originalValue + " to " + (originalValue+delta));
        var newPoint = point.set(index, value);
        prescription._distribution.set_user_defined_points(List.of(newPoint));
    }

    @Override
    public String toString() {
        var points = prescription._distribution.get_user_defined_points();
        if (points == null || points.size() == 0) {
            throw new RuntimeException("No user defined points");
        }
        var point = points.get(0);
        return "Ray Dist Point [x,y] " + point.toString();
    }
}
