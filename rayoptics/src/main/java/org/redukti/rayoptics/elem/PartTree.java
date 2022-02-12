package org.redukti.rayoptics.elem;

import org.redukti.rayoptics.math.Transform3;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.seq.Gap;
import org.redukti.rayoptics.seq.Interface;
import org.redukti.rayoptics.seq.SeqPathComponent;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PartTree {

    OpticalModel opt_model;
    public Node root_node;

    public PartTree(OpticalModel opt_model) {
        this.opt_model = opt_model;
        this.root_node = new Node("root", this, "#group#root");
    }

    /* Initialize part tree using a *seq_model*. */
    public void init_from_sequence(SequentialModel seq_model) {
        Node root_node = this.root_node;
        List<SeqPathComponent> seq = SequentialModel.zip_longest(seq_model.ifcs, seq_model.gaps,
                seq_model.z_dir);
        for (int i = 0; i < seq.size(); i++) {
            SeqPathComponent sgz = seq.get(i);
            Interface s = sgz.ifc;
            Gap gap = sgz.gap;
            ZDir z_dir = sgz.z_dir;
            new Node("i" + i, s, "#ifc", root_node);
            if (gap != null) {
                new Node("g" + i, new Pair<>(gap, z_dir), "#gap", root_node);
            }
        }
        return;
    }

    public List<Node> nodes_with_tag(String tag) {
        return nodes_with_tag(tag, "", null);
    }

    /* Return a list of nodes that contain the requested `tag`. */
    public List<Node> nodes_with_tag(String tag, String not_tag, Node root) {
        String[] tags = tag.split("#");
        String[] not_tags = not_tag != null ? not_tag.split("#") : new String[0];
        Node root_node = root == null ? this.root_node : root;
        List<Node> nodes = new ArrayList<>();
        root_node.dfsScan((n) -> {
            for (String t : tags) {
                if (t.isEmpty())
                    // FIXME
                    continue;
                if (n.tag.contains(t)) {
                    for (String nt : not_tags) {
                        if (nt.isEmpty())
                            // FIXME
                            continue;
                        if (n.tag.contains(nt))
                            return;
                    }
                    nodes.add(n);
                    return;
                }
            }
        });
        return nodes;
    }

    /**
     * Return the node paired with `obj`.
     */
    public Node node(Object obj) {
        List<Node> result = new ArrayList<>();
        root_node.dfsScan((n) -> {
            if (Objects.equals(n.id, obj))
                result.add(n);
        });
        return result.size() == 0 ? null : result.get(0);
    }

    /**
     * Return the parent node for `obj`, filtered by `tag`.
     */
    public Node parent_node(Object obj, String tag) {
        String[] tags = tag.split("#");
        Node leaf_node = node(obj);
        Node parent_node = leaf_node != null ? leaf_node.parent() : null;
        while (parent_node != null) {
            for (String t : tags) {
                if (t.isEmpty())
                    // FIXME
                    continue;
                if (parent_node.tag.contains(t))
                    return parent_node;
            }
            parent_node = parent_node.parent();
        }
        return parent_node;
    }

    public Node parent_node(Object obj) {
        return parent_node(obj, "#element#airgap#dummyifc");
    }


    public Node add_element_to_tree(IElement e, KWArgs args) {
        Node e_node = e.tree(args);
        e_node.name = e.get_label();
        List<Node> leaves = e_node.leaves();
        for (Node leaf_node : leaves) {
            // TODO check if this is doing what's it supposed to
            Node dup_node = node(leaf_node.id);
            if (dup_node != null)
                dup_node.set_parent(null);
        }
        e_node.set_parent(this.root_node);
        return e_node;
    }

    /* Resequence part tree using a *seq_model*. */
    public void sort_using_sequence(SequentialModel seq_model) {
        List<Node> e_node_list = new ArrayList<>();
        List<SeqPathComponent> path = SequentialModel.zip_longest(seq_model.ifcs, seq_model.gaps, seq_model.z_dir);
        for (int i = 0; i < path.size(); i++) {
            SeqPathComponent sgz = path.get(i);
            Interface ifc = sgz.ifc;
            Gap gap = sgz.gap;
            ZDir z_dir = sgz.z_dir;

            Node e_node = parent_node(ifc);
            if (e_node != null && !e_node_list.contains(e_node)) {
                e_node_list.add(e_node);
            }
            if (gap != null) {
                Node g_node = parent_node(new Pair<>(gap, z_dir));
                if (g_node != null && !e_node_list.contains(g_node)) {
                    e_node_list.add(g_node);
                }
            }
        }
        if (!e_node_list.isEmpty())
            this.root_node.set_children(e_node_list);
    }

    /**
     * Update node names to track element labels.
     */
    void sync_part_tree_on_update(ElementModel ele_model, SequentialModel seq_model, Node root_node) {
        Map<String, IElement> ele_dict = ele_model.as_dict();
        for (Node node : root_node.all()) {
            String name = node.name;
            if (name.charAt(0) == 'i') {
                int idx = seq_model.ifcs.indexOf(node.id);
                node.name = "i" + idx;
            } else if (name.charAt(0) == 'g') {
                Pair<Gap, ZDir> gd = (Pair<Gap, ZDir>) node.id;
                Gap gap = gd.first;
                ZDir z_dir = gd.second;
                int idx = seq_model.gaps.indexOf(gap);
                z_dir = seq_model.z_dir.get(idx);
                node.id = new Pair<>(gap, z_dir);
                node.name = "g" + idx;
            } else if (name.charAt(0) == 'p') {
                String p_name = node.parent().name;
                IElement e = ele_dict.get(p_name);
                int idx = name.length() > 1 ? Integer.parseInt(name.substring(1)) : 0;
                node.id = e.interface_list().get(idx).profile;
            } else if (name.startsWith("tl")) {
                String p_name = node.parent().name;
                IElement e = ele_dict.get(p_name);
                node.id = e.interface_list().get(0);
                int idx = seq_model.ifcs.indexOf(node.id);
                node.name = "tl" + idx;
            } else if (name.charAt(0) == 't') {
                String p_name = node.parent().name;
                IElement e = ele_dict.get(p_name);
                int idx = name.length() > 1 ? Integer.parseInt(name.substring(1)) : 0;
                node.id = e.gap_list().get(idx);
            } else if (name.equals("root")) {
                ;
            } else {
                if (node.id instanceof IElement) {
                    node.name = ((IElement) node.id).get_label();
                } else if (node.id instanceof Surface) {
                    node.name = ((Surface) node.id).label;
                }
//                if hasattr(node.id, 'label'):
//                node.name = node.id.label
            }
        }
        return;
    }

    /* generate an element list from a sequential model */
    public void elements_from_sequence(ElementModel ele_model, SequentialModel seq_model, PartTree part_tree) {
        if (part_tree.root_node.children().size() == 0)
            // initialize part tree using the seq_model
            part_tree.init_from_sequence(seq_model);
        List<Transform3> g_tfrms = seq_model.compute_global_coords(1);
        boolean buried_reflector = false;
        List<Quint<Integer, Interface, Gap, ZDir, Transform3>> eles = new ArrayList<>();

        List<SeqPathComponent> path = seq_model.path(null, null, null, 1);
        int i = 0;
        for (SeqPathComponent seg : path) {
            Interface ifc = seg.ifc;
            Gap g = seg.gap;
            Transform3 tfrm = seg.transform3;
            ZDir z_dir = seg.z_dir;
            if (part_tree.parent_node(ifc) == null) {
                Transform3 g_tfrm = g_tfrms.get(i);
                if (g != null) {
                    if (g.medium.name().equalsIgnoreCase("air")) {
                        int num_eles = eles.size();
                        if (num_eles == 0) {
                            process_airgap(
                                    ele_model, seq_model, part_tree,
                                    i, g, z_dir, ifc, g_tfrm, true);
                        } else {
                            Quint<Integer, Interface, Gap, ZDir, Transform3> el;
                            if (buried_reflector) {
                                num_eles = Math.floorDiv(num_eles, 2);
                                eles.add(new Quint(i, ifc, g, z_dir, g_tfrm));
                                el = eles.get(1);
                                i = el.first;
                                ifc = el.second;
                                g = el.third;
                                z_dir = el.fourth;
                                g_tfrm = el.fifth;
                            }
                            if (num_eles == 1) {
                                el = eles.get(0);
                                int i1 = el.first;
                                Interface s1 = el.second;
                                Gap g1 = el.third;
                                ZDir z_dir1 = el.fourth;
                                Transform3 g_tfrm1 = el.fifth;
                                double sd = Math.max(s1.surface_od(), ifc.surface_od());
                                Element e = new Element(s1, ifc, g1, tfrm, i1, i, sd, null);
                                Node e_node = part_tree.add_element_to_tree(e, new KWArgs("z_dir", z_dir1));
                                ele_model.add_element(e);
                                e.parent = ele_model;
                                if (buried_reflector) {
                                    // TODO
                                    // set up for airgap
                                    el = Lists.get(eles, -1);
                                    i = el.first;
                                    ifc = el.second;
                                    g = el.third;
                                    z_dir = el.fourth;
                                    g_tfrm = el.fifth;
                                }
                            } else if (num_eles > 1) {
                                if (!buried_reflector)
                                    eles.add(new Quint<>(i, ifc, g, z_dir, g_tfrm));
                                CementedElement e = new CementedElement(Lists.upto(eles, num_eles + 1), null);
                                Node e_node = part_tree.add_element_to_tree(e, new KWArgs("z_dir", z_dir));
                                ele_model.add_element(e);
                                e.parent = ele_model;
                                if (buried_reflector) {
                                    // TODO
                                }
                                // set up for airgap
                                el = Lists.get(eles, -1);
                                i = el.first;
                                ifc = el.second;
                                g = el.third;
                                z_dir = el.fourth;
                                g_tfrm = el.fifth;
                            }
                            // add an AirGap
                            AirGap ag = new AirGap(g, i, g_tfrm, null);
                            Node ag_node = part_tree.add_element_to_tree(ag, new KWArgs("z_dir", z_dir));
                            ag_node.leaves().get(0).id = new Pair<>(g, z_dir);
                            ele_model.add_element(ag);
                            ag.parent = ele_model;

                            eles = new ArrayList<>();
                            buried_reflector = false;
                        }
                    } else {
                        // a non-air medium
                        // handle buried mirror, e.g. prism or Mangin mirror
                        if (ifc.interact_mode.equalsIgnoreCase("reflect"))
                            buried_reflector = true;
                        eles.add(new Quint<>(i, ifc, g, z_dir, g_tfrm));
                    }
                } else {
                    process_airgap(ele_model, seq_model, part_tree,
                            i, g, z_dir, ifc, g_tfrm, true);
                }
            }
            i++;
        }

        // rename and tag the Image space airgap
        Node node = part_tree.parent_node(new Pair<>(Lists.get(seq_model.gaps, -1), Lists.get(seq_model.z_dir, -1)));
        if (!node.name.equals("Object space")) {
//            node.name = node.id.label = 'Image space'
//            node.tag = node.tag + "#image";
        }

        // sort the final tree by seq_model order
        part_tree.sort_using_sequence(seq_model);
    }

    private void process_airgap(ElementModel ele_model, SequentialModel seq_model, PartTree part_tree, int i, Gap g, ZDir z_dir, Interface s, Transform3 g_tfrm, boolean add_ele) {
        if (s.interact_mode.equals("reflect") && add_ele) {
            // TODO
            throw new UnsupportedOperationException();
        }
        //else if (s instanceof ThinLens && add_ele) {
        // TODO
        //}
        else if (s.interact_mode.equals("dummy") || s.interact_mode.equals("transmit")) {
            boolean add_dummy = false;
            String dummy_label = null;
            String dummy_tag = "";
            if (i == 0) {
                add_dummy = true; //add dummy for the object
                dummy_label = "Object";
                dummy_tag = "#object";
            } else if (i == seq_model.get_num_surfaces() - 1) {
                add_dummy = true; // add dummy for the object
                dummy_label = "Image";
                dummy_tag = "#image";
            } else {  // i > 0
                Gap gp = Lists.get(seq_model.gaps, i - 1);
                if (gp.medium.name().equalsIgnoreCase("air")) {
                    add_dummy = true;
                    if (seq_model.stop_surface == i) {
                        dummy_label = "Stop";
                        dummy_tag = "#stop";
                    }
                }
            }
            if (add_dummy) {
                double sd = s.surface_od();
                DummyInterface di = new DummyInterface(s, sd, g_tfrm, i, dummy_label);
                part_tree.add_element_to_tree(di, new KWArgs("tag", dummy_tag));
                ele_model.add_element(di);
            }
        } else {
            throw new UnsupportedOperationException();
        }
        if (g != null) {
            // add an AirGap
            String gap_label, gap_tag;
            if (i == 0) {
                gap_label = "Object space";
                gap_tag = "#object";
            } else {  //i > 0
                gap_label = null;
                gap_tag = "";
            }
            AirGap ag = new AirGap(g, i, g_tfrm, gap_label);
            Node ag_node = part_tree.add_element_to_tree(ag, new KWArgs("z_dir", z_dir).add("tag", gap_tag));
            ag_node.leaves().get(0).id = new Pair<>(g, z_dir);
            ele_model.add_element(ag);
        }

    }

    public void update_model() {
        sync_part_tree_on_update(
                opt_model.ele_model,
                opt_model.seq_model,
                root_node);
        sort_using_sequence(opt_model.seq_model);
    }
}
