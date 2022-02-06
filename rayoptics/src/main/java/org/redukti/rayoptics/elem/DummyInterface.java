package org.redukti.rayoptics.elem;

import org.redukti.rayoptics.math.Matrix3;
import org.redukti.rayoptics.math.Transform3;
import org.redukti.rayoptics.math.Vector3;
import org.redukti.rayoptics.seq.Interface;
import org.redukti.rayoptics.util.KWArgs;

public class DummyInterface implements IElement {
    static int serial_number = 0;

    String label;
    Transform3 tfrm;
    Interface ref_ifc;
    int idx;
    String medium_name;
    double sd;

    public DummyInterface(Interface ifc, Double sd, Transform3 tfrm, int idx, String label) {
        if (label == null) {
            serial_number++;
            this.label = "D" + serial_number;
        } else
            this.label = label;
        if (tfrm != null)
            this.tfrm = tfrm;
        else
            this.tfrm = new Transform3(Matrix3.IDENTITY, Vector3.ZERO);
        this.ref_ifc = ifc;
        this.idx = idx;
        this.medium_name = "Interface";
        if (sd != null)
            this.sd = sd;
        else
            this.sd = ifc.max_aperture;
    }

    @Override
    public Node tree(KWArgs args) {
        String default_tag = "#dummyifc";
        String tag = default_tag + args.get("tag", "");
        Node di = new Node("DI", this, tag);
        Node p = new Node("p", ref_ifc.profile, "#profile", di);
        new Node("i" + idx, ref_ifc, "#ifc", p);
        return di;
    }

    @Override
    public String toString() {
        return ref_ifc.toString();
    }

    @Override
    public String get_label() {
        return label;
    }
}
