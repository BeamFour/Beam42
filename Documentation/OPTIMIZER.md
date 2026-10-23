# Beam42 Optimizer

The Optimizer in Beam42 is not suitable for designing lenses from scratch as it
lacks global optimization, the ability to assign glasses, and other advanced features.
Its main use case is to fine tune existing designs.
It has also been used successfully in reverse engineering attempts
where glass types are known and initial estimates of curvatures and distances are
possible.

There is no UI for the optimizer. There are two ways it can be used:

* There is a Builder class in the library that can be used to construct the configuration
  and then execute it.
* You can add configurations in the LensTool2 input prescriptions. This is covered in
  the rest of this document.

Beam42 prescription files can include optimizer configurations alongside the lens specification.
Each [trial n] section defines an optimization run for one lens configuration (scenario).
An optional [pipeline n] section runs several trials in sequence, each starting from the previous
trial’s result. Pipelines are particularly useful for zoom lenses, where trials can alternate
between configurations that share the same optical surfaces, or for combining coarse
and fine optimization stages.

## Optimization Trials

An optimization trial is a section in a prescription file that describes an optimizer
run. It says which fields to evaluate, what the solver may change,
what constraints to apply, and what goals to aim for.

A prescription file can hold any number of trials, numbered `[trial 1]`, `[trial 2]` and
so on.

## Anatomy of a Trial

A trial has four parts, and the sections below follow them:

- **general settings**, these define what fields to use, wavelengths and weights, vignetting,
  solver tolerances, output directory, etc.
- **variables**, the parameters the solver may change: radii, thicknesses and aspheric
  terms;
- **goals**, what it aims to solve for;
- **constraints**, help ensure that the design maintains certain parameters during optimization.

Each goal contributes one or more *residuals*, a residual is the difference between the goal's
result and the specified target, i.e. `value − target`. The solver, Levenberg-Marquardt,
minimizes the sum of the squares of all the residuals, with a goal's weight multiplying its
squared residual, so weight 4 counts a miss like two of weight 1. The trial run prints as
the merit the root mean square of the weighted residuals, before and after.

When a ray misses a surface, or an analysis fails, the goal reports a huge value
instead; the solver reads this as a step to reject rather than a direction to move in.

## Trial syntax

```ini
[trial 1]
description   Contrast, every parameter free
# Anything after a # is a comment
fields        0  0.3  0.7  1.0
```

- A trial starts at a line `[trial <n>]` and runs to the next line that starts with `[`.
  `<n>` is a whole number, unique within the file. Trials need not be in order and the
  numbers need not be consecutive.
- The prescription reader skips sections it does not know, so a file with trials still
  works everywhere a prescription does.
- Each line is a keyword followed by values. Unlike the prescription sections, which
  must be tab separated, values here may be separated by spaces or tabs.
- `#` starts a comment that runs to the end of the line. Blank lines are ignored.
- Keywords and values are case insensitive, except wavelength letters. Yes/no settings
  accept `yes`/`no`, `true`/`false` and `on`/`off`.
- Lines may appear in any order. A setting given twice is an error.
- Anything the reader does not understand is an error naming the line: an unknown
  keyword, the wrong number of per-field values, a surface the prescription does not
  have. Settings that are not given take the defaults listed below.


## General Settings

| Keyword | Values | Default |
|---|---|---|
| `description` | Free text, shown when the trial runs. | none |
| `outdir` | Directory for the trial's optimized prescription and report, relative to the prescription's directory unless absolute. Created if missing; a command-line `--outdir` takes precedence. | the prescription's directory |
| `configuration` | Configuration to optimize, counting from 0. Only matters for a zoom. | `0` |
| `fields` | Relative field heights between 0 and 1, the first being 0. `0 to 1 step 0.1` is shorthand for eleven evenly spaced fields. | required |
| `wavelengths` | The wavelengths to evaluate, as Fraunhofer letters, and optionally their weights: `wavelengths <letters> [weights standard\|k9\|<values>]`. See below. | `d C e F g  weights standard` |
| `vignetting` | A `--vig-type` name (`none`, `paraxial`, `set-vig`, `set-pupil`, ...), optionally followed by `frozen` to measure it once and hold it for the run. | `set-pupil` |
| `check-spot-apertures` | Whether Gaussian-quadrature spot rays are stopped by surface apertures. | `yes` |
| `solver ftol\|xtol\|gtol <value>` | One of the solver's stopping tolerances, for this trial only. See below. | the solver's own |
| `solver max-evaluations <n>` | Trial steps the solver may take before it gives up. See below. | `100 * (variables + 1)` |

### Wavelengths

Defines set of wavelengths and weights to use for analysis.

```ini
wavelengths  d
wavelengths  d F C
wavelengths  d C e F g  weights standard
wavelengths  d C e F g  weights k9
```

The letters are the Fraunhofer ones and are case sensitive: `C` is 656.2725 nm and `c`
is not a line.

**Use d, C, F, e, C', F' and g.** The available wavelengths are limited by the supported
refractive indexes that are loaded into the glass catalog.

Omitting the `weights` clause weights every wavelength 1.0. Two predefined weightings
are available for the wavelength set `d C e F g`:

| name | d | C | e | F | g |
| --- | --- | --- | --- | --- | --- |
| `standard` | 1.0 | 0.475 | 0.98 | 0.49 | 0.15 |
| `k9` | 1.0 | 0.330 | 1.046 | 0.654 | 0.235 |

Any other weighting must be written out: `wavelengths d F C  weights 1 0.5 0.25`.

A wavelength's weight applies to everything: the analyses combine per-wavelength
results with it, and each per-wavelength residual's weight is its field and
orientation weight times its wavelength weight.

The trial does not read wavelengths or weights from the prescription file.

Use `standard` for general design work and `k9` when comparing with measurements from
the ZEISS MTF-Tester K9. The K9 weights are approximate readings from its published
spectral response curve.

See [Wavelength weighting](OPTIMIZER_NOTES.md#wavelength-weighting) for their sources.

### Solver tolerances

Leave these settings at their defaults unless you need to adjust the stopping criteria
or allow more trial steps. Each override applies only to this trial.

```ini
solver ftol             1.0E-8
solver xtol             1.0E-5
solver gtol             0
solver max-evaluations  500
```

| Setting | Stopping criterion | Default |
|---|---|---|
| `ftol` | Relative improvement in the sum of squared residuals is sufficiently small. | about `1.49E-8` |
| `xtol` | The scaled change in the variables is sufficiently small. | `0` (disabled) |
| `gtol` | Residuals are sufficiently close to orthogonal to every Jacobian column. | about `1.49E-8` |
| `max-evaluations` | The trial-step budget is reached. | `100 * (variables + 1)` |

Raise `max-evaluations` if the solver reaches its budget while the merit is still
improving. Smaller tolerances ask for stricter convergence. Keep `xtol` disabled unless
you want small parameter steps to stop the run; ray-trace noise can make these steps tiny.

The budget excludes evaluations used to estimate derivatives, so the total number of
merit evaluations can be much larger. Run with `--verbose` to see the settings and
iteration progress. See [Solver stopping criteria](OPTIMIZER_NOTES.md#solver-stopping-criteria)
for implementation details.

## Variables

```ini
vary curvatures   all except 7 10 24
vary thicknesses  7 14 19
vary aspherics    existing
vary aspherics    0  K  1:1e6  2  3  4
```

- `vary curvatures <list>` varies the radii of the listed surfaces.
- `vary thicknesses <list>` varies thicknesses. On a zoom, a thickness that differs
  between configurations is varied for the trial's configuration only.
- `vary aspherics existing` makes a variable of every non-zero conic constant and
  aspheric coefficient already in the prescription. A sphere stays a sphere, and an
  asphere gains no orders it did not have.
  Here "existing" means nonzero when the optimization setup is built, not merely
  present as a column in the input. Both explicitly supplied zeros and zeros inserted
  for omitted powers stay fixed. The variable list remains fixed during that solve,
  even if a coefficient reaches zero. A later pipeline stage builds a fresh list
  from the preceding stage's result.
- `vary aspherics <surface> <terms>` varies the named terms of one surface, adding any
  it does not have yet, starting from zero. An explicit row for a surface takes
  precedence over `existing` for that surface.

### Referring to surfaces

A surface is numbered by its position in `[lens data]`, counting from 0, as
`OptimizationBuilder` numbers surfaces: the first row is surface 0, and every row counts,
the aperture stop included. A thickness is numbered by the surface it follows, so
thickness 25 is the gap after surface 25 - the back focus, when that is the last row.

The ids in the first column of `[lens data]` are not used, nor are the distance names in
`[variable distances]`. So on a lens whose rows are labelled 1, 2, ..., 16, 16AS, 17, 18,
surface 16 is the stop, and surface 18 the last. The `prescription.txt` LensTool2 writes
numbers surfaces from 1, so its surface *n* is a trial's surface *n* − 1.

A surface list takes one of three forms:

| Form | Meaning |
|---|---|
| `all` | For curvatures, every surface except the aperture stop, field stops and flat (`Infinity`) surfaces. For thicknesses, every surface whose thickness in the configuration is not zero. |
| `all except 7 10 24` | The same set without the listed surfaces. |
| `2 4 5 12` | Exactly these surfaces, including flat surfaces if explicitly listed. |

An explicitly listed flat surface cannot be combined with `constrain curvatures`:
the fractional constraint needs a finite, non-zero starting curvature. Remove the
flat surface from the list or omit the curvature constraint for that trial.

### Aspheric terms

Terms are `K`, the conic constant, and coefficient indices, the index into the surface's
coefficient array that `VarAsphCoeff` and `varyAsphericCoefficient` take:

| Asphere | Index *i* is the coefficient of | So |
|---|---|---|
| even | r<sup>2(*i*+1)</sup> | 1 is A4, 2 is A6, 3 is A8, ... |
| even with A2 | r<sup>2(*i*+1)</sup> | 0 is A2, 1 is A4, ... |
| radial (`RADIAL`, both odd and even powers) | r<sup>*i*+1</sup> | 2 is A3, 3 is A4, 5 is A6, ... |

These are normalized array indices, not column numbers in Optical Bench's compact
aspheric row. For `AsphericalOddCount = 1` and supplied A3, A4, A6, A8, A10, A12,
the indices are **2, 3, 5, 7, 9, 11**. `vary aspherics existing` varies those
coefficients if nonzero, plus a nonzero conic constant. An explicit index 4 instead
requests A5, even though that power was omitted from the input. If it becomes
nonzero, prescription export increases the odd count to preserve it. An explicit
row replaces the entire `existing` selection for that surface, including its conic
constant; include `K` in that row if it should also vary.

An index the surface's type does not have - 0 on an even asphere without A2, 0 or 1 on
a radial one - is an error. A spherical surface becomes an asphere of the type the
prescription already uses, which is even when it has no aspheres.

A coefficient may have an explicit scale after a colon, for example `2:1e9`.
The solver varies the coefficient multiplied by that scale. If omitted, the scale is
chosen automatically from the existing coefficient or, for a zero coefficient, the
surface diameter. A surface with no diameter needs explicit scales for zero coefficients.

`K` is used unscaled and takes no scale. See
[Aspheric coefficient scaling](OPTIMIZER_NOTES.md#aspheric-coefficient-scaling)
for the scale calculation.

## Goals

Every goal line starts with `goal` and the goal type. Per-field rows take exactly one
value per entry in `fields`, in the same order. Weights left out default to 1.

| Goal | What each residual measures                                                                                                                              | Target                                      |
|---|----------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------|
| `goal contrast` | Reduces contrast loss at specified MTF frequency. Target is 0 loss.                                                                                      | 0                                           |
| `goal contrast balance` | Reduces the difference between the sagittal and tangential contrast loss at a field. Only available with contrast goals. May or may not be helpful.      | 0                                           |
| `goal mtf` | Geometric MTF at a frequency, field and direction.                                                                                                       | Target MTF percentage, 100 to maximize      |
| `goal spot-rms` | RMS spot radius at a field, in microns.                                                                                                                  | Target spot radius in microns, 0 to minimize |
| `goal spot-max-radius` | The largest sampled ray miss at a field, in microns. The current builder forces hexapolar sampling, including the rim.                                   | Target radius in microns                    |
| `goal spot-deviation` | The signed X or Y miss of each sampled ray, in microns: the RMS spot broken into its parts, so the solver sees which rays are wrong and in which direction. | 0                                           |
| `goal ray-aberrations` | Transverse aberration at each point of the classical sagittal and tangential ray fans.                                                                   | 0                                           |
| `goal paraxial` | A first-order quantity: focal length, back focus, f-number, pupil positions and the rest.                                                                | Target value                                |

Choose goals according to the result you need:

- Use `contrast` to improve MTF at selected frequencies. It works best on designs with
  relatively small aberrations.
- Use `mtf` to fit specified geometric MTF values.
- Use spot goals when a compact spot or a specified spot radius is the objective.
- Use paraxial goals to preserve or target first-order quantities such as back focus.

Enable curvature and gap constraints when refining an existing design. The automatic
EFL and f-number goals preserve the basic specification; add other paraxial goals as
needed. Adjust weights to balance optical performance against changes to the design.
Spot-deviation weights usually need to be far below 1; see
[Choosing spot-deviation weights](#choosing-spot-deviation-weights).

See [Goal residual granularity](OPTIMIZER_NOTES.md#goal-residual-granularity)
for the distinction between aggregate and per-sample goals.

### Supported sampling patterns

The following patterns are available for optimization trials. GQ means Gaussian quadrature.

| Goal | Supported sampling | Default or selection |
|---|---|---|
| `spot-rms` | GQ or hexapolar | GQ by default. |
| `spot-max-radius` | Hexapolar | Selects hexapolar for all spot and geometric-MTF goals in the stage, even when a `gaussian` sampling line is given. |
| `spot-deviation` | GQ | GQ only. |
| `mtf` | GQ or hexapolar | Uses the stage's spot pattern; GQ by default. |
| `contrast` | Separate GQ-based contrast sampling | Set with `goal contrast sampling`; default `6 12`. |
| `contrast balance` | The contrast goal's samples | Requires contrast goals. |
| `ray-aberrations` | Sagittal and tangential ray fans | Ten samples per fan. |
| `paraxial` and layout constraints | No spot sampling | Spot-pattern settings do not apply. |

There is one spot pattern per stage, shared by all spot and geometric-MTF goals.
Grid sampling is not available for optimization trials. Contrast sampling is independent:
a stage can use hexapolar spots and GQ-based contrast samples simultaneously. Sampling
settings do not enable goals themselves.

`check-spot-apertures` applies only to GQ spots. Hexapolar spots always check physical
apertures. Contrast does not check physical apertures by default; the trial spot setting
does not change this. The optional GQ spot inner radius does not configure contrast sampling.

### Contrast Goals

```ini
goal contrast   10 30 50
goal contrast   sag  3 3 3 3 3 3 3 2 2 2 2
goal contrast   tan  1 1 1 1 1 1 1 1 1 1 1
goal contrast   30 tan  1 1 1 1 1 1 1 1 1 0.5 0.5
goal contrast   balance  all except 0.9 1.0   weight 1.0
goal contrast   sampling 6 12
```

| Line | Meaning                                                                                                                                              | Default |
|---|------------------------------------------------------------------------------------------------------------------------------------------------------|---|
| `goal contrast <frequencies>` | Frequencies to optimize contrast at. Required for contrast goals.                                                                                    | |
| `goal contrast sag <weights>` | Sagittal weight per field, for every contrast frequency.                                                                                             | 1 |
| `goal contrast tan <weights>` | Tangential weight per field, for every contrast frequency.                                                                                           | 1 |
| `goal contrast <frequency> sag\|tan <weights>` | Weights for one frequency, overriding the rows above.                                                                                                | |
| `goal contrast balance <fields> [weight <w>]` | Attempts to balance sagittal and tangential contrast loss. `<fields>` is `all`, `all except <field values>`, or one `yes`/`no` per field. | weight 0.1 |
| `goal contrast sampling <rings> <spokes>` | Separate GQ-derived contrast pattern; at least 1 ring and 3 spokes.                                                                                  | `6 12` |
| `goal contrast overlap-quadrature yes\|no` | Integrate each orientation's full entrance-pupil overlap with independent ray pairs. Cannot be combined with exit-pupil aiming. | `no` |
| `goal contrast calibrate yes\|no` | Correct the pupil shift so each sample realises the requested frequency.                                                                             | `no` |
| `goal contrast exit-pupil-aiming yes\|no` | Aim the sheared rays on the exit pupil. Cannot be combined with `calibrate`.                                                                         | `no` |
| `goal contrast centering yes\|no` | Subtract the constant part of each contrast block.                                                                                                   | `no` |

Contrast begins with `rings × spokes` weighted GQ points for each field, wavelength,
and frequency. Each point has a reference ray and two displaced partners. The pattern
is mapped and contracted to keep the triplets in the common vignetted-pupil overlap;
weights are adjusted and normalized. This is not the ordinary spot pattern. There is
currently no contrast pattern selector for grid or hexapolar. The builder/trial default
is `6 12`; direct `ContrastOptions` defaults to `3 6`.

With `goal contrast overlap-quadrature yes`, each orientation instead uses
`rings × spokes` independent pairs over its full entrance-pupil overlap. `rings`
sets the Gaussian order along the shear; `spokes` sets the total transverse order,
split between the two pupil halves. Weights are normalized separately per orientation.
The circular and piecewise elliptical vignetted pupils are supported. Surface-aperture
clipping is still controlled separately. This option cannot be combined with exit-pupil
aiming; it does not correct nonlinear entrance-to-exit pupil mapping. It remains opt-in
so existing optimization results can be compared with the changed integration region.

In `balance`, fields are named by their value in `fields`, so nothing has to be
counted. The weight's scale is unlike the contrast weights'; see "Controlling
astigmatism" in [OPTIMIZER_NOTES.md](OPTIMIZER_NOTES.md).

### MTF Goals

```ini
goal mtf   10 sag   93 93 94 93
goal mtf   10 tan   93 93 90 82
goal mtf   10 weights   1 1 2 2
goal mtf   10 tan weights   1 1 2 4
```

| Line | Meaning |
|---|---|
| `goal mtf <frequency> sag\|tan <targets>` | Target geometric MTF per field, in percent. Both rows are required for each frequency. |
| `goal mtf <frequency> weights <weights>` | Weights per field for both directions. |
| `goal mtf <frequency> sag\|tan weights <weights>` | Weights per field for one direction. |

The MTF is measured at the frequencies these rows name, and at no others.

### Spot Goal

```ini
goal spot-rms         15 30 50 70
goal spot-rms weights 1 1 1 1
goal spot sampling    gaussian 6 12
```

| Line | Meaning | Default |
|---|---|---|
| `goal spot-rms <targets>` | Target RMS spot radius per field, in microns. | |
| `goal spot-max-radius <targets>` | Target maximum spot radius per field, in microns. | |
| `goal spot-rms\|spot-max-radius weights <weights>` | Weights for those targets. | 1 |
| `goal spot-deviation <weights>` | Minimize RMS spot size through each ray's signed X and Y deviation, with one weight per field. The weights are normally far below 1; see [Choosing spot-deviation weights](#choosing-spot-deviation-weights). | |
| `goal spot-deviation x\|y <weights>` | Separate X and Y weights; both rows are needed. | |
| `goal spot sampling gaussian <rings> <spokes> [<inner radius>]` | Gaussian-quadrature spot pattern: rings >= 1, spokes >= 3, inner radius in [0, 1). | `gaussian 14 20` |
| `goal spot sampling hexapolar <rings>` | Use hexapolar sampling; integer >= 1. Despite the Java parameter name `numRays`, this is a ring count, not a total ray count. | 64 rings when selected |

`spot-max-radius` switches the shared spot pattern to hexapolar, even when a Gaussian
sampling line is present. This is a builder policy intended to include pupil-rim rays.
The metric is still the maximum over sampled rays, not a guaranteed continuous maximum.

`spot-deviation` currently requires GQ and cannot be combined with explicit hexapolar
sampling, `spot-max-radius`, or `spot-rms`. The sampling conflicts are rejected during
trial parsing with a source line; the `spot-rms` conflict is rejected when building the
setup. Separate X/Y deviation-weight rows follow the same restrictions.

Both Gaussian and hexapolar sampling lines may be given when there are no
`spot-deviation` goals. They store separate settings, not two simultaneous patterns:
hexapolar takes precedence regardless of line order. Its count is a number of rings;
the generated point count is approximately `1 + 3*rings*(rings + 1)`, before tracing
failures. The default of 64 rings is therefore about 12,500 rays per field and
wavelength, against 280 for the default `gaussian 14 20`. GQ uses `rings * spokes` samples.

#### Choosing spot-deviation weights

Spot-deviation residuals are measured in microns. At weight 1 their squared contribution
can overwhelm the constraints and other goals. Choose a spatial frequency ν in cycles/mm
and use this starting weight:

```text
weight = (ν / 1000)²
```

| ν (cycles/mm) | 5 | 10 | 20 | 30 | 40 | 50 |
|---|---|---|---|---|---|---|
| weight | 2.5e-5 | 1e-4 | 4e-4 | 9e-4 | 1.6e-3 | 2.5e-3 |

This puts the spot goal on approximately the same merit scale as contrast at ν with
unit sagittal and tangential weights. A lower frequency gives it less influence.
Multiply by the desired field importance; for several frequencies, add their weights.

- This is a first-order approximation, measured on one lens. Adjust the weights as needed.
- The weight sets importance, not a blur size or tolerance; every residual targets zero.
- Lens data must be in millimetres; the goal converts to microns by multiplying by 1000.
- A zero weight removes a field from the merit but still traces it.
- This rule applies only to `spot-deviation`, not `spot-rms` or `spot-max-radius`.
- Equal merit scales do not imply equal optimized MTF. Use spot goals for compact spots
  and contrast or `goal mtf` for MTF.

See [Spot-deviation weight scaling](OPTIMIZER_NOTES.md#spot-deviation-weight-scaling)
for the derivation, measurements and examples.

### Ray Aberration Goals

```ini
goal ray-aberrations  yes
```

`yes` adds the ten-sample sagittal and tangential ray fans for every field and
wavelength, so `wavelengths d` is what restricts them to the d line. The default for
`goal ray-aberrations` is `no`.

### Paraxial Goals

```ini
goal paraxial   bfl  39.38
goal paraxial   efl  50    weight 2
```

`goal paraxial <quantity> <target> [weight <w>]` targets a first-order quantity, with a
weight of 1 by default. Every run already holds the effective focal length and
f-number at the prescription's values for the configuration, at weight 1. A trial's
`efl` or `fno` goal replaces that target rather than adding a second one.

| Quantity | Meaning |
|---|---|
| `efl` | Effective focal length |
| `bfl` | Back focal length |
| `ffl` | Front focal length |
| `fno` | f-number at the working conjugates |
| `img-dist` | Paraxial image distance |
| `obj-dist` | Object distance |
| `pp1` | Front principal plane, from the first surface |
| `ppk` | Rear principal plane, from the last surface |
| `enp-dist` | Entrance pupil distance, from the first surface |
| `enp-radius` | Entrance pupil radius |
| `exp-dist` | Exit pupil distance, from the last surface |
| `exp-radius` | Exit pupil radius |
| `img-ht` | Image height |
| `obj-ang` | Maximum object angle, in degrees |
| `obj-na` | Numerical aperture in object space |
| `img-na` | Numerical aperture in image space |
| `red` | Reduction ratio |
| `power` | Optical power |
| `opt-inv` | Optical invariant |

The first-order specification is held the same way, by the paraxial goals: the automatic
focal length and f-number anchors, and any `goal paraxial` line of your own - a back
focus, say. They hold the design to its basic specs while the optical goals work on it,
so they are constraints in everything but name, and the format calls them goals only
because that is how they are built.

Unlike curvature, thickness and edge constraints, paraxial goals use the plain
difference from the target, not a fraction of it, so at the same weight a quantity of
larger magnitude is held proportionally tighter: a 1% drift is a residual of 0.5 on a
50mm focal length, but only 0.02 on f/2. Raise the weight on the f-number, or on any
small quantity, if it moves more than you want.

## Constraints

An optical merit function has no opinion about mechanical layout: left alone, and depending on what it may vary,
the solver will collapse air spaces and drive elements through one another. Constraints such as below
try to prevent this.

| Constraint | Holds |
|---|---|
| `constrain thicknesses` | Each varied thickness, at its centre. |
| `constrain edges` | The edge separation of every gap a varied parameter can move, measured at the smaller of the two surfaces' semi-diameters. Complements the one above: two surfaces can keep their centre thickness and still cross further out. |
| `constrain curvatures` | Each varied surface's curvature - not its radius, which runs away to infinity on a near-flat surface. |

These residuals are *fractional*: the weight is divided by the square of the starting
value, so a 1% change costs the same on a 0.1mm air gap as on a 39mm back focus, and one
weight is sensible across a whole prescription.


```ini
constrain curvatures    1.0
constrain thicknesses   1.0
constrain edges         1.0
```

The number is the weight, and may be left out; it defaults to 1.0, the builder's nominal
weight. See "Preserving the starting lens design" in
[OPTIMIZER_NOTES.md](OPTIMIZER_NOTES.md) for how the weight behaves as it is raised.


## Running a trial

```bash
lenstool2 --specfile lens.txt --optimize 2
```

LensTool2 then:

1. reads `lens.txt` and its `[trial 2]` section;
2. builds the prescription for the trial's configuration, using the trial's
   `wavelengths` setting, and runs the solver;
3. prints the trial's description, the merit function before and after, the solver
   status, and the final value of every variable;
4. writes the optimized prescription, `lens-trial2.txt`, to the trial's output directory
   (see [The optimized prescription](#the-optimized-prescription));
5. produces the usual report from the optimized prescription in the same directory, as
   if LensTool2 had been run on `lens-trial2.txt`.

The output directory is the `--outdir` given on the command line; without one, the
trial's own `outdir`; without that, the directory `lens.txt` is in. Giving each trial an
`outdir` keeps their reports apart, since report files such as `layout.svg` are not
named after the prescription.

The number is the argument following `--optimize`, when that argument does not start with
`--`. It may name a `[trial n]` or a [pipeline](#pipelines); the two share one numbering,
so a number claimed by both is an error, as is one the file does not define.
`--optimize` without a number keeps its existing meaning, the routine air-space
optimization controlled by `--optimize-goal`.

## Pipelines

A pipeline runs trials one after another, each starting from the design the one before it
produced:

```ini
[pipeline 5]
description   Zoom: wide, then tele, twice over
outdir        trials/zoom
trials        1 2 1 2
```

`lenstool2 --specfile lens.txt --optimize 5` then runs trial 1, trial 2, trial 1 again and
trial 2 again, and writes `lens-pipeline5.txt`.

A zoom is what it was made for. Only one configuration is optimized at a time, so the wide
end is optimized first and the tele end starts from that result. The two configurations
share every radius and glass thickness, so the second stage partly undoes the first, which
is why the stages are usually written to alternate until the design settles. Nothing about
it is specific to zooms: a coarse stage followed by a fine one, or spot goals followed by
contrast, chain the same way.

| Line | Meaning | Default |
|---|---|---|
| `trials` | The trials to run, in order. A trial may appear more than once. | required |
| `description` | Free text, shown when the pipeline runs. | none |
| `outdir` | Where the result goes; the stages' own `outdir` lines are not used, since a pipeline writes one result. | the prescription's directory |

- Each stage reads its trial afresh, so a stage's `configuration` and `wavelengths`
  are its own.
- A pipeline runs trials, not other pipelines.
- Only the final design is written. The file carries the prescription, the pipeline, and
  every trial the pipeline names, so it can run the pipeline again as it stands - which is
  how you take another two passes at a zoom that has not settled.

## The optimized prescription

The optimized prescription is written to `<prescription>-trial<n>.txt` in the trial's
output directory (see [Running a trial](#running-a-trial)), replacing any earlier one
from the same trial. It is the optimized lens as Beam42 writes a prescription - the same
format as the `prescription.txt` in a LensTool2 report - followed by the trial that was
run, as the builder writes it, so the file can be reported on, or the trial run again, as
it stands.

The trial is written back in the builder's own form: shorthand such as `0 to 1 step 0.1`
is written out, and comments and blank lines are not kept.

Every setting the trial's goals consult is written out, its default included - the
contrast settings when it has contrast goals, the spot sampling when a goal needs the spot
analysis, and the wavelengths, vignetting and configuration always. A saved trial therefore
keeps its meaning if a default later changes. Settings nothing in the trial consults are
left out, as are weight rows of 1, since "an omitted weight is 1" is part of the format
rather than a default that could drift.

Being Beam42's own format, it holds what Beam42 reads and nothing else, whatever the input
carried:

- surfaces are labelled 1, 2, 3, ... in order;
- a zoom keeps only its configured scenarios, renumbered from 0 in the same order, so a
  trial's `configuration` still means the same zoom position;
- thicknesses that do not differ between configurations are written as numbers;
- data derived from the design - total length, principal points, element and group
  focal lengths - and anything else Beam42 does not use is left out, since it would no
  longer describe the optimized lens.

Surface positions are unchanged by this, so the trial carried over still refers to the
same surfaces. Other trials in the input are not carried over. A [pipeline](#pipelines)
writes `<prescription>-pipeline<n>.txt` the same way, carrying the pipeline and each trial
it names.

## Examples

The trials below reproduce setups from the `examples` package.

`NikkorZ85mmf12`:

```ini
[trial 1]
description           Contrast, every parameter free
fields                0 to 1 step 0.1
wavelengths           d F C
vignetting            set-vig frozen
check-spot-apertures  no

vary curvatures       all
vary thicknesses      all
vary aspherics        existing

constrain curvatures
constrain thicknesses
constrain edges

goal contrast         10 30 50
goal contrast         sag  3 3 3 3 3 3 3 2 2 2 2
goal contrast         balance  all except 0.9 1.0   weight 1.0
goal contrast         sampling 6 12
```

`ZeissOtusML50mm`:

```ini
[trial 1]
description       MTF targets, selected curvatures and the back focus
fields            0 0.3 0.7 1.0

vary curvatures   all except 7 10 24
vary thicknesses  25
vary aspherics    existing

goal mtf   10 sag   93 93 94 93
goal mtf   10 tan   93 93 90 82
goal mtf   20 sag   85 85 85 80
goal mtf   20 tan   85 85 78 62
goal mtf   40 sag   65 65 64 58
goal mtf   40 tan   65 62 45 38
goal ray-aberrations  yes
```

`NoctNikkor58mm`:

```ini
[trial 1]
description       RMS spot size, aspherising the front surface
fields            0 0.3 0.7 1.0

vary curvatures   all
vary aspherics    0  K  1:1e6  2:1e9  3:1e11  4:1e14

constrain curvatures

goal spot-rms        15 30 50 70
goal spot sampling   gaussian 6 12
goal paraxial        bfl 37.78
```

`Pentax80200mmf28`:

```ini
[trial 1]
description           Moving groups at the wide end
configuration         0
fields                0 to 1 step 0.1
wavelengths           d F C
vignetting            set-vig frozen
check-spot-apertures  no

vary thicknesses      7 14 19

goal contrast         10 30 50
goal contrast         balance  all except 0 0.9 1.0   weight 1.0
goal contrast         sampling 6 12
```

[OPTIMIZER_NOTES.md](OPTIMIZER_NOTES.md) goes further into what the goals measure and how to weigh
them against each other.
