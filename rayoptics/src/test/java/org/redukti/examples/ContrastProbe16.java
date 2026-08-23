package org.redukti.examples;

import org.redukti.optim.Var;
import org.redukti.optim.VarRadius;
import org.redukti.optim.VarThickness;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.spec.Prescription;
import org.redukti.spec.RayOpticsModelBuilder;
import org.redukti.spec.VigType;

/**
 * Does vignetting move during an optimization, and by how much per Jacobian step?
 *
 * <p>Apertures are never optimization variables, so it is tempting to assume the
 * vignetting factors are fixed. They are not: they are a property of where rays land on
 * those fixed apertures, and every curvature and thickness the solver moves changes that.
 * {@code Analysis.compute()} rebuilds the optical model from scratch on every evaluation,
 * so the factors are recomputed each time - including on each of the {@code 2n} probes
 * that make up one finite-difference Jacobian.
 *
 * <p>For every variable this perturbs the prescription by exactly the Jacobian step
 * {@code Var._d_delta}, rebuilds, and reports how far the vignetting factors moved. A
 * merit function whose sampling region shifts under its own derivative probe is
 * differentiating the design and the pupil at the same time.
 */
public class ContrastProbe16 {

    private static final double[] FIELDS = {0.0, 0.3, 0.7, 1.0};

    public static void main(String[] args) throws Exception {
        for (VigType mode : new VigType[]{VigType.Paraxial, VigType.SetVig, VigType.SetPupil}) {
            report(mode);
        }
        sweep();
    }

    /**
     * Size alone does not decide whether the drift hurts. A smooth dependence just means
     * the merit also differentiates the pupil; a stepped or noisy one poisons the
     * finite difference. This walks surface 0's radius across four Jacobian steps and
     * prints the full-field factors, so successive differences can be eyeballed for
     * kinks.
     */
    private static void sweep() throws Exception {
        System.out.println("=== full-field vuy across +/-2 Jacobian steps of surface 0 radius ===");
        System.out.println("        step |   Paraxial     d    |    SetVig       d    |   SetPupil      d");
        Double[] previous = {null, null, null};
        for (int i = -4; i <= 4; i++) {
            StringBuilder line = new StringBuilder(String.format("%12.1f |", i * 0.5));
            for (int m = 0; m < 3; m++) {
                VigType mode = new VigType[]{VigType.Paraxial, VigType.SetVig, VigType.SetPupil}[m];
                var prescription = LeicaApo75mmMandler.getPrescription(
                        ContrastProbes.leicaInputPath(), false, false);
                var v = new VarRadius(prescription, 0);
                double keep = v.read_from_prescription();
                v.set_scaled_value(keep + i * 0.5 * v._d_delta * v.get_scaling_factor());
                v.write_to_prescription();
                double vuy = factors(prescription, mode)[FIELDS.length - 1][2];
                line.append(String.format("  %.7f", vuy));
                line.append(previous[m] == null ? "         " : String.format("  %+.2e", vuy - previous[m]));
                line.append(" |");
                previous[m] = vuy;
            }
            System.out.println(line);
        }
    }

    private static void report(VigType mode) throws Exception {
        var prescription = LeicaApo75mmMandler.getPrescription(
                ContrastProbes.leicaInputPath(), false, false);

        // The same variable set createContrastSetup uses: all curvatures, all thicknesses.
        var variables = new java.util.ArrayList<Var>();
        for (int s = 0; s < prescription._surfaces.length; s++) {
            if (prescription._surfaces[s]._radius != 0.0)
                variables.add(new VarRadius(prescription, s));
        }
        for (int s = 0; s < prescription._surfaces.length; s++) {
            if (prescription._surfaces[s]._thickness != 0.0)
                variables.add(new VarThickness(prescription, s));
        }

        double[][] base = factors(prescription, mode);

        double worst = 0.0;
        String worstWhere = "";
        int movers = 0;
        double sumAbs = 0.0;
        for (Var v : variables) {
            double keep = v.read_from_prescription();
            v.set_scaled_value(keep + v._d_delta * v.get_scaling_factor());
            v.write_to_prescription();
            double[][] probed = factors(prescription, mode);
            v.set_scaled_value(keep);
            v.write_to_prescription();

            double maxDelta = 0.0;
            for (int fi = 0; fi < base.length; fi++)
                for (int k = 0; k < 4; k++)
                    maxDelta = Math.max(maxDelta, Math.abs(probed[fi][k] - base[fi][k]));
            sumAbs += maxDelta;
            if (maxDelta > 1.0e-9) movers++;
            if (maxDelta > worst) {
                worst = maxDelta;
                worstWhere = v.toString();
            }
        }

        System.out.println("=== VigType." + mode + " ===");
        System.out.printf("variables probed at their Jacobian step : %d%n", variables.size());
        System.out.printf("variables that move a vignetting factor : %d%n", movers);
        System.out.printf("mean |max vig change| per probe         : %.3e%n", sumAbs / variables.size());
        System.out.printf("worst |vig change| in one probe         : %.3e   (%s)%n", worst, worstWhere);
        System.out.println();
    }

    /** Vignetting factors [field][vux, vlx, vuy, vly] for a freshly built model. */
    private static double[][] factors(Prescription prescription, VigType mode) {
        OpticalModel model = new RayOpticsModelBuilder(prescription)
                .build_optical_model(true, FIELDS, false, mode, true, 0);
        var fields = model.optical_spec.fov.fields;
        double[][] out = new double[fields.length][4];
        for (int fi = 0; fi < fields.length; fi++) {
            out[fi][0] = fields[fi].vux;
            out[fi][1] = fields[fi].vlx;
            out[fi][2] = fields[fi].vuy;
            out[fi][3] = fields[fi].vly;
        }
        return out;
    }
}
