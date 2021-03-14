package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.curve.Curve;
import org.redukti.jfotoptix.curve.Flat;
import org.redukti.jfotoptix.curve.Sphere;
import org.redukti.jfotoptix.material.Abbe;
import org.redukti.jfotoptix.material.MaterialBase;
import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.shape.Disk;
import org.redukti.jfotoptix.shape.Shape;

import java.util.ArrayList;
import java.util.List;

public class Lens extends Group {

    public Lens(int id, Vector3Pair position, Transform3 transform, List<Element> elementList) {
        super(id, position, transform, elementList);
    }

    public static class Builder extends Group.Builder {
        List<OpticalSurface.Builder> opticalSurfaces = new ArrayList<>();
        int _last_pos = 0;
        MaterialBase _next_mat = null;

        @Override
        public Element build() {
            return new Lens(id, position, transform, getElements());
        }

        public void add_surface(double curvature, double radius, double thickness, Abbe glass) {
            Curve curve;
            if (curvature == 0.)
                curve = Flat.flat;
            else
                curve = new Sphere(curvature);
            add_surface(curve, new Disk(radius), thickness,
                    glass);
        }

        public void add_surface(Curve curve, Shape shape, double thickness, MaterialBase glass) {
            assert (thickness >= 0.);
            OpticalSurface.Builder surface = new OpticalSurface.Builder()
                    .position(new Vector3Pair(new Vector3(0, 0, _last_pos), Vector3.vector3_0))
                    .curve(curve)
                    .shape(shape)
                    .leftMaterial(_next_mat)
                    .rightMaterial(glass);

            opticalSurfaces.add(surface);
            _next_mat = glass;
            _last_pos += thickness;
            super.elements.add(surface);
        }
    }
}
