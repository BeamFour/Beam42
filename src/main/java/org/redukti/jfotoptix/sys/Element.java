package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Position;

public abstract class Element {

    final OpticalSystem opticalSystem;
    final Group group;
    final Transform3 transform;

    public Element(OpticalSystem opticalSystem, Group group, Vector3Position p) {
        this.opticalSystem = opticalSystem;
        this.group = group;
        this.transform = new Transform3(p);
    }

    public Vector3 getLocalPosition() {
        return this.transform.translation;
    }

    public static class Builder {
        OpticalSystem.Builder opticalSystem;
        Group.Builder group;
        Vector3Position position;

        public Builder system(OpticalSystem.Builder system) {
            this.opticalSystem = system;
            return this;
        }

        public Builder group(Group.Builder group) {
            this.group = group;
            return this;
        }

        public Builder position(Vector3Position position) {
            this.position = position;
            return this;
        }
    }
}
