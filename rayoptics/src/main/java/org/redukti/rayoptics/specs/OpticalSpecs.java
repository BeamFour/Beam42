// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.specs;

import org.redukti.mathlib.M;
import org.redukti.mathlib.Matrix3;
import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.parax.Etendue;
import org.redukti.rayoptics.parax.FirstOrder;
import org.redukti.rayoptics.parax.ParaxData;
import org.redukti.rayoptics.raytr.PupilType;
import org.redukti.rayoptics.raytr.Trace;
import org.redukti.rayoptics.util.Lists;
import org.redukti.rayoptics.util.Pair;
import org.redukti.rayoptics.util.Triple;

/**
 * The OpticalSpecs class holds the optical usage definition of the model.
 * Aperture, field of view, wavelength, and focal position are all aspects of
 * the OpticalSpecs.
 * *
 * The first order properties are calculated and maintained by OpticalSpecs
 * in the parax_data variable. This is an instance of ParaxData that includes
 * the paraxial axial and chief rays, and the FirstOrderData that contains
 * first order properties.
 */
public class OpticalSpecs {

    public static boolean do_aiming_default = true;

    /**
     * Aperture specification
     */
    public PupilSpec pupil;
    /**
     * Field of view specification
     */
    public FieldSpec fov;
    /**
     * Wavelengths
     */
    public WvlSpec wvls;
    /**
     * Focal position
     */
    public FocusRange focus;

    public ParaxData parax_data;
    public OpticalModel opt_model;
    public boolean do_aiming;

    public OpticalSpecs(OpticalModel opt_model) {
        this.opt_model = opt_model;
        this.wvls = new WvlSpec(new WvlWt[]{new WvlWt("d", 1.)}, 0);
        this.pupil = new PupilSpec(this, new Pair<>(ImageKey.Object, ValueKey.EPD), 1.0);
        this.fov = new FieldSpec(this, new Pair<>(ImageKey.Object, ValueKey.Angle), new double[]{0.});
        this.focus = new FocusRange();
        this.parax_data = null;
        this.do_aiming = OpticalSpecs.do_aiming_default;
    }

    public void update_model() {
        wvls.update_model();
        pupil.update_model();
        fov.update_model();
    }

    public void update_optical_properies() {
        var opm = opt_model;
        var sm = opm.seq_model;
        if (sm.get_num_surfaces() > 2) {
            var stop = sm.stop_surface;
            var wvl = wvls.central_wvl();
            var parax_pkg = FirstOrder.compute_first_order(opm,stop,wvl);
            parax_data = parax_pkg;
            if (do_aiming) {
                for (int i = 0; i < fov.fields.length; i++) {
                    Field fld = fov.fields[i];
                    var res = Trace.aim_chief_ray(opt_model, fld, wvl);
                    if (res.first != null)
                        fld.aim_info = res.first;
                    else
                        fld.z_enp = res.second;
                }
            }
        }
    }

    public void apply_scale_factor(double scale_factor) {
        wvls.apply_scale_factor(scale_factor);
        pupil.apply_scale_factor(scale_factor);
        fov.apply_scale_factor(scale_factor);
        focus.apply_scale_factor(scale_factor);
    }

    public Coord obj_coords(Field fld) {
        return fov.obj_coords(fld);
    }

    public FocusRange defocus() {
        return focus;
    }

    /* returns field, wavelength and defocus data
    Args:
        fi (int): index into the field_of_view list of Fields
        wl (int): index into the spectral_region list of wavelengths
        fr (float): focus range parameter, -1.0 to 1.0
    Returns:
        (**fld**, **wvl**, **foc**)

        - **fld** - :class:`Field` instance for field_of_view[fi]
        - **wvl** - wavelength in nm
        - **foc** - focus shift from image interface
    */
    public Triple<Field, Double, Double> lookup_fld_wvl_focus(int fi, Integer wl, Double fr) {
        double wvl;
        if (fr == null)
            fr = 0.0;
        if (wl == null)
            wvl = wvls.central_wvl();
        else
            wvl = wvls.wavelengths[wl];
        Field fld = fov.fields[fi];
        double foc = defocus().get_focus(fr);
        return new Triple<>(fld, wvl, foc);
    }

    public Triple<Field, Double, Double> lookup_fld_wvl_focus(int fi) {
        return lookup_fld_wvl_focus(fi, null, 0.0);
    }


    public ConjugateType conjugate_type(ImageKey space) {
        if (space == null)
            space = ImageKey.Object;
        var seq_model = opt_model.seq_model;
        var conj_type = ConjugateType.FINITE;
        if (space == ImageKey.Object) {
            if (M.is_kinda_big(seq_model.gaps.get(0).thi))
                conj_type = ConjugateType.INFINITE;
        }
        else if (space == ImageKey.Image) {
            if (M.is_kinda_big(seq_model.gaps.get(seq_model.gaps.size()-1).thi))
                conj_type = ConjugateType.INFINITE;
        }
        else
            throw new IllegalArgumentException("Unrecognized value for space " + space);
        return conj_type;
    }

    /**
     * return the refractive indices in object and image space.
     */
    public Pair<Double,Double> obj_img_rindex() {
        var seq_model = opt_model.seq_model;
        var n_obj = Lists.get(seq_model.z_dir,0).value * seq_model.central_rndx(0);
        var n_img = Lists.get(seq_model.z_dir,-1).value * seq_model.central_rndx(-1);
        return new Pair<>(n_obj,n_img);
    }

    /**
     * turn pupil and field specs into ray start specification.
     *
     *         Args:
     *             pupil: aperture coordinates of ray
     *             fld: instance of :class:`~.Field`
     *             pupil_type: controls how `pupil` data is interpreted
     *                 - 'rel pupil': relative pupil coordinates
     *                 - 'aim pt': aim point on pupil plane
     *                 - 'aim dir': aim direction in object space
     */
    public Coord ray_start_from_osp(double[] pupil, Field fld, PupilType pupil_type) {
        var pupil_oi_key = this.pupil.key.imageKey;
        var pupil_value_key = this.pupil.key.valueKey;
        var pupil_value = this.pupil.value;
        var obj_img_rindx = obj_img_rindex();
        var n_obj = obj_img_rindx.first;
        var n_img = obj_img_rindx.second;
        Coord coord = obj_coords(fld);
        var p0 = coord.pt;
        var d0 = coord.dir;

        var opt_model = this.opt_model;
        var fod = parax_data.fod;
        // if image space specification, swap in the corresponding first order
        // object space parameter
        if (pupil_oi_key == ImageKey.Image) {
            if (Math.abs(fod.m) < 1e-10) { // infinite object distance
                if (pupil_value_key == ValueKey.EPD)
                    pupil_value = 2.0 * fod.enp_radius;
                else {
                    pupil_value_key = ValueKey.EPD;
                    pupil_value = 2.0 * fod.enp_radius;
                }
            }
            else { // finite conjugate
                if (Math.abs(fod.enp_dist) > 1e10) { // telecentric entrance pupil
                    pupil_value_key = ValueKey.NA;
                    var slp0 = Etendue.na2slp_parax(fod.obj_na, n_obj);
                    pupil_value = Etendue.slp2na(slp0, n_obj);
                }
                else {
                    pupil_value_key = ValueKey.EPD;
                    pupil_value = 2.0 * fod.enp_radius;
                }
            }
        }

        Vector3 pt0 = null;
        Vector3 dir0 = null;
        var z_enp = fod.enp_dist;
        // generate starting pt and dir depending on whether the pupil spec is
        // spatial or angular
        if (ValueKey.EPD == pupil_value_key) {
            Vector3 pt1 = null;
            if (pupil_type == PupilType.AIM_PT) {
                pt0 = p0;
                pt1 = new Vector3(pupil[0], pupil[1], fod.obj_dist + z_enp);
            }
            else {
                var eprad = pupil_value / 2.0;
                if (fov.is_wide_angle) {
                    // transform pupil_pt, in direction coords into surf#1 coordinates
                    var pupil_pt = new Vector3(pupil[0], pupil[1], 0.0).times(eprad);
                    var rot_mat_d2s = Matrix3.rot_v1_into_v2(d0, Vector3.vector3_001);
                    pt1 = rot_mat_d2s.multiply(pupil_pt);
                    if (fld.z_enp != null)
                        z_enp = fld.z_enp;
                    var obj2enp_dist = -(fod.obj_dist + z_enp);
                    // rotate the on-axis object pt into the incident direction
                    // and then position wrt z_enp
                    var enp_pt = new Vector3(0.0, 0.0, obj2enp_dist);
                    var rot_mat_s2d = Matrix3.rot_v1_into_v2(Vector3.vector3_001, d0);
                    pt0 = rot_mat_s2d.multiply(enp_pt).minus(enp_pt);
                    pt1 = new Vector3(pt1.x, pt1.y, pt1.z - obj2enp_dist);
                }
                else {
                    var aim_pt = fld.aim_info;
                    var obj2enp_dist = -(fod.obj_dist + z_enp);
                    pt1 = new Vector3(eprad*pupil[0]+aim_pt[0],
                                    eprad*pupil[1]+aim_pt[1],
                                    fod.obj_dist+z_enp);
                    pt0 = new Vector3(d0.x/d0.z,d0.y/d0.z, 0.0).times(obj2enp_dist);
                }
            }
            dir0 = pt1.minus(pt0).normalize();
        }
        else { //an angular based measure
            double[] dir_tot;
            if (pupil_type == PupilType.AIM_DIR) {
                dir_tot = pupil;
                pt0 = p0;
            }
            else {
                double slope;
                double[] pupil_dir;
                if (pupil_value_key == ValueKey.NA) {
                    double n;
                    if (pupil_oi_key == ImageKey.Object)
                        n = n_obj;
                    else
                        n = n_img;
                    var na = pupil_value;
                    var sin_ang = na / n;
                    pupil_dir = new Vector2(pupil[0],pupil[1]).times(sin_ang).as_array();
                }
                else if (pupil_value_key == ValueKey.Fnum) {
                    var fno = pupil_value;
                    slope = -1.0 / (2.0 * fno);
                    var hypt = Math.sqrt(1.0 + (pupil[0] * slope) * (pupil[0] * slope) + (pupil[1] * slope) * (pupil[1] * slope));
                    pupil_dir = new double[]{slope * pupil[0] / hypt, slope * pupil[1] / hypt};
                }
                else
                    throw new IllegalArgumentException("Invalid pupil value: " + pupil_value_key);
                pt0 = p0;
                double[] cr_dir;
                if (d0 != null) {
                    cr_dir = new double[]{d0.x,d0.y};
                }
                else {
                    var aim_pt = fld.aim_info;
                    var pt1 = new Vector3(aim_pt[0], aim_pt[1],
                                    fod.obj_dist+fod.enp_dist);
                    var diff = pt1.minus(p0).normalize();
                    cr_dir = new double[]{diff.x,diff.y};
                }
                dir_tot = new double[] {pupil_dir[0] + cr_dir[0],
                        pupil_dir[1] + cr_dir[1]};
            }
            dir0 = new Vector3(dir_tot[0], dir_tot[1],
                             Math.sqrt(1.0 - dir_tot[0]*dir_tot[0] - dir_tot[1]*dir_tot[1]));
        }
        return new Coord(pt0,dir0);
    }

    public WvlSpec spectral_region() {
        return wvls;
    }
    public FieldSpec field_of_view() {
        return fov;
    }
}
