// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.specs.ReadOnlyField;

import java.util.Collections;
import java.util.List;

/**
 * Ray and optical path length, plus wavelength
 */
public class RayPkg {
    /**
     * List of RaySegs
     */
    public final List<RaySeg> ray;
    /**
     * optical path length between pupils
     */
    public final double op_delta;
    /**
     * wavelength (in nm) that the ray was traced in
     */
    public final double wvl;
    /**
     *  readonly coy of the field as at the time of tracing
     */
    public final ReadOnlyField fld;
    /**
     * Input pupil
     */
    public final Vector2 input_pupil;
    /**
     * Vignetted pupil
     */
    public final Vector2 vig_pupil;

    public RayPkg(List<RaySeg> ray, double op_delta, double wvl) {
        this(ray,op_delta,wvl,null,null,null);
    }

    public RayPkg(List<RaySeg> ray, double op_delta, double wvl,Field fld,Vector2 input_pupil,Vector2 vig_pupil) {
        this.ray = Collections.unmodifiableList(ray);
        this.op_delta = op_delta;
        this.wvl = wvl;
        this.fld = fld != null ? new ReadOnlyField(fld) : null; // Find ways to avoid making copies
        this.input_pupil = input_pupil;
        this.vig_pupil = vig_pupil;
    }

    public RayPkg with(Field fld,Vector2 input_pupil,Vector2 vig_pupil) {
        return new RayPkg(this.ray,this.op_delta,this.wvl,fld,input_pupil,vig_pupil);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" +
                "ray=" + ray +
                ", op_delta=" + op_delta +
                ", wvl=" + wvl +
                ')';
    }
}
