package org.redukti.rayoptics.analysis;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.*;
import org.redukti.rayoptics.specs.Field;

public class WavefrontAberrationAnalysis {

    public static Double opd(OpticalModel opt_model, Vector2 p, int xy, RayPkg ray_pkg, Field fld, double wvl, double foc) {
        var convert_to_waves = 1.0/opt_model.nm_to_sys_units(wvl);
        if (ray_pkg.ray != null) {
            var fod = opt_model.optical_spec.parax_data.fod;
            var ops = WaveAbr.wave_abr_full_calc(fod, fld, wvl, foc, ray_pkg,
                                         fld.chief_ray, fld.ref_sphere);
            return convert_to_waves * ops;
        }
        return null;
    }
    public static TraceFanResult eval_opd_fan(OpticalModel opt_model, int fi, int xy, int num_rays, TraceOptions trace_options) {
        var seq_model =  opt_model.seq_model;
        return seq_model.trace_fan(WavefrontAberrationAnalysis::opd,fi,xy,num_rays,trace_options).setFanType(RayFanType.OpticalPathDifference);
    }
    public static RayAberrationResult eval(OpticalModel opt_model, int num_rays, TraceOptions trace_options) {
        RayAberrationResult result = new RayAberrationResult();
        var fov = opt_model.optical_spec.fov;
        for (int fi = 0; fi < fov.fields.length; fi++) {
            for (int xy = 0; xy < 2; xy++) {
                result.add(eval_opd_fan(opt_model,fi,xy,num_rays,trace_options));
            }
        }
        return result;
    }
}
