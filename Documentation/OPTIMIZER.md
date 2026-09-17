# Beam42 Optimizer

The Optimizer in Beam42 is not suitable for designing lenses from scratch as it
lacks global optimization, and other features necessary. Its main use case is to fine
tune existing designs. It has also been used successfully in reverse engineering attempts
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
run. It says which fields and frequencies are evaluated, what the solver may change,
what holds the starting design together, and what the run aims for. It does the job
that otherwise needs an `OptimizationBuilder` program like the ones in the `examples`
package, and LensTool2 runs it, in both the Java and C++ versions.

A prescription file can hold any number of trials, numbered `[trial 1]`, `[trial 2]` and
so on, so the different setups tried on a lens sit next to the lens they belong to.

## What a trial says

A trial has three parts, and the sections below follow them:

- **variables**, the parameters the solver may change: radii, thicknesses and aspheric
  terms;
- **goals**, what it aims at;
- **constraints**, what holds the starting design together while it does.

Each goal contributes one or more *residuals*, each the miss between what the design
gives and what the goal asks for: `value − target`. The solver, Levenberg-Marquardt,
minimizes the sum of the squares of all of them, with a goal's weight multiplying its
squared residual, so weight 4 counts a miss like two of weight 1. What the run prints as
the merit is the root mean square of the weighted residuals, before and after.

A ray that misses a surface, or an analysis that fails, makes a goal report a huge value
instead, which the solver reads as a step to reject rather than a direction to move in.

### The goals

| Goal | What each residual measures                                                                                                                              | Target                           | Aggregate |
|---|----------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------|-----------|
| `goal contrast` | Reduces contrast loss at specified MTF frequency. Target is 0 loss.                                                                                      | 0                                | No        |
| `goal contrast balance` | Reduces the difference between the sagittal and tangential contrast loss at a field. Only available with contrast goals. May or may not be helpful.      | 0                                | Yes       |
| `goal mtf` | Geometric MTF at a frequency, field and direction.                                                                                                       | your percentage, 100 to maximize | Yes       |
| `goal spot-rms` | RMS spot radius at a field, in microns.                                                                                                                  | your radius, 0 to minimize       | Yes       |
| `goal spot-max-radius` | The largest sampled ray miss at a field, in microns. The current builder forces hexapolar sampling, including the rim.                                   | your radius                      | Yes       |
| `goal spot-deviation` | The signed X or Y miss of each sampled ray, in microns: the RMS spot broken into its parts, so the solver sees which rays are wrong and in which direction. | 0                                | No        |
| `goal ray-aberrations` | Transverse aberration at each point of the classical sagittal and tangential ray fans.                                                                   | 0                                | No        |
| `goal paraxial` | A first-order quantity: focal length, back focus, f-number, pupil positions and the rest.                                                                | your value                       | Yes       |

The goals are classified as aggregate or not. An aggregate goal targets a metric at field/wavelength level that is affected by the 
design as a whole, the optimizer cannot see the effect of each individual ray trace. The non-aggregate goals allow the optimizer
to see through the impact of each ray trace.

The non-aggregate goals, particularly contrast and spot deviations, can cause the optimizer to significantly alter
a design. This is particularly true if the design is relatively simple or heavily aberrated. You must always enable
constraints on curvatures and gaps, as well as on paraxial values, if you want the design to be a refinement of the
original. Spot deviations at weight 1 also work on a far larger scale than the constraints; see
[Choosing spot-deviation weights](#choosing-spot-deviation-weights).

Every run anchors the effective focal length and the f-number to the prescription's
values at weight 1, so a solve cannot quietly rescale the lens. A `goal paraxial efl` or
`fno` line replaces that anchor.

The contrast goals produce the best MTF results, but may not work well with older aberrated
designs. The spot size goals are less effective in improving MTF because a spot size is not necessarily
correlated to MTF. The Geometric MTF goals are useful when you want to reproduce an MTF curve
rather than achieve the best MTF. But the Geometric MTF goals work at an aggregate level and may not 
be as effective.

You will need to play with weights to influence the outcome. Often it requires repeated trial and error
to come up with a configuration that gives good results.

### The constraints

An optical merit function has no opinion about mechanical layout: left alone, and depending on what it may vary, 
the solver will collapse air spaces and drive elements through one another. Each constraint holds a
varied parameter near where it started, as a cost rather than a bound - the parameter may
still move, it just has to earn it.

| Constraint | Holds |
|---|---|
| `constrain thicknesses` | Each varied thickness, at its centre. |
| `constrain edges` | The edge separation of every gap a varied parameter can move, measured at the smaller of the two surfaces' semi-diameters. Complements the one above: two surfaces can keep their centre thickness and still cross further out. |
| `constrain curvatures` | Each varied surface's curvature - not its radius, which runs away to infinity on a near-flat surface. |

These residuals are *fractional*: the weight is divided by the square of the starting
value, so a 1% change costs the same on a 0.1mm air gap as on a 39mm back focus, and one
weight is sensible across a whole prescription. A constraint never reports the huge value
described above: it exists to steer the solve, not to end it.

The first-order specification is held the same way, by the paraxial goals: the automatic
focal length and f-number anchors, and any `goal paraxial` line of your own - a back
focus, say. They hold the design to its basic specs while the optical goals work on it,
so they are constraints in everything but name, and the format calls them goals only
because that is how they are built.

They differ from the three above in a way worth knowing. Their residual is the plain
difference from the target, not a fraction of it, so at the same weight a quantity of
larger magnitude is held proportionally tighter: a 1% drift is a residual of 0.5 on a
50mm focal length, but only 0.02 on f/2. Raise the weight on the f-number, or on any
small quantity, if it moves more than you want.

## Running a trial

```bash
lenstool2 --specfile lens.txt --optimize 2
```

LensTool2 then:

1. reads `lens.txt` and its `[trial 2]` section;
2. builds the prescription for the trial's configuration, using the trial's `weighted`
   and `d-line-only` settings, and runs the solver;
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

## Layout

```ini
[trial 1]
description   Contrast, every parameter free
# Anything after a # is a comment
fields        0  0.3  0.7  1.0
frequencies   10 30 50
```

- A trial starts at a line `[trial <n>]` and runs to the next line that starts with `[`.
  `<n>` is a whole number, unique within the file. Trials need not be in order and the
  numbers need not be consecutive.
- The prescription reader skips sections it does not know, so a file with trials still
  works everywhere a prescription does.
- Each line is a keyword followed by values. Unlike the prescription sections, which
  must be tab separated, values here may be separated by spaces or tabs.
- `#` starts a comment that runs to the end of the line. Blank lines are ignored.
- Keywords and values are case insensitive. Yes/no settings accept `yes`/`no`,
  `true`/`false` and `on`/`off`.
- Lines may appear in any order. A setting given twice is an error.
- Anything the reader does not understand is an error naming the line: an unknown
  keyword, the wrong number of per-field values, a surface the prescription does not
  have. Settings that are not given take the defaults listed below.

## Referring to surfaces

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

## Settings

| Keyword | Values | Default |
|---|---|---|
| `description` | Free text, shown when the trial runs. | none |
| `outdir` | Directory for the trial's optimized prescription and report, relative to the prescription's directory unless absolute. Created if missing; a command-line `--outdir` takes precedence. | the prescription's directory |
| `configuration` | Configuration to optimize, counting from 0. Only matters for a zoom. | `0` |
| `fields` | Relative field heights between 0 and 1, the first being 0. `0 to 1 step 0.1` is shorthand for eleven evenly spaced fields. | required |
| `frequencies` | Frequencies, in cycles/mm, at which the MTF is measured. | required |
| `weighted` | `yes` selects the five d, C, e, F and g lines with built-in weights; `no` selects the three d, F and C lines with equal weights. | `yes` |
| `d-line-only` | Use only the d line for all goals in the trial, overriding the wavelength set selected by `weighted`. | `no` |
| `vignetting` | A `--vig-type` name (`none`, `paraxial`, `set-vig`, `set-pupil`, ...), optionally followed by `frozen` to measure it once and hold it for the run. | `set-pupil` |
| `check-spot-apertures` | Whether Gaussian-quadrature spot rays are stopped by surface apertures. | `yes` |
| `solver ftol\|xtol\|gtol <value>` | One of the solver's stopping tolerances, for this trial only. See below. | the solver's own |
| `solver max-evaluations <n>` | Trial steps the solver may take before it gives up. See below. | `100 * (variables + 1)` |

These settings build the prescription used for the run, as the example programs do.
With `weighted yes`, the weights for d, C, e, F and g are respectively 1.0, 0.475,
0.98, 0.49 and 0.15. With `weighted no`, d, F and C each have weight 1.0.
With `d-line-only yes`, the d line alone has weight 1.0. These are built-in choices;
the trial does not read wavelength weights from the prescription file.

### Solver tolerances

`solver` overrides one of the stopping tests in lmder, the least-squares solver, for
this trial. Anything not named keeps its default, so a trial that says nothing about
the solver runs as every trial did before these settings existed.

```
solver ftol             1.0E-8
solver xtol             1.0E-5
solver gtol             0
solver max-evaluations  500
```

| Setting | Stops the solve when | Default |
|---|---|---|
| `ftol` | the sum of squares improves by less than this, relatively | the square root of the machine epsilon, about `1.49E-8` |
| `xtol` | the variables stop moving: one iteration changes the varied curvatures, thicknesses and aspheric terms by less than this, relative to their own size. It measures the step in the design, not in the merit | `0`, which never stops it: ray-trace noise makes late steps tiny |
| `gtol` | the gradient is this flat | the square root of the machine epsilon |
| `max-evaluations` | the solver has taken this many trial steps | `100 * (variables + 1)` |

`max-evaluations` is the `maxfev` of lmder, and it counts trial steps only. The
Jacobian probes around them, two per variable per iteration, are not counted, so a
solve costs far more evaluations of the merit than this number suggests. Run with
`--verbose` to see the tolerances in use and one line per iteration.

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
- `vary aspherics <surface> <terms>` varies the named terms of one surface, adding any
  it does not have yet, starting from zero. An explicit row for a surface takes
  precedence over `existing` for that surface.

### Aspheric terms

Terms are `K`, the conic constant, and coefficient indices, the index into the surface's
coefficient array that `VarAsphCoeff` and `varyAsphericCoefficient` take:

| Asphere | Index *i* is the coefficient of | So |
|---|---|---|
| even | r<sup>2(*i*+1)</sup> | 1 is A4, 2 is A6, 3 is A8, ... |
| even with A2 | r<sup>2(*i*+1)</sup> | 0 is A2, 1 is A4, ... |
| odd | r<sup>*i*+1</sup> | 2 is A3, 3 is A4, ... |

An index the surface's type does not have - 0 on an even asphere without A2, 0 or 1 on
an odd one - is an error. A spherical surface becomes an asphere of the type the
prescription already uses, which is even when it has no aspheres.

Each coefficient variable carries a scale, so that the solver works with values of order
one: the variable is the coefficient times the scale. Give it after a colon, as in
`2:1e9`. Without one:

- an existing non-zero coefficient uses 10<sup>−floor(log10 |value|)</sup>, as
  `vary aspherics existing` does;
- a coefficient starting at zero uses 10<sup>round(log10 h<sup>n</sup>)</sup>, where h
  is half the surface's diameter from `[lens data]` and n the power of r. One unit of the
  scaled variable then moves the sag at the rim by about one lens unit. On the
  Noct-Nikkor's front surface this gives 1e6, 1e8, 1e11 and 1e14 for coefficients 1 to 4,
  against the hand-chosen 1e6, 1e9, 1e11 and 1e14 in `NoctNikkor58mm`. A surface with no
  diameter needs explicit scales.

`K` is used unscaled and takes no scale.

## Constraints

```ini
constrain curvatures    1.0
constrain thicknesses   1.0
constrain edges         1.0
```

What each one holds is described in [The constraints](#the-constraints) above. The number
is the weight, and may be left out; it defaults to 1.0, the builder's nominal weight. See
"Preserving the starting lens design" in [OPTIMIZER_NOTES.md](OPTIMIZER_NOTES.md) for how the weight
behaves as it is raised.

## Goals

What each goal measures is described in [The goals](#the-goals) above; this section is
the syntax.

Every goal line starts with `goal` and the goal type. Per-field rows take exactly one
value per entry in `fields`, in the same order. Weights left out default to 1.

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

### Contrast

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
| `goal contrast calibrate yes\|no` | Correct the pupil shift so each sample realises the requested frequency.                                                                             | `no` |
| `goal contrast exit-pupil-aiming yes\|no` | Aim the sheared rays on the exit pupil. Cannot be combined with `calibrate`.                                                                         | `no` |
| `goal contrast centering yes\|no` | Subtract the constant part of each contrast block.                                                                                                   | `no` |

Contrast begins with `rings × spokes` weighted GQ points for each field, wavelength,
and frequency. Each point has a reference ray and two displaced partners. The pattern
is mapped and contracted to keep the triplets in the common vignetted-pupil overlap;
weights are adjusted and normalized. This is not the ordinary spot pattern. There is
currently no contrast pattern selector for grid or hexapolar. The builder/trial default
is `6 12`; direct `ContrastOptions` defaults to `3 6`.

In `balance`, fields are named by their value in `fields`, so nothing has to be
counted. The weight's scale is unlike the contrast weights'; see "Controlling
astigmatism" in [OPTIMIZER_NOTES.md](OPTIMIZER_NOTES.md).

### MTF

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

The frequency must be one of `frequencies`.

### Spot

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

Each spot deviation residual is a ray miss in microns, and the weight multiplies its
square. Nothing else in a trial is measured on that scale. Constraints are fractional
changes, paraxial goals are plain differences in millimetres or f-number, and contrast
residuals are in waves. So at weight 1 the spot deviations outweigh everything else by
orders of magnitude, and the solver buys a smaller spot by reshaping the lens.

Large constraint weights do not stop this. On the Leica R Apo 75/2 (`specs-original.txt`,
trial 2), spot weights of 1 with `constrain edges 128` and `constrain thicknesses 64` still
let one thickness change by 29% and one curvature by 99%. The back focus ended 3.6mm short
of its target. See
[REVIEW.md](REVIEW.md#spot-deviations-outweigh-the-constraints-by-orders-of-magnitude).

Instead of starting from 1, choose a spatial frequency ν in cycles/mm and set

```text
weight = (ν / 1000)²
```

| ν (cycles/mm) | 5 | 10 | 20 | 30 | 40 | 50 |
|---|---|---|---|---|---|---|
| weight | 2.5e-5 | 1e-4 | 4e-4 | 9e-4 | 1.6e-3 | 2.5e-3 |

To first order, the wavefront difference a contrast sample measures at ν cycles/mm is ν
times the ray's transverse miss in millimetres. At this weight, therefore, a field's spot
deviations cost about as much as `goal contrast` at ν with `sag` and `tan` weights of 1 at
that field. The `x` weights stand for `sag`, and the `y` weights for `tan`.

The two goals are summed over fields and wavelengths in the same way, so this holds for
any `fields` and either `weighted` setting. On the Noct-Nikkor 58/1.2 the two agreed
within 0.8–1.4× at every field.

In practice:

- **Constraint weights carry over.** Weights that hold a design under a contrast goal at ν
  also hold it under spot deviations weighted for ν. Weights tuned against spot weights of
  1 do not transfer to anything else.
- **Field importance multiplies in.** `goal spot-deviation 0 0 1e-4 2e-4` asks for spot at
  10 cycles/mm at field 0.7, and at twice that importance at 1.0, like contrast weights of
  1 and 2.
- **Frequencies add.** To stand in for contrast at 10, 20 and 40 cycles/mm, use
  1e-4 + 4e-4 + 1.6e-3 = 2.1e-3.
- **Mixing with contrast, the frequency sets the balance.** In a trial with both, it says
  how much the spot goal counts beside the contrast goals. For example,
  `goal spot-deviation 0 0 0 2.5e-5` next to contrast at 10, 20 and 40 cycles/mm weighs
  field 1.0 as a contrast goal at 5 cycles/mm would.

Which ν to use is a design decision. A lower frequency gives the spot goal less say
against the constraints and the paraxial goals. The frequencies you would give a contrast
goal on the same lens give it as much say as contrast.

Keep in mind:

- **It is a starting point.** The relation is first order and has been measured on one
  lens. It is closest on a well-corrected design, where the wavefront is smooth across the
  shear. Adjust from there as with any other weight.
- **The weight is not a blur size or a tolerance.** Every residual still aims at zero; the
  weight only sets how much it counts.
- **Lens data must be in millimetres.** The goal converts system units to microns by
  multiplying by 1000.
- **A zero weight still traces the field.** It only removes the field from the merit.
- **Only `spot-deviation` has been worked out.** `spot-rms` and `spot-max-radius` are also
  in microns, but this rule has not been derived for them.

### Ray aberrations

```ini
goal ray-aberrations  yes
```

`yes` adds the ten-sample sagittal and tangential ray fans for every field and
wavelength. `d-line-only` restricts these and all other goals in the trial to the d
line. The default for `goal ray-aberrations` is `no`.

### Paraxial

```ini
goal paraxial   bfl  39.38
goal paraxial   efl  50    weight 2
```

`goal paraxial <quantity> <target> [weight <w>]` targets a first-order quantity, with a
weight of 1 by default. Every run already holds the effective focal length and
f-number at the prescription's values for the configuration; a trial's `efl` or `fno`
goal replaces that target rather than adding a second one.

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

- Each stage reads its trial afresh, so a stage's `configuration`, `weighted` and
  `d-line-only` are its own.
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
frequencies           10 30 50
weighted              no
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
frequencies       10 20 40

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
frequencies       10 30 50

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
frequencies           10 30 50
weighted              no
d-line-only           no
vignetting            set-vig frozen
check-spot-apertures  no

vary thicknesses      7 14 19

goal contrast         10 30 50
goal contrast         balance  all except 0 0.9 1.0   weight 1.0
goal contrast         sampling 6 12
```

## Implementation notes

- `OptimizationTrial.read(text, n, useGlassTypes)` reads `[trial n]` into an
  `OptimizationBuilder` for the prescription in the same text, built with the trial's
  `weighted` and `d-line-only`. `OptimizationBuilder.toTrial(n)` writes a builder back.
  Both are ported to C++ like-for-like.
- Each line is one builder call, with the same values:

  | Trial | Builder |
  |---|---|
  | `description`, `outdir` | `description`, `outdir` |
  | `configuration` | `scenario` |
  | `fields`, `frequencies`, `weighted`, `d-line-only` | `fields`, `mtfFrequencies`, `weighted`, `dLineOnly` |
  | `vignetting`, `frozen`, `check-spot-apertures` | `vignetting`, `freezeVignetting`, `checkSpotApertures` |
  | `vary curvatures`, `vary thicknesses` | `varyAllCurvatures`/`varyAllCurvaturesExcept`/`varyCurvatures`, and the same for thicknesses |
  | `vary aspherics existing` | `varyExistingAspherics` |
  | `vary aspherics <surface> K <index>[:<scale>] ...` | `varyConic`, `varyAsphericCoefficient` |
  | `constrain ...` | `applyCurvatureConstraints`, `applyThicknessConstraints`, `applyEdgeThicknessConstraints` |
  | `goal contrast ...` | `contrastGoals`, `contrastBalanceGoals`, `contrastSampling`, `calibrateContrastFrequency`, `aimContrastAtExitPupil`, `centerContrastResiduals` |
  | `goal mtf ...` | `mtfGoals` |
  | `goal spot-rms`, `goal spot-max-radius`, `goal spot-deviation` | `spotRmsGoals`, `spotMaxRadiusGoals`, `spotDeviationGoals` |
  | `goal spot sampling gaussian`, `goal spot sampling hexapolar` | `gaussianQuadratureSampling`, `hexapolarSampling` |
  | `goal ray-aberrations` | `rayAberrationGoals` |
  | `goal paraxial` | `paraxialGoal`, with the `ParaxHelper` id of the quantity |

- `additionalVariables` and `additionalGoals` take code rather than values, so a builder
  that uses them cannot be written as a trial; `toTrial` says so rather than dropping them.
- `OptimizationTrial.readPipeline(text, n)` reads `[pipeline n]` into an
  `OptimizationPipeline`, or returns null when the number names a trial;
  `OptimizationPipeline.toPipeline` writes it back. LensTool2 runs the stages in order,
  handing each the previous stage's prescription with the pipeline and its trials appended,
  so a stage reads its own trial from the text exactly as a single run does.
- The optimized prescription is `Prescription.to_opt_bench_str` followed by
  `OptimizationBuilder.toTrial`.
- Tests: each example trial is read into a builder and the setup it builds compared with
  the example program's, variable by variable and goal by goal - before and after a round
  trip through `toTrial`. The same trial files drive the C++ tests.

[OPTIMIZER_NOTES.md](OPTIMIZER_NOTES.md) goes further into what the goals measure and how to weigh
them against each other.
