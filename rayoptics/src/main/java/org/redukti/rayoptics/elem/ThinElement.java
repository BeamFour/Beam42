package org.redukti.rayoptics.elem;

import org.redukti.rayoptics.math.Matrix3;
import org.redukti.rayoptics.math.Transform3;
import org.redukti.rayoptics.math.Vector3;
import org.redukti.rayoptics.seq.Gap;
import org.redukti.rayoptics.seq.Interface;
import org.redukti.rayoptics.util.KWArgs;

import java.util.Collections;
import java.util.List;

public class ThinElement implements IElement {

    static int serial_number = 0;

    String label;
    Transform3 tfrm;
    Interface intrfc;
    int intrfc_indx;
    String medium_name;
    double sd;

    public ElementModel parent = null;

    public ThinElement(Interface ifc, Transform3 tfrm, int idx, Double sd, String label) {
        if (label == null) {
            serial_number++;
            this.label = "TL" + serial_number;
        } else
            this.label = label;
        if (tfrm != null)
            this.tfrm = tfrm;
        else
            this.tfrm = new Transform3(Matrix3.IDENTITY, Vector3.ZERO);
        this.intrfc = ifc;
        this.intrfc_indx = idx;
        this.medium_name = "Thin Element";
        if (sd != null)
            this.sd = sd;
        else
            this.sd = ifc.max_aperture;
    }

    @Override
    public Node tree(KWArgs args) {
        String default_tag = "#element#thinlens";
        String tag = default_tag + args.get("tag", "");
        Node tle = new Node("TL", this, tag);
        new Node("tl", intrfc, "#ifc", tle);
        return tle;
    }

    @Override
    public String get_label() {
        return label;
    }

    public List<Interface> interface_list() {
        return List.of(intrfc);
    }

    public List<Gap> gap_list() {
        return Collections.emptyList();
    }

    @Override
    public void set_parent(ElementModel ele_model) {
        this.parent = ele_model;
    }

}
