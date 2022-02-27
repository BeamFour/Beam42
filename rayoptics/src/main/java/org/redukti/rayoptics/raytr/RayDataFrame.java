package org.redukti.rayoptics.raytr;


import org.redukti.mathlib.Vector3;

import java.util.List;

public class RayDataFrame {
    public final Vector3[] inc_pt;
    public final Vector3[] after_dir;
    public final double[] after_dst;
    public final Vector3[] normal;

    public RayDataFrame(List<RaySeg> raySegList) {
        int size = raySegList.size();
        inc_pt = new Vector3[size];
        after_dir = new Vector3[size];
        after_dst = new double[size];
        normal = new Vector3[size];
        for (int i = 0; i < raySegList.size(); i++) {
            RaySeg raySeg = raySegList.get(i);
            inc_pt[i] = raySeg.p;
            after_dir[i] = raySeg.d;
            after_dst[i] = raySeg.dst;
            normal[i] = raySeg.nrml;
        }
    }

}
