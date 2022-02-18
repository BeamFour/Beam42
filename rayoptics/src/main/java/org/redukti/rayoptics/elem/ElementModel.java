package org.redukti.rayoptics.elem;

import org.redukti.rayoptics.math.Transform3;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.seq.SequentialModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maintain the element based representation of the optical model
 * <p>
 * Attributes:
 * opt_model: the :class:`~rayoptics.optical.opticalmodel.OpticalModel`
 * elements: list of element type things
 */
public class ElementModel {

    OpticalModel opt_model;
    List<IElement> elements;

    public ElementModel(OpticalModel opt_model) {
        this.opt_model = opt_model;
        this.elements = new ArrayList<>();
    }

    public void update_model() {
        SequentialModel seq_model = opt_model.seq_model;
        List<Transform3> tfrms = seq_model.compute_global_coords(1);
        // dynamically build element list from part_tree
        PartTree part_tree = opt_model.part_tree;
        List<Node> nodes = part_tree.nodes_with_tag("#element#airgap#dummyifc", null, null);
        return;
    }

    public void add_element(IElement e) {
        elements.add(e);
    }

    public static String get_key(IElement e) {
        return e.get_label();
    }

    public Map<String, IElement> as_dict() {
        Map<String, IElement> result = new HashMap<>();
        elements.stream().forEach(e -> result.put(e.get_label(), e));
        return result;
    }

    public String list_model() {
        return list_model("#element#dummyifc");
    }

    public String list_model(String tag) {
        List<Node> nodes = opt_model.part_tree.nodes_with_tag(tag);
        List<Object> els = nodes.stream().map(n -> n.id).collect(Collectors.toList());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < els.size(); i++) {
            Object ele = els.get(i);
            sb.append(i).append(": ")
                    .append(getLabel(ele))
                    .append("(")
                    .append(ele.getClass().getSimpleName()).append(") ")
                    .append(ele)
                    .append(System.lineSeparator());
        }
        return sb.toString();
    }

    public String list_elements() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < elements.size(); i++) {
            IElement ele = elements.get(i);
            sb.append(i).append(": ")
                    .append(ele.get_label())
                    .append(" (")
                    .append(ele.getClass().getSimpleName())
                    .append(") ")
                    .append(ele)
                    .append(System.lineSeparator());
        }
        return sb.toString();
    }

    private String getLabel(Object ele) {
        if (ele instanceof IElement)
            return ((IElement) ele).get_label();
        else if (ele instanceof Surface)
            return ((Surface) ele).label;
        else
            return "''";
    }
}
