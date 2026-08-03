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

        // A run of adjacent glass gaps is one cemented assembly. Keeping it
        // together records that each internal interface is physically shared.
        for (int i = 0; i < sm.gaps.size();) {
            if (isAir(sm.gaps.get(i), wvl)) {
                i++;
                continue;
            }
            int firstGap = i;
            while (i < sm.gaps.size() && !isAir(sm.gaps.get(i), wvl)) i++;
            int gapCount = i - firstGap;
            if (i >= sm.ifcs.size()) continue;

            if (gapCount == 1
                    && sm.ifcs.get(firstGap) instanceof Surface s1
                    && sm.ifcs.get(firstGap + 1) instanceof Surface s2) {
                next.add(new LensElement(firstGap, firstGap + 1, s1, s2, sm.gaps.get(firstGap)));
            } else if (gapCount > 1) {
                List<Integer> indices = new ArrayList<>(gapCount + 1);
                List<Surface> surfaces = new ArrayList<>(gapCount + 1);
                List<Gap> gaps = new ArrayList<>(gapCount);
                boolean allSurfaces = true;
                for (int j = firstGap; j <= i; j++) {
                    indices.add(j);
                    if (sm.ifcs.get(j) instanceof Surface surface) surfaces.add(surface);
                    else allSurfaces = false;
                    if (j < i) gaps.add(sm.gaps.get(j));
                }
                if (allSurfaces) next.add(new CementedElement(indices, surfaces, gaps));
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
