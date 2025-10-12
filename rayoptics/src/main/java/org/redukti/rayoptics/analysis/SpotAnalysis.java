// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.analysis;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.GridItem;
import org.redukti.rayoptics.raytr.RayPkg;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.util.Lists;

import java.util.List;

public class SpotAnalysis {

    public static GridItem spot(Vector2 p, int wi, RayPkg ray_pkg, Field fld, double wvl, double foc) {
        if (ray_pkg != null) {
            var image_pt = fld.ref_sphere.image_pt;
            var ray = ray_pkg.ray;
            var dist = foc / Lists.get(ray,-1).d.z;
            var defocussed_pt = Lists.get(ray,-1).d.plus(Lists.get(ray,-1).d.times(dist));
            var t_abr = defocussed_pt.minus(image_pt);
            return new GridItem(t_abr.x,t_abr.y,ray_pkg);
        }
        else
            return null;
    }

    public static List<List<GridItem>> eval_grid(OpticalModel opt_model, int fi, Integer wl, int num_rays, TraceOptions trace_options) {
//        var osp = opt_model.optical_spec;
        var seq_model =  opt_model.seq_model;
//        var t = osp.lookup_fld_wvl_focus(fi,wl,null);
//        var fld = t.first;
//        var wvl = t.second;
//        var foc = t.third;
        return seq_model.trace_grid(SpotAnalysis::spot,fi,wl,num_rays,false,trace_options);
    }
}
