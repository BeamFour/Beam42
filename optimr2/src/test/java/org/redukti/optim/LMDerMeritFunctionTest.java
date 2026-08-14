package org.redukti.optim;

import org.junit.jupiter.api.Test;
import org.redukti.mathlib.LMLSolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LMDerMeritFunctionTest {

    @Test
    void usesCentralDifferenceWhenBothPerturbationsAreValid() {
        Fixture fixture = new Fixture(value -> true);
        double[] jacobian = new double[1];

        assertTrue(fixture.merit.buildJacobian(new double[]{1.0}, jacobian, 1));

        assertEquals(2.0, jacobian[0], 1.0e-8);
        assertEquals(1.0, fixture.analysis.value, 0.0);
    }

    @Test
    void reducesStepAndUsesOneSidedDifferenceAtValidityBoundary() {
        Fixture fixture = new Fixture(value -> value <= 1.0);
        double[] jacobian = new double[1];

        assertTrue(fixture.merit.buildJacobian(new double[]{1.0}, jacobian, 1));

        assertEquals(2.0, jacobian[0], 1.0e-5);
        assertEquals(1.0, fixture.analysis.value, 0.0);
    }

    @Test
    void rejectsUnknownDerivativeAndRestoresBasePoint() {
        Fixture fixture = new Fixture(value -> value == 1.0);
        double[] jacobian = new double[1];

        assertFalse(fixture.merit.buildJacobian(new double[]{1.0}, jacobian, 1));

        assertEquals(1.0, fixture.analysis.value, 0.0);
        assertTrue(fixture.analysis.valid);
        assertEquals(1.0, fixture.goal.value(), 0.0);
    }

    private interface Validity {
        boolean valid(double value);
    }

    private static final class Fixture {
        final TestAnalysis analysis;
        final TestVar variable;
        final TestGoal goal;
        final LMDerMeritFunction merit;

        Fixture(Validity validity) {
            analysis = new TestAnalysis(validity);
            variable = new TestVar(analysis);
            goal = new TestGoal(analysis);
            merit = new LMDerMeritFunction(
                    analysis, new Var[]{variable}, new Goal[]{goal}, false);
        }
    }

    private static final class TestAnalysis extends Analysis {
        private final Validity validity;
        double value = 1.0;
        boolean valid = true;

        TestAnalysis(Validity validity) {
            super(null, new double[]{0.0}, new int[]{1});
            this.validity = validity;
        }

        @Override
        public void compute() {
            valid = validity.valid(value);
            if (!valid) throw new IllegalStateException("synthetic killed ray");
        }
    }

    private static final class TestVar extends Var {
        private final TestAnalysis analysis;

        TestVar(TestAnalysis analysis) {
            super(null);
            this.analysis = analysis;
            _d_delta = 1.0e-4;
        }

        @Override
        public double read_from_prescription() {
            set_unscaled_value(analysis.value);
            return get_scaled_value();
        }

        @Override
        public void write_to_prescription() {
            analysis.value = get_unscaled_value();
        }
    }

    private static final class TestGoal extends Goal {
        private final TestAnalysis analysis;

        TestGoal(TestAnalysis analysis) {
            super(analysis, 0.0, 1.0);
            this.analysis = analysis;
        }

        @Override
        public double value() {
            return analysis.valid ? analysis.value * analysis.value : LMLSolver.BIGVAL;
        }
    }
}
