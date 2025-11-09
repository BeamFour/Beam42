package org.redukti.rayoptics.analysis;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.RayPkg;
import org.redukti.rayoptics.raytr.TraceFanResult;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.rayoptics.raytr.WaveAbr;
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
    public static TraceFanResult eval_opd_fan(OpticalModel opt_model, int fi, Integer wl, int num_rays, TraceOptions trace_options) {
        var seq_model =  opt_model.seq_model;
        return seq_model.trace_fan(WavefrontAberrationAnalysis::opd,fi,wl,num_rays,trace_options);
    }
}
