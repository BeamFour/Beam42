// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.specs;

import org.redukti.mathlib.Matrix3;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.parax.Etendue;
import org.redukti.rayoptics.raytr.Wideangle;
import org.redukti.rayoptics.util.Lists;
import org.redukti.rayoptics.util.Pair;
import org.redukti.rayoptics.util.Triple;

import java.util.ArrayList;
import java.util.List;

/**
 * Field of view specification
 *
 *     Attributes:
 *         key: 'object'|'image', 'height'|'angle'
 *         value: maximum field, per the key
 *         fields: list of Field instances
 *         is_relative: if True, `fields` are relative to max field
 *         is_wide_angle: if True, aim at real entrance pupil
 */
public class FieldSpec {

    public OpticalSpecs optical_spec;
    /**
     * 'field', 'object'|'image', 'height'|'angle'
     */
    public SpecKey key;
    /**
     * maximum field, per the key
     */
    public double value;
    /**
     * if True, `fields` are relative to max field
     */
    public boolean is_relative;
    public boolean is_wide_angle;
    /**
     * list of Field instances
     */
    public Field[] fields;
    public String[] index_labels;

    public FieldSpec(OpticalSpecs parent, Pair<ImageKey, ValueKey> key, Double value, double[] flds,
                     Boolean is_relative, Boolean is_wide_angle, Boolean do_init) {
        if (key == null) key = new Pair<>(ImageKey.Object, ValueKey.Angle);
        if (value == null) value = 0.0;
        if (is_relative == null) is_relative = false;
        if (is_wide_angle == null) is_wide_angle = false;
        if (do_init == null) do_init = true;
        optical_spec = parent;
        this.key = new SpecKey(SpecType.Field, key.first, key.second);
        this.value = value;
        this.is_relative = is_relative;
        this.is_wide_angle = is_wide_angle;
        if (do_init) {
            if (flds == null) flds = new double[] { 0., 1.};
            set_from_list(flds);
        }
        else
            this.fields = new Field[0];
    }

    public FieldSpec(OpticalSpecs parent, Pair<ImageKey, ValueKey> key, double[] flds) {
        this(parent, key, 0.0, flds, null, null, null);
    }
    public FieldSpec(OpticalSpecs parent, Pair<ImageKey, ValueKey> key, double[] flds, boolean is_wide_angle) {
        this(parent, key, 0.0, flds, null, is_wide_angle, null);
    }

    private Field[] set_from_list(double[] flds) {
        fields = new Field[flds.length];
        for (int i = 0; i < flds.length; i++) {
            fields[i] = new Field();
            fields[i].y = flds[i];
        }
        value = max_field().first;
        return fields;
    }

    /**
     * return pupil spec as paraxial height or slope value
     */
    public Triple<ImageKey,ValueKey,Double> derive_parax_params() {
        var fov_oi_key = key.imageKey;
        var fov_value_key = key.valueKey;
        // guard against zero as a field spec for parax calc
        var fov_value = this.value != 0 ? this.value : 1.0;
        ValueKey field_key = null;
        double field_value = 0.0;

        if (ValueKey.Angle == fov_value_key) {
            var slope_bar = Etendue.ang2slp(fov_value);
            field_key = ValueKey.Slope;
            field_value = slope_bar;
        }
        else if (ValueKey.Height == fov_value_key) {
            var height_bar = fov_value;
            field_key = ValueKey.Height;
            field_value = height_bar;
        }
        else if (ValueKey.RealHeight == fov_value_key) {
            var height_bar = fov_value;
            field_key = ValueKey.Height;
            field_value = height_bar;
        }
        return new Triple<>(fov_oi_key, field_key, field_value);
    }

    /**
     * Checks for object angles greater than the threshold.
     */
    public boolean check_is_wide_angle(double angle_threshold) {
        is_wide_angle = false;
        if (key.imageKey == ImageKey.Image &&
                key.valueKey == ValueKey.RealHeight &&
            optical_spec.conjugate_type(ImageKey.Object) == ConjugateType.INFINITE) {
            is_wide_angle = true;
        }
        else if (key.imageKey == ImageKey.Object &&
                key.valueKey == ValueKey.Angle) {
            var max_angle = max_field().second;
            is_wide_angle = (max_angle > angle_threshold);
        }
        return is_wide_angle;
    }
    public boolean check_is_wide_angle() {
        return check_is_wide_angle(45.);
    }

    public void update_model() {
        for (Field f : fields) {
            f.update();
        }
        // recalculate max_field and relabel fields.
        //  relabeling really assumes the fields are radial, specifically,
        //  y axis only
        double field_norm;
        if (is_relative)
            field_norm = 1.0;
        else
            field_norm = (value == 0.0) ? 1.0 : 1.0 / value;

        List<String> index_labels = new ArrayList<>();
        for (Field f : fields) {
            String fldx, fldy;
            if (f.x != 0.0)
                fldx = String.format("%5.2fx", field_norm * f.x);
            else
                fldx = "";
            if (f.y != 0.0)
                fldy = String.format("%5.2fy", field_norm * f.y);
            else
                fldy = "";
            index_labels.add(fldx + fldy);
        }
        index_labels.set(0, "axis");
        if (index_labels.size() > 1)
            Lists.set(index_labels, -1, "edge");
        this.index_labels = index_labels.toArray(new String[0]);
    }

    public void apply_scale_factor(double scale_factor) {
        ValueKey value_key = key.valueKey;
        if (value_key == ValueKey.Height) {
            if (!is_relative) {
                for (var f: fields) {
                    f.apply_scale_factor(scale_factor);
                }
            }
            value *= scale_factor;
        }
    }

    /**
     * Return a pt, direction pair characterizing `fld`.
     *
     *         If a field point is defined in image space, the paraxial object
     *         space data is used to calculate the field coordinates.
     */
    public Coord obj_coords(Field fld) {
        ImageKey obj_img_key = key.imageKey;
        ValueKey value_key = key.valueKey;

        Vector3 fld_coord = new Vector3(fld.x, fld.y, 0.0);
        Vector3 rel_fld_coord = new Vector3(fld.x, fld.y, 0.0);
        if (is_relative)
            fld_coord = fld_coord.times(value);
        else if (value != 0.0)
            rel_fld_coord = rel_fld_coord.divide(value);

        var opt_model = optical_spec.opt_model;
        var pr = optical_spec.parax_data.pr_ray;
        var fod = optical_spec.parax_data.fod;
        Vector3 obj_pt = null;
        Vector3 obj_dir = null;

        var obj2enp_dist = -(fod.obj_dist + fod.enp_dist);
        var pt1 = new Vector3(0.0, 0.0, obj2enp_dist);
        ConjugateType obj_conj = optical_spec.conjugate_type(ImageKey.Object);
        if (obj_conj == ConjugateType.INFINITE) {
            // generate 'object', 'angle' fld_spec
            Vector3 fld_angle;
            if (obj_img_key == ImageKey.Image) {
                double max_field_ang;
                if (value_key == ValueKey.RealHeight) {
                    double wvl = optical_spec.wvls.central_wvl();
                    var pkg = Wideangle.eval_real_image_ht(opt_model,fld,wvl);
                    obj_pt = pkg.ray_data.pt;
                    obj_dir = pkg.ray_data.dir;
                    fld.z_enp = pkg.z_enp;
                    return new Coord(obj_pt,obj_dir);
                }
                else {
                    max_field_ang = Math.atan(pr.get(0).slp);
                    fld_angle = rel_fld_coord.times(max_field_ang);
                }
            }
            else {
                if (value_key == ValueKey.Angle) {
                    fld_angle = fld_coord.deg2rad();
                }
                else {
                    obj_pt = fld_coord;
                    obj_dir = pt1.minus(obj_pt).normalize();
                    return new Coord(obj_pt,obj_dir);
                }
            }
            var dir_cos = fld_angle.sin();
            var z = Math.sqrt(1.0 - dir_cos.x * dir_cos.x - dir_cos.y * dir_cos.y);
            dir_cos = new Vector3(dir_cos.x, dir_cos.y, z);
            if (is_wide_angle) {
                var rot_mat = Matrix3.rot_v1_into_v2(Vector3.vector3_001,dir_cos);
                obj_pt = rot_mat.multiply(pt1).minus(pt1);
            }
            else {
                obj_pt = new Vector3(dir_cos.x/dir_cos.z,
                        dir_cos.y/dir_cos.z,0.0).times(obj2enp_dist);
            }
            obj_dir = dir_cos;
        }
        else if (obj_conj == ConjugateType.FINITE) {
            if (obj_img_key == ImageKey.Image) {
                var max_field_ht = pr.get(0).ht;
                obj_pt = rel_fld_coord.times(max_field_ht);
            }
            else {
                if (value_key == ValueKey.Angle) {
                    var fld_angle = fld_coord.deg2rad();
                    obj_dir = fld_angle.sin();
                    var z = Math.sqrt(1.0 - obj_dir.x * obj_dir.x - obj_dir.y * obj_dir.y);
                    obj_dir = new Vector3(obj_dir.x, obj_dir.y, z);
                    obj_pt = new Vector3(obj_dir.x/obj_dir.z, obj_dir.y/obj_dir.z,0.0).times(obj2enp_dist);
                    return new Coord(obj_pt,obj_dir);
                }
                else {
                    obj_pt = fld_coord;
                }
                obj_dir = pt1.minus(obj_pt).normalize();
            }
        }
        return new Coord(obj_pt,obj_dir);
    }

    /**
     * calculates the maximum field of view
     *
     *         Returns:
     *             magnitude of maximum field, maximum Field instance
     */
    public Pair<Double, Integer> max_field() {
        Integer max_fld = null;
        double max_fld_sqrd = -1.0;
        for (int i = 0; i < fields.length; i++) {
            Field f = fields[i];
            double fld_sqrd = f.x * f.x + f.y * f.y;
            if (fld_sqrd > max_fld_sqrd) {
                max_fld_sqrd = fld_sqrd;
                max_fld = i;
            }
        }
        double max_fld_value = Math.sqrt(max_fld_sqrd);
        if (is_relative)
            max_fld_value *= value;
        return new Pair<>(max_fld_value, max_fld);
    }


    /**
     * Reset the vignetting to 0 for all fields.
     */
    public void clear_vignetting() {
        for (var f: fields) {
            f.clear_vignetting();
        }
    }

    @Override
    public String toString() {
        return "FieldSpec(key=" + key + ", max field=" + max_field().first + ", is wide angle=" + is_wide_angle + ")";
    }
}
