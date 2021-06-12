package org.redukti.jfotoptix.parax;

import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.medium.Medium;
import org.redukti.jfotoptix.model.Element;
import org.redukti.jfotoptix.model.OpticalSurface;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class YNUTrace {

    /**
     * Implementation of paraxial ray trace. The ray height y, slope angle u, and angle of incidence are
     * recorded at each surface and returned via a map.
     *
     * @param seq A sequence of OpticalSurface elements
     * @param object_height The starting point of the ray in terms of the object height
     * @param initial_slope_angle The slope angle - the angle by which ray is rotated to reach the axis
     * @param object_distance The distance to the object from first surface, note that first surface is at z=0.
     *                        Hence if object is to left this distance will be negative. If 0, then object_height value is taken
     *                        to be the initial height y where the ray intersects the first surface.
     * @return A map of element id to the values
     */
    public Map<Integer, YNUTraceData> trace(List<OpticalSurface> seq, double object_height, double initial_slope_angle, double object_distance) {
        Map<Integer, YNUTraceData> tracedata = new LinkedHashMap<>();
        /*
           The implementation below is based on description in
           Modern Optical Engineering, W.J.Smith.
           Section 2.6, Example D.
           Also see section 5.9 in MIL-HDBK-141
         */
        double y1 = object_distance != 0.0 ?
                object_height + object_distance*initial_slope_angle: // Note object_distance will usually be negative
                object_height; // y = height
        double u1 = initial_slope_angle;  // angle
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
                //double power = surface.power(SpectralLine.d);
                YNUTraceData data = new YNUTraceData(y1, u1, aoi);
                System.out.println("id="+ e.id() + data);
                tracedata.put(e.id(), data);
            }
            else {
                continue;
            }
        }
        // FIXME we should add this against the image surface
        tracedata.put(0, new YNUTraceData(y2, u1, aoi)); // For the image plane, but perhaps we should make Image an OpticalSurface
        //double l = -y1/u1;
        //System.out.println(l);
        //System.out.println(-1.0/u1);
        System.out.println("Image id=0,"+tracedata.get(0));
        return tracedata;
    }

}
