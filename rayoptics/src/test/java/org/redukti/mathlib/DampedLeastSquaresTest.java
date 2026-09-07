package org.redukti.mathlib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DampedLeastSquaresTest {
    @Test void solvesRosenbrockWithFiniteDifferences() {
        var o = new DampedLeastSquares.Options(); o.maxIterations = 200;
        var r = new DampedLeastSquares(x -> new double[]{10 * (x[1] - x[0]*x[0]), 1-x[0]},
                new double[]{-1.2, 1}, o).run();
        assertTrue(r.success(), r.message());
        assertArrayEquals(new double[]{1, 1}, r.x(), 1e-5);
        assertTrue(r.nfev() > r.njev());
    }

    @Test void solvesEqualityAndReportsMultipliers() {
        var p = new DampedLeastSquares.Problem() {
            public double[] residuals(double[] x) { return new double[]{x[0]-2, x[1]-3}; }
            public double[][] jacobian(double[] x) { return new double[][]{{1,0},{0,1}}; }
            public double[] equalities(double[] x) { return new double[]{x[0]+x[1]-1}; }
        };
        var r = new DampedLeastSquares(p, new double[]{0,0}).run();
        assertTrue(r.success(), r.message());
        assertArrayEquals(new double[]{0,1}, r.x(), 1e-6);
        assertEquals(2, r.equalityMultipliers()[0], 1e-5);
    }

    @Test void addsAndDropsActiveInequalities() {
        var p = new DampedLeastSquares.Problem() {
            public double[] residuals(double[] x) { return new double[]{x[0]+2, x[1]-3}; }
            public double[] inequalities(double[] x) { return x.clone(); }
        };
        var r = new DampedLeastSquares(p, new double[]{1,0}).run();
        assertTrue(r.success(), r.message());
        assertArrayEquals(new double[]{0,3}, r.x(), 1e-5);
        assertArrayEquals(new int[]{0}, r.activeInequalities());
        assertEquals(-2, r.inequalityMultipliers()[0], 1e-5);
    }

    @Test void handlesRedundantConstraintsAndUnderdeterminedResiduals() {
        var p = new DampedLeastSquares.Problem() {
            public double[] residuals(double[] x) { return new double[]{x[0]+x[1]-2}; }
            public double[] equalities(double[] x) { return new double[]{x[0]-x[1], 2*(x[0]-x[1])}; }
        };
        var r = new DampedLeastSquares(p, new double[]{0,0}).run();
        assertTrue(r.success(), r.message());
        assertArrayEquals(new double[]{1,1}, r.x(), 1e-6);
    }

    @Test void singularUndampedSystemReturnsMinimumNormStep() {
        var o = new DampedLeastSquares.Options(); o.damping = new double[]{0};
        var p = new DampedLeastSquares.Problem() {
            public double[] residuals(double[] x) { return new double[]{x[0] + 2*x[1] - 5}; }
            public double[][] jacobian(double[] x) { return new double[][]{{1,2}}; }
        };
        var r = new DampedLeastSquares(p, new double[]{0,0}, o).run();
        assertTrue(r.success(), r.message());
        assertArrayEquals(new double[]{1,2}, r.x(), 1e-12);
    }

    @Test void zeroNormalMatrixHasFiniteZeroStep() {
        var o = new DampedLeastSquares.Options(); o.damping = new double[]{0};
        var r = new DampedLeastSquares(x -> new double[]{1}, new double[]{0}, o).run();
        assertTrue(r.success(), r.message());
        assertEquals(0, r.x()[0]);
        assertEquals(.5, r.cost());
    }

    @Test void usesSensitivityDampingAndUniformTrustScaling() {
        var o = new DampedLeastSquares.Options(); o.damping = new double[]{1,2};
        o.dampingMode = DampedLeastSquares.DampingMode.SENSITIVITY;
        o.trustRadii = new double[]{.1, Double.POSITIVE_INFINITY};
        var r = new DampedLeastSquares(x -> new double[]{2*x[0]-4, x[1]-3}, new double[]{0,0}, o).step();
        assertArrayEquals(new double[]{.1,.1}, r.x(), 1e-9);
        assertArrayEquals(new double[]{4,2}, r.history().get(0).dampingDiagonal(), 1e-8);
        assertEquals(.1, r.history().get(0).trustScale(), 1e-9);
    }

    @Test void adaptiveDampingRecoversRejectedFullStep() {
        var o = new DampedLeastSquares.Options(); o.maxLineSearch = 0; o.maxDampingAttempts = 12;
        o.adaptiveDamping = true;
        var r = new DampedLeastSquares(x -> new double[]{x[0]*x[0]-1}, new double[]{.1}, o).step();
        assertEquals(1, r.iterations());
        assertTrue(r.cost() < .5*.99*.99);
        assertTrue(r.history().get(0).dampingAttempts() > 0);
    }

    @Test void infeasibleStationaryPointIsNotSuccess() {
        var p = new DampedLeastSquares.Problem() {
            public double[] residuals(double[] x) { return new double[]{x[0]}; }
            public double[] equalities(double[] x) { return new double[]{1}; }
        };
        var r = new DampedLeastSquares(p, new double[]{0}).run();
        assertFalse(r.success()); assertEquals("line search failed", r.message());
    }

    @Test void rejectsNonfiniteTrialsWithoutLosingAcceptedPoint() {
        var o = new DampedLeastSquares.Options(); o.maxLineSearch = 0;
        var p = new DampedLeastSquares.Problem() {
            public double[] residuals(double[] x) { return new double[]{x[0] > 0 ? Double.NaN : 1}; }
            public double[][] jacobian(double[] x) { return new double[][]{{-1}}; }
        };
        var r = new DampedLeastSquares(p, new double[]{0}, o).run();
        assertFalse(r.success()); assertEquals(0, r.x()[0]);
    }

    @Test void iterationLimitIsNotSuccess() {
        var o = new DampedLeastSquares.Options(); o.maxIterations = 2;
        var r = new DampedLeastSquares(x -> new double[]{10 * (x[1] - x[0]*x[0]), 1-x[0]},
                new double[]{-1.2, 1}, o).run();
        // Unconstrained, so the point is trivially feasible; Prysm calls that success.
        assertTrue(r.feasible());
        assertEquals(DampedLeastSquares.Status.ITERATION_LIMIT, r.status());
        assertFalse(r.success(), "an exhausted budget must not read as convergence");
    }

    @Test void buildsOneJacobianPerIterationDespiteDampingRetries() {
        var o = new DampedLeastSquares.Options();
        o.maxLineSearch = 0; o.maxDampingAttempts = 12; o.adaptiveDamping = true;
        int[] calls = new int[1];
        var p = new DampedLeastSquares.Problem() {
            public double[] residuals(double[] x) { return new double[]{x[0]*x[0]-1}; }
            public double[][] jacobian(double[] x) { calls[0]++; return new double[][]{{2*x[0]}}; }
        };
        var r = new DampedLeastSquares(p, new double[]{.1}, o).step();
        // The retries reject a step without moving x, so the Jacobian there is identical.
        assertTrue(r.history().get(0).dampingAttempts() > 0, "expected the first step to be rejected");
        assertEquals(1, calls[0]);
        assertEquals(1, r.njev());
    }

    @Test void usesSuppliedConstraintJacobianInsteadOfDifferencing() {
        int[] differenced = new int[1], supplied = new int[1];
        var r = solveBoundedCorner(differenced, false);
        var s = solveBoundedCorner(supplied, true);
        assertTrue(r.success(), r.message());
        assertTrue(s.success(), s.message());
        // Same answer, and the exact Jacobian is the more accurate of the two: the
        // differenced run stops a few 1e-6 short, the step size showing through.
        assertArrayEquals(new double[]{0, 3}, r.x(), 1e-5);
        assertArrayEquals(new double[]{0, 3}, s.x(), 1e-10);
        // Differencing costs 2n=4 extra constraint calls per iteration; supplying the
        // Jacobian leaves only the one call per candidate point.
        assertTrue(supplied[0] < differenced[0],
                supplied[0] + " constraint evaluations supplied vs " + differenced[0] + " differenced");
    }

    private static DampedLeastSquares.Result solveBoundedCorner(int[] calls, boolean supplyJacobian) {
        var p = new DampedLeastSquares.Problem() {
            public double[] residuals(double[] x) { return new double[]{x[0]+2, x[1]-3}; }
            public double[][] jacobian(double[] x) { return new double[][]{{1,0},{0,1}}; }
            public double[] inequalities(double[] x) { calls[0]++; return x.clone(); }
            public double[][] inequalityJacobian(double[] x) {
                return supplyJacobian ? new double[][]{{1,0},{0,1}} : null;
            }
        };
        return new DampedLeastSquares(p, new double[]{1,0}).run();
    }

    @Test void feasibilityToleranceGovernsProgressAlongACurvedBoundary() {
        // Minimise the distance to (3,3) inside the unit disc. The optimum is on the
        // boundary, and the boundary is curved, so a step along its linearisation leaves
        // the disc by a second-order amount. Demand feasibility to 1e-12 and the line
        // search must halve until that amount vanishes, which stalls the solve; allow a
        // little slack and it converges. This is the mechanism behind the Beam42 default
        // of 1e-6 for a Bound, which is a length in mm.
        var tight = solveInsideUnitDisc(1e-12);
        var slack = solveInsideUnitDisc(1e-6);
        double optimum = Math.sqrt(0.5);
        assertArrayEquals(new double[]{optimum, optimum}, slack.x(), 1e-3, slack.message());
        assertTrue(slack.iterations() < tight.iterations(),
                "slack took " + slack.iterations() + " iterations, tight " + tight.iterations());
        // Both stay inside the disc: the slack tolerance buys progress, not violations.
        for (var r : new DampedLeastSquares.Result[]{tight, slack})
            assertTrue(r.constraintViolation() <= 1e-6, "violation " + r.constraintViolation());
    }

    private static DampedLeastSquares.Result solveInsideUnitDisc(double constraintTolerance) {
        var o = new DampedLeastSquares.Options();
        o.constraintTolerance = constraintTolerance;
        o.dampingMode = DampedLeastSquares.DampingMode.SENSITIVITY;
        o.adaptiveDamping = true;
        o.maxIterations = 60;
        var p = new DampedLeastSquares.Problem() {
            public double[] residuals(double[] x) { return new double[]{x[0]-3, x[1]-3}; }
            public double[][] jacobian(double[] x) { return new double[][]{{1,0},{0,1}}; }
            public double[] inequalities(double[] x) { return new double[]{1 - x[0]*x[0] - x[1]*x[1]}; }
            public double[][] inequalityJacobian(double[] x) { return new double[][]{{-2*x[0], -2*x[1]}}; }
        };
        return new DampedLeastSquares(p, new double[]{.1, .1}, o).run();
    }

    @Test void rejectsSuppliedConstraintJacobianOfTheWrongShape() {
        var p = new DampedLeastSquares.Problem() {
            public double[] residuals(double[] x) { return new double[]{x[0]}; }
            public double[] inequalities(double[] x) { return new double[]{x[0], 1-x[0]}; }
            public double[][] inequalityJacobian(double[] x) { return new double[][]{{1}}; }
        };
        var solver = new DampedLeastSquares(p, new double[]{.5});
        assertThrows(IllegalArgumentException.class, solver::run);
    }

    @Test void validatesOptionsAndSnapshotsState() {
        var o = new DampedLeastSquares.Options(); o.trustRadii = new double[]{Double.NaN};
        assertThrows(IllegalArgumentException.class, () -> new DampedLeastSquares(x -> x, new double[]{1}, o));
        var solver = new DampedLeastSquares(x -> x, new double[]{1});
        solver.result().x()[0] = 99;
        assertEquals(1, solver.result().x()[0]);
        solver.run();
        assertThrows(IllegalStateException.class, solver::step);
    }
}
