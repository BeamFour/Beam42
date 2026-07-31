package org.redukti.rayoptics.layout;

import org.redukti.rayoptics.elem.surface.Surface;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.seq.Air;
import org.redukti.rayoptics.seq.Gap;
import org.redukti.rayoptics.seq.InteractMode;
import org.redukti.rayoptics.seq.SequentialModel;

import java.util.ArrayList;
import java.util.List;

/** Static physical-element view of a sequential optical model. */
public final class ElementModel {
    private final OpticalModel opticalModel;
    private List<Element> elements = List.of();

    public ElementModel(OpticalModel opticalModel) {
        this.opticalModel = opticalModel;
        updateModel();
    }

    public void updateModel() {
        SequentialModel sm = opticalModel.seq_model;
        List<Element> next = new ArrayList<>();
        int lastSurface = sm.ifcs.size() - 1;
        next.add(new DummyInterface(0, sm.ifcs.get(0), "Object"));
        double wvl = sm.central_wavelength();

        for (int i = 1; i < lastSurface; i++) {
            var ifc = sm.ifcs.get(i);
            if (ifc.interact_mode == InteractMode.REFLECT) {
                next.add(new Mirror(i, ifc));
            }
            if (sm.stop_surface != null && sm.stop_surface == i) {
                next.add(new Stop(i, ifc));
            } else if (isAperture(sm, i, wvl)) {
                next.add(new Aperture(i, ifc));
            }
        }

       for (int i = 0; i < sm.gaps.size(); i++) {
            Gap gap = sm.gaps.get(i);
            if (!isAir(gap, wvl) && i + 1 < sm.ifcs.size()
                    && sm.ifcs.get(i) instanceof Surface s1
                    && sm.ifcs.get(i + 1) instanceof Surface s2) {
                next.add(new Lens(i, i + 1, s1, s2, gap));
            }
        }
        next.add(new DummyInterface(lastSurface, sm.ifcs.get(lastSurface), "Image"));
        elements = List.copyOf(next);
    }

    static boolean isAperture(SequentialModel sm, int surfaceIndex, double wavelength) {
        if (surfaceIndex < 1 || surfaceIndex >= sm.ifcs.size() - 1 || surfaceIndex >= sm.gaps.size())
            return false;
        var ifc = sm.ifcs.get(surfaceIndex);
        Gap precedingGap = sm.gaps.get(surfaceIndex - 1);
        Gap followingGap = sm.gaps.get(surfaceIndex);
        return ifc.interact_mode != InteractMode.REFLECT
                && ifc.profile != null
                && Math.abs(ifc.profile.cv) < 1.0e-12
                && Math.abs(followingGap.thi) < 1.0e-12
                && isAir(followingGap, wavelength);
    }
    public List<Element> elements() { return elements; }

    public static boolean isAir(Gap gap, double wavelength) {
        if (gap == null || gap.medium == null) return true;
        if (gap.medium instanceof Air) return true;
        String name = gap.medium.name();
        if (name != null && name.equalsIgnoreCase("air")) return true;
        return Math.abs(gap.medium.rindex(wavelength) - 1.0) < 1.0e-12;
    }
}
