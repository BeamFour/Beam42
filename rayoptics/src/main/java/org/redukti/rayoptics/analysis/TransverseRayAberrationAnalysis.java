package org.redukti.rayoptics.analysis;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.RayFanType;
import org.redukti.rayoptics.raytr.RayPkg;
import org.redukti.rayoptics.raytr.TraceFanResult;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.util.Lists;

public class TransverseRayAberrationAnalysis {

    public static Double ray_abr(OpticalModel opt_model, Vector2 p, int xy, RayPkg ray_pkg, Field fld, double wvl, double foc) {
        if (ray_pkg.ray != null) {
            var image_pt = fld.ref_sphere.image_pt;
            var ray = ray_pkg.ray;
            var dist = foc / Lists.get(ray,-1).d.z;
            var defocused_pt = Lists.get(ray,-1).p.plus(Lists.get(ray,-1).d.times(dist));
            var t_abr = defocused_pt.minus(image_pt);
            return t_abr.v(xy);
        }
        return null;
    }

    public static TraceFanResult eval_abr_fan(OpticalModel opt_model,int fi,int xy,int num_rays,TraceOptions trace_options) {
        var seq_model =  opt_model.seq_model;
        return seq_model.trace_fan(TransverseRayAberrationAnalysis::ray_abr,fi,xy,num_rays,trace_options).setFanType(RayFanType.TransverseRayFan);
    }

    public static RayAberrationResult eval(OpticalModel opt_model, int num_rays, TraceOptions trace_options) {
        RayAberrationResult result = new RayAberrationResult();
        var fov = opt_model.optical_spec.fov;
        for (int fi = 0; fi < fov.fields.length; fi++) {
            for (int xy = 0; xy < 2; xy++) {
                result.add(eval_abr_fan(opt_model,fi,xy,num_rays,trace_options));
            }
        }
        return result;
    }
}
