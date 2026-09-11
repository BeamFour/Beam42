# Optimization Trials

An optimization trial is a section in a prescription file that describes an optimizer
run. It says which fields and frequencies are evaluated, what the solver may change,
what holds the starting design together, and what the run aims for. It does the job
that otherwise needs an `OptimizationBuilder` program like the ones in the `examples`
package, and LensTool2 runs it, in both the Java and C++ versions.

A prescription file can hold any number of trials, numbered `[trial 1]`, `[trial 2]` and
so on, so the different setups tried on a lens sit next to the lens they belong to.

A trial is an `OptimizationBuilder` written as text, and goes both ways:
`OptimizationTrial.read` reads a trial into a builder, and `OptimizationBuilder.toTrial`
writes a builder out as a trial. The values in a trial are the values the builder takes in
code - surfaces are its zero-based positions, aspheric coefficients its coefficient
indices, MTF targets percentages - so a setup reads the same in either form.

[OPTIMIZER_NOTES.md](OPTIMIZER_NOTES.md) goes further into what the goals measure and how to weigh
them against each other.

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

Two things follow from that. A goal that yields many residuals - contrast or spot
deviation, one per sampled ray - lets the solver see through to what a change is doing:
it differentiates every residual against every variable, so it learns that moving a
surface helps these rays and hurts those, and can trade the two off. Aggregate the same
information into one number, an RMS spot radius say, and that cancellation has already
happened before the solver sees it; all it can tell is whether the total got better or
worse. And a block of thousands of residuals outweighs a handful of others unless the
weights say otherwise, so weights are best compared between blocks, not chosen one goal
at a time.

A ray that misses a surface, or an analysis that fails, makes a goal report a huge value
instead, which the solver reads as a rejected step rather than a direction to move in.

### The goals

| Goal | What each residual measures | Target |
|---|---|---|
| `goal contrast` | The wavefront difference between a pupil sample and a sheared copy of itself, per field, wavelength, sample and direction. A smooth, well behaved stand-in for MTF at that frequency. | 0 |
| `goal contrast balance` | The difference between the sagittal and tangential contrast blocks at a field: one residual that holds the two meridians level. | 0 |
| `goal mtf` | Geometric MTF at a frequency, field and direction, from the spot diagram. One residual each. | your percentage |
| `goal spot-rms` | RMS spot radius at a field, in microns. One residual per field. | your radius, 0 to minimize |
| `goal spot-max-radius` | The largest ray miss at a field, in microns. Needs hexapolar sampling, which samples the rim. | your radius |
| `goal spot-deviation` | The signed X or Y miss of each sampled ray, in microns: the RMS spot broken into its parts, so the solver sees which rays are wrong and in which direction. | 0 |
| `goal ray-aberrations` | Transverse aberration at each point of the classical sagittal and tangential ray fans. | 0 |
| `goal paraxial` | A first-order quantity: focal length, back focus, f-number, pupil positions and the rest. | your value |

Every run also anchors the effective focal length and the f-number to the prescription's
values at weight 1, so a solve cannot quietly rescale the lens. A `goal paraxial efl` or
`fno` line replaces that anchor.

Which to use: contrast is the refinement tool, and works best on a design that is already
reasonably corrected, since it assumes small phase differences; the geometric MTF goals
measure the same thing more directly but their merit surface is rough at the scale the
solver steps, so a solve driven by them stalls more easily; the spot goals suit a design
that is still far out. [OPTIMIZER_NOTES.md](OPTIMIZER_NOTES.md) covers what contrast samples mean,
how many residuals each choice produces, and how to choose between them.

### The constraints

An optical merit function has no opinion about mechanical layout: left alone the solver
will collapse air spaces and drive elements through one another. Each constraint holds a
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
| `2 4 5 12` | Exactly these surfaces. A flat surface listed here is varied, and so becomes curved. |

## Settings

| Keyword | Values | Default |
|---|---|---|
| `description` | Free text, shown when the trial runs. | none |
| `outdir` | Directory for the trial's optimized prescription and report, relative to the prescription's directory unless absolute. Created if missing; a command-line `--outdir` takes precedence. | the prescription's directory |
| `configuration` | Configuration to optimize, counting from 0. Only matters for a zoom. | `0` |
| `fields` | Relative field heights between 0 and 1, the first being 0. `0 to 1 step 0.1` is shorthand for eleven evenly spaced fields. | required |
| `frequencies` | Frequencies, in cycles/mm, at which the MTF is measured. | required |
| `weighted` | `yes` uses the prescription's wavelength weights; `no` weighs every wavelength equally. | `yes` |
| `d-line-only` | Restrict ray-aberration goals to the d line. | `no` |
| `vignetting` | A `--vig-type` name (`none`, `paraxial`, `set-vig`, `set-pupil`, ...), optionally followed by `frozen` to measure it once and hold it for the run. | `set-pupil` |
| `check-spot-apertures` | Whether Gaussian-quadrature spot rays are stopped by surface apertures. | `yes` |

`weighted` and `d-line-only` also shape the prescription used for the run, as the
example programs do.

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

### Contrast

```ini
goal contrast   10 30 50
goal contrast   sag  3 3 3 3 3 3 3 2 2 2 2
goal contrast   tan  1 1 1 1 1 1 1 1 1 1 1
goal contrast   30 tan  1 1 1 1 1 1 1 1 1 0.5 0.5
goal contrast   balance  all except 0.9 1.0   weight 1.0
goal contrast   sampling 6 12
```

| Line | Meaning | Default |
|---|---|---|
| `goal contrast <frequencies>` | Frequencies to optimize contrast at. Required for contrast goals. | |
| `goal contrast sag <weights>` | Sagittal weight per field, for every contrast frequency. | 1 |
| `goal contrast tan <weights>` | Tangential weight per field, for every contrast frequency. | 1 |
| `goal contrast <frequency> sag\|tan <weights>` | Weights for one frequency, overriding the rows above. | |
| `goal contrast balance <fields> [weight <w>]` | Hold sagittal and tangential contrast in balance. `<fields>` is `all`, `all except <field values>`, or one `yes`/`no` per field. | weight 0.1 |
| `goal contrast sampling <rings> <spokes>` | Pupil sampling for contrast. | `6 12` |
| `goal contrast calibrate yes\|no` | Correct the pupil shift so each sample realises the requested frequency. | `no` |
| `goal contrast exit-pupil-aiming yes\|no` | Aim the sheared rays on the exit pupil. Cannot be combined with `calibrate`. | `no` |
| `goal contrast centering yes\|no` | Subtract the constant part of each contrast block. | `no` |

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
| `goal spot-deviation <weights>` | Minimize RMS spot size through each ray's signed X and Y deviation, with one weight per field. | |
| `goal spot-deviation x\|y <weights>` | Separate X and Y weights; both rows are needed. | |
| `goal spot sampling gaussian <rings> <spokes> [<inner radius>]` | Gaussian-quadrature spot pattern, optionally annular. | `gaussian 14 20` |
| `goal spot sampling hexapolar <rays>` | Use the hexapolar spot pattern. | |

`spot-max-radius` switches the spot pattern to hexapolar. `spot-deviation` needs the
Gaussian-quadrature pattern, and cannot be combined with `spot-rms`. The two sampling
lines are separate settings and may both be given: the Gaussian-quadrature rings and
spokes are kept even when hexapolar sampling is chosen.

### Ray aberrations

```ini
goal ray-aberrations  yes
```

`yes` adds the ten-sample sagittal and tangential ray fans for every field and
wavelength, restricted to the d line by `d-line-only`. The default is `no`.

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

## The optimized prescription

The optimized prescription is written to `<prescription>-trial<n>.txt` in the trial's
output directory (see [Running a trial](#running-a-trial)), replacing any earlier one
from the same trial. It is the optimized lens as Beam42 writes a prescription - the same
format as the `prescription.txt` in a LensTool2 report - followed by the trial that was
run, as the builder writes it, so the file can be reported on, or the trial run again, as
it stands.

The trial is written back in the builder's own form: settings at their defaults are left
out, shorthand such as `0 to 1 step 0.1` is written out, and comments and blank lines are
not kept.

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
