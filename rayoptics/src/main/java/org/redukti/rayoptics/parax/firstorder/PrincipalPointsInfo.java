// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.parax.firstorder;

import java.util.List;

public class PrincipalPointsInfo {

    public final List<ParaxComponent> p_ray;
    public final List<ParaxComponent> q_ray;
    public final double power;
    public final double fl_obj;
    public final double fl_img;
    public final double efl;
    public final double pp1;
    public final double ppk;
    public final double ffl;
    public final double bfl;
    public final double pp_sep;

    public PrincipalPointsInfo(
            List<ParaxComponent> p_ray,
            List<ParaxComponent> q_ray,
            double power,
            double efl,
            double fl_obj,
            double fl_img,
            double pp1,
            double ppk,
            double pp_sep,
            double ffl,
            double bfl) {
        this.p_ray = p_ray;
        this.q_ray = q_ray;
        this.power = power;
        this.fl_obj = fl_obj;
        this.fl_img = fl_img;
        this.efl = efl;
        this.pp1 = pp1;
        this.ppk = ppk;
        this.ffl = ffl;
        this.bfl = bfl;
        this.pp_sep = pp_sep;
    }
}
