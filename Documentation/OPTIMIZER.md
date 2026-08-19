# Optimizer

## Goal Contrast

Contrast optimization uses pupil wavefront differences as a fast, smooth proxy for
MTF. It is intended primarily as a refinement method: it works best when the starting
prescription is already reasonably corrected and the phase differences are small.

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

### Comparison with Gaussian-quadrature MTF goals

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
Assessment for a General Optical System](https://doi.org/10.1364/AO.473823),
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
normalized separation is

```text
delta = 2 lambda F# nu
```

and Beam42 asks for the following physical vector on the exit-pupil reference sphere:

```text
Delta X_sag = R_exit (delta, 0)
Delta X_tan = R_exit (0, delta)
```

where `R_exit` is the paraxial exit-pupil radius and `X(u)` is the reference-sphere
coordinate reached by a ray launched at normalized entrance-pupil coordinate `u`.

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
induced cross-axis shear. The current limits are ten Newton iterations and a convergence
tolerance of `2e-7 R_exit`. Failure to trace, a singular inverse map, or failure to
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

## Per-ray RMS spot optimization

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
