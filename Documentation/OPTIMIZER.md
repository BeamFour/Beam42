# Optimizer

## Goal Contrast

Contrast optimization uses pupil wavefront differences as a fast, smooth proxy for
MTF. It is intended primarily as a refinement method: it works best when the starting
prescription is already reasonably corrected and the phase differences are small.

### What a sample means

A contrast sample is one location `p = (x, y)` in the entrance pupil at which the
wavefront is compared with slightly displaced copies of itself. The OTF shear is
physically defined at the exit pupil, but Beam43 launches rays in entrance-pupil
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

For every field and wavelength, Beam43 traces a centred probe pair separately in the
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
across individual samples remains. Correcting that last component would require aiming
each partner ray iteratively and would be substantially more expensive.

Calibration is off by default because it changes the sampled frequencies and therefore
the numerical merit function. It should normally be enabled for new contrast-
optimization work, while old regression cases may leave it disabled to preserve their
historical values.

## Preserving the starting lens design

Contrast optimization has a broad, smooth capture range and can substantially rearrange
a lens when many prescription parameters are free. Optical performance goals alone do
not preserve element shape, air gaps or mechanical layout. `OptimizationBuilder`
therefore provides optional soft constraints that anchor varied parameters to their
starting values.

```java
.constrainCurvature(curvatureWeight)
.constrainThickness(thicknessWeight)
```

These methods add constraints only for the corresponding parameters that are actually
varied. They pair naturally with `allCurvatureSurfaces()` and `allThicknessSurfaces()`,
but work equally with explicit surface lists.

For a surface whose starting radius is `r0`, the curvature constraint returns

```text
r0 / r - 1
```

which is the fractional change in curvature `c/c0 - 1`. Curvature is used rather than
radius because a large radius change near a flat surface can represent a very small
optical change.

For a thickness whose starting value is `t0`, the thickness constraint returns

```text
t / t0 - 1
```

Both are zero at the starting prescription. The merit function applies the square root
of the configured weight, so a parameter's squared-merit contribution is

```text
weight * fractional_change^2
```

`ConstraintThickness` and `ConstraintCurvature` are named for what they express, but they
are implemented as penalty residuals in the least-squares merit rather than as hard
bounds: they are soft constraints, not feasibility limits. A parameter is always free to
move, it simply costs merit to do so. Increasing the weight keeps the design closer to
its original form; decreasing it gives the optimizer more freedom. Nothing prevents a
sufficiently strong optical gradient from pushing a parameter a long way regardless of
weight.

Two consequences follow. Thickness constraints hold axial centre thickness only and do
not guarantee positive edge separation, which also depends on the sag of the two bounding
surfaces. And since neither imposes an absolute bound, final prescriptions still need
mechanical checks for negative gaps, element intersections, extreme curvatures and
clearance.

Because a contrast merit can contain thousands of sample residuals but only a few dozen
parameter-preservation residuals, compare their aggregate sum-of-squares contributions
when choosing weights. A nominal weight of `1.0` is a useful starting point, but is not
automatically equal in influence to the complete optical merit.

## Per-ray RMS spot optimization

`GoalSpotRMS` exposes one aggregate spot-radius value per field. Although suitable for
measurement, differentiating a single square-rooted aggregate gives the solver much less
information than exposing the signed ray deviations that make up the same RMS value.

Enable the granular form with one field weight per configured field:

```java
.gaussianQuadratureSampling(6, 12)
.spotRmsRayGoals(new double[] {1.0, 1.0, 1.0, 1.0})
```

Separate X and Y field weights are also supported:

```java
.spotRmsRayGoals(xWeights, yWeights)
```

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

Per-ray RMS goals cannot be combined with aggregate `spotRmsGoals`, maximum-radius spot
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
