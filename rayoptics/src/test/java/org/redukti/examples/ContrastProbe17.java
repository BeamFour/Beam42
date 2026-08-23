package org.redukti.examples;

import org.redukti.optim.Analysis;
import org.redukti.optim.ConstraintEdgeThickness;
import org.redukti.optim.ConstraintThickness;
import org.redukti.spec.Prescription;

/**
 * Starting edge gaps on the Leica 75/2, and how much slack the axial thickness
 * constraint leaves.
 *
 * <p>Prints the axial thickness beside the edge separation for every gap. Where the edge
 * gap is much the smaller of the two, {@link ConstraintThickness} is holding a quantity
 * with room to spare while the one that actually goes negative moves freely - which is
 * what {@link ConstraintEdgeThickness} exists to catch.
 */
public class ContrastProbe17 {

    public static void main(String[] args) throws Exception {
        Prescription prescription = LeicaApo75mmMandler.getPrescription(
                ContrastProbes.leicaInputPath(), false, false);
        Analysis analysis = new Analysis(prescription, new double[]{0.0}, new int[]{10});

        System.out.println("surf |  axial t  |  edge gap  | edge/axial | constrainable");
        for (int s = 0; s < prescription._surfaces.length - 1; s++) {
            double axial = prescription._surfaces[s].get_thickness_by_scenario(0);
            boolean ok = ConstraintEdgeThickness.is_constrainable(analysis, s);
            if (!ok) {
                System.out.printf("%4d | %9.4f |          - |          - | no%n", s, axial);
                continue;
            }
            var edge = new ConstraintEdgeThickness(analysis, s, 1.0);
            System.out.printf("%4d | %9.4f | %10.4f | %10.3f | yes  (h=%.3f)%n",
                    s, axial, edge.value(), edge.value() / axial, edge._height);
        }
    }
}
