package org.redukti.jfotoptix.tools;

import org.redukti.jfotoptix.math.MathUtils;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;

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
                double x = x_center + r*MathUtils.cosd(a);
                double y = y_center + r*MathUtils.sind(a);
                rayStarts.add(new Vector3(x,y,z));
            }
        }
        return rayStarts;
    }
}
