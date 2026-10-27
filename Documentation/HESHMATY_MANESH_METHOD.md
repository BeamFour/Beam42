# Heshmaty-Manesh method for geometric OTF optimization

This note describes the sine-transformed transverse-aberration merit function and
describes `GoalMTFProxy` within its existing ray-fan scope. The validation,
invalid-sample handling and initial frequency diagnostic below are implemented.

## Method

Heshmaty-Manesh [1, Chapter 3, pp. 30-32] defines sagittal and tangential terms as

$$
G_s(s)=\sum_j\sin^2(\pi s\,\delta\xi_j),\qquad
G_t(t)=\sum_j\sin^2(\pi t\,\delta\eta_j).
$$

Here, `s` and `t` are spatial frequencies, and `delta xi` and `delta eta` are the
X and Y components of transverse ray aberration. Equation 3.9 combines these terms
with the remaining squared aberration or constraint errors:

$$
\Psi=\sum_n[G_s(s)+G_t(t)]_n+\sum_i d_i^2.
$$

The accompanying procedure replaces each controlled transverse ray aberration by
`sin(pi * frequency * aberration)` and passes the resulting residual array to the
existing damped least-squares optimizer. Other merit-function terms remain available.
The transformation is also described by Kidger and Benham [2, Section 2.2].

For Beam42's weight convention, one goal supplies

$$
r_j=\sin(\pi f\delta_j),\qquad
\Psi_{\mathrm{proxy}}=\sum_j w_j r_j^2.
$$

`LMDerMeritFunction` multiplies each goal error by `sqrt(weight)` before the solver
squares it. Consequently:

- `GoalMTFProxy.value()` must return the signed, unsquared sine.
- Its target must be zero. The target is a residual target, not an MTF fraction.
- The supplied weight multiplies the squared residual. It can include field,
  orientation and wavelength importance.
- Do not square an already summed `G_s` or `G_t`; that gives a different merit function.

## Meaning and limits

The derivation starts from the real part of the geometric OTF. For normalized pupil
weights `q_j`, with `sum(q_j) = 1`, its sampled expression is

$$
\operatorname{Re}L(f)\approx
1-2\sum_j q_j\sin^2(\pi f\delta_j).
$$

The method does not directly compute diffraction MTF or the modulus of a complex OTF.
In particular, the tangential imaginary OTF component is omitted in the thesis.

`GoalMTFProxy` reads one transverse-aberration sample from a sagittal or tangential
diameter fan. The existing analysis traces ten samples per fan. Replacing these
aberrations by sine residuals follows the stated optimization procedure. The fan
sum is a sparse proxy for the pupil integral; it must not be reported as a
pupil-integrated OTF value. A pupil-area implementation would be a separate extension,
not a prerequisite for retaining this goal within its current scope.

## Frequency selection

For an aberration magnitude `A`, the squared sine increases monotonically with `A`
only within the first interval

$$
|\pi f\delta|<\frac{\pi}{2}.
$$

Heshmaty-Manesh recommends a preliminary ray trace and a frequency satisfying

$$
f<\frac{1}{2A_{\max}},
$$

where `A_max` is the largest absolute controlled transverse-aberration component.
At the boundary the derivative is zero. Beyond it, reducing the merit can increase
the aberration. There are further zero residuals at `delta = k/f` for integer `k`.
For example, at 10 cycles/mm, the monotonic interval is `abs(delta) < 0.05 mm`,
and an aberration of 0.1 mm gives another zero residual.

For small arguments,

$$
\sin^2(\pi f\delta)\approx(\pi f)^2\delta^2.
$$

The merit then approaches ordinary transverse-aberration least squares, scaled
relative to the other constraints. Frequency therefore affects both aberration
balancing and the relative strength of this goal family.

Keep the selected frequency fixed during a solve. Automatically changing it as the
design changes would change the objective and its derivatives. Do not clamp the
sine argument or replace it by its absolute value as a periodicity fix.

## GoalMTFProxy implementation

### Validation and residual handling

1. **Require a zero target.** Retain the existing constructor signature for
   compatibility, but reject a nonzero or non-finite target. An overload without a
   target argument can delegate with zero. A nonzero target minimizes
   `(sin(pi*f*delta) - target)^2`, which is not the merit defined above.

2. **Validate configuration at construction.** Require a non-null analysis, a
   one-based field within `analysis._fields`, a valid orientation, a finite positive
   frequency and wavelength, and a finite non-negative weight. Require the wavelength
   to match a configured prescription wavelength; obtain it from the prescription
   array rather than relying on a separately rounded literal. The current fan lookup
   compares wavelength values exactly.

3. **Validate the sample index.** Preserve the existing negative-index convention:
   `-1` means the last sample. Validate against `Analysis.NUM_TRANSVERSE_RAYS`, and
   check against the actual `fan_y` length when evaluating. Reject an impossible
   configured index instead of silently giving it a finite residual.

4. **Use the standard invalid-evaluation penalty.** Return
   `LMDerMeritFunction.BIGVAL` for unavailable analysis results, a missing fan, an
   unavailable sample, a null ray result, a non-finite aberration, or a non-finite
   calculated sine argument/result. Check the aberration before applying the sine.
   Replace the broad exception handler and its `1.0` return with explicit checks.
   Missing data during a solver evaluation must not throw from `value()`.

5. **Document the contract.** State that this is a single ray-fan residual for real
   geometric OTF optimization, with a zero target. Frequency is in reciprocal
   model-length units: cycles/mm when transverse aberrations are in mm. Wavelength
   uses the analysis convention of nanometres and selects the fan; it is not an
   additional factor in the sine argument.

The former `1.0` failure return is within the valid sine range. Although it is the
largest squared residual for a zero-target, unit-weight sample, the solver treats
it as a usable observation, including during Jacobian estimation. `BIGVAL` marks
the evaluation as invalid under the existing solver convention. Missing fans now
return that penalty rather than throwing from the solver's goal-evaluation loop.

### Frequency diagnostic

`LMDerMeritFunction.getSolver()` runs a preflight check after the initial
`Analysis.compute()`. For each frequency, it inspects the samples selected by
positive-weight proxy goals and reports the largest `abs(pi*f*delta)`, together
with its field, orientation, wavelength and sample index. Safe maxima are logged
at CONFIG level; values at or above `pi/2` and invalid samples produce warnings.
`GoalMTFProxy.frequencyDiagnostics(goals)` exposes the same summaries for callers
that manage their own solver setup. It reads existing results without tracing rays.

This check diagnoses the sampled merit; it cannot establish a bound over unsampled
pupil positions. An initial pass also does not guarantee that later trial designs
stay in the monotonic interval. A runtime diagnostic can report crossings without
altering the residual. Rejecting such trials would be an additional domain
constraint and should be a separate, explicit option.

There is no automatic runtime diagnostic or additional domain constraint. Neither
`value()` nor the diagnostic changes the selected frequency or clamps residuals.

### Regression checks

`GoalMTFProxyTest` uses synthetic fan results to verify:

- Zero, positive and negative aberrations, including
  `f = 10/mm`, `delta = +/-0.025 mm`, which give `+/-sqrt(0.5)`.
- The solver contribution is `weight * sin(pi*f*delta)^2`.
- Zero-target enforcement and configuration validation, including negative indices.
- Null, missing, NaN and infinite samples return `BIGVAL` without escaping exceptions.
- Invalid samples remain invalid in both residual and Jacobian evaluation.
- The periodic zero at `delta = 1/f` is retained and the frequency diagnostic flags it.

The corrections are confined to Beam42-owned optimizer code and tests. Ported
tracing code is unchanged. The changes to `GoalMTFProxy` and the solver preflight
still need back-porting to rayoptics-cpp, with corresponding expectation updates.

## References

1. D. Heshmaty-Manesh, "The utilization of aberration polynomials in computer
   programs for optical design," Ph.D. thesis, Imperial College, University of London
   (1978), Chapter 3, especially Eqs. 3.6-3.9 and pp. 31-32.
   [Institutional record](https://hdl.handle.net/10044/1/22964).
2. M. Kidger and P. Benham, "Optimization of the optical transfer function,"
   Proc. SPIE **1354**, 92-96, International Lens Design Conference (1990),
   Sections 2.1-2.3.
