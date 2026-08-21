# Optical path difference and the finite reference sphere

This note explains the finite-pupil optical path difference calculation in
`WaveAbr.wave_abr_full_calc_finite_pup()`. The construction follows H. H. Hopkins,
[Calculation of the aberrations and image assessment for a general optical
system](https://doi.org/10.1080/713820605), particularly the equally inclined chord
construction.

The implementation returns the optical path difference of a test ray relative to the
chief ray:

```java
var opd = -n_obj*e1 - ray_op + n_img*ekp + cr_op - n_img*ep;
```

It is easier to read after grouping the terms:

```text
OPD = -n_obj e1 + (cr_op - ray_op) + n_img(ekp - ep)
```

The sign convention is **chief-ray optical path minus test-ray optical path**. A test
ray that travels farther than the chief ray therefore makes a negative contribution.

The comparison is not made directly between arbitrary points on the two rays. Equally
inclined chords establish corresponding input- and output-side reference points. On the
output side, the chord point is then moved onto the finite reference sphere centred at
the ideal image point.

## Overall interpretation

```text
common input wavefront
        |
        |  entrance correction e1
        v
first real surface
        |
        |  ray_op and cr_op through the sequential system
        v
last real surface
        |
        |  exit equally-inclined-chord correction ekp
        v
exit-pupil chord point B~'
        |
        |  reference-sphere correction ep
        v
reference-sphere point B'
```

The reference sphere is centred at the ideal image point and passes through the chief
ray's exit-pupil point. It represents the ideal spherical wavefront converging toward
the image. The OPD comparison terminates on the surface of this sphere, not at its
centre.

## The equally inclined chord distance

For two rays described by points and unit directions `(p, d)` and `(p0, d0)`, Beam43
uses

```java
e = ((d + d0).dot(p - p0)) / (1 + d.dot(d0));
```

This is `WaveAbr.eic_distance()`. It returns the signed distance along the first ray
from the corresponding equally inclined chord point to `p`.

## Entrance-space term: `-n_obj*e1`

Let the test and chief rays start at the same finite object point `O` and meet the first
surface at `P` and `P0`. Write their unit directions as `d` and `d0`, and their geometric
path lengths as `s` and `s0`:

```text
P  = O + s d
P0 = O + s0 d0
```

The entrance equally inclined chord distance is

```text
e1 = (d + d0).(P - P0) / (1 + d.d0)
```

Since

```text
P - P0 = s d - s0 d0,
```

and both directions have unit length,

```text
(d + d0).(P - P0)
    = s(1 + d.d0) - s0(1 + d.d0)
    = (s - s0)(1 + d.d0).
```

Therefore

```text
e1 = s - s0.
```

It is exactly the test-ray geometric path minus the chief-ray geometric path from the
object to the first surface. Multiplication by the homogeneous object-space index turns
this into optical path. Because the OPD convention is chief minus test,

```text
entrance OPD = n_obj(s0 - s) = -n_obj e1.
```

The chord formula remains useful for a very distant object because it obtains the path
difference from the ray positions and directions without explicitly representing the
distant object point.

## Through-system term: `cr_op - ray_op`

`RayPkg.op_delta` is the accumulated optical path with the ray tracer's equally
inclined-chord endpoint convention. It includes refractive-index-weighted propagation
through glass and air spaces; it is not merely geometric distance inside the glass.

The internal contribution is consequently

```text
chief-ray path - test-ray path = cr_op - ray_op.
```

## Exit-space chord term: `n_img*ekp`

The same chord formula is applied to the rays leaving the last real surface. To see its
interpretation, first suppose that both rays meet at a common image point `I`. If their
remaining geometric distances are `s` and `s0`, then

```text
I = P  + s d
I = P0 + s0 d0,
```

so

```text
P - P0 = -s d + s0 d0.
```

Substitution in the chord formula gives

```text
ekp = (d + d0).(P - P0) / (1 + d.d0)
    = s0 - s.
```

Thus, when both rays meet at the same image point,

```text
n_img ekp = chief remaining OPL - test remaining OPL.
```

An aberrated test ray generally does not pass through the ideal image point. In that
case `ekp` supplies the corresponding equally-inclined-chord comparison without
requiring the two rays to intersect. It takes the calculation to the chord point `B~'`
near the exit pupil, rather than all the way to the reference sphere.

## Reference-sphere correction: `ep`

Let

```text
p = p_coord                 chord point B~' relative to the exit-pupil centre
d = b4_dir                  test-ray unit direction after the last surface
r = ref_dir                 direction toward the reference-sphere centre
R = ref_sphere_radius       signed sphere radius
e = ep                      required signed distance along the test ray
```

The point reached after travelling `e` from the chord point is

```text
q(e) = p + e d.
```

The sphere centre is

```text
C = R r.
```

For `q(e)` to lie on the sphere,

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
J = p.p/R - 2r.p.
```

The ray-sphere equation is therefore

```text
e^2 - 2RF e + RJ = 0,
```

with discriminant

```text
4R^2(F^2 - J/R).
```

The relevant solution contains

```text
sqrt(F^2 - J/R).
```

The direct root `R(F - sqrt(...))` can subtract two nearly equal values because the
chord point is already close to the sphere. The implementation uses the rationalized,
numerically safer form

```text
ep = J / (F + sign*sqrt(F^2 - J/R)).
```

`sign` selects the appropriate one of the two intersections according to the signed
radius and propagation direction. Thus

```text
B' = B~' + ep d
```

lies on the reference sphere. Multiplying `ep` by `n_img` converts this final geometric
correction into optical path.

`ep` is not the full distance from the image point back to the exit pupil. That large
common distance is implicit in the reference sphere and cancels in the differential
comparison. `ep` is only the generally small distance between the chord point and the
nearby sphere intersection. Whether it is physically forward or backward along the
stored ray direction depends on the system's sign convention.

The complete output-space contribution is consequently

```text
n_img(ekp - ep).
```

## Geometric invariant

The reference-sphere calculation can be tested independently of any stored OPD value.
After calculating `ep`, either of the following residuals must be zero to numerical
precision:

```text
|p + ep d - Rr|^2 - R^2
```

or

```text
ep^2 - 2RF ep + RJ.
```

An off-axis, off-centre pupil ray should be used for this test. For the chief ray,
`p`, `J`, and `ep` are normally zero, so an incorrect discriminant can be hidden.

## Relationship to exit-pupil aiming

`ExitPupilAiming.chord_coord()` reconstructs the same `p_coord` that the finite-pupil
OPD calculation uses. `ExitPupilAiming.sphere_coord()` then computes

```text
p_coord + ep*b4_dir
```

because contrast aiming needs the actual per-ray coordinate on the reference sphere.
`WaveAbr` needs only `ekp` and `ep` for the scalar optical path calculation. These are
two consumers of the same geometry.

The calculation is also the ordinary ray-sphere quadratic used by Optiland's
[`reference_geometry.py`](https://github.com/optiland/optiland/blob/master/optiland/wavefront/reference_geometry.py#L43).
For coefficients

```text
a = 1,
b = -2RF,
c = RJ,
```

the standard discriminant `b^2 - 4ac` reduces to
`4R^2(F^2 - J/R)`.

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
