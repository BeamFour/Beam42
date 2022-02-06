package org.redukti.rayoptics.elem;

import org.redukti.rayoptics.math.Matrix3;
import org.redukti.rayoptics.math.Transform3;
import org.redukti.rayoptics.math.Vector3;
import org.redukti.rayoptics.seq.Interface;
import org.redukti.rayoptics.util.KWArgs;

public class ThinElement implements IElement {

    static int serial_number = 0;

    String label;
    Transform3 tfrm;
    Interface intrfc;
    int intrfc_indx;
    String medium_name;
    double sd;

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
}
