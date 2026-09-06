package org.redukti.optim;

import org.junit.jupiter.api.Test;
import org.redukti.mathlib.DampedLeastSquares;
import static org.junit.jupiter.api.Assertions.*;

class DampedLeastSquaresSolverTest {
    private static class Fixture extends Analysis {
        double value = 1, computed;
        boolean failAboveTwo;
        Fixture() { super(null, new double[]{0}, new int[]{1}); }
        @Override public void compute() {
            if (failAboveTwo && value > 2) throw new IllegalStateException("killed ray");
            computed = value;
        }
        Var variable() {
            return new Var(null) {
                public double read_from_prescription() { set_unscaled_value(value); return get_scaled_value(); }
                public void write_to_prescription() { value = get_unscaled_value(); }
            };
        }
        Goal goal(double target, double weight) {
            return new Goal(this, target, weight) { public double value() { return computed; } };
        }
    }

    @Test void preservesWeightedTargetsAndFinalAnalysis() {
        var f = new Fixture();
        var solver = new DampedLeastSquaresSolver(f, new Var[]{f.variable()},
                new Goal[]{f.goal(2,1), f.goal(4,3)}, new DampedLeastSquares.Options());
        assertEquals(1, solver.solve());
        assertEquals(3.5, f.value, 1e-6);
        assertEquals(f.value, f.computed);
        assertEquals(1.5, solver.result().cost(), 1e-8);
    }

    @Test void evaluatesHardConstraintsAtTheirOwnPrescription() {
        var f = new Fixture();
        var solver = new DampedLeastSquaresSolver(f, new Var[]{f.variable()},
                new Goal[]{f.goal(4,1)}, new DampedLeastSquares.Options(), x -> new double[0],
                x -> { assertEquals(x[0], f.computed); return new double[]{2-f.computed}; });
        assertEquals(1, solver.solve());
        assertEquals(2, f.value, 1e-8);
        assertEquals(f.value, f.computed);
    }

    @Test void restoresAcceptedPrescriptionAfterKilledRay() {
        var f = new Fixture(); f.failAboveTwo = true;
        var o = new DampedLeastSquares.Options(); o.maxLineSearch = 0;
        var solver = new DampedLeastSquaresSolver(f, new Var[]{f.variable()},
                new Goal[]{f.goal(4,1)}, o);
        assertEquals(0, solver.solve());
        assertEquals("line search failed", solver.result().message());
        assertEquals(1, f.value); assertEquals(1, f.computed);
    }
}
