# Optimizer Design Notes
These are the design and implementation notes for the optimizer: what each goal measures,
why it behaves as it does, and what was measured along the way. For using the optimizer -
the trial and pipeline sections of a prescription, and what each setting does - see
[OPTIMIZER.md](OPTIMIZER.md).

The current sampling support matrix is in that user guide. Implementation gaps and
proposed changes are recorded in [Sampling limitations and follow-up work](#sampling-limitations-and-follow-up-work) below.

## Contrast Optimization

Contrast optimization uses pupil wavefront differences as a fast, smooth proxy for
MTF. It is intended primarily as a refinement method: it works best when the starting
prescription is already reasonably corrected and the phase differences are small.

The implementation is based on the description in following paper:

E. Elliott, K. Moore, C. Normanshire, S. Gay, J. Aiona, and M. G. Nicholson, "Contrast optimization: a faster and
better technique for optimizing on MTF," in *International Optical Design Conference 2017*, Proc. SPIE 10590,
1059014 (2017), [doi:10.1117/12.2292761](https://doi.org/10.1117/12.2292761)

### What a sample means

A contrast sample is one location `p = (x, y)` in the entrance pupil at which the
wavefront is compared with slightly displaced copies of itself. The OTF shear is
physically defined at the exit pupil, but Beam42 launches rays in entrance-pupil
coordinates. The optional frequency calibration described below rescales those launch
coordinates so that the ray pairs realise the requested image-space frequency.

For each sample, three rays are traced:

- a reference ray at `p`;
- a sagittal partner at `p + (s, 0)`;
- a tangential partner at `p + (0, s)`.

The nominal entrance-pupil displacement is

```text
s = 2 lambda F# frequency
```

where `lambda` is the wavelength, `F#` is the working f-number, and `frequency` is the
requested spatial frequency. With calibration disabled, this same nominal displacement
is used in both directions. With calibration enabled, sagittal and tangential scale
factors are measured separately for every field and wavelength, so the actual entrance-
pupil displacements may differ from each other and from `s`.

The analysis calculates two wavefront differences:

```text
deltaW_s(p) = W(p + (s, 0)) - W(p)
deltaW_t(p) = W(p + (0, s)) - W(p)
```

These become two optimizer residuals:

```text
r_s(p) = sqrt(w_p) deltaW_s(p)
r_t(p) = sqrt(w_p) deltaW_t(p)
```

Here `w_p` is the Gaussian-quadrature weight of the pupil sample. One sample therefore
produces three traced rays and two residuals. The sagittal and tangential comparisons
share the reference ray, saving one ray relative to tracing the two pairs separately.

### Sample and residual counts

With the default 6-ring by 12-spoke pattern, each field, wavelength, and frequency uses

```text
6 * 12 = 72 pupil samples
```

This produces 144 residuals (72 sagittal and 72 tangential) and traces 216 rays. For
four fields, three wavelengths, and three frequencies, the full contrast merit contains

```text
72 * 2 * 4 * 3 * 3 = 5184 residuals
```

### Physical interpretation

If the wavefront difference varies little across the pupil, the two displaced pupil
wavefronts retain nearly the same shape. Contributions from different parts of the
pupil then remain comparatively well phased, which generally corresponds to good
contrast at that frequency.

If `deltaW` varies strongly across the pupil, different pupil regions acquire different
phases and increasingly cancel in the optical transfer function, reducing MTF.

`GoalContrast` minimizes the weighted sum

```text
sum_p w_p deltaW(p)^2
```

It does not calculate MTF directly. It minimizes a smooth least-squares proxy for the
loss of contrast.

### Residual centring

For a single wavelength block the sum above is the un-centred second moment, and it
decomposes as

```text
sum_p w_p deltaW(p)^2 = Var(deltaW) + mean(deltaW)^2
```

Only the first term describes the monochromatic loss of contrast. The OTF modulus depends
on the *variance* of the phase difference, through
`|OTF| = |<exp(i Phi)>| ~ 1 - Var(Phi)/2`. A constant `deltaW` across
the pupil is wavefront tilt, which displaces the image: it moves the phase transfer
function and leaves the modulus untouched. The geometric MTF used to validate the
surrogate is translation invariant too, since spot analysis measures about the centroid.

Enable centring through the builder:

```java
.centerContrastResiduals(true)
```

For the reference wavelength the residual becomes
`sqrt(w_p) (deltaW(p) - deltaW_bar_ref)`, where `deltaW_bar_ref` is its
quadrature-weighted mean over the valid samples. The same reference-wavelength offset is
then subtracted from every other wavelength.

Two properties are worth knowing before enabling it.

**Defocus is not removed.** Defocus makes `deltaW` vary linearly across the pupil in the
shear direction rather than being constant, so its mean over a symmetric pupil is already
zero. Only the constant part goes; defocus, coma, spherical and astigmatism keep their
full contribution.

**The reference-wavelength mean is subtracted per (frequency, field, orientation), not a
separate mean per wavelength.** A tilt
common to every wavelength is a harmless image shift. A tilt that differs between
wavelengths is lateral colour, and that genuinely does reduce polychromatic MTF, because
the per-wavelength complex transfer functions acquire different phases and partly cancel.
Subtracting the reference wavelength's mean from every wavelength discards the common part
and preserves every wavelength's displacement relative to the reference. This follows the
same reference-image convention as Beam42's polychromatic spot analysis. It is exactly the
centred variance for the reference wavelength; across all wavelengths it is a second
moment about that reference image, deliberately retaining lateral colour rather than the
variance about a combined polychromatic mean.

The sagittal mean is identically zero on a rotationally symmetric system, since `deltaW`
is odd about the shear centre, so in practice only tangential residuals move. That does
not make the change sagittal-neutral. The tangential block loses up to 57 percent of its
sum of squares, so the balance between the two orientations shifts, and the two trade
against each other through the astigmatic focus split. Measured on the Leica 75/2, centring
raises sagittal's weight relative to tangential by a factor of about 2 at fields 0.7
through 1.0.

There is a second reason to prefer it. The common `mean^2` term is *reducible* by adding tilt,
which costs no MTF, so leaving it in offers the solver merit reduction that buys no optical
improvement.

Centring is off by default because it changes every contrast residual and therefore every
committed regression value.

### Operating range and limitation

The proxy is most faithful in the small-phase regime:

```text
abs(2 pi deltaW) << 1
```

Measurements so far suggest that it behaves well when the RMS wavefront difference is
roughly below 0.1 waves. This explains its good performance when refining an already
reasonable prescription such as the Otus example.

On a poorly corrected starting prescription, the phase differences may span or wrap
through one or more cycles. In that regime, the squared wavefront differences no longer
uniquely determine MTF and can improve while independently measured MTF gets worse.
Contrast merit should therefore be validated against a separate spot/MTF analysis.

### Exit-pupil frequency calibration

The relation

```text
s = 2 lambda F# frequency
```

describes the shear of the exit-pupil autocorrelation. `REL_PUPIL`, however, specifies
where a ray passes through the entrance pupil. Pupil aberration means that a rigid shift
at the entrance pupil does not generally produce the same normalized shift at the exit
pupil. Without correction, the realized spatial frequency can therefore vary with field,
wavelength and direction, with the largest error normally occurring at outer fields.

Enable the correction through the builder:

```java
.calibrateContrastFrequency(true)
```

For every field and wavelength, Beam42 traces a centred probe pair separately in the
sagittal and tangential directions. Two rays produce image-space fringes at the requested
frequency when their image-space direction cosines differ by

```text
lambda * frequency
```

The ratio between that required difference and the measured difference becomes a scale
factor for the entrance-pupil shift. This costs four probe rays per field and wavelength,
which is small compared with the full contrast sampling pattern.

This is a calibration rather than an exact construction in exit-pupil coordinates. It
removes the dominant field- and direction-dependent frequency bias, but uses one scale
factor for the complete pupil. Residual variation caused by nonlinear pupil mapping
across individual samples remains. It can be measured diagnostically, but correcting it
properly requires aiming the displaced ray to the requested exit-pupil separation.

Calibration is off by default because it changes the sampled frequencies and therefore
the numerical merit function. It is cheaper than direct aiming and can remove much of a
wide-field bias, but it is not the preferred reference for new accuracy work: its probe
uses an image-ray direction metric and one scale cannot represent the nonlinear
two-dimensional pupil map. Old regression cases may leave it disabled to preserve their
historical values.

### Direct exit-pupil aiming

The direct correction is available as an opt-in alternative to block calibration:

```java
.aimContrastAtExitPupil()
```

For every quadrature reference ray, Beam42 computes its coordinate on the finite
exit-pupil reference sphere. It then inverse-aims each sagittal and tangential partner
with a two-dimensional Newton iteration until the partner has the requested vector
separation there. Solving both coordinates matters: pupil aberration can rotate or skew
an entrance-pupil displacement, so correcting only its nominal axis is insufficient.
The OPD is evaluated from the newly traced partner ray; no finite OPD difference is
rescaled.

#### Relation to Hopkins' OTF coordinates

Hopkins writes the two-dimensional transfer function as a pupil autocorrelation. In the
notation of equations (10.22) and (10.26) of [Calculation of the Aberrations and Image
Assessment for a General Optical System](https://doi.org/10.1080/713820605),
the essential form is

```text
               1
D(s,t) = ------------- integral-over-overlap
             A(s,t)

          f(x + s/2, y + t/2) f*(x - s/2, y - t/2) dx dy
```

Thus `(s,t)` is a vector separation between two pupil-function samples, not a ray angle
or a transverse image aberration. Hopkins states after (10.28) that `(x,y)` denotes the
difference of exit-pupil coordinates obtained from ray tracing. The discussion after
(10.30) further stresses that the pupil surface must be identified with the reference
sphere rather than an arbitrary pupil plane.

Beam42 traces a reference sample at `p` and a partner at `p + delta`, instead of the
symmetric `p - delta/2` and `p + delta/2` notation above. A translation of the integration
variable makes these equivalent over the common overlap. For image-space frequency
`nu`, wavelength `lambda` expressed in system units, and working f-number `F#`, the
traditional normalized entrance-pupil separation is

```text
delta = 2 lambda F# nu
```

This remains the initial guess supplied to the inverse aiming solve. The physical target
is derived directly on the field- and wavelength-specific reference sphere. Two
image-space directions form fringes of frequency `nu` when

```text
n_img |Delta d| = lambda nu.
```

On a reference sphere of radius `R`, the corresponding pupil-coordinate separation is

```text
Delta = |R| lambda nu / |n_img|,
```

so Beam42 asks for

```text
Delta X_sag = (Delta, 0)
Delta X_tan = (0, Delta).
```

Earlier versions multiplied `delta` by the paraxial exit-pupil radius. That is equivalent
only under the paraxial identity `|R| = 2 |n_img| F# R_exit`. Using `R` directly avoids
mixing a field-specific traced reference sphere with a paraxial pupil radius. `X(u)` is
the reference-sphere coordinate reached by a ray launched at normalized entrance-pupil
coordinate `u`.

#### Reference-sphere coordinate

`ExitPupilAiming.sphere_coord()` first reconstructs the equally-inclined-chord coordinate
`c` relative to the chief-ray exit-pupil centre. It then intersects the transformed ray
direction `d` with the finite reference sphere. In the implementation, with reference
direction `r` and signed sphere radius `R`,

```text
F  = r.d - d.c/R
J  = c.c/R - 2 r.c
ep = J / (F + sqrt(F^2 - J/R))
X  = c + ep d
```

The rationalized expression for `ep` avoids the cancellation of the equivalent quadratic
root. Afocal systems have no finite reference sphere and therefore cannot use this mode.

#### Inverse aiming

For a reference ray launched at `p`, and requested physical separation `Delta X`, the
partner's target is

```text
T = X(p) + Delta X.
```

The unknown is the two-component normalized entrance-pupil coordinate `u`. Beam42 solves

```text
G(u) = (Xx(u) - Tx, Xy(u) - Ty) = (0,0).
```

It starts from the traditional rigid entrance-pupil guess `u0 = p + delta`. At each
iteration it traces two finite-difference probes to form the full 2-by-2 Jacobian
`J = dG/du`, solves

```text
J du = -G,
```

and uses backtracking until the reference-sphere error decreases. The full matrix is
important: it corrects both scale error along the requested axis and pupil-aberration
induced cross-axis shear. The current limit is ten Newton iterations. The solver targets
`2e-7 R_exit`; if numerical roundoff prevents another decreasing step, a result within
`1e-6 R_exit` is still accepted. The latter is about 6 nm for a 6 mm exit-pupil radius
and remains negligible compared with a contrast shear. Failure to trace, a singular inverse map, or failure to
converge becomes an ordinary failed contrast partner ray with its context preserved.

After convergence, the contrast residual uses the actual finite wavefront difference

```text
dW = W(u) - W(p),
```

not a rescaled version of the OPD from the original guessed ray. This distinction matters
because `W(p + delta) - W(p)` is nonlinear for a finite shear.

This is more faithful to the reduced exit-pupil coordinate used by the Hopkins OTF than
`calibrateContrastFrequency(true)`, but it is also more expensive because each partner
can require several trial traces. The two options are mutually exclusive. A failed or
unavailable inverse map is reported as a failed contrast ray, preserving the fixed merit
layout and the existing failure diagnostics.

This first implementation retains Gaussian quadrature for the reference rays in the
entrance pupil and the existing entrance-pupil overlap construction. It corrects the
partner separation, but it does not remap the quadrature nodes or integration weights
into a uniform exit-pupil quadrature. It is therefore an important correction rather
than a complete exit-pupil OTF integral, and should be compared against independently
calculated MTF before becoming the default.

#### Regression evidence

The tests compare the maximum two-dimensional error of both sagittal and tangential
pairs over several pupil azimuths at 40 cycles/mm:

| Lens and field | Rigid entrance shift | Block calibration | Direct aiming |
| --- | ---: | ---: | ---: |
| Nikkor Z 14-30 mm at 57.68 degrees | 0.69209 mm | not measured | 0.00000119 mm |
| US 3,549,241 Example 5 at 45 degrees | 0.63498 mm | 0.09335 mm | 0.00000129 mm |

The second case shows the distinction clearly. Block calibration removes about 85 percent
of the maximum error, so it is useful, but its residual is still about 72,000 times the
direct-aiming residual. These tests validate the ray-pair coordinate construction; they
do not claim that the resulting optimized MTF must improve. On the Leica APO 75/2 the
final independently measured MTF difference was small and mixed, which is consistent
with the remaining quadrature approximation and with optimization finding a nearby
design basin.

### Per-sample frequency measurement

This measurement is also available on its own, as a diagnostic that changes no residual.
It is an analysis-level option rather than a builder one, since it does not affect
optimization:

```java
new ContrastOptions(frequency).measure_frequency(true)
```

That populates `ContrastAnalysisResult.Shear` for every sample with the reference ray's
exit-pupil coordinate, the shear each partner produced there, and both realized
frequencies. Neither option costs an extra ray; both quantities come from rays the samples
already trace.

The currently reported `sagittalFrequency` and `tangentialFrequency` use image-ray
direction differences, not the reduced exit-pupil separation used by direct aiming.
They remain useful diagnostics, but are not an independent validation of the aimed
Hopkins frequency coordinate.

### Controlling astigmatism

The contrast merit minimizes `sum(sagittal^2) + sum(tangential^2)`. At a fixed total that
barely discriminates how astigmatism is split between the two meridians, and a designer
discriminates sharply. On the Leica 75/2 a solve produced this at 50 cycles/mm:

```text
field    0.0    0.2    0.4    0.6    0.7    0.8    0.9    1.0
sag     .447   .407   .453   .295   .177   .156   .288   .565
tan     .447   .413   .531   .657   .706   .725   .627   .539
sum     .894   .820   .984   .952   .883   .881   .915  1.104
```

The sum stays within 15 percent of itself across the whole field while the difference goes
from zero to 0.57. The lens is not worse in that zone, it is lopsided there, and nothing in
the merit had an opinion about that.

`GoalContrastBalance` supplies the opinion. Its value is the difference between what the
two orientations contribute to the merit,

```text
sum_wavelengths w (
    w_sagittal sum_samples r_sagittal^2
  - w_tangential sum_samples r_tangential^2
)
```

against a target of zero, positive when sagittal is the worse meridian. Defining it on the
residuals rather than on raw wavefront differences means it follows whatever those already
account for, including residual centring, frequency calibration, wavelength weights and
the configured sagittal/tangential field weights. Its value is smooth and quadratic, with
no modulus and no square root. Since the least-squares solver squares every goal value,
its final merit contribution is quartic in the wavefront differences.

The weights in that expression are exactly the weights used by the ordinary contrast
goals. If a field gives sagittal contrast weight 2 and tangential weight 0.5, balance is
reached when those *weighted merit contributions* are equal, not when the two unweighted
residual energies are equal. A zero orientation weight removes that orientation from both
the ordinary contrast block and the balance comparison. This keeps the balance goal from
quietly imposing a different sagittal/tangential weighting policy from the contrast merit
it accompanies.

Enable it per field, since the outermost field usually wants leniency:

```java
.contrastBalanceGoals(new boolean[] {false, true, true, true, false})
.contrastBalanceGoals(fields, 0.05)
```

One flag per configured field, in field order; false adds no explicit balance constraint at
that field. The ordinary contrast residuals still constrain the two meridians independently.
The goal applies to every configured contrast frequency, so it adds one residual per enabled
field per frequency.

**Leave it off on axis.** At field zero the two meridians are identical by rotational
symmetry, so there is nothing to balance and the value reduces to

```text
(w_sagittal - w_tangential) * S
```

where `S` is the axial residual energy. With equal orientation weights that is exactly
zero and the goal is inert. With unequal weights it is not: it silently becomes a second
axial contrast goal of strength `w_sagittal - w_tangential`, which is normally a number
that fell out of a field taper rather than a decision about axial emphasis. Measured on the
Leica 75/2 with weights 8 and 4 on axis, it contributed 50.3 of a 802.6 merit — 6.3 percent,
none of it balance.

It also behaves unlike the contrast goals it is shadowing. `S` is already a sum of squares,
so this residual is quadratic where the per-sample residuals are linear: it pushes hardest
while axial aberration is large and fades quadratically as the design improves. If axial
emphasis is what is wanted, raise the field-zero entries in the sagittal and tangential
weight arrays instead. Those act through the ordinary residuals, scale predictably, and do
not evaporate on convergence.

**Set the weight from a measurement, not from the default.** A balance residual is a
difference of sums of squares, so it is large exactly where a per-sample contrast residual
is small. On the Leica starting design at 10/30/50 cycles/mm over 11 fields, the balance
block at weight 1.0 came to 43.6 against the contrast block's 52.6 — 83 percent of the
optical merit, from 33 residuals against 14256. `NOMINAL_BALANCE_WEIGHT` is 0.1, which puts
it near 8 percent there, but nothing in this goal adapts to the design the way the
fractional design-preservation constraints do.

Be clear about what this is. Residual centring corrects an error in the merit; this does
not. It tells the optimizer a design preference it has no way to infer — that astigmatism
should be shared between the meridians rather than dumped on one of them.

## Spot goals

### Per-ray RMS spot optimization

`GoalSpotRMS` exposes one aggregate spot-radius value per field. Although suitable for
measurement, differentiating a single square-rooted aggregate gives the solver much less
information than exposing the signed ray deviations that make up the same RMS value.

Enable the granular form with one field weight per configured field:

```java
.gaussianQuadratureSampling(6, 12)
.spotDeviationGoals(new double[] {1.0, 1.0, 1.0, 1.0})
```

Separate X and Y field weights are also supported:

```java
.spotDeviationGoals(xWeights, yWeights)
```

Note that the array is *weights*, not targets: every residual aims at zero, so these
goals minimize spot size rather than steer it to a value. This is the one difference in
argument meaning from the neighbouring `spotRmsGoals(targets)` and
`spotMaxRadiusGoals(targets)`.

The sampling pattern is Gaussian quadrature. For every field, wavelength and pupil
sample, the builder creates two `GoalSpotDeviation` residuals. If `(dx, dy)` is the ray
intercept relative to the reference-wavelength centroid and `w_p` is its quadrature
weight, their values in microns are

```text
r_x = 1000 sqrt(w_p) dx
r_y = 1000 sqrt(w_p) dy
```

The merit function additionally applies the square roots of the field/orientation and
wavelength weights. Consequently, minimizing the sum of the individual squared
residuals is mathematically equivalent to minimizing the corresponding weighted RMS
spot radius, while retaining the sign and direction of every ray error for the Jacobian.

`gaussianQuadratureSampling` configures the common ordinary spot pattern used by
per-ray spot goals, aggregate Gaussian spot analysis and geometric MTF. The historical
default remains 14 rings by 20 spokes; 6 by 12 gives 72 pupil rays and 144 residuals per
field and wavelength when a smaller optimization merit is wanted. Contrast retains a
separate `contrastSampling` setting because it integrates over the overlap of sheared
pupils rather than the ordinary spot pupil.

For a concentric annular entrance pupil, pass its normalized inner radius as a third
argument, for example `gaussianQuadratureSampling(6, 12, 0.50)`. See
[`GAUSSIAN_QUADRATURE.md`](GAUSSIAN_QUADRATURE.md) for the formula, vignetting
weighting, spoke-count guidance, and the exact scope relative to the reference paper.
Sampling must remain fixed throughout an optimization; failed rays therefore retain
their sample positions and report an invalid goal instead of being removed and shifting
the remaining goal indices.

Spot deviation goals cannot be combined with aggregate `spotRmsGoals`, maximum-radius spot
goals, or explicitly requested hexapolar sampling in the same builder configuration.
Maximum radius is inherently controlled by the worst sampled ray rather than a
Gaussian-weighted RMS distribution and remains a separate hexapolar use case.

### Suggested comparison measurements

When comparing contrast optimization across prescriptions, record:

- initial and final contrast merit;
- initial and final independently calculated Gaussian-quadrature MTF;
- initial and final spot RMS;
- RMS `deltaW` for every field, frequency, and orientation;
- the number of invalid contrast samples;
- runtime and solver evaluation counts.

The per-group RMS `deltaW` is particularly useful for identifying when the contrast
goal is acting as a faithful MTF refiner and when it has moved outside its reliable
small-phase operating range.

## Goal scaling and importance weights

### Issue and current behaviour

This is a design review of the current `rayoptics` optimizer, not an implemented
change. The proposals and syntax below are illustrative; existing prescriptions retain
their current meaning.

A useful merit function lets the designer specify both what constitutes a significant
error and how important that error is relative to others. Currently a goal weight often
has to express both. A small weight need not mean that an optical property is unimportant:
it may merely compensate for the units in which its error is measured.

`LMDerMeritFunction` takes the square root of `Goal._weight` and forms

```text
r_i = sqrt(w_i) (value_i - target_i)
M   = sum_i r_i^2 = sum_i w_i (value_i - target_i)^2
```

Thus the supplied weight multiplies the squared error, not the residual amplitude.
Changing a length residual from millimetres to microns increases its squared contribution
by one million unless the weight compensates. Some goals already contain quadrature
weights or normalization, so `value_i` is not always a raw physical measurement.

Two prescriptions expose the problem:

- [Nikkor version 5, trial 3](../Examples/jfotoptix/nikkor-58mm-f1.2/version5/Noct-Nikkor-58mmf1.2.txt)
  uses `goal spot-deviation 0 0 0 2.5e-5` alongside contrast goals. Its comment explicitly
  describes scaling spot deviation for 5/mm.
- [Leica APO 75/2, trial 2](../Examples/jfotoptix/leica-r-apo-75mm-f2-mandler/specs-original.txt)
  uses unit spot-deviation weights, with edge and thickness constraint weights of 128
  and 64. These numbers combine layout preference with compensation for spot merit scale.
  They do not by themselves establish how strongly those constraints ought to act.

### Review of all current goal families

The following inventory follows the goal classes, `OptimizationBuilder`, and the analysis
values they consume. Length units are normally millimetres; the spot classes explicitly
multiply system lengths by 1000 and thereby assume millimetres when reporting microns.

| Goal | Current value/error convention | Scaling issue and possible reference scale |
| --- | --- | --- |
| `GoalSpotDeviation` | Signed X/Y deviation about the reference-wavelength centroid, converted to microns and multiplied by `sqrt(pupil weight)`; target zero. Builder adds wavelength and field/orientation weights. | Divide by a fixed reference spot radius in microns; retain the signed per-ray residuals. |
| `GoalSpotRMS` | Aggregate RMS radius in microns minus the requested radius. | Reference radius error in microns. A nonzero target is an equality goal: a smaller radius can also incur error. |
| `GoalSpotMaxRadius` | Maximum sampled radius in microns minus the requested radius. | Reference radius error in microns. Scaling does not remove the change of controlling ray or turn the target into an upper bound. |
| `GoalRayAberration` | Signed transverse fan displacement in system length units; generated goals target zero. | Reference transverse error in those units. Unlike spot deviation, it has no micron conversion or Gaussian pupil weight. |
| `GoalGeoMTF` | MTF fraction minus target fraction; builder converts percentage targets by dividing by 100. | A meaningful MTF error, e.g. 0.05 for five percentage points. Dimensionless values still need an error scale. |
| `GoalMTFProxy` | `sin(pi * frequency * transverse_aberration)`, with reciprocal-length frequency. | Already dimensionless and frequency-scaled, but its characteristic error and sample aggregation still need a convention. Scaling cannot remove the proxy's periodicity. |
| `GoalContrast` | Wavefront difference in waves, optionally centred, multiplied by `sqrt(pupil weight)`; target zero. | Fixed reference wavefront difference in waves. This is a contrast proxy, not an MTF-fraction error. |
| `GoalContrastBalance` | Difference of weighted sagittal and tangential contrast energies, including wavelength and orientation weights; target zero. | Its value is quadratic in wavefront differences and its squared merit is quartic. It needs an energy scale and an explicit policy for embedded weights. |
| `GoalParax` | Raw first-order value minus target. Depending on the selected quantity this is a length, inverse length, angle in degrees, or a dimensionless quantity. | A scale per physical quantity or per goal. EFL and f-number anchors currently default to weight 1 despite having different units and acceptable deviations. |
| `ConstraintThickness` | Axial thickness relative to its starting value. | Already normalized: stored weight is caller weight divided by starting thickness squared. Expose this scale separately if adopting a common model. |
| `ConstraintEdgeThickness` | Edge separation relative to its starting value. | Same hidden fractional normalization; small initial gaps imply strong resistance to a given absolute change. |
| `ConstraintCurvature` | Curvature relative to starting curvature. | Same normalization in inverse-length units. Near-zero starting curvature raises the same question of relative versus absolute tolerance. |

The three layout constraints are soft anchors, not feasibility bounds. Their base class
requires a finite, nonzero starting value. A general normalization scheme must not apply
their existing `1/base^2` factor a second time.

### Separate error scale from importance

The recommended general model is

```text
r_i = sqrt(a_i) e_i / s_i
M   = sum_i a_i (e_i / s_i)^2
```

Here `e_i` is the error in its documented physical units, `s_i` is a fixed positive
reference error in the same units, and `a_i` is a dimensionless importance weight.
An error of one reference scale contributes `a_i` for a scalar goal. For sampled goals,
retain the integration weights separately: `r_i = sqrt(a_i q_i) e_i / s_i`.
The scale applies to the physical error, not to a single quadrature-weighted sample's
apparent magnitude. This makes it independent of the number of pupil samples.

A reference scale does not define a dead band, an acceptable upper bound, or a nonzero
target. Spot deviation still aims at zero. Likewise, dividing an MTF equality residual
by a tolerance does not make it a minimum-MTF constraint. Those are separate choices
about the shape of the objective.

For the Nikkor example, a reference radius of 200 microns and importance 1 gives exactly
the existing spot contribution:

```text
1 / 200^2 = 2.5e-5

# Proposed syntax only; not currently accepted by the parser:
goal spot-deviation scale 200
goal spot-deviation       0 0 0 1
```

The reciprocal of 5 cycles/mm is 0.2 mm, or 200 microns. This explains the existing
numeric scaling, but does not establish equivalence to contrast optimization at 5/mm:
contrast also depends on pupil shear, wavefront structure and its own residual convention.
The value 200 is an exact translation of that trial, not a recommended universal blur size.

For a layout anchor, `s = abs(start)` reproduces the current fractional merit. A designer
could instead specify a fraction of the start, or an absolute tolerance. If both are
supported, an explicit rule such as `s = max(absolute_floor, fraction * abs(start))`
handles near-zero starts without inventing an arbitrary denominator. Choosing this rule
would change the design preference and should be opt-in.

### Aggregation is a separate source of scale

Ordinary Gaussian pupil weights sum to one. Increasing the pupil sample count therefore
refines a pupil average rather than simply multiplying spot merit by the ray count.
Do not divide that merit by the sample count again. Contrast also carries its own pupil
quadrature weights; preserve its overlap integration convention.

Other axes are currently accumulated. The builder supplies wavelength weights directly
(or 1 per wavelength in unweighted mode), and sums fields, orientations and frequencies.
Adding wavelengths or fields can therefore strengthen optical goals against scalar
paraxial anchors and layout penalties. Ray-fan goals have no analogous Gaussian sample
normalization. Conversely, aggregate RMS and MTF values already perform averaging within
their analysis. Equal numeric goal weights do not imply equal family contributions.

There are two reasonable policies: retain a sum where each added requirement carries
additional importance, or define a weighted average where extra samples refine the same
requirement. A future design should specify this per axis and family, with separate
family importance and within-family distribution weights if needed. Normalizing all
field weights automatically would silently change existing prescriptions and could
erase an intentional increase in total importance.

`LMDerMeritFunction.rms()` reports `sqrt(sum(r_i^2) / number_of_goals)`. This is useful
within a fixed residual layout, but is not a physically comparable score across layouts:
adding quadrature residuals or zero-weight entries can change the denominator without
the corresponding change in optical quality. The solver's sum of squares and diagnostic
family averages should be distinguished.

### Special case: contrast balance

Let `E_s` and `E_t` denote the current weighted contrast energies. The balance contribution
is `b * (E_s - E_t)^2`. If ordinary wavefront-difference residuals are divided by a common
scale `s`, their energies are divided by `s^2`; a balance computed from those normalized
energies is consequently divided by `s^4` in the final merit. Applying a generic
amplitude scale to balance without accounting for this would give inconsistent results.

Two options merit consideration: retain the existing weighted-energy difference with an
explicit energy scale, or define balance from normalized contrast energies. For either,
decide whether balance should continue to mean equal *weighted contributions* or equal
physical sagittal/tangential errors. Currently changing an orientation weight changes
both ordinary contrast importance and the balance goal itself. That coupling should be
documented or deliberately redesigned, not accidentally removed by normalization.

A relative difference such as `(E_s - E_t) / (E_s + E_t + epsilon)` is another option,
but changes the objective and becomes sensitive to the denominator near a well-corrected
design. It is not simply a unit conversion or a necessary part of this proposal.

### Alternatives and tradeoffs

| Approach | Benefit | Limitation |
| --- | --- | --- |
| Change spot residuals to millimetres | Small implementation change. | Moves the arbitrary unit scale; changes existing mixed merits and leaves every other family unresolved. |
| Fixed documented family defaults | Easier initial prescriptions. | No universal spot, MTF or paraxial tolerance fits all designs; allow explicit overrides and preserve legacy mode. |
| Explicit physical reference errors | Readable, stable tradeoffs; works for zero targets. | Requires the designer to choose meaningful scales. Recommended foundation. |
| Divide by target magnitude | Convenient for nonzero anchors. | Fails at zero targets and confuses target size with acceptable error; poor general rule. |
| Freeze scales measured from the initial design | Can start families at comparable magnitudes. | Starting-design dependent; initially small or zero errors require floors, and a bad initial error may receive too little importance. Best as a diagnostic suggestion. |
| Recompute scales during optimization | Keeps numbers superficially comparable. | Changes the objective as the design moves and can cancel improvement; not recommended for a fixed least-squares merit. |
| Scale Jacobian columns or variables | Can improve numerical conditioning. | Addresses parameter step sizes, not physical tradeoffs among residuals. Keep separate from goal normalization. |

### Potential implementation and validation

Prefer a common goal-level representation of target, physical scale and importance,
while preserving physical `value()` reporting. A builder-only conversion to
`effective_weight = importance / scale^2` is mathematically sufficient for simple goals,
but repeats today's hidden normalization unless the original scale and importance are
also retained and reported. Sample weights and contrast balance require explicit handling.

An incremental implementation could start with spot deviation, then cover all families
under the same contract. Omitted scales should preserve existing behaviour, including
the fractional constraints. For a legacy simple goal, exact migration requires
`new_importance = old_weight * new_scale^2`, with unchanged sampling and aggregation.
Constraint migration should use the original caller importance and `abs(start)` scale;
balance migration must additionally account for any changed embedded energy weights.

Residual evaluation, Jacobian construction and reported merit must use the same scaling.
Scales must be finite and strictly positive, importance finite and nonnegative. Invalid
analysis values must retain failure semantics rather than becoming acceptable because
of a large scale or zero importance. Existing finite layout-penalty behaviour also needs
to be preserved deliberately.

Useful diagnostics would report each family's physical error, reference scale,
dimensionless error, importance, total squared contribution and fraction of total merit,
along with field/wavelength/frequency aggregation and invalid-sample counts. This would
show whether a large weight expresses a real preference or compensates for a convention.

Before adopting new defaults, validate exact legacy-merit and Jacobian equivalence,
consistent unit conversion, zero-target goals, fractional constraints, balance's quartic
scaling, and the chosen sampling/aggregation policies. Compare the two cited trials using
identical physical objectives first; assess changed defaults separately using spot/MTF
and layout measurements. No optimization runs or new default values are established by
this review.

## Contrast versus geometric MTF goals

A Gaussian-quadrature geometric MTF goal traces pupil rays to image-plane intercepts,
constructs a spot distribution, and estimates MTF from that distribution:

```text
pupil ray -> image intercept -> spot distribution -> estimated MTF
```

Contrast optimization instead compares pairs of wavefront samples separated by the
frequency-dependent pupil shear:

```text
paired pupil rays -> OPD difference -> least-squares residual
```

A contrast sample is therefore associated with one spatial frequency. A spot sample,
by contrast, can contribute to every MTF frequency calculated from the same spot
distribution.

Contrast goals are normally much smoother and cheaper to evaluate, but they are a
surrogate. Gaussian-quadrature MTF provides the more direct result and is useful both as
an alternative optimization goal and as an independent validation measurement.

## Preserving the starting lens design

Contrast optimization has a broad, smooth capture range and can substantially rearrange
a lens when many prescription parameters are free. Optical performance goals alone do
not preserve element shape, air gaps or mechanical layout. `OptimizationBuilder`
therefore provides optional soft constraints that anchor varied parameters to their
starting values.

```java
.applyCurvatureConstraints()
.applyThicknessConstraints()
.applyEdgeThicknessConstraints()
```

These methods add constraints only for the corresponding parameters that are actually
varied. They pair naturally with `varyAllCurvatures()` and `varyAllThicknesses()`,
but work equally with explicit surface lists.

Both take an optional weight. The no-argument form uses
`OptimizationBuilder.NOMINAL_CONSTRAINT_WEIGHT`, which is normally what you want: because
the residuals are fractions of each starting value, the per-parameter scaling is already
handled, and the weight sets only the global trade between optical performance and
preserving the layout.

Each constraint reports the parameter itself against a target of its starting value, in
the same shape as `GoalParax`. A curvature constraint returns the surface curvature `1/r`
against a target of `1/r0`; a thickness constraint returns `t` against a target of `t0`.
Curvature is used rather than radius because a large radius change near a flat surface
can represent a very small optical change.

The solver forms `(value - target) * sqrt(weight)`, and what these constraints resist is
a *fractional* change rather than an absolute one. That normalization is folded into the
weight, since

```text
(v/v0 - 1) * sqrt(w) = (v - v0) * sqrt(w / v0^2)
```

so the stored weight is the configured weight divided by the square of the starting
value, and a parameter's squared-merit contribution is

```text
weight * fractional_change^2
```

as it would be for an explicit fractional residual. One practical consequence: the weight
held on a constraint is not the number passed to the builder. It is larger for small
parameters and smaller for large ones, which is exactly what makes a 0.1mm air gap and a
39mm back focus resist the same proportional change equally. `Constraint` exposes
`fractional_deviation()` for reporting the proportional change directly.

`ConstraintThickness`, `ConstraintEdgeThickness` and `ConstraintCurvature` are named for
what they express, but they are implemented as penalty residuals in the least-squares
merit rather than as hard bounds: they are soft constraints, not feasibility limits. A
parameter is always free to move, it simply costs merit to do so. Increasing the weight
keeps the design closer to its original form; decreasing it gives the optimizer more
freedom. Nothing prevents a sufficiently strong optical gradient from pushing a parameter
a long way regardless of weight.

Since none of these imposes an absolute bound, final prescriptions still need mechanical
checks for extreme curvatures and clearance.

### Edge separation

`ConstraintThickness` holds axial centre thickness, which is not the same as keeping two
surfaces apart. The separation at height `h` is

```text
gap(h) = t + sag_next(h) - sag_this(h)
```

so curvature can bend two surfaces through each other while the axial gap sits untouched
at its starting value. That is how a solve with thickness constraints already in place
produced overlapping first and second surfaces on the Leica 75/2.

`applyEdgeThicknessConstraints()` anchors the quantity that actually goes negative,
measured by default at the smaller of the two bounding semi-diameters — the outermost
height at which both surfaces exist. It complements the axial constraint rather than
replacing it, and both are worth having whenever curvatures and thicknesses are varied
together.

On the Leica the two quantities are only loosely related, which is why one cannot stand in
for the other:

```text
surf |  axial t  |  edge gap  | edge/axial
   2 |    8.0000 |     0.9694 |      0.121
   9 |    1.5000 |     0.3052 |      0.203
   1 |    0.1000 |     4.7926 |     47.926
```

The tightest real clearance in that lens is 0.305mm at surface 9, where the axial
constraint is anchored to 1.5mm. A fractional move the axial constraint treats as small is
most of the actual clearance.

Gaps whose starting edge separation is not positive and finite are skipped: a fractional
constraint cannot be formed around zero, and a design that already starts with coincident
or crossed surfaces has nothing useful to anchor to.

### Choosing the weight

Because a contrast merit can contain thousands of sample residuals but only a few dozen
parameter-preservation residuals, compare their aggregate sum-of-squares contributions
when choosing weights. The nominal weight is a useful starting point, but is not
automatically equal in influence to the complete optical merit.

This matters more than it first appears, because the optical merit grows with field count
while the constraint count does not. Moving a setup from 4 fields to 11 took one contrast
block from 5184 residuals to 14256 — a factor of 2.75 — against an unchanged 29
constraints, so the nominal weight no longer held the line it was tuned to hold and the
layout collapsed. If you add fields, scale the constraint weight with them.

Raising the weight does tighten the design. On a fifteen-element f/2 with every air space
free, the worst thickness excursion fell from 39% to 11% to 3% at weights of 1, 10 and
100. The useful range is narrow, though: past the nominal weight the optical cost outruns
the benefit, and the constraints begin to dominate the Jacobian and stall the solver.

A single global weight is usually enough precisely because the residuals are fractional,
so the per-parameter scaling is already handled. When one particular surface or space
does need holding harder than the rest, construct the constraint directly rather than
raising the global weight:

```java
.additionalGoals(analysis -> new ConstraintThickness(analysis, 7, 10.0))
```

The factory receives the same `Analysis` the setup owns, and the constraint still reads
its starting value before any solving, so it anchors to the original prescription.

## The pupil the merit sees

Every residual is evaluated over a pupil, and which pupil that is depends on the
vignetting mode, on whether the factors are held fixed, and on whether rays are also
rejected by the physical surface apertures. All three are configurable.

### Vignetting mode

```java
.vignetting(VigType.SetPupil)   // the default
```

`SetPupil` resizes the pupil so the axial marginal ray meets the stop edge, then measures
all four vignetting factors with real rays. `SetVig` measures the same factors without the
resize and agrees closely, within 0.005 of pupil half-width and three to four MTF decimals
on both test lenses.

`Paraxial` is cheaper and behaves differently in a way that matters. It sets only the `y`
factors, because a paraxial ray is meridional and can say nothing about the sagittal
pupil, so `x` comes out unvignetted at every field. The pupil is then an ellipse even on
axis, where sagittal and tangential MTF must be equal by rotational symmetry: measured
0.148 apart at 40 cycles/mm on the Leica 75/2, and 0.010 on the Otus. Optimizing under it
means the sagittal pupil is a superset of the real one and the tangential pupil a subset,
roughly 19 percent short of the real tangential aperture at full field.

### Freezing the factors

Apertures are never optimization variables, but vignetting is not therefore constant: it
is where rays land on those fixed apertures. On the Leica 75/2, 28 of 29 variables move a
vignetting factor within a single Jacobian step. The drift is smooth, so it does not
corrupt the finite-difference Jacobian, but it does mean the solver differentiates the
design and the pupil together — and a more heavily vignetted lens has less aberration and
better MTF. Shrinking the pupil is therefore a way to improve the merit that costs nothing
in the merit and real light in the lens.

```java
.freezeVignetting()
```

This measures the factors once from a reference build and holds them for the run, so every
iteration is compared on the same pupil. The cost is staleness: the factors describe the
design at capture, and the further a solve travels the more the assumed pupil diverges
from the real one. Call `Analysis.discard_frozen_vignetting()` between solver restarts to
re-measure.

With `SetPupil` the captured pupil value is held as well, since factors measured at one
working f-number do not describe another. That pins `fod.fno`, which makes a `GoalParax`
on `Fno` inert in that combination.

### Physical aperture checking

```java
.checkSpotApertures(false)
```

Whether Gaussian-quadrature spot rays are additionally rejected when they cross a physical
surface aperture. On by default. Grid and hexapolar sampling always check, so this setting
applies to the Gaussian-quadrature path only.

Contrast sampling is the other way round: it never checks by default, because its samples
already occupy the common vignetted-pupil overlap and turning temporary clipping into a
discontinuous failure would hurt the optimizer. `ContrastOptions.check_apertures(true)`
overrides that for validation.

Frozen factors together with `checkSpotApertures(false)` gives a pupil that is entirely
factor-defined and fixed for the run, which is close to the conventional arrangement in
commercial optimizers. The trade-off is that nothing then catches factors which are wrong
or have gone stale: rays that the real lens blocks still contribute, so the merit can
optimize light the lens does not pass.

### Investigation: updating vignetting during optimization

[Optimize the Apertures, Not the Vignetting Factors](https://www.linkedin.com/pulse/optimize-apertures-vignetting-factors-javier-ruiz-uw0yf/)
argues that a sparsely sampled optimization merit should evaluate the vignetted pupil.
Vignetting factors remap normalized pupil coordinates into an ellipse fitted to the
surviving pupil. This is a sampling aid rather than a change to the optical system: with
a sufficiently dense pupil grid, an analysis should converge to the same result without
the remapping.

For Gaussian/Forbes quadrature the remapping is particularly important. Sampling the
complete entrance pupil directly can leave many nodes outside a cat's-eye-shaped
transmitted pupil. Discarding those rays biases the quadrature and can make the merit
change merely because the set of surviving samples changed. Vignetting factors allow a
small, fixed-size sample set to represent the transmitted pupil much more accurately.
They improve RMS convergence much more readily than a worst-ray quantity such as maximum
geometric spot radius, which still requires adequate sampling near the pupil boundary.

The article recommends recalculating the factors as the design evolves, rather than
necessarily freezing them at the start. That differs from Beam42's current optional
`freezeVignetting()` strategy. Freezing is reasonable for a local refinement in which the
prescription and its clipping change little, and it prevents the optimizer from improving
the merit simply by reducing the transmitted pupil. Its weakness is that the assumed
pupil becomes stale when the design moves substantially. This is especially relevant to
global optimization and to any future support for varying clear-aperture semi-diameters.

The physical variables should be the surface clear-aperture semi-diameters, not the
vignetting factors themselves. Factors are only an elliptical approximation to the
surviving pupil, and arbitrary optimized factors need not correspond to any realizable
set of apertures. If apertures become variables, relative illumination or throughput also
needs a constraint so that the optimizer cannot obtain better image quality merely by
discarding more of the pupil.

Vignetting remapping and physical aperture checking are separate operations:

- vignetting factors move sparse pupil samples into an approximation of the transmitted
  pupil;
- aperture checking verifies that each remapped ray actually passes every physical
  aperture, since the fitted ellipse is not the exact cat's-eye boundary.

The desired end state may therefore be to apply current vignetting factors and also check
physical apertures during optimization. A failed ray must not simply be omitted from an
RMS or contrast calculation: doing so changes the population being optimized and can
reward additional clipping.

This needs investigation before changing the default. In particular, compare the
following policies on local and large-displacement optimizations:

1. factors measured and frozen at the starting prescription;
2. factors recalculated for every merit-function evaluation;
3. the same two policies with physical aperture checking enabled;
4. dense, non-remapped analysis of each final prescription as the reference result.

Record merit continuity, failed-ray counts, independently measured spot RMS and MTF,
relative illumination, and the drift between frozen factors and factors recalculated for
the final prescription. This should establish whether dynamic factors give a more
accurate merit without introducing finite-difference noise or allowing uncontrolled
throughput loss.

## Useful links
* https://www.linkedin.com/pulse/optimize-apertures-vignetting-factors-javier-ruiz-uw0yf/

## Sampling limitations and follow-up work

Audit date: 2026-09-13. User-facing pattern choices are documented in
[OPTIMIZER.md](OPTIMIZER.md#supported-sampling-patterns).

### 1. Grid exists below the optimizer, but not in its execution path

**Current limitation:** `SpotOptions` and `SpotAnalysis.eval()` support grid, hexapolar,
and GQ. `OptimizationBuilder` and trial syntax expose only GQ and hexapolar.
`Analysis.compute()` treats any non-GQ pattern as hexapolar, so assigning
`PATTERN_GRID` directly silently selects the wrong pattern.

**Possible resolution:** add explicit pattern dispatch in `Analysis`, a builder grid
setting, parser/writer support, and tests that verify generated coordinates rather than
only the pattern flag. Reject unknown patterns instead of falling back to hexapolar.
Grid support in the analysis library alone must not be advertised as optimizer support.

### 2. Per-ray spot deviation assumes the GQ sample layout

**Current limitation:** the builder allocates `rings * spokes` samples for every field
and wavelength. `GoalSpotDeviation` reads a fixed sample index and multiplies the
deviation by the square root of its sample weight. The goal's formula itself does not
require Gaussian nodes, but the surrounding code currently does.

Only the GQ branch of `SpotAnalysis` forwards the option to preserve failed rays.
Grid and hexapolar spot wrappers pass `append_if_none=false`, so failures remove
samples and can change subsequent indices during a solve. Simply removing the
builder/parser restriction would produce incorrect residual correspondence.

**Possible resolution:** introduce a common weighted sample set containing coordinates,
stable indices and count. Construct residuals from that set and preserve a validity slot
for every ray in every tracing path. Normalize integration weights consistently so that
changing the number of grid/hexapolar rays does not arbitrarily rescale the merit.
Test failure/recovery of individual rays, residual count stability, and agreement between
the sum of squared deviations and the intended weighted RMS metric.

The prohibition on combining aggregate RMS with per-ray deviations is a separate
builder policy; it should be evaluated separately from pattern support.

### 3. Maximum-radius goals force a sampling preference

**Current limitation:** a built-in maximum-radius goal forces hexapolar sampling for
every spot and geometric-MTF goal in the stage. The metric itself just takes the maximum
sampled image-plane radius and can operate on other patterns. Ordinary GQ radial nodes
are interior, so they do not explicitly test the pupil rim.

**Possible resolution:** make rim coverage a goal requirement or a documented default,
rather than silently overriding explicit pattern selection. Options include a separate
boundary sample set or a pattern with boundary nodes. Test convergence of the maximum
with both radial and angular refinement; sampling the rim alone does not prove that the
largest image-plane miss occurs there.

Custom goal factories are a distinct escape hatch: a custom maximum-radius goal requests
hexapolar unless built-in deviation goals keep GQ for residual stability. Custom factories
cannot be written as trials, and this behavior is not evidence of general trial support.

### 4. Contrast sampling is coupled to GQ generation

**Current limitation:** `Trace.generate_contrast_quadrature()` starts with weighted GQ
points. It maps them into the vignetted pupil, positions them about the overlap centre,
and contracts the whole pattern until each reference and both displaced partners fit.
Weights include the vignetting Jacobian and are normalized. `GoalContrast` and balance
goals consume these results; ordinary spot-pattern settings have no effect on them.

**Possible resolution:** separate the base weighted sampler from the shear/overlap
transformation. Other patterns would need appropriate weights, stable counts and partner
validity, not just different point coordinates. GQ can remain the efficient default for
integration without being a universal requirement of the contrast objective.

**Separate numerical question:** increasing sample count refines the contracted region,
not necessarily the complete common overlap. Compare the current contrast proxy against
integration over the full overlap before claiming that a denser or different pattern
removes this approximation. Preserve frequency calibration, exit-pupil aiming and
centering behavior in such comparisons.

### 5. Sampling parameter names obscure computational cost

**Current limitation:** `hexapolarSampling(int numRays)`, `SpotOptions.num_rays()` and
historical documentation call the hexapolar setting a ray count. It reaches
`TraceRingsDef.num_rings`; the point count grows approximately as
`1 + 3*n*(n + 1)`. The default 64 therefore means thousands of rays, not 64 rays.
GQ's count is `rings * spokes`.

**Possible resolution:** introduce a clearly named ring-count API while retaining an
alias for existing callers. Keep the numeric meaning of saved trials unchanged; silently
reinterpreting existing values as total rays would violate round-trip compatibility.

### 6. Aperture and annulus settings are not uniform across samplers

**Current limitation:** GQ spot tracing honors `check-spot-apertures`; hexapolar and
lower-level grid tracing force physical-aperture checking. GQ spot inner radius applies
only to that spot sampler. Contrast uses separate options, defaults to no physical-aperture
checking, and does not inherit the spot annulus.

**Possible resolution:** explicitly describe each sampler's domain and clipping policy.
Any unification needs regression tests for vignetting, obscurations, failed rays and
weight normalization. Do not assume that choosing the same pattern name makes the
sampled pupil or merit identical.

### Corrected configuration defects

- **Invalid configuration silently repaired on writing:** the shared effective-pattern
  decision suppressed configured hexapolar sampling when spot-deviation was present.
  The writer now preserves configured intent; parsing rejects deviation with hexapolar
  or maximum-radius goals and reports the conflicting line. Regression tests cover
  writing an invalid Java builder and both input orders, including X/Y deviation rows.
- **Contrast accepted too few spokes:** the builder, reader and `ContrastOptions`
  accepted one or two spokes although the shared GQ generator requires at least three.
  These entry points now reject those values before tracing. One ring and three spokes
  are the structural minimum, not a convergence recommendation.

### Evidence and scope

The audit follows `OptimizationConfiguration`, `OptimizationBuilder`,
`OptimizationTrial`, `Analysis`, `SpotAnalysis`, `SpotIntercepts`, `ContrastAnalysis`,
and the sampling routines in `Trace`. Configuration tests cover pattern selection,
round-trip stability, unsupported syntax and numerical bounds. Existing tracing and
optimization tests cover representative lenses; this audit does not establish optical
convergence for every lens or implement the proposed pattern extensions.
