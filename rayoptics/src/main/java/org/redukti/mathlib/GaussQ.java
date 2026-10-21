/*
 * Gaussian quadrature utilities.
 *
 * The direct Gauss-Legendre implementation follows W. H. Press et al.,
 * Numerical Recipes: The Art of Scientific Computing, 3rd ed., section 4.6
 * (Cambridge University Press, 2007).
 *
 * The alternative implementation is a Java port of the Legendre, no-fixed-
 * endpoint path through gaussq.f from Netlib's "go" (Golden Oldies)
 * collection: https://netlib.org/go/gaussq.f
 *
 * Original gaussq.f notices:
 *
 *     original version 20 jan 1975 from stanford
 *     modified 21 dec 1983 by eric grosse
 *       imtql2 => gausq2
 *       hex constant => d1mach (from core library)
 *       compute pi using datan
 *       removed accuracy claims, description of method
 *       added single precision version
 *
 * References retained from gaussq.f:
 *
 * 1. G. H. Golub and J. H. Welsch, "Calculation of Gaussian Quadrature
 *    Rules," Mathematics of Computation 23 (1969), 221-230.
 * 2. G. H. Golub, "Some Modified Matrix Eigenvalue Problems," SIAM Review
 *    15 (1973), 318-334, section 7.
 * 3. A. H. Stroud and D. Secrest, Gaussian Quadrature Formulas,
 *    Prentice-Hall, Englewood Cliffs, N.J., 1966.
 */
package org.redukti.mathlib;

/** Generates Gauss-Legendre nodes and weights on {@code [-1, 1]}. */
public final class GaussQ {

    private GaussQ() {
    }

    /**
     * Computes an {@code order}-point Gauss-Legendre rule by refining the
     * roots of the Legendre polynomial with Newton's method.
     *
     * @return {@code {nodes, weights}}, with nodes in ascending order
     */
    public static double[][] gauss_legendre_nodes_and_weights(int order) {
        requirePositiveOrder(order);

        double[] nodes = new double[order];
        double[] weights = new double[order];
        int rootsToFind = (order + 1) / 2;

        for (int i = 0; i < rootsToFind; i++) {
            double x = Math.cos(Math.PI * (i + 0.75) / (order + 0.5));
            double derivative;
            double delta;
            do {
                double p0 = 1.0;
                double p1 = x;
                for (int degree = 2; degree <= order; degree++) {
                    double p2 = ((2.0 * degree - 1.0) * x * p1
                            - (degree - 1.0) * p0) / degree;
                    p0 = p1;
                    p1 = p2;
                }
                double polynomial = p1;
                double previousPolynomial = p0;
                derivative = order * (x * polynomial - previousPolynomial)
                        / (x * x - 1.0);
                delta = polynomial / derivative;
                x -= delta;
            } while (Math.abs(delta) > 1.0e-15);

            double weight = 2.0 / ((1.0 - x * x) * derivative * derivative);
            nodes[i] = -x;
            nodes[order - 1 - i] = x;
            weights[i] = weight;
            weights[order - 1 - i] = weight;
        }
        return new double[][]{nodes, weights};
    }

    /**
     * Computes the same rule using the Golub-Welsch Jacobi-matrix method.
     * This is the {@code kind=1}, {@code kpts=0} path through Netlib
     * {@code gaussq.f}.
     *
     * @return {@code {nodes, weights}}, with nodes in ascending order
     */
    public static double[][] gauss_legendre_nodes_and_weights_golub_welsch(int order) {
        requirePositiveOrder(order);

        double[] nodes = new double[order];
        double[] offDiagonal = new double[order];
        double[] firstComponents = new double[order];

        /*
         * kind = 1: Legendre polynomials p(x) on (-1, +1), w(x) = 1.
         * The matrix is symmetric.  nodes contains its diagonal elements and
         * offDiagonal its off-diagonal elements.
         */
        for (int i = 1; i < order; i++) {
            double index = i;
            offDiagonal[i - 1] = index / Math.sqrt(4.0 * index * index - 1.0);
        }

        /* The first row of the identity matrix. */
        firstComponents[0] = 1.0;

        int errorIndex = gausq2(nodes, offDiagonal, firstComponents);
        if (errorIndex != 0) {
            throw new ArithmeticException(
                    "Gauss-Legendre eigensolver did not converge at index " + errorIndex);
        }

        /* muzero = integral from -1 to 1 of w(x) dx = 2. */
        double[] weights = new double[order];
        for (int i = 0; i < order; i++) {
            weights[i] = 2.0 * firstComponents[i] * firstComponents[i];
        }
        return new double[][]{nodes, weights};
    }

    private static void requirePositiveOrder(int order) {
        if (order < 1) {
            throw new IllegalArgumentException("Quadrature order must be at least 1");
        }
    }

    /**
     * This routine is a translation of an ALGOL procedure, Numerische
     * Mathematik 12, 377-383 (1968), by Martin and Wilkinson, as modified in
     * Numerische Mathematik 15, 450 (1970), by Dubrulle; Handbook for
     * Automatic Computation, vol. II, Linear Algebra, 241-248 (1971).
     * This is a modified version of the EISPACK routine IMTQL2.
     *
     * It finds the eigenvalues and first components of the eigenvectors of a
     * symmetric tridiagonal matrix by the implicit QL method.
     *
     * @param diagonal on input, the diagonal elements of the symmetric
     *                 tridiagonal matrix; on output, its eigenvalues in
     *                 ascending order
     * @param offDiagonal on input, the subdiagonal elements in positions
     *                    {@code 0} through {@code n - 2}; position
     *                    {@code n - 1} is unused. The array is destroyed
     *                    during the calculation.
     * @param firstComponents on input, the first row of the identity matrix,
     *                        {@code [1, 0, ..., 0]}; on output, the first
     *                        component of each normalized eigenvector, in the
     *                        same order as the returned eigenvalues
     * @return zero for normal completion, or the one-based index of an
     *         eigenvalue not determined after 30 iterations
     */
    private static int gausq2(
            double[] diagonal, double[] offDiagonal, double[] firstComponents) {
        int order = diagonal.length;
        if (order == 1) {
            return 0;
        }

        offDiagonal[order - 1] = 0.0;
        double machinePrecision = Math.ulp(1.0);

        for (int l = 0; l < order; l++) {
            int iterations = 0;
            while (true) {
                /* Look for a small sub-diagonal element. */
                int m;
                for (m = l; m < order; m++) {
                    if (m == order - 1
                            || Math.abs(offDiagonal[m]) <= machinePrecision
                            * (Math.abs(diagonal[m]) + Math.abs(diagonal[m + 1]))) {
                        break;
                    }
                }

                double p = diagonal[l];
                if (m == l) {
                    break;
                }
                if (iterations == 30) {
                    return l + 1;
                }
                iterations++;

                /* Form shift. */
                double g = (diagonal[l + 1] - p) / (2.0 * offDiagonal[l]);
                double r = Math.hypot(g, 1.0);
                g = diagonal[m] - p
                        + offDiagonal[l] / (g + Math.copySign(r, g));
                double sine = 1.0;
                double cosine = 1.0;
                p = 0.0;

                for (int i = m - 1; i >= l; i--) {
                    double f = sine * offDiagonal[i];
                    double b = cosine * offDiagonal[i];
                    if (Math.abs(f) >= Math.abs(g)) {
                        cosine = g / f;
                        r = Math.hypot(cosine, 1.0);
                        offDiagonal[i + 1] = f * r;
                        sine = 1.0 / r;
                        cosine *= sine;
                    }
                    else {
                        sine = f / g;
                        r = Math.hypot(sine, 1.0);
                        offDiagonal[i + 1] = g * r;
                        cosine = 1.0 / r;
                        sine *= cosine;
                    }
                    g = diagonal[i + 1] - p;
                    r = (diagonal[i] - g) * sine + 2.0 * cosine * b;
                    p = sine * r;
                    diagonal[i + 1] = g + p;
                    g = cosine * r - b;

                    /* Form first component of vector. */
                    f = firstComponents[i + 1];
                    firstComponents[i + 1] = sine * firstComponents[i] + cosine * f;
                    firstComponents[i] = cosine * firstComponents[i] - sine * f;
                }

                diagonal[l] -= p;
                offDiagonal[l] = g;
                offDiagonal[m] = 0.0;
            }
        }

        /* Order eigenvalues and eigenvectors. */
        for (int i = 0; i < order - 1; i++) {
            int smallest = i;
            double value = diagonal[i];
            for (int j = i + 1; j < order; j++) {
                if (diagonal[j] < value) {
                    smallest = j;
                    value = diagonal[j];
                }
            }
            if (smallest != i) {
                diagonal[smallest] = diagonal[i];
                diagonal[i] = value;
                double component = firstComponents[i];
                firstComponents[i] = firstComponents[smallest];
                firstComponents[smallest] = component;
            }
        }
        return 0;
    }
}
