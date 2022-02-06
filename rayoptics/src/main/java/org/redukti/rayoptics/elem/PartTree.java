package org.redukti.rayoptics.elem;

import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.seq.Gap;
import org.redukti.rayoptics.seq.Interface;
import org.redukti.rayoptics.seq.SeqPathComponent;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.util.KWArgs;
import org.redukti.rayoptics.util.Pair;
import org.redukti.rayoptics.util.ZDir;

import java.util.ArrayList;
import java.util.List;
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

    /* Return a list of nodes that contain the requested `tag`. */
    public List<Node> nodes_with_tag(String tag, String not_tag, Node root) {
        String[] tags = tag.split("#");
        String[] not_tags = not_tag != null ? not_tag.split("#") : new String[0];
        Node root_node = root == null ? this.root_node : root;
        List<Node> nodes = new ArrayList<>();
        root_node.dfsScan((n) -> {
            for (String t : tags) {
                if (t.isEmpty())
                    continue;
                if (n.tag.contains(t)) {
                    for (String nt : not_tags) {
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

//    static void dfsScan(Node root_node, Consumer<Node> f) {
//        f.accept(root_node);
//        for (Node c: root_node.children) {
//            dfsScan(c, f);
//        }
//    }

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
        Node parent_node = leaf_node != null ? leaf_node.parent : null;
        while (parent_node != null) {
            for (String t : tags) {
                if (parent_node.tag.contains(t))
                    return parent_node;
            }
            parent_node = parent_node.parent;
        }
        return parent_node;
    }

    public Node parent_node(Object obj) {
        return parent_node(obj, "#element#airgap#dummyifc");
    }

//    public static List<Node> leaves(Node root_node) {
//        List<Node> result = new ArrayList<>();
//        root_node.dfsScan((n) -> {
//            if (n.is_leaf())
//                result.add(n);
//        });
//        return result;
//    }

    public Node add_element_to_tree(IElement e, KWArgs args) {
        Node e_node = e.tree(args);
        e_node.name = e.get_label();
        List<Node> leaves = e_node.leaves();
        for (Node leaf_node : leaves) {
            // TODO check if this is doing what's it supposed to
            Node dup_node = node(leaf_node.id);
            if (dup_node != null)
                dup_node.parent = null;
        }
        e_node.parent = this.root_node;
        return e_node;
    }

    public void sort_using_sequence(SequentialModel seq_model) {
    }
}
