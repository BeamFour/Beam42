package org.redukti.jfotoptix.parax;

import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.medium.Medium;
import org.redukti.jfotoptix.model.Element;
import org.redukti.jfotoptix.model.OpticalSurface;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class YNUTrace {

    public Map<Integer, Vector3> trace(List<OpticalSurface> seq, double object_height, double initial_angle, double object_distance) {
        Map<Integer, Vector3> tracedata = new LinkedHashMap<>();
        /*
           The implementation below is based on description in
           Modern Optical Engineering, W.J.Smith.
           Section 2.6, Example D.
           Also see section 5.9 in MIL-HDBK-141
         */
        double y1 = object_distance != 0.0 ?
                object_height + object_distance*initial_angle:
                object_height; // y = height
        double y1_ = y1;
        double u1 = initial_angle;  // angle
        double y2 = y1;
        double aoi = 0.0;
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
                double n1_u1_ = -y1 * C1 * (n1_ - n1)  + n1*u1; // Eq 57 in MIL-HDBK-141,, Eq 2.31 in MOE
                // Calculate y for next surface
                y2 = y1 + t1 * (n1_u1_)/n1_;    // Eq 56 in MIL-HDBK-141, Eq 2.32 in MOE
                u1 = n1_u1_/n1_; // ray angle
                aoi = u1 + y1 * C1; // Eq 1.51 in handbook of Optical Dsign
                double power = surface.power(SpectralLine.d);
                System.out.println("id="+ e.id() + new Vector3(y1, u1, aoi).toString());
                tracedata.put(e.id(), new Vector3(y1, u1, aoi));
            }
            else {
                continue;
            }
        }
        // FIXME we should add this against the image surface
        tracedata.put(0, new Vector3(y2, u1, aoi)); // For the image plane, but perhaps we should make Image an OpticalSurface
        //double l = -y1/u1;
        //System.out.println(l);
        //System.out.println(-1.0/u1);
        System.out.println("Image id=0,"+tracedata.get(0));
        return tracedata;
    }

}
