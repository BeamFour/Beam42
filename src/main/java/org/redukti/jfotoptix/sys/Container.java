package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.io.Renderer;
import org.redukti.jfotoptix.io.RendererViewport;
import org.redukti.jfotoptix.math.Vector3Pair;

import java.util.List;

public interface Container {
    List<? extends Element> elements();
    Vector3Pair get_bounding_box ();
    void draw_2d_fit (RendererViewport r, boolean keep_aspect);
    void draw_2d (Renderer r);
}
