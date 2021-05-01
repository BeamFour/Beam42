package org.redukti.jfotoptix.parax;

import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.medium.Air;
import org.redukti.jfotoptix.medium.Medium;
import org.redukti.jfotoptix.model.*;

import java.util.List;

public class YNUTrace {

    public void trace(OpticalSystem system, double object_height, double initial_angle, double object_distance) {

        List<Element> seq = system.get_sequence();
        System.out.println(seq);

        /*
           The implementation below is based on description in
           Modern Optical Engineering, W.J.Smith.
           Section 2.6, Example D.
           Also see section 5.9 in MIL-HDBK-141
         */
        double y1 = object_height - object_distance*initial_angle; // y = height
        double u1 = initial_angle;  // angle
        double y2 = y1;
        for (Element e: seq) {
            if (e instanceof OpticalSurface) {
                y1 = y2; // height on this surface
                OpticalSurface surface = (OpticalSurface) e;
                Medium leftMedium = surface.get_material(0);
                double t1 = surface.get_thickness();
                Medium rightMedium = surface.get_material(1);
                double C1 = surface.get_curve().get_curvature();
                double n1 = leftMedium.get_refractive_index(SpectralLine.d);
                double n1_ = rightMedium.get_refractive_index(SpectralLine.d);
                double n1_u1_ = -y1 * C1 * (n1_ - n1)  + n1*u1; // Eq 57 in MIL-HDBK-141
                // Calculate y for next surface
                y2 = y1 + t1 * (n1_u1_)/n1_;    // Eq 56 in MIL-HDBK-141
                u1 = n1_u1_/n1_; // ray angle
            }
            else {
                continue;
            }
        }
        double l = -y1/u1;
        System.out.println(l);
    }

}
