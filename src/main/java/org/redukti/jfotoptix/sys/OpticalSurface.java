package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.math.Vector3Position;

public class OpticalSurface extends Surface {
    public OpticalSurface(OpticalSystem system, Group group, Vector3Position p) {
        super(system, group, p);
    }
}
