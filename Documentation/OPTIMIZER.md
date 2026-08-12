# Optimizer

## Goal Contrast

Contrast optimization uses pupil wavefront differences as a fast, smooth proxy for
MTF. It is intended primarily as a refinement method: it works best when the starting
prescription is already reasonably corrected and the phase differences are small.

### What a sample means

A contrast sample is one location `p = (x, y)` in the entrance pupil at which the
wavefront is compared with slightly displaced copies of itself. For each sample, three
rays are traced:

- a reference ray at `p`;
- a sagittal partner at `p + (s, 0)`;
- a tangential partner at `p + (0, s)`.

The displacement is

```text
s = 2 lambda F# frequency
```

where `lambda` is the wavelength, `F#` is the working f-number, and `frequency` is the
requested spatial frequency. The requested MTF frequency therefore determines the
separation of each pair of pupil rays.

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

