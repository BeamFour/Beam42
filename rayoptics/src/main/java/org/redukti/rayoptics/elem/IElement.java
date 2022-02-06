package org.redukti.rayoptics.elem;

import org.redukti.rayoptics.util.KWArgs;

public interface IElement {
    String get_label();

    Node tree(KWArgs args);
}
