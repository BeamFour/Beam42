package org.redukti.rayoptics.optical;

import org.redukti.rayoptics.elem.*;
import org.redukti.rayoptics.math.Transform3;
import org.redukti.rayoptics.parax.ParaxialModel;
import org.redukti.rayoptics.seq.Gap;
import org.redukti.rayoptics.seq.Interface;
import org.redukti.rayoptics.seq.SeqPathComponent;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.specs.OpticalSpecs;
import org.redukti.rayoptics.specs.SystemSpec;
import org.redukti.rayoptics.util.*;

import java.util.ArrayList;
import java.util.List;

public class OpticalModel {

    public SequentialModel seq_model;
    public OpticalSpecs optical_spec;
    public ParaxialModel parax_model;
    public ElementModel element_model;
    public PartTree part_tree;
    public SystemSpec system_spec;
    public boolean radius_mode;
    public String dimensions = "mm";

    public OpticalModel() {
        seq_model = new SequentialModel(this);
        optical_spec = new OpticalSpecs(this);
        parax_model = new ParaxialModel(this, seq_model);
        element_model = new ElementModel(this);
        part_tree = new PartTree(this);
        system_spec = new SystemSpec();

        // if (doInit)
        seq_model.update_model();
        elements_from_sequence(element_model, seq_model, part_tree);
    }

    public void update_model() {
        seq_model.update_model();
        optical_spec.update_model();
        parax_model.update_model();
        element_model.update_model();
    }

    /* generate an element list from a sequential model */
    void elements_from_sequence(ElementModel ele_model, SequentialModel seq_model, PartTree part_tree) {
        if (part_tree.root_node.children.size() == 0)
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
            double rindx = seg.rndx;
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
                        }
                        // add an AirGap
                        AirGap ag = new AirGap(g, i, g_tfrm, null);
                        Node ag_node = part_tree.add_element_to_tree(ag, new KWArgs("z_dir", z_dir));
                        ag_node.leaves().get(0).id = new Pair<>(g, z_dir);
                        ele_model.add_element(ag);
                        ag.parent = ele_model;

                        eles = new ArrayList<>();
                        buried_reflector = false;
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
        //if (!node.name.equals("Object space"))
        //    node.name = node.id.label = 'Image space'
        node.tag = node.tag + "#image";

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
    }

    /**
     * convert nm to system units
     * <p>
     * Args:
     * nm (float): value in nm
     * <p>
     * Returns:
     * float: value converted to system units
     *
     * @param nm
     * @return
     */
    public double nm_to_sys_units(double nm) {
        if ("m".equals(dimensions))
            return 1e-9 * nm;
        else if ("cm".equals(dimensions))
            return 1e-7 * nm;
        else if ("mm".equals(dimensions))
            return 1e-6 * nm;
        else if ("in".equals(dimensions))
            return 1e-6 * nm / 25.4;
        else if ("ft".equals(dimensions))
            return 1e-6 * nm / 304.8;
        else
            return nm;
    }
}
