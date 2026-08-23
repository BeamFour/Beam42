package org.redukti.exporters;

import org.redukti.mathlib.M;
import org.redukti.mathlib.Vector3;

import java.util.ArrayList;
import java.util.List;

public class RayGenerator {

    public static List<Vector3> makeCircularRayStarts(double x_center, double y_center, double z, double R, int ncircles) {
        List<Vector3> rayStarts = new ArrayList<>();

        rayStarts.add(new Vector3(x_center,y_center,z));
        for (int icirc=1; icirc<=ncircles; icirc++)
        {
            double daz = 60.0 / icirc;
            double offset = (icirc%2 == 0) ? 0.0 : 0.5*daz;
            double r = icirc * R / ncircles;
            for (int jaz = 0; jaz<6*icirc; jaz++)
            {
                double a = offset + jaz*daz;
                double x = x_center + r*M.cosd(a);
                double y = y_center + r*M.sind(a);
                rayStarts.add(new Vector3(x,y,z));
            }
        }
        return rayStarts;
    }
}
