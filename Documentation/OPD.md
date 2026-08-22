# Optical path difference and the finite reference sphere

This note explains the finite-pupil optical path difference calculation in
`WaveAbr.wave_abr_full_calc_finite_pup()`. The calculation follows H. H. Hopkins,
[Calculation of the aberrations and image assessment for a general optical
system](https://doi.org/10.1080/713820605), particularly his use of equally inclined
chords.

## Ideal definition of the OPD

Ideally, the optical path difference is obtained by comparing the complete optical
paths of a test ray and the chief ray. Both paths start on a common object-side
wavefront, pass through the optical system, and end on the finite reference sphere in
image space.

The reference sphere is centred at the ideal image point and passes through the chief
ray at the exit pupil. It represents the ideal spherical wavefront converging toward
the image. Each emerging test ray is extended to its intersection with this sphere;
the OPD comparison ends at that intersection, not at the centre of the sphere or at the
image plane.

Beam42 uses the sign convention

```text
OPD = chief-ray optical path - test-ray optical path.
```

A test ray whose total optical path is longer than that of the chief ray therefore has
a negative OPD.

Conceptually, the comparison is:

```text
common object-side wavefront
        |
        |  complete chief-ray and test-ray optical paths
        v
optical system
        |
        |  emerging rays continued in image space
        v
finite reference sphere centred at the ideal image point
```

This ideal definition is simple, but evaluating it by constructing two large absolute
optical paths and subtracting them is numerically unattractive. Object and image
distances may be very large while their relevant difference is very small. Subtraction
would consequently discard precision, and an object at infinity cannot conveniently be
represented by a literal starting point.

## Numerically stable reformulation

The implementation evaluates the same differential optical path without first
constructing the two large absolute paths. Equally inclined chord identities calculate
the small object- and image-space differences directly. The code returns

```java
var opd = -n_obj*e1 - ray_op + n_img*ekp + cr_op - n_img*ep;
```

or, after grouping the terms,

```text
OPD = -n_obj e1 + (cr_op - ray_op) + n_img(ekp - ep).
```

The three parts are:

```text
-n_obj e1              object-space path difference
cr_op - ray_op         accumulated path difference through the sequential system
n_img(ekp - ep)        image-space path difference to the reference sphere
```

The auxiliary chord points used to obtain these differences are part of the stable
calculation, not additional physical surfaces traversed by the rays. In particular,
the output chord point `B~'` is near the reference sphere, but the ideal comparison ends
at `B'`, the exact ray-sphere intersection.

The computational decomposition is:

```text
first real surface
        |
        |  object-space difference represented by e1
        |  accumulated system difference represented by ray_op and cr_op
        v
last real surface
        |
        |  image-space difference represented by ekp
        v
auxiliary equally-inclined-chord point B~'
        |
        |  small correction ep
        v
exact reference-sphere intersection B'
```

## The equally inclined chord distance

For two rays described by points and unit directions `(p, d)` and `(p0, d0)`, Beam42
uses

```java
e = ((d + d0).dot(p - p0)) / (1 + d.dot(d0));
```

This is `WaveAbr.eic_distance()`. It returns the signed distance along the first ray
from the corresponding equally inclined chord point to `p`. Its importance here is
that it obtains a path difference from local ray positions and directions, without
constructing the potentially distant point at which the two paths begin or end.

## Object-space term: `-n_obj*e1`

For a finite object, let the test and chief rays start at the same object point `O` and
meet the first surface at `P` and `P0`. Write their unit directions as `d` and `d0`, and
their geometric path lengths as `s` and `s0`:

```text
P  = O + s d
P0 = O + s0 d0.
```

The entrance equally inclined chord distance is

```text
e1 = (d + d0).(P - P0) / (1 + d.d0).
```

Since `P - P0 = s d - s0 d0` and both directions have unit length,

```text
(d + d0).(P - P0)
    = s(1 + d.d0) - s0(1 + d.d0)
    = (s - s0)(1 + d.d0),
```

and therefore

```text
e1 = s - s0.
```

Thus `e1` is exactly the test-ray geometric path minus the chief-ray geometric path
from the object to the first surface. Multiplication by the homogeneous object-space
index converts it to optical path. Under the chief-minus-test convention,

```text
object-space OPD = n_obj(s0 - s) = -n_obj e1.
```

The equally inclined chord expression remains usable for a very distant object because
it obtains this difference without explicitly representing the distant object point.

## Through-system term: `cr_op - ray_op`

`RayPkg.op_delta` is the accumulated optical path using the ray tracer's endpoint
convention. It includes refractive-index-weighted propagation through the air and glass
spaces; it is not merely geometric distance within the glass.

The contribution through the sequential optical system is consequently

```text
chief-ray path - test-ray path = cr_op - ray_op.
```

## Image-space chord term: `n_img*ekp`

In the ideal description, the two rays emerging from the last real surface must be
continued to their corresponding endpoints on the reference sphere. Directly
constructing their full image-space paths would again introduce large distances that
mostly cancel. The exit equally inclined chord distance `ekp` supplies their
differential propagation directly.

Its sign can be seen by temporarily considering two rays which meet at a common point
`I`. If their remaining geometric distances are `s` and `s0`, then

```text
I = P  + s d
I = P0 + s0 d0,
```

so `P - P0 = -s d + s0 d0`. Substitution in the chord formula gives

```text
ekp = (d + d0).(P - P0) / (1 + d.d0)
    = s0 - s.
```

Consequently,

```text
n_img ekp = chief remaining OPL - test remaining OPL.
```

An aberrated test ray generally does not pass through the ideal image point and the
actual endpoints lie on the reference sphere rather than at a common point. The same
equally inclined chord construction still provides the required differential path. It
locates the auxiliary chord point `B~'` close to the sphere without constructing and
subtracting the full image-space distances.

## Exact reference-sphere correction: `ep`

The chord construction leaves the test ray at `B~'`, close to but not generally on the
reference sphere. The small correction `ep` moves this point along the test ray to the
exact sphere intersection `B'`.

Let

```text
p = p_coord                 B~' relative to the chief-ray exit-pupil point
d = b4_dir                  test-ray unit direction after the last surface
r = ref_dir                 direction toward the reference-sphere centre
R = ref_sphere_radius       signed sphere radius
e = ep                      required signed distance along the test ray.
```

The point reached after travelling `e` from the chord point is `q(e) = p + e d`. In
these coordinates the sphere centre is `C = R r`. For `q(e)` to lie on the sphere,

```text
|p + e d - Rr|^2 = R^2.
```

Expanding this equation, using `d.d = r.r = 1`, gives

```text
e^2 + 2(p.d - R r.d)e + p.p - 2R r.p = 0.
```

The code defines

```text
F = r.d - d.p/R
J = p.p/R - 2r.p,
```

so the ray-sphere equation and its discriminant are

```text
e^2 - 2RF e + RJ = 0,
discriminant = 4R^2(F^2 - J/R).
```

The appropriate direct root can be written

```text
ep = R(F - sign*sqrt(F^2 - J/R)).
```

Because `B~'` is already close to the sphere, this expression may subtract two nearly
equal values. The implementation instead uses the algebraically equivalent,
numerically safer rationalized form

```text
ep = J / (F + sign*sqrt(F^2 - J/R)).
```

`sign` selects the appropriate one of the two ray-sphere intersections according to
the signed geometry and propagation direction. Thus `B' = B~' + ep d` lies on the
reference sphere. Multiplication by `n_img` converts this final geometric correction
into optical path.

`ep` is not the full distance between the exit pupil and the image point. That large
common distance is implicit in the reference sphere and cancels in the differential
comparison. It is only the generally small displacement from the auxiliary chord point
to the nearby sphere intersection. Whether that displacement is forward or backward
along the stored ray direction depends on the system's sign convention.

The complete image-space contribution is therefore

```text
n_img(ekp - ep).
```

## Term-by-term correspondence with the ideal OPD

| Ideal contribution | Stable implementation |
| --- | --- |
| Chief path minus test path from the common object wavefront to the first surface | `-n_obj*e1` |
| Chief path minus test path through the sequential system | `cr_op - ray_op` |
| Chief path minus test path from the last surface toward the reference sphere | `n_img*ekp` |
| Move the test-ray auxiliary chord point to its exact sphere intersection | `-n_img*ep` |

Combining these terms gives the same chief-minus-test optical path difference that
would ideally be obtained from the two complete paths, while avoiding subtraction of
large, nearly equal object- and image-space distances.

## Geometric invariant

The reference-sphere calculation can be tested independently of any stored OPD value.
After calculating `ep`, either residual

```text
|p + ep d - Rr|^2 - R^2
```

or

```text
ep^2 - 2RF ep + RJ
```

must be zero to numerical precision. An off-axis, off-centre pupil ray should be used
for this test. For the chief ray, `p`, `J`, and `ep` are normally zero, so an incorrect
discriminant can be hidden.

## Relationship to exit-pupil aiming

`ExitPupilAiming.chord_coord()` reconstructs the same auxiliary `p_coord` used by the
finite-pupil OPD calculation. `ExitPupilAiming.sphere_coord()` then computes

```text
p_coord + ep*b4_dir
```

because contrast aiming needs the actual per-ray coordinate on the reference sphere.
`WaveAbr` needs only `ekp` and `ep` for the scalar optical-path calculation. These are
two consumers of the same geometry.

The sphere correction is also the ordinary ray-sphere quadratic used by Optiland's
[`reference_geometry.py`](https://github.com/optiland/optiland/blob/master/optiland/wavefront/reference_geometry.py#L43).
For coefficients `a = 1`, `b = -2RF`, and `c = RJ`, the standard discriminant
`b^2 - 4ac` reduces to `4R^2(F^2 - J/R)`.

## Upstream compatibility note

The original Python `ray-optics` implementation historically used

```python
sqrt(F**2 + J/ref_sphere_radius)
```

in `wave_abr_full_calc_finite_pup()`. With the definitions of `F` and `J` above, direct
expansion of the sphere equation and the geometric invariant require the minus sign.
Tests whose purpose is exact comparison with historical upstream numerical results will
therefore differ when the geometrically exact expression is used. Such compatibility
expectations should be kept distinct from tests of the reference-sphere invariant until
the upstream issue is resolved.
