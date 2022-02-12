package org.redukti.rayoptics.elem;

import org.redukti.rayoptics.seq.Gap;
import org.redukti.rayoptics.seq.Interface;
import org.redukti.rayoptics.util.KWArgs;

import java.util.List;

public interface IElement {
    String get_label();

    Node tree(KWArgs args);

    List<Interface> interface_list();

    List<Gap> gap_list();

    void set_parent(ElementModel ele_model);
}
