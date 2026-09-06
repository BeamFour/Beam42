# Constrained damped least squares

Java port of Prysm's [DampedLeastSquares](https://github.com/brandondube/prysm/blob/master/prysm/x/optym/least_squares.py#L430), in `org.redukti.mathlib.DampedLeastSquares`.
The MIT attribution is included in the built jar. The local `mathlib.jama` classes
supply the pivoted LU decomposition and SVD factors. The solver applies an SVD
pseudoinverse for singular KKT systems; no additional math dependency is needed.

Use an existing `OptimizationBuilder.OptimizationSetup`:

```java
var options = new DampedLeastSquares.Options();
options.maxIterations = 100;
options.dampingMode = DampedLeastSquares.DampingMode.SENSITIVITY;
options.adaptiveDamping = true;
var solver = setup.dampedLeastSquaresSolver(options);
solver.solve();
System.out.println(solver.result());
```

For hard constraints, pass two vector callbacks (empty arrays mean no constraints):

```java
var solver = setup.dampedLeastSquaresSolver(options,
        x -> new double[]{x[0] + x[1] - 1.0},  // equality: zero
        x -> new double[]{x[0], 2.0 - x[0]});   // inequalities: >= zero
```

These illustrative constraints act on **scaled optimizer variables**, in the order
returned by `setup.variables()`. Callbacks can also read `setup.analysis()` and
its prescription, which the adapter updates before each callback. Callbacks must
not mutate them. Existing constraint Goals remain weighted residual penalties;
they are not automatically converted to hard constraints.

The adapter reuses Beam42's square-root goal weighting, target subtraction, and
per-variable finite-difference Jacobian with failed-ray handling. It restores the
accepted prescription and recomputes Analysis after termination or exceptions.
Invalid residual trials are rejected; an invalid Jacobian at the accepted point
raises an exception. Constraint callback exceptions propagate.

The standalone solver accepts a `Problem` with `residuals(x)` and optional
`jacobian(x)`, `equalities(x)`, and `inequalities(x)`. Missing residual Jacobians
and constraint Jacobians use central finite differences. `step()` performs one
iteration; `run()` runs to termination; `result()` returns a detached snapshot.

Identity/sensitivity damping, scalar or per-variable damping, uniform trust-radius
scaling, adaptive damping, multipliers, and accepted-iteration history are supported.
Nonlinear constraints use sequential linearization and feasibility-first line
search; difficult feasible nonlinear boundaries may cause line-search failure.

Port conventions and differences:

- Like Prysm, reaching the iteration limit reports success if the current point
  is feasible. Check `message()` as well as `success()` to distinguish convergence
  from budget exhaustion. `iterations()` counts accepted steps.
- `solve()` returns 1 on success and 0 on failure, not MINPACK status codes.
- Inequality multipliers use the KKT sign convention and are nonpositive when active.
- `nfev` counts calls to the residual callback; `njev` counts Jacobian requests;
  `ncev` counts individual equality/inequality callback calls. Evaluations internal
  to Beam42's supplied Jacobian are not included in `nfev`.
- Invalid inputs and changing callback dimensions are rejected. Active-set
  exhaustion reports failure rather than returning inconsistent multipliers.
- No Python governor framework is required; stopping checks are implemented locally.

Run the focused tests:

```text
mvn -pl rayoptics -Dtest=DampedLeastSquaresTest,DampedLeastSquaresSolverTest,LMDerMeritFunctionTest,OptimizationBuilderTest test
```

## Otus comparison experiment

An opt-in method in `ZeissOtusML50mmTest` compares the existing LMDER solver,
default DLS (25 iterations), and sensitivity-scaled adaptive DLS (100 iterations):

```text
mvn -pl rayoptics -Dtest=ZeissOtusML50mmTest#comparesDampedLeastSquaresWithLmder -Doptimization.compareSolvers=true test
```

Each solver loads a fresh copy of the patent prescription and uses the test's
fixed contrast setup. Add `-Doptimization.comparisonScenario=direct` to use its
fixed direct-MTF setup instead. Select a subset or change the execution order
with `-Doptimization.comparisonSolvers=lmder,dls-adaptive`; set the adaptive DLS
iteration cap with `-Doptimization.dlsIterations=100`.

Results, final prescriptions, and DLS iteration histories are written under
`rayoptics/target/solver-comparison/<scenario>/`. Rerunning a scenario overwrites
its results CSV and the selected solvers' artifacts. The comparison measures
solve time and actual Analysis compute attempts (including Jacobian probes,
restoration, and failed rays). Warm-up and final optical measurement are excluded.
It reports merit RMS plus independently measured spot radii and 40 cycles/mm MTF.

This compares the configured stopping policies, not equal evaluation budgets.
Single-run timings include JVM warm-up/order effects and should be treated as
indicative; reverse the solver order or repeat runs for timing studies. A DLS
iteration-limit success means feasibility, not convergence. The experiment is
skipped during normal test runs and does not change the existing absolute lens
regression expectations.

### Measured contrast comparison (2026-09-06)

The opt-in experiment passed on the local machine using 44 variables and 1,010
goals. All three runs started at RMS merit **0.0872013274**. No additional hard
constraints or trust radii were supplied; both solvers used the same Beam42
weighted-residual and ray-trace-aware Jacobian implementation.

| Solver configuration | Solve seconds | Analysis evaluations | Final RMS merit | Stopping reason |
| --- | ---: | ---: | ---: | --- |
| LMDER, existing settings | 71.57 | 3,385 | 0.007114288 | MINPACK info=1 (function tolerance) |
| DLS, defaults | 49.64 | 2,364 | 0.019993057 | 25-iteration limit |
| DLS, sensitivity damping + adaptive damping, cap 100 | 62.37 | 2,972 | 0.007167238 | Step tolerance, 21 accepted iterations |

Adaptive DLS requested 31 Jacobians (including damping retries) and made 181
residual callback calls. LMDER reported 37 Jacobians and 54 residual calls.
The Analysis evaluation counts above include the actual work inside Jacobian
construction; they are the more useful cost comparison here.

Final optical measurements, in field order **0.0, 0.3, 0.7, 1.0**:

| Measurement | LMDER | Default DLS | Adaptive DLS |
| --- | --- | --- | --- |
| Spot RMS (µm) | 2.4030, 3.4263, 3.8247, 3.9637 | 10.7539, 9.9502, 8.5401, 7.2392 | 2.4531, 3.4193, 3.8297, 4.1043 |
| Sagittal MTF at 40 cycles/mm | .9092, .8696, .7950, .7945 | .0769, .2139, .3445, .4712 | .9083, .8702, .7997, .8007 |
| Tangential MTF at 40 cycles/mm | .9092, .8080, .8054, .7870 | .0769, .1680, .4589, .7210 | .9083, .8019, .8059, .7626 |
| Effective focal length (mm) | 50.1510 | 50.1898 | 50.1505 |
| F-number | 1.41614 | 1.40670 | 1.41784 |

Adaptive DLS used 12.2% fewer Analysis evaluations and took 12.9% less time,
with 0.74% higher final RMS merit. Its full-field spot RMS was 3.55% larger,
and full-field tangential MTF was lower by 0.0244. Its final accepted step used
the smallest permitted line-search fraction (1/4096), so step-tolerance
termination should not be interpreted as proof of an equally good optimum.
Default DLS was still limited by its iteration budget and was substantially
worse optically. These results favor LMDER for final merit and full-field
tangential performance, with adaptive DLS offering a promising alternative
at somewhat lower evaluation cost on this case.

This is one contrast-case run per configuration, executed in the table's order;
it is not a repeated timing benchmark or a direct-MTF-case comparison. LMDER's
optical results reproduce the existing test's regression values. Raw CSVs,
prescriptions, and histories are in `target/solver-comparison/contrast/`.
