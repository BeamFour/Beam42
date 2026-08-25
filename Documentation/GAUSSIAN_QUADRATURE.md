# Gaussian quadrature implementation

This note relates the `rayoptics` pupil sampler to Bauman and Xiao,
*Gaussian Quadrature for Optical Design with Non-circular Pupils and Fields,
and Broad Wavelength Ranges* (`Documentation/gaussian-quadrature.pdf`). Equation
numbers below refer to that paper.

## Filled circular pupil

`Trace.generate_gaussian_quadrature` constructs a polar tensor-product rule.
It obtains `n` Gauss-Legendre nodes `L_i` and weights `w_i` on `[-1,1]`, then
applies equation (3):

```text
u_i = (1 + L_i) / 2
rho_i = sqrt(u_i)
W_i,j = (w_i / 2) / numberOfSpokes
```

Every radial node is repeated at uniformly spaced angles. The weights sum to
one, so consumers compute a pupil average rather than the unnormalized integral
whose area is pi. This normalization does not change centroids, RMS spot radii,
or normalized geometric MTF.

The paper's Figure 1 labels its innermost three-ring radius `0.3535`, but its
equation (4) gives `0.3357`; the implementation follows the equation.

## Concentric annular pupil

Set the normalized inner radius explicitly:

```java
new SpotOptions()
    .use_gaussian_quadrature()
    .num_rings(6)
    .num_spokes(12)
    .inner_pupil_radius(0.50);
```

Optimization configuration provides the equivalent shorthand:

```java
builder.gaussianQuadratureSampling(6, 12, 0.50);
```

The implementation uses equation (6), generalized to outer radius `b`:

```text
rho_i = sqrt(a^2 + u_i (b^2 - a^2))
```

Here `a` is `TraceRingsDef.min_radius` and `b` is `max_radius`. Annular
sampling is explicit because the optical model can contain several obscuring
surface apertures; it cannot reliably infer which one represents a concentric
entrance-pupil obscuration.

## Field vignetting

`Field.apply_vignetting` maps normalized pupil coordinates using the field's
lower and upper x/y vignetting factors. These factors define the pupil into
which rays are launched; they allow the ordinary GQ rule to be used when the
resulting rays pass through the system. The quadrature weights therefore remain
weights in normalized pupil coordinates and are not multiplied by the mapping's
area Jacobian. This agrees with the OpticStudio vignetting-factor workflow, in
which GQ retains its normal pupil sampling after the factors are set.

This should not be confused with integrating a function over a separately
specified physical clipped aperture using a change of variables. Such an
integral would require the appropriate Jacobian. Nor is this mapping the
circular-arc sector/PSWF construction in section 2.4 of the paper. Exact
quadrature for arbitrary clipped circular arcs remains outside the
implementation's scope; use dense aperture-checked sampling when vignetting
factors do not adequately describe the transmitted pupil.

## Choosing rings and spokes

`n` radial nodes integrate polynomials in squared radius through degree
`2n - 1`. Angular samples are uniform, as in section 2.2. At least three spokes
are required to avoid a degenerate rule that fails basic disk moments, but this
minimum is not a general accuracy recommendation.

For an angular Fourier order through `m`, the paper requires at least `m + 1`
uniform angles. Choose the spoke count from the highest aberration order that
must be represented and verify convergence by increasing both rings and spokes.
The default `14 x 20` rule is retained for compatibility.

## Scope

Implemented:

- filled circular pupil quadrature (sections 2.1-2.2);
- concentric annular pupil quadrature (section 2.3);
- ordinary GQ weights over the normalized pupil defined by field vignetting
  factors;
- normalized weights consumed by spot, RMS, optimization, and geometric-MTF
  calculations.

Not implemented:

- PSWF angular quadrature for circular-arc vignetted sectors (section 2.4);
- Gaussian quadrature over wavelength (section 3);
- circular or rectangular field quadrature (section 4);
- automatic derivation of an entrance-pupil obscuration from surface apertures.

The focused regression tests are in
`GaussianQuadraturePatternTest` and `WeightedSpotAnalysisTest`. They check disk
moments, annular moments, normalization, and invalid rule
dimensions.
