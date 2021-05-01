package org.redukti.jfotoptix.parax;

import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.medium.Air;
import org.redukti.jfotoptix.medium.Medium;
import org.redukti.jfotoptix.model.*;

import java.util.List;

public class YNUTrace {

    public void trace(OpticalSystem system, double initial_height, double initial_angle, double t0) {

        List<Element> seq = system.get_sequence();
        System.out.println(seq);

        /*
           The implementation below is based on description in
           Modern Optical Engineering, W.J.Smith.
           Section 2.6, Example D.
         */
        //double t0 = -1e10;
        //double l1 = -300;
        double y1 = initial_height - t0*initial_angle;
        double u1 = initial_angle; // -y1/l1;
        double y2 = y1;
        for (Element e: seq) {
            if (e instanceof Stop) {
                y1 = y2;
                Stop surface = (Stop) e;
                Medium leftMedium = Air.air;
                double t1 = surface.get_thickness();
                Medium rightMedium = Air.air;
                double C1 = 0.0; //surface.get_curve().get_curvature();
                double n1 = leftMedium.get_refractive_index(SpectralLine.d);
                double n1_ = rightMedium.get_refractive_index(SpectralLine.d);
                double n1_u1_ = -y1 *(n1_ - n1) * C1 + n1*u1;
                y2 = y1 + t1 * (n1_u1_)/n1_;
                u1 = n1_u1_/n1_;
            }
            else if (e instanceof OpticalSurface) {
                y1 = y2;
                OpticalSurface surface = (OpticalSurface) e;
                Medium leftMedium = surface.get_material(0);
                double t1 = surface.get_thickness();
                Medium rightMedium = surface.get_material(1);
                double C1 = surface.get_curve().get_curvature();
                double n1 = leftMedium.get_refractive_index(SpectralLine.d);
                double n1_ = rightMedium.get_refractive_index(SpectralLine.d);
                double n1_u1_ = -y1 *(n1_ - n1) * C1 + n1*u1;
                y2 = y1 + t1 * (n1_u1_)/n1_;
                u1 = n1_u1_/n1_;
            }
            else {
                continue;
            }
        }
        double l = -y1/u1;
        System.out.println(l);
    }

}
