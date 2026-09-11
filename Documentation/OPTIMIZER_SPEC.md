# Optimization Trials

An optimization trial is a section in a prescription file that describes an optimizer
run. It says which fields and frequencies are evaluated, what the solver may change,
what holds the starting design together, and what the run aims for. It does the job
that otherwise needs an `OptimizationBuilder` program like the ones in the `examples`
package, and LensTool2 runs it, in both the Java and C++ versions.

A prescription file can hold any number of trials, numbered `[trial 1]`, `[trial 2]` and
so on, so the different setups tried on a lens sit next to the lens they belong to.

For what the goals measure and how to choose weights, see [OPTIMIZER.md](OPTIMIZER.md).

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

The trial number is the argument following `--optimize`, when that argument does not
start with `--`. A number the file does not define is an error that lists the trials it
does define. `--optimize` without a number keeps its existing meaning, the routine
air-space optimization controlled by `--optimize-goal`.

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
vary aspherics    0  K  A4:1e6  A6  A8  A10
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

Terms are `K`, the conic constant, and `A<n>`, the coefficient of r<sup>n</sup>,
numbered as in the prescription's `[aspherical data]`: `A4`, `A6`, ... for an even
asphere, and `A3`, `A4`, ... for an odd one. `A2` is only available when the prescription
declares even aspheres with an A2 term. A surface with no aspherical data becomes an
asphere of the type the prescription declares, which is even unless it says otherwise.

Each coefficient variable carries a scale, so that the solver works with values of order
one. Give it after a colon, as in `A6:1e9`. Without one:

- an existing non-zero coefficient uses 10<sup>−floor(log10 |value|)</sup>, as
  `vary aspherics existing` does;
- a coefficient starting at zero uses 10<sup>round(log10 h<sup>n</sup>)</sup>, where h
  is half the surface's diameter from `[lens data]`. One unit of the scaled variable
  then moves the sag at the rim by about one lens unit. On the Noct-Nikkor's front
  surface this gives 1e6, 1e8, 1e11 and 1e14 for A4 to A10, against the hand-chosen
  1e6, 1e9, 1e11 and 1e14 in `NoctNikkor58mm`. A surface with no diameter needs
  explicit scales.

`K` is used unscaled and takes no scale.

## Constraints

```ini
constrain curvatures    1.0
constrain thicknesses   1.0
constrain edges         1.0
```

Each holds the varied parameters near their starting values: curvatures, centre
thicknesses, and the edge separation of every gap a varied parameter can move. The
number is the weight, and may be left out; it defaults to 1.0, the builder's nominal
weight. See "Preserving the starting lens design" in [OPTIMIZER.md](OPTIMIZER.md).

## Goals

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
astigmatism" in [OPTIMIZER.md](OPTIMIZER.md).

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
| `goal spot sampling gaussian <rings> <spokes> [<inner radius>]` | Gaussian-quadrature spot pattern, optionally annular. | `gaussian 14 20` |
| `goal spot sampling hexapolar <rays>` | Hexapolar spot pattern. | |

`spot-max-radius` switches the spot pattern to hexapolar.

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
run, so the file can be reported on, or the trial run again, as it stands.

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
same surfaces. Other trials in the input are not carried over.

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
vary aspherics    0  K  A4:1e6  A6:1e9  A8:1e11  A10:1e14

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

- `OptimizationTrial` in `org.redukti.optim` turns one numbered section into a
  configured `OptimizationBuilder`, and is ported to C++ like-for-like. Surface numbers
  pass straight through as the builder's zero-based indices; `all except` uses the
  builder's own `all` rules.
- Keywords map onto existing builder calls:

  | Trial | Builder |
  |---|---|
  | `configuration` | `scenario` |
  | `fields`, `frequencies`, `weighted`, `d-line-only` | `fields`, `mtfFrequencies`, `weighted`, `dLineOnly` |
  | `vignetting`, `frozen`, `check-spot-apertures` | `vignetting`, `freezeVignetting`, `checkSpotApertures` |
  | `vary curvatures`, `vary thicknesses` | `varyAllCurvatures`/`varyAllCurvaturesExcept`/`varyCurvatures`, and the same for thicknesses |
  | `vary aspherics existing` | `varyExistingAspherics`, or `varyExistingAsphericsExcept` when some surfaces have explicit rows |
  | `vary aspherics <surface> <terms>` | `additionalVariables` with `VarAsphK`/`VarAsphCoeff`, after extending the surface's coefficients |
  | `constrain ...` | `applyCurvatureConstraints`, `applyThicknessConstraints`, `applyEdgeThicknessConstraints` |
  | `goal contrast ...` | `contrastGoals`, `contrastBalanceGoals`, `contrastSampling`, `calibrateContrastFrequency`, `aimContrastAtExitPupil`, `centerContrastResiduals` |
  | `goal mtf ...` | `mtfGoals` |
  | `goal spot-...`, `goal spot sampling` | `spotRmsGoals`, `spotMaxRadiusGoals`, `gaussianQuadratureSampling`, `hexapolarSampling` |
  | `goal ray-aberrations` | `rayAberrationGoals` |
  | `goal paraxial` | `focalLengthGoal`, `fNumberGoal`, or `additionalGoals` with `GoalParax` |

- The builder's per-ray `spotDeviationGoals` are not exposed yet.
- The optimized prescription is `Prescription.to_opt_bench_str` followed by the trial's
  section text (`OptimizationTrial.optimizedPrescription`).
- Tests: each example trial is parsed into a setup and compared with the setup built by
  the example program, variable by variable and goal by goal. The same trial files drive
  the C++ tests.
