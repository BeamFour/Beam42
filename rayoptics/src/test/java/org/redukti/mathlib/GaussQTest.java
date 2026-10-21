package org.redukti.mathlib;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GaussQTest {

    /*
     * Order-16 reference rule from John Burkardt's QUADRATURE_RULES_LEGENDRE
     * dataset, distributed under the GNU LGPL:
     * https://people.sc.fsu.edu/~jburkardt/datasets/quadrature_rules_legendre/
     */
    private static final double[] ORDER_16_NODES = {
            -0.9894009349916499,
            -0.9445750230732326,
            -0.8656312023878318,
            -0.7554044083550030,
            -0.6178762444026438,
            -0.4580167776572274,
            -0.2816035507792589,
            -0.09501250983763744,
            0.09501250983763744,
            0.2816035507792589,
            0.4580167776572274,
            0.6178762444026438,
            0.7554044083550030,
            0.8656312023878318,
            0.9445750230732326,
            0.9894009349916499
    };

    private static final double[] ORDER_16_WEIGHTS = {
            0.02715245941175407,
            0.06225352393864787,
            0.09515851168249277,
            0.1246289712555339,
            0.1495959888165767,
            0.1691565193950026,
            0.1826034150449236,
            0.1894506104550686,
            0.1894506104550686,
            0.1826034150449236,
            0.1691565193950026,
            0.1495959888165767,
            0.1246289712555339,
            0.09515851168249277,
            0.06225352393864787,
            0.02715245941175407
    };

    @Test
    void directRuleHasKnownThreePointValues() {
        double[][] rule = GaussQ.gauss_legendre_nodes_and_weights(3);

        assertEquals(-Math.sqrt(3.0 / 5.0), rule[0][0], 1.0e-15);
        assertEquals(0.0, rule[0][1], 1.0e-15);
        assertEquals(Math.sqrt(3.0 / 5.0), rule[0][2], 1.0e-15);
        assertEquals(5.0 / 9.0, rule[1][0], 5.0e-15);
        assertEquals(8.0 / 9.0, rule[1][1], 5.0e-15);
        assertEquals(5.0 / 9.0, rule[1][2], 5.0e-15);
    }

    @Test
    void golubWelschAgreesWithDirectRootRefinement() {
        for (int order = 1; order <= 64; order++) {
            double[][] direct = GaussQ.gauss_legendre_nodes_and_weights(order);
            double[][] golubWelsch =
                    GaussQ.gauss_legendre_nodes_and_weights_golub_welsch(order);

            for (int i = 0; i < order; i++) {
                assertEquals(direct[0][i], golubWelsch[0][i], 2.0e-14,
                        "node, order=" + order + ", index=" + i);
                assertEquals(direct[1][i], golubWelsch[1][i], 2.0e-14,
                        "weight, order=" + order + ", index=" + i);
            }
        }
    }

    @Test
    void bothRulesAgreeWithPublishedOrderSixteenData() {
        assertReferenceRule(GaussQ.gauss_legendre_nodes_and_weights(16));
        assertReferenceRule(
                GaussQ.gauss_legendre_nodes_and_weights_golub_welsch(16));
    }

    @Test
    void bothRulesIntegratePolynomialMoments() {
        for (int order = 1; order <= 8; order++) {
            assertMoments(GaussQ.gauss_legendre_nodes_and_weights(order), order);
            assertMoments(
                    GaussQ.gauss_legendre_nodes_and_weights_golub_welsch(order), order);
        }
    }

    @Test
    void rulesRejectNonPositiveOrder() {
        assertThrows(IllegalArgumentException.class,
                () -> GaussQ.gauss_legendre_nodes_and_weights(0));
        assertThrows(IllegalArgumentException.class,
                () -> GaussQ.gauss_legendre_nodes_and_weights_golub_welsch(-1));
    }

    private static void assertMoments(double[][] rule, int order) {
        assertEquals(2.0, sum(rule[1]), 2.0e-14);
        for (int degree = 0; degree < 2 * order; degree++) {
            double actual = 0.0;
            for (int i = 0; i < order; i++) {
                actual += rule[1][i] * Math.pow(rule[0][i], degree);
            }
            double expected = degree % 2 == 0 ? 2.0 / (degree + 1.0) : 0.0;
            assertEquals(expected, actual, 2.0e-14,
                    "order=" + order + ", degree=" + degree);
        }
        for (int i = 1; i < order; i++) {
            assertTrue(rule[0][i - 1] < rule[0][i]);
        }
    }

    private static void assertReferenceRule(double[][] rule) {
        for (int i = 0; i < ORDER_16_NODES.length; i++) {
            assertEquals(ORDER_16_NODES[i], rule[0][i], 5.0e-15,
                    "node, index=" + i);
            assertEquals(ORDER_16_WEIGHTS[i], rule[1][i], 5.0e-15,
                    "weight, index=" + i);
        }
    }

    private static double sum(double[] values) {
        double result = 0.0;
        for (double value : values) {
            result += value;
        }
        return result;
    }
}
