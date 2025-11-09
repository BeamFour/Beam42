// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.parax;

import java.util.List;

/**
 * tuple grouping together paraxial rays and first order properties
 *
 *     Attributes:
 *         ax_ray: axial marginal ray data, y, u, i
 *         pr_ray: chief ray data, y, u, i
 *         fod: instance of :class:`~.FirstOrderData`
 */
public class ParaxData {

    public List<ParaxComponent> ax_ray;
    public List<ParaxComponent> pr_ray;
    public FirstOrderData fod;

    public ParaxData(List<ParaxComponent> ax_ray, List<ParaxComponent> pr_ray, FirstOrderData fod) {
        this.ax_ray = ax_ray;
        this.pr_ray = pr_ray;
        this.fod = fod;
    }
}
