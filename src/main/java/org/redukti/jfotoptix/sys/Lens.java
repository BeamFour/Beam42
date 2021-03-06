package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Position;

import java.util.ArrayList;

public class Lens extends Group {

    private ArrayList<OpticalSurface> surfaces = new ArrayList<>();

    public Lens(OpticalSystem opticalSystem, Group group, Vector3Position position) {
        super(opticalSystem, group, position);
    }

    public static class Builder extends Element.Builder {

        private ArrayList<OpticalSurface> surfaces = new ArrayList<>();
        private Vector3Position position = new Vector3Position(Vector3.vector3_0, Vector3.vector3_001);

        public Builder position(Vector3Position position) {
            this.position = position;
            return this;
        }

        public Lens build() {
            return new Lens(this.opticalSystem.build(), this.group.build(), position);
        }

    }

}
