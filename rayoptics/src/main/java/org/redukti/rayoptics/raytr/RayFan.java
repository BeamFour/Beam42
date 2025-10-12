// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.specs.OpticalSpecs;
import org.redukti.rayoptics.util.Pair;

import java.util.List;

/**
 * A fan of rays across the pupil at the given field and wavelength.
 * <p>
 * Attributes:
 * opt_model: :class:`~.OpticalModel` instance
 * f: index into :class:`~.FieldSpec` or a :class:`~.Field` instance
 * wl: wavelength (nm) to trace the fan, or central wavelength if None
 * foc: focus shift to apply to the results
 * image_pt_2d: image offset to apply to the results
 * num_rays: number of samples along the fan
 * xyfan: 'x' or 'y', specifies the axis the fan is sampled on
 */
public class RayFan {

    public OpticalModel opt_model;
    public Field fld;
    public double wvl;
    public double foc;
    public Vector2 image_pt_2d;
    public int num_rays;
    public int xyfan;
    String output_filter;
    String rayerr_filter;
    Pair<List<RayFanItem>, List<WaveAbrPreCalc>> fan_pkg;
    List<Pair<Vector2, Vector3>> fan;

    public RayFan(OpticalModel opt_model, Field f, Double wl, Double foc, Vector2 image_pt_2d,
                  int num_rays, String xyfan) {
        this.opt_model = opt_model;
        OpticalSpecs osp = opt_model.optical_spec;
        this.fld = f;
        this.wvl = wl == null ? osp.spectral_region.central_wvl() : wl;
        this.foc = foc == null ? osp.defocus().focus_shift : foc;
        this.image_pt_2d = image_pt_2d;
        this.num_rays = num_rays;

        if ("x".equals(xyfan))
            this.xyfan = 0;
        else if ("y".equals(xyfan))
            this.xyfan = 1;
        else
            this.xyfan = Integer.parseInt(xyfan);

        // TODO
        //this.output_filter =
        //this.rayerr_filter =

        update_data("rebuild");
    }

    void update_data(String build) {
//        if ("rebuild".equals(build)) {
//            fan_pkg = Analysis.trace_fan(
//                    opt_model, fld, wvl, foc, xyfan,
//                    image_pt_2d, num_rays, output_filter,
//                    rayerr_filter);
//        }
//        fan = Analysis.focus_fan(opt_model, fan_pkg, fld, wvl, foc,
//                image_pt_2d);
    }
}
