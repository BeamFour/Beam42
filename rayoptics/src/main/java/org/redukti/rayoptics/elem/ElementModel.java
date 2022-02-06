package org.redukti.rayoptics.elem;

import org.redukti.rayoptics.math.Transform3;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.seq.SequentialModel;

import java.util.ArrayList;
import java.util.List;

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
}
