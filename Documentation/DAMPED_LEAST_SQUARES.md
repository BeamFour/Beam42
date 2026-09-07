# Constrained damped least squares

Java port of Prysm's [DampedLeastSquares](https://github.com/brandondube/prysm/blob/master/prysm/x/optym/least_squares.py#L430), in `org.redukti.mathlib.DampedLeastSquares`.
The MIT attribution is included in the built jar. The local `mathlib.jama` classes
supply the pivoted LU decomposition and SVD factors. The solver applies an SVD
pseudoinverse for singular KKT systems; no additional math dependency is needed.

Use an existing `OptimizationBuilder.OptimizationSetup`:

```java
var solver = setup.dampedLeastSquaresSolver();   // Beam42 defaults, not Prysm's
int status = solver.solve();
System.out.println(solver.result());
```

`DampedLeastSquaresSolver.defaultOptions()` is what the no-argument form uses, and
it deliberately differs from `new DampedLeastSquares.Options()`. Prysm's defaults do
not travel to lens design: see [Beam42 defaults](#beam42-defaults) for each change
and the measurement behind it. Pass an `Options` explicitly to override.

## Bounds

The reason to have this solver at all. A `Bound` is a hard inequality on the
prescription - `value() >= 0` - and the only thing in the optimizer that states
where a design *may not go* rather than charging it merit for going there:

```java
var setup = OptimizationBuilder.builder(prescription)
        // ... variables and optical goals ...
        .boundThicknesses(0.5)        // no axial gap below half its starting value
        .boundEdgeThicknesses(0.5)    // nor any edge separation
        .build();
setup.dampedLeastSquaresSolver().solve();
```

`LMDerSolver` ignores bounds; it has no way to represent them. A setup meant for
both solvers wants `applyThicknessConstraints()` as well or instead.

A `Bound` must be computable from the prescription alone, never from `Analysis`.
That is what makes it affordable: the adapter differentiates the whole bound vector
by central differences over the prescription, with no ray trace anywhere, so the
constraint Jacobian costs nothing measured against the optical one. A constraint
that needed a trace would cost `2n` full analyses per iteration to differentiate -
on a 44-variable lens, 88 traces for a quantity that is pure geometry.

`BoundThickness` and `BoundEdgeThickness` are the hard counterparts of
`ConstraintThickness` and `ConstraintEdgeThickness`. The penalty versions are still
there and still useful; they are not converted automatically, and a setup may use
both. See [Bound](../rayoptics/src/main/java/org/redukti/optim/Bound.java) for why
the two forms behave differently even at equivalent-looking strengths.

## Raw constraint callbacks

For anything a `Bound` cannot express, pass two vector callbacks (empty arrays mean
no constraints):

```java
var solver = setup.dampedLeastSquaresSolver(options,
        x -> new double[]{x[0] + x[1] - 1.0},  // equality: zero
        x -> new double[]{x[0], 2.0 - x[0]});   // inequalities: >= zero
```

These illustrative constraints act on **scaled optimizer variables**, in the order
returned by `setup.variables()`. Callbacks can also read `setup.analysis()` and
its prescription, which the adapter updates before each callback. Callbacks must
not mutate them.

Prefer a `Bound` wherever the quantity is geometric. A callback is differentiated by
the solver with central differences, and because the adapter recomputes `Analysis`
before each call, that is `2n` full ray traces per iteration - roughly doubling the
cost of an iteration. A callback returning an empty array at the starting point is
never invoked again, so an unused callback costs nothing.

The adapter reuses Beam42's square-root goal weighting, target subtraction, and
per-variable finite-difference Jacobian with failed-ray handling. It restores the
accepted prescription and recomputes Analysis after termination or exceptions.
Invalid residual trials are rejected; an invalid Jacobian at the accepted point
raises an exception. Constraint callback exceptions propagate.

The standalone solver accepts a `Problem` with `residuals(x)` and optional
`jacobian(x)`, `equalities(x)`, `inequalities(x)`, `equalityJacobian(x)` and
`inequalityJacobian(x)`. Any Jacobian left null uses central finite differences.
`step()` performs one iteration; `run()` runs to termination; `result()` returns a
detached snapshot.

Identity/sensitivity damping, scalar or per-variable damping, uniform trust-radius
scaling, adaptive damping, multipliers, and accepted-iteration history are supported.
Nonlinear constraints use sequential linearization and feasibility-first line
search; difficult feasible nonlinear boundaries may cause line-search failure.

Port conventions and differences:

- `result().status()` says why the solve stopped. `success()` means `CONVERGED` and
  feasible, and nothing else. **This is a deliberate departure from Prysm**, which
  reports success on reaching the iteration limit whenever the point is feasible -
  unconditional for an unconstrained problem, and it blessed a measured Otus run that
  stopped at 25 iterations with an on-axis MTF of 0.077. Use `feasible()` for the
  Prysm reading. `iterations()` counts accepted steps.
- `solve()` returns `DampedLeastSquaresSolver.CONVERGED` (1), `ITERATION_LIMIT` (2)
  or `FAILED` (0). Not MINPACK status codes.
- **The Jacobian is built once per accepted iteration, not once per damping attempt.**
  Prysm rebuilds it inside the retry loop, where `x` has not moved and the result is
  identical; only the damping diagonal changes. On the measured Otus solve, 10 of 31
  Jacobians were identical rebuilds - 880 of 2,972 analyses, 30% of the run.
- **One inequality is dropped from the active set per pass**, the one whose multiplier
  most clearly says it wants to be inactive. Prysm drops every such constraint at once,
  which cycles: with a long undamped step every bound looks violated and is added, the
  constrained solve puts them all on their boundaries with positive multipliers, they
  are all dropped together, and the next pass reproduces the original step exactly.
  Measured on the Leica 75/2 with 29 bounds, where the original rule never terminated.
- Inequality multipliers use the KKT sign convention and are nonpositive when active.
- `nfev` counts calls to the residual callback; `njev` counts residual Jacobian
  requests; `ncev` counts individual equality/inequality callback calls. Supplied
  constraint Jacobians are not counted, and evaluations internal to Beam42's supplied
  Jacobian are not included in `nfev`.
- Invalid inputs and changing callback dimensions are rejected. Active-set
  exhaustion reports failure rather than returning inconsistent multipliers.
- No Python governor framework is required; stopping checks are implemented locally.
- The adapter runs `LMDerMeritFunction.validateInputs()` before the solve, the same
  start-of-solve check the LMDER path has always run. An unrepresentable start now
  names the first failed contrast sample instead of failing opaquely later.

## Beam42 defaults

`DampedLeastSquaresSolver.defaultOptions()`, and why each differs from Prysm:

| Option | Prysm | Beam42 | Why |
| --- | --- | --- | --- |
| `dampingMode` | `IDENTITY` | `SENSITIVITY` | The parameter vector mixes radii of 14mm and 2000mm with 0.1mm air spaces and scaled aspheric coefficients. One damping value against an identity matrix has no consistent meaning across it. Sensitivity damping scales by `diag(J^T J)` - Marquardt's scaling, roughly what `lmder` does internally with `mode=1`. |
| `adaptiveDamping` | off | on | Without it a single rejected line search ends the solve. |
| `maxIterations` | 25 | 100 | The measured Otus solve was still moving at 21, and the 25-iteration run was optically ruined. |
| `ftol` | `1e-12` | `sqrt(eps)` ≈ `1.5e-8` | Against a cost of order `1e-2`, `1e-12` is effectively absolute and the cost-tolerance test cannot fire. `sqrt(eps)` is what `lmder` uses. |
| `maxActiveIterations` | 20 | 200 | A lens bounded gap by gap has dozens of constraints and the set is refined one at a time, so 20 passes cannot visit them all. Free: a pass is a dense solve of order n, not a ray trace. |
| `constraintTolerance` | `1e-10` | `1e-6` | The only option with units. A `Bound` is a length in mm, so `1e-10` asks the design to sit on its boundary to within a ten-thousandth of an Angstrom. An edge gap is not linear in curvature, so a step running along the linearised boundary leaves it by a second-order amount and the line search must halve until that falls under the tolerance. Measured below. |

Run the focused tests. `-Dsurefire.failIfNoSpecifiedTests=false` is required: without
it the reactor fails on the first module with no matching test, before reaching
`rayoptics`.

```text
mvn -o test -Dtest=DampedLeastSquaresTest,DampedLeastSquaresSolverTest,LMDerMeritFunctionTest,OptimizationBuilderTest -Dsurefire.failIfNoSpecifiedTests=false
```

The Leica bounds experiment is opt-in and takes about 45 minutes for its four
configurations:

```text
mvn -o test -Dtest=LeicaLayoutBoundsTest -Dsurefire.failIfNoSpecifiedTests=false -Doptimization.leicaBounds=true
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

### Re-measured after the Jacobian fix (2026-09-07)

**The cost figures above are superseded.** Thirty percent of adaptive DLS's cost
was rebuilding a Jacobian it already had, once per damping retry at an unmoved
`x`. Hoisting it out of the retry loop, with nothing else changed, gives a
bit-identical answer for a third less work:

| Solver configuration | Analysis evaluations | Jacobians | Final RMS merit | Status |
| --- | ---: | ---: | ---: | --- |
| LMDER, existing settings | 3,385 | 37 | 0.007114287657481064 | MINPACK info=1 |
| DLS, Prysm defaults | 2,365 | 25 | 0.019993056894741082 | `ITERATION_LIMIT` |
| DLS, sensitivity + adaptive, cap 100 | **2,073** | **21** | 0.00716723772955372 | `CONVERGED` (step tolerance) |

Adaptive DLS went from 2,972 evaluations to 2,073, a **30.2% saving**, and its
Jacobian count fell from 31 to 21 - now exactly its accepted-iteration count,
which is the tell that the surplus was pure duplication. The final merit is
unchanged to the last digit, and LMDER's numbers are untouched, so the optical
table above still stands.

Against LMDER the comparison is now **38.8% fewer Analysis evaluations for 0.74%
higher RMS merit**, rather than the 12.2% recorded above. The optical caveats are
unchanged: full-field tangential MTF is still lower, and step-tolerance
termination at a line-search fraction of 1/4096 is still a stall rather than a
proof of an equally good optimum.

Note also that `DLS, Prysm defaults` now reports `ITERATION_LIMIT` where it
previously returned success. Nothing about that run changed; it was always an
optically ruined lens (on-axis MTF 0.077) reported as a successful solve.

### Leica 75/2: hard bounds against penalty constraints (2026-09-07)

The go/no-go measurement for bounds, on the case the layout constraints were
written for. `LeicaLayoutBoundsTest`, 29 variables, 11 fields, 3 frequencies, 6x12
contrast sampling, 14,331 goals. Every row starts from the same prescription at
optical RMS **0.2372392** and keeps `applyCurvatureConstraints()`; only the layout
treatment varies. Merit is measured on a constraint-free yardstick setup, since the
rows' own merit functions differ and their RMS values are not comparable.

| Configuration | Evaluations | Optical RMS | min edge | min t/t0 | crossed | Status |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| LMDER, thickness penalty 5.0 | 1,423 | 0.0230213 | 0.2953 | 0.886 | 0 | MINPACK info=1 |
| LMDER, all penalties nominal | 1,727 | 0.0217512 | 0.2899 | 0.816 | 0 | MINPACK info=1 |
| DLS, all penalties nominal | 2,474 | **0.0202231** | 0.2969 | 0.845 | 0 | `CONVERGED` |
| DLS, hard bounds at 0.5 of start | 6,171 | 0.0265324 | 0.1526 | 0.691 | 0 | `ITERATION_LIMIT` (100) |

Three things follow.

**The bounds are correct.** No gap crossed in any row, and the bounded run finished
with four bounds exactly on their floors carrying the right (negative) KKT
multipliers. It is the only row that used its freedom: gaps 6, 8 and 13 grew to
1.52x, 1.62x and 1.31x of their starting values while four others sat on their
floors. The penalty rows held everything inside roughly +/-25% of start, which is
what a fractional penalty anchored to the starting value does - it resists opening
a gap exactly as hard as closing it.

**Penalty detuning did not reproduce here.** REVIEW.md open question 2 predicted
that nominal weights would fail to hold the line at 11 fields. They held: the
nominal LMDER row reached a *better* merit than the tuned row with no crossings and
a healthier minimum edge. On this lens and this configuration the weight question
is not urgent.

**The constrained step is where it falls down.** The bounded row is the worst of
the four and the only one that did not converge. Its history shows why:

```text
iter   1   cost 199.3    stepNorm 70.8   3 active
iter  20   cost 5.6434   violation 9e-11
iter 100   cost 5.6105   violation 6e-12
```

Violations sit at 1e-11, far inside the 1e-6 feasibility tolerance, so the line
search is not being blocked. It takes an enormous first step, lands on three
constraints at once, and then zig-zags on two active bounds for eighty iterations
to buy 0.6% of cost. Since the penalty row's endpoint (0.973 of the starting edge)
is comfortably feasible for a floor at 0.5, the bounded optimum must be at least as
good as the penalty result - the solver is failing to reach it, not being held away
from it.

#### It is the starting damping, not the bounds

Two attempts to avoid the early lunge onto the boundary. On the reduced
configuration:

| Variant | Evaluations | Optical RMS | Status |
| --- | ---: | ---: | --- |
| bounds, defaults | 6,075 | 0.0722164 | `CONVERGED` |
| bounds, starting damping 1e-2 | 4,779 | 0.0728675 | `CONVERGED` |
| bounds, 1mm trust radius | 185 | 1.35927 | `FAILED` (active set) |

The trust radius is not a fix and is arguably a second defect: `trustRadii` scales
`dx` uniformly *after* the KKT solve, which throws away the constraint satisfaction
that solve just established, and the run failed with the design essentially unmoved
(every thickness within 1% of start).

The damping, however, is decisive at full scale:

| Full-configuration variant | Evaluations | Iterations | Optical RMS | Status |
| --- | ---: | ---: | ---: | --- |
| bounds, starting damping 1e-6 | 6,171 | 100 | 0.0265324 | `ITERATION_LIMIT` |
| bounds, starting damping 1e-2 | 12,342 | 200 | **0.0223508** | `ITERATION_LIMIT` |

and its history shows steady progress rather than a stall:

```text
iter 100   cost 4.2801   alpha 0.25   4 active
iter 150   cost 4.0636   alpha 1.0    4 active
iter 200   cost 3.7013   alpha 1.0    5 active
```

That is 13.5% of cost in the last fifty iterations, still falling when the budget
ran out. **The zig-zag was not the bounds and not the active set; it was Prysm's
near-zero starting damping, which is right for an unconstrained problem and wrong
for a bounded one.** With a sane start the bounded solve tracks the penalty result
(0.02235 against LMDER-nominal's 0.02175) and had not finished.

What it costs is iterations: 12,342 evaluations against LMDER's 1,727 for a merit
still slightly behind. That is the real charge against bounds on this evidence -
not that they distort the design or fail to hold, but that the constrained step is
expensive and converges slowly. `boundedDefaultOptions()` now starts bounded
problems at 1e-2, and `dampedLeastSquaresSolver()` selects it when the setup
carries bounds.

Note the reduced configuration reaches the opposite conclusion from the full one -
there, bounds converge and beat the penalty run (0.0722 against 0.0736). Reduced
runs are for exercising the code, not for deciding the question.

Raw CSVs, prescriptions, layout tables and iteration histories are in
`target/leica-bounds/`.
