package org.redukti.rayoptics.elem;

import org.redukti.mathlib.Matrix3;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.math.Transform3;
import org.redukti.rayoptics.seq.Gap;
import org.redukti.rayoptics.seq.Interface;
import org.redukti.rayoptics.util.KWArgs;
import org.redukti.rayoptics.util.Pair;
import org.redukti.rayoptics.util.ZDir;

import java.util.List;
import java.util.Objects;

/**
 * Lens element domain model. Manage rendering and selection/editing.
 * <p>
 * An Element consists of 2 Surfaces, 1 Gap, and edge_extent information.
 * <p>
 * Attributes:
 * parent: the :class:`ElementModel`
 * label: string identifier
 * s1: first/origin :class:`~rayoptics.seq.interface.Interface`
 * s2: second/last :class:`~rayoptics.seq.interface.Interface`
 * gap: element thickness and material :class:`~rayoptics.seq.gap.Gap`
 * tfrm: global transform to element origin, (Rot3, trans3)
 * medium_name: the material filling the gap
 * flat1, flat2: semi-diameter of flat or None. Setting to None will result in
 * re-evaluation of flat ID
 * do_flat1, do_flat2: 'if concave', 'always', 'never', 'if convex'
 * handles: dict of graphical entities
 * actions: dict of actions associated with the graphical handles
 */
public class Element implements IElement {

    static int serial_number = 0;

    String label;
    Transform3 tfrm;
    Interface s1;
    int s1_indx;
    Interface s2;
    int s2_indx;
    Gap gap;
    String medium_name;
    double sd;
    Double flat1;
    Double flat2;
    String do_flat1;
    String do_flat2;

    public ElementModel parent = null;

    public Element(Interface s1, Interface s2, Gap g, Transform3 tfrm, int idx, int idx2, double sd,
                   String label) {
        if (label == null) {
            serial_number += 1;
            this.label = "E" + serial_number;
        } else
            this.label = label;
        if (tfrm != null)
            this.tfrm = tfrm;
        else
            this.tfrm = new Transform3(Matrix3.IDENTITY, Vector3.ZERO);
        this.s1 = s1;
        this.s1_indx = idx;
        this.s2 = s2;
        this.s2_indx = idx2;
        this.gap = g;
        this.medium_name = gap.medium.name();
        this.sd = sd;
        this.flat1 = null;
        this.flat2 = null;
        this.do_flat1 = "if concave"; // alternatives are 'never', 'always',
        this.do_flat2 = "if concave"; // or 'if convex'
    }

    public Element(Interface s1, Interface s2, Gap g) {
        this(s1, s2, g, null, 0, 1, 1.0, null);
    }

    /* Build tree linking sequence to element model. */
    @Override
    public Node tree(KWArgs args) {
        ZDir z_dir = args.get("z_dir", ZDir.PROPAGATE_RIGHT);
        String default_tag = "#element#lens";
        String tag = args.get("tag", "");
        tag = default_tag + tag;
        ZDir zdir = z_dir != null ? z_dir : ZDir.PROPAGATE_RIGHT;

        // Interface branch 1
        Node e = new Node("E", this, tag);
        Node p1 = new Node("p1", s1.profile, "#profile", e);
        new Node("i" + s1_indx, s1, "#ifc", p1);

        // Gap branch
        Node t = new Node("t", gap, "#thic", e);
        new Node("g" + s1_indx, new Pair<>(gap, zdir), "#gap", t);

        // Interface branch 2
        Node p2 = new Node("p2", s2.profile, "#profile", e);
        new Node("i" + s2_indx, s2, "#ifc", p2);
        return e;
    }

    @Override
    public String get_label() {
        return label;
    }

    public List<Interface> interface_list() {
        return List.of(s1, s2);
    }

    public List<Gap> gap_list() {
        return List.of(gap);
    }

    @Override
    public void set_parent(ElementModel ele_model) {
        this.parent = ele_model;
    }

    @Override
    public String toString() {
        return "Element: " + s1.profile
                + ", " + s2.profile
                + ", t=" + Objects.toString(gap.thi)
                + ", sd=" + Objects.toString(sd)
                + ", glass: " + gap.medium.name();
    }
}
