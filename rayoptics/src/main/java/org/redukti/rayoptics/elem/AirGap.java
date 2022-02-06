package org.redukti.rayoptics.elem;

import org.redukti.rayoptics.math.Matrix3;
import org.redukti.rayoptics.math.Transform3;
import org.redukti.rayoptics.math.Vector3;
import org.redukti.rayoptics.seq.Gap;
import org.redukti.rayoptics.util.KWArgs;
import org.redukti.rayoptics.util.Pair;
import org.redukti.rayoptics.util.ZDir;

public class AirGap implements IElement {

    static int serial_number = 0;

    public ElementModel parent;

    String label;
    Transform3 tfrm;
    Gap gap;
    String medium_name;
    int idx;

    public AirGap(Gap g, int idx, Transform3 tfrm, String label) {
        if (label == null) {
            serial_number += 1;
            this.label = "AG" + serial_number;
        } else
            this.label = label;

        if (tfrm != null)
            this.tfrm = tfrm;
        else
            this.tfrm = new Transform3(Matrix3.IDENTITY, Vector3.ZERO);
        this.gap = g;
        this.medium_name = this.gap.medium.name();
        this.idx = idx;
    }

    @Override
    public Node tree(KWArgs args) {
        String default_tag = "#airgap";
        String tag = default_tag + args.get("tag", "");
        Node ag = new Node("AG", this, tag);
        Node t = new Node("t", gap, "#thic", ag);
        ZDir zdir = args.get("z_dir", ZDir.PROPAGATE_RIGHT);
        new Node("g" + idx, new Pair(gap, zdir), "#gap", t);
        return ag;
    }

    @Override
    public String get_label() {
        return label;
    }
}
