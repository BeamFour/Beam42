package org.redukti.jfotoptix.parax;

import org.redukti.jfotoptix.curve.Curve;
import org.redukti.jfotoptix.curve.Flat;
import org.redukti.jfotoptix.curve.RotationalRoc;
import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.medium.Medium;
import org.redukti.jfotoptix.model.Element;
import org.redukti.jfotoptix.model.OpticalSurface;
import org.redukti.jfotoptix.model.OpticalSystem;

import java.util.List;

public class YNUTrace {

    public void trace(OpticalSystem system, double l1, double y1) {

        List<Element> seq = system.get_sequence();
        System.out.println(seq);

        double u1 = -y1/l1;
        double y2 = y1;
        for (Element e: seq) {
            if (e instanceof OpticalSurface) {
                y1 = y2;
                OpticalSurface surface = (OpticalSurface) e;
                Medium leftMedium = surface.get_material(0);
                double t1 = surface.get_thickness();
                Medium rightMedium = surface.get_material(1);
                Curve curve1  = surface.get_curve();
                double C1;
                if (curve1 instanceof RotationalRoc) {
                    C1 = ((RotationalRoc)curve1).get_curvature();
                }
                else if (curve1 instanceof Flat) {
                    C1 = 0;
                }
                else {
                    throw new IllegalStateException();
                }
                double n1 = leftMedium.get_refractive_index(SpectralLine.d);
                double n1_ = rightMedium.get_refractive_index(SpectralLine.d);
                double n1_u1_ = -y1 *(n1_ - n1) * C1 + n1*u1;
                y2 = y1 + t1 * (n1_u1_)/n1_;
                u1 = n1_u1_/n1_;
            }
            else {
                throw new IllegalStateException();
            }
        }
        double l = -y1/u1;
        System.out.println(l);
    }

}
