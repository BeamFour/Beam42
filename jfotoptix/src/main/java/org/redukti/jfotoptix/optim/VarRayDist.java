package org.redukti.jfotoptix.optim;

import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.spec.Prescription;

import java.util.List;

public class VarRayDist extends Var {
    public int index;
    public VarRayDist(Prescription prescription, int index, double originalValue, double dDelta) {
        super(prescription, originalValue, dDelta);
        this.index = index;
        var points = prescription.distribution.get_user_defined_points();
        Vector2 point;
        if (points == null || points.size() == 0) {
            point = new Vector2(0, 0);
        }
        else {
            point = points.get(0);
        }
        var newPoint = point.set(index,originalValue);
        prescription.distribution.set_user_defined_points(List.of(newPoint));
    }
    @Override
    public void shift(double delta) {
        var points = prescription.distribution.get_user_defined_points();
        if (points == null || points.size() == 0) {
            throw new RuntimeException("No user defined points");
        }
        var point = points.get(0);
        System.out.println("Shifting Dist Point[" + index + "] from " + originalValue + " to " + (originalValue+delta));
        var newPoint = point.set(index, originalValue + delta);
        prescription.distribution.set_user_defined_points(List.of(newPoint));
    }
    @Override
    public String toString() {
        var points = prescription.distribution.get_user_defined_points();
        if (points == null || points.size() == 0) {
            throw new RuntimeException("No user defined points");
        }
        var point = points.get(0);
        return "Ray Dist Point " + point.toString();
    }
}
