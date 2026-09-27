# Upstream verification scripts

Tools for checking the `rayoptics` Java port against
[mjhoptics/ray-optics](https://github.com/mjhoptics/ray-optics), the project it is
ported from.

The port has no independent oracle, and upstream's own test suite does not provide one:
`test_sequential.py` traces the same ray two ways and compares them to each other rather
than to fixtures, `test_ideal_imager.py` covers a module that is not ported, and most of
`test_profiles.py` compares booleans. Checking the port therefore means running upstream
and comparing against it, which is what these scripts do.

## Setup

A Python environment with upstream ray-optics installed:

```
python -m venv <venv-dir>
<venv-dir>/Scripts/python -m pip install -e <path-to-ray-optics-checkout>
```

Install from a **local checkout** rather than PyPI. Two of the defects found in the port
were un-ported upstream *fixes* rather than mistranslations, so which upstream sits on the
other side of the comparison matters, and an editable install tracks whatever that checkout
is currently on.

Point `PYTHON` at the resulting interpreter:

```
export PYTHON=<venv-dir>/Scripts/python
```

Everything below also needs a built tree — run `mvn compile` first.

## `generate_upstream_test.sh`

Generates a JUnit regression test for one lens.

```
./generate_upstream_test.sh Examples/jfotoptix/<lens>/<spec>.txt \
    --only-d-line --dont-use-glass-types --vig-type set-vig
```

Three steps, all driven from the same `ModelSpec` so the Python and Java sides describe the
same system by construction: emit the Python model, run it under upstream to capture
reference values, then emit the Java test with those values inlined as literals. Output goes
to `rayoptics/src/test/java/org/redukti/rayoptics/upstream/`.

Values are inlined rather than loaded from a fixture at run time so that when a change moves
a number, the diff names the quantity and shows the delta. Regenerate rather than edit.

The generated test asserts, in separate methods so a failure names the layer that moved:

| Method | Covers |
| --- | --- |
| `first_order_data` | every `FirstOrderData` field |
| `paraxial_rays` | `ax` and `pr` ray `ht`, `slp`, `aoi` per surface |
| `vignetting_factors` | `vlx`, `vux`, `vly`, `vuy` per field |
| `chief_ray_aiming` | `z_enp` for wide angle models, the aim point otherwise |
| `ray_trace_f<field>_p<pupil>` | full ray segments through every surface - point, direction, distance, surface normal - plus `op_delta` |
| `aberration_fans` | transverse ray aberration and OPD per field, wavelength, x/y fan and ray |
| `seidel_coefficients` | per surface including aspheric contributions, plus the sum |

`ray_trace_*` is the only assertion that reaches the trace kernel directly - `bend`,
`reflect`, the surface intersections and the per-surface transforms. Everything else
exercises those through aggregates, where a defect shows up indirectly if at all. One test
method is emitted per ray: a single method holding every assertion would run to thousands of
statements and risk the 64KB limit on compiled method size, and splitting means a failure
names which ray diverged. `RAY_PUPILS` in `dump_reference.py` controls how many rays; one is
enough to detect a kernel regression, the rest add diagnostic breadth at the cost of
generated file size.

`aberration_fans` is the one the optimizer leans on most — its ray-aberration and MTF goals
are built on those quantities, so a regression there shows up as a moved optimum rather than
as an obviously wrong number. It goes through the same entry points the optimizer does,
`TransverseRayAberrationAnalysis.eval_abr_fan` and `WavefrontAberrationAnalysis.eval_opd_fan`,
and OPD is compared in waves on both sides.

`chief_ray_aiming` is the only assertion that reaches `find_real_enp_rev1` directly on a wide
angle model. Vignetting depends on it too, but only at solver tolerance, so without this a
regression in the wide angle search could slip through.

Tolerances are split by kind — 1e-12 relative for analytic quantities, 1e-6 for vignetting,
aiming and ray data. None of the three is an arbitrary loosening; the reasoning, and why
tightening them produces failures unrelated to correctness, is under **Traps** in
[../../../../Documentation/UPSTREAM_VERIFICATION.md](../../../../Documentation/UPSTREAM_VERIFICATION.md).

Add a new lens by running the script and committing the result. Pick lenses that exercise
something distinct — the current five cover the three aspheric coefficient conventions, a
plain double Gauss, and a wide angle model.

## `dump_reference.py`

Builds a model from a generated ray-optics script and writes the reference values as flat,
sorted `key=value` text.

```
$PYTHON dump_reference.py model.py > reference.txt
```

Flat text rather than JSON: no JSON dependency is needed on the Java side, and a regenerated
file diffs line by line so a moved number names itself. Normally invoked by
`generate_upstream_test.sh` rather than directly.

## `obench_diff.py`

Compares the sequential model upstream builds from an optical bench file against the one the
exporter describes.

```
$PYTHON obench_diff.py <spec>.txt model.py [--no-glass]
```

Upstream's `rayoptics/optical/obench.py` reads the same optical bench format Beam43 imports,
giving a second independent path to the same lens. This diffs curvature, thickness,
semi-diameter, refractive index, stop surface, profile type, conic constant and aspheric
coefficients — checking the importer and exporter with no ray tracing involved.

Generate the model with `--vig-type none`; vignetting is not compared by this route and only
slows it down.

## Things that will bite you

These live in
[../../../../Documentation/UPSTREAM_VERIFICATION.md](../../../../Documentation/UPSTREAM_VERIFICATION.md),
under **Traps**, so there is one copy to keep current. Read them before trusting a
comparison — several read as port defects when they are artifacts of the source data or of
upstream's own conventions:

- obench validates the sequential model, not the optical spec
- obench only ever reads scenario 0, so zoom configurations cannot be checked this way
- trailing zero coefficients from a stray tab in the source file
- lens data that puts a second glass name in the maker column, breaking obench's lookup
- the two dispersion models, which agree only at the d line
- tolerances on solver-dependent quantities, and why they are what they are
- `apply_vignetting` mutating its argument upstream but not here
- recording which upstream commit a fixture came from
