package org.redukti.rayoptics.elem;

import org.redukti.rayoptics.math.Matrix3;
import org.redukti.rayoptics.math.Transform3;
import org.redukti.rayoptics.math.Vector3;
import org.redukti.rayoptics.seq.Gap;
import org.redukti.rayoptics.seq.Interface;
import org.redukti.rayoptics.util.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Cemented element domain model. Manage rendering and selection/editing.
 * <p>
 * A CementedElement consists of 3 or more Surfaces, 2 or more Gaps, and
 * edge_extent information.
 * <p>
 * Attributes:
 * parent: the :class:`ElementModel`
 * label: string identifier
 * idxs: list of seq_model interface indices
 * ifcs: list of :class:`~rayoptics.seq.interface.Interface`
 * gaps: list of thickness and material :class:`~rayoptics.seq.gap.Gap`
 * tfrm: global transform to element origin, (Rot3, trans3)
 * medium_name: the material filling the gap
 * flats: semi-diameter of flat if ifc is concave, or None
 * handles: dict of graphical entities
 * actions: dict of actions associated with the graphical handles
 */
public class CementedElement implements IElement {

    static int serial_number = 0;

    String label;
    Transform3 tfrm;
    String medium_name = "";

    List<Integer> idxs = new ArrayList<>();
    List<Interface> ifcs = new ArrayList<>();
    List<Gap> gaps = new ArrayList<>();

    public ElementModel parent = null;

    public CementedElement(List<Quint<Integer, Interface, Gap, ZDir, Transform3>> ifc_list, String label) {
        if (label == null) {
            serial_number += 1;
            this.label = "CE" + serial_number;
        } else
            this.label = label;
        Transform3 g_tfrm = ifc_list.get(0).fifth;
        if (g_tfrm != null)
            this.tfrm = g_tfrm;
        else
            this.tfrm = new Transform3(Matrix3.IDENTITY, Vector3.ZERO);
        for (Quint<Integer, Interface, Gap, ZDir, Transform3> q : ifc_list) {
            Integer i = q.first;
            Interface ifc = q.second;
            Gap g = q.third;
            ZDir z_dir = q.fourth;
            g_tfrm = q.fifth;
            idxs.add(i);
            ifcs.add(ifc);
            if (g != null) {
                this.gaps.add(g);
                if (!this.medium_name.isEmpty())
                    this.medium_name += ", ";
                this.medium_name += g.medium.name();
            }
        }
        if (this.gaps.size() == this.ifcs.size()) {
            gaps.remove(gaps.size() - 1);
            if (this.medium_name.contains(","))
                this.medium_name = this.medium_name.substring(0, this.medium_name.lastIndexOf(','));
        }
    }

    @Override
    public Node tree(KWArgs args) {
        String default_tag = "#element#cemented";
        String tag = default_tag + args.get("tag", "");
        ZDir zdir = args.get("z_dir", ZDir.PROPAGATE_RIGHT);
        Node ce = new Node("CE", this, tag);
        List<Pair<Interface, Gap>> list = Lists.zip_longest(ifcs, gaps);
        for (int j = 0; j < list.size(); j++) {
            Pair<Interface, Gap> sg = list.get(j);
            Interface ifc = sg.first;
            Gap gap = sg.second;
            int i = j + 1;
            String pid = "p" + i;
            Node p = new Node(pid, ifc.profile, "#profile", ce);
            new Node("i" + idxs.get(j), ifc, "#ifc", p);
            // Gap branch
            if (gap != null) {
                Node t = new Node("t" + i, gap, "#thic", ce);
                new Node("g" + idxs.get(j), new Pair<>(gap, zdir),
                        "#gap", t);
            }
        }
        return ce;
    }

    @Override
    public String get_label() {
        return label;
    }

    public List<Interface> interface_list() {
        return ifcs;
    }

    public List<Gap> gap_list() {
        return gaps;
    }

    @Override
    public void set_parent(ElementModel ele_model) {
        this.parent = ele_model;
    }

    @Override
    public String toString() {
        return idxs.toString();
    }
}
