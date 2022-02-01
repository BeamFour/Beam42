package org.redukti.rayoptics.specs;

import org.redukti.rayoptics.math.Vector3;
import org.redukti.rayoptics.parax.FirstOrderData;
import org.redukti.rayoptics.util.Lists;
import org.redukti.rayoptics.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Field of view specification. The FieldSpec can be defined in object or image space.
 * The defining parameters can be height or angle, where angle is given in degrees.
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
    /**
     * list of Field instances
     */
    public Field[] fields;
    public String[] index_labels;

    public FieldSpec(OpticalSpecs parent, Pair<String, String> key, double value, double[] flds,
                     boolean is_relative, boolean do_init) {
        optical_spec = parent;
        this.key = new SpecKey("field", key.first, key.second);
        this.value = value;
        this.is_relative = is_relative;
        this.fields = do_init ? set_from_list(flds) : new Field[0];
    }

    public FieldSpec(OpticalSpecs parent) {
        this(parent, new Pair<>("object", "angle"), 0.0, new double[]{0.0}, false, true);
    }

    public FieldSpec(OpticalSpecs parent, Pair<String, String> key, double[] flds) {
        this(parent, key, 0.0, flds, false, true);
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
     * calculates the maximum field of view
     *
     * @return magnitude of maximum field, maximum Field instance
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
        return new Pair(max_fld_value, max_fld);
    }

    /**
     * calculates object coordinates
     *
     * @param fld Field
     * @return Vector3
     */
    public Vector3 obj_coords(Field fld) {
        Vector3 fld_coord = new Vector3(fld.x, fld.y, 0.0);
        if (is_relative)
            fld_coord = fld_coord.times(value);

        String field = key.type;
        String obj_img_key = key.imageKey;
        String value_key = key.valueKey;

        Vector3 obj_pt = null;

        FirstOrderData fod = optical_spec.parax_data.fod;
        if (obj_img_key.equals("object")) {
            if (value_key.equals("angle")) {
                Vector3 dir_tan = fld_coord.deg2rad().tan();
                obj_pt = dir_tan.times(fod.obj_dist + fod.enp_dist).negate();
            } else if (value_key.equals("height")) {
                obj_pt = fld_coord;
            }
        } else if (obj_img_key.equals("image")) {
            if (value_key.equals("height")) {
                Vector3 img_pt = fld_coord;
                obj_pt = img_pt.times(fod.red);
            }
        }
        return obj_pt;
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

}
