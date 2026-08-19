# Review: Contrast Optimization implementation

Review of the contrast-optimization implementation on branch `contrast_opt`, against:

> *Contrast Optimization: A faster and better technique for optimizing on MTF*
> Ken Moore, Erin Elliott, Mark Nicholson, Chris Normanshire, Shawn Gay, Jade Aiona — Zemax, LLC

Test case: `ZeissOtusML50mmTest.optimizesPatentPrescriptionUsingContrast()`

Findings 1–7 were written against the state of the branch at the time of the review;
finding 8 was added on 2026-08-15 and finding 9 on 2026-08-19. Everything through
finding 8 was revisited as of 2026-08-15; finding 9 reviews the frequency-normalization
work added afterwards and narrows the conclusion previously drawn in finding 7.
See [Open questions](#open-questions) for what is unresolved and
[Changes since this review was written](#changes-since-this-review-was-written) for work
that landed afterwards and does not belong to any single finding.

## Status

| # | Finding | Status |
| --- | --- | --- |
| [1](#1-vignetting-silently-rescales-the-shear--the-off-axis-merit-runs-at-the-wrong-frequency) | Vignetting rescales the shear | **Fixed** — shear now 40.0 cyc/mm at every field |
| [2](#2-the-default-3x6-sampling-is-too-coarse-and-the-optimizer-exploits-it) | 3×6 sampling too coarse | **Applied** — builder default now 6×12 |
| [3](#3-the-residual-is-the-un-centred-second-moment-not-the-variance) | Un-centred second moment | **Fixed, opt-in.** `centerContrastResiduals(true)`; may bear on the sagittal drop through the sag/tan balance |
| [4](#4-the-three-frequencies-are-near-collinear--two-thirds-of-the-ray-budget-is-wasted) | Frequencies near-collinear | **Open** |
| [5](#5-high-frequencies-fail-without-a-diagnostic) | High-frequency degeneracy | **Partly fixed** — the silent collapse now throws; the ~0.707×cutoff ceiling remains |
| [6](#6-the-least-squares-reduction-only-tracks-mtf-in-the-small-phase-regime) | Least-squares only valid at small phase | **Open, but downgraded.** The diagnosis stands; the regression that motivated it turned out to be defocus — see the [postscript](#postscript-the-motivating-regression-was-defocus) |
| [7](#7-the-shear-is-applied-at-the-entrance-pupil-not-the-exit-pupil) | Shear applied at entrance, not exit, pupil | **Fixed, opt-in.** `aimContrastAtExitPupil()` inverse-aims every partner to its requested reference-sphere separation; block calibration remains a cheaper approximation |
| [8](#8-vignetting-is-a-moving-mode-dependent-reference-frame) | Vignetting is a moving, mode-dependent reference frame | **Partly addressed.** Mode is configurable and the factors can be frozen; the perverse incentive it exposes is unmeasured |
| [9](#9-frequency-normalization-uses-ray-direction-not-hopkins-exit-pupil-shear) | Frequency normalization used image-ray direction rather than exit-pupil separation | **Resolved by tracing.** Residual rescaling remains removed; direct two-dimensional reference-sphere aiming now traces the ray whose OPD is required |

Findings 6 and 8 are two attempts at the same symptom — a Leica 75/2 mid-field sagittal
MTF drop — and neither currently explains it. Finding 6 blamed the least-squares
reduction, then its postscript blamed defocus; that account held only for the
curvature-only configuration it was measured on. Finding 8 records what vignetting does
instead, which is real and measured, but the run that would settle whether it is the cause
collapsed for unrelated reasons. [Open questions](#open-questions) has the current state.

Read that as a caution about this document generally: findings 1, 2, 5 and 7 rest on
direct measurement and have held up. The chain of explanations for the sagittal drop has
been revised three times.

## Code under review

Line references are as of 2026-08-15.

| File | Role |
| --- | --- |
| [ContrastAnalysis.java](../rayoptics/src/main/java/org/redukti/rayoptics/analysis/ContrastAnalysis.java) | frequency → pupil shear, wavefront-difference sampling, frequency calibration |
| [ContrastAnalysisResult.java](../rayoptics/src/main/java/org/redukti/rayoptics/analysis/ContrastAnalysisResult.java) | sample/residual container |
| [ContrastOptions.java](../rayoptics/src/main/java/org/redukti/rayoptics/analysis/ContrastOptions.java) | sampling configuration |
| [Trace.java:755](../rayoptics/src/main/java/org/redukti/rayoptics/raytr/Trace.java:755) | `trace_contrast` — overlap region and ray triplets |
| [SequentialModel.java:924](../rayoptics/src/main/java/org/redukti/rayoptics/seq/SequentialModel.java:924) | per-wavelength chief ray / reference sphere setup |
| [GoalContrast.java](../optimcommon/src/main/java/org/redukti/optim/GoalContrast.java) | one residual per (freq, field, wavelength, sample, orientation) |
| [Analysis.java:126](../optimcommon/src/main/java/org/redukti/optim/Analysis.java:126) | analysis driver — one `ContrastAnalysis.eval` per frequency |
| [OptimizationBuilder.java:544](../optimr2/src/main/java/org/redukti/optim/OptimizationBuilder.java:544) | contrast goal construction |
| [LMDerMeritFunction.java:116](../optimr2/src/main/java/org/redukti/optim/LMDerMeritFunction.java:116) | `buildJacobian` — added after the review, see §5 and the changes section |

## Verdict

The structure is faithful to the paper. One real bug materially corrupts the off-axis
merit; a second tuning problem is costing measurable lens quality. Both are fixable
without redesigning anything — and both now are.

The one structural limitation that remains (finding 6) is inherent to the technique as
implemented, not a coding error: the least-squares reduction is a faithful MTF proxy
only while the pupil phase difference stays well under a radian. Inside that regime it
is excellent and fast; outside it, it is not merely imprecise but non-monotone.
Contrast optimization is therefore best read as a *refiner* — superb once a design is
roughly corrected, and to be validated against an independent MTF measurement when it is
not.

(This paragraph originally ended "and it can walk a design backwards", citing the Leica
mid-field drop. That claim was withdrawn when the drop looked like defocus, and the
defocus account has since been narrowed to the configuration it was measured on — see
both postscripts to §6. The non-monotonicity is measured and real; what has never been
established is that it costs a design anything in practice.)

A limitation the original review did not consider at all: the merit's *pupil* is as much
a modelling choice as its residuals, it differs between vignetting modes, and it moves
while the solver works. [Finding 8](#8-vignetting-is-a-moving-mode-dependent-reference-frame)
covers it.

**What is correct:**

- Shear `= 2·λ·F#·ν` in normalized pupil radii
  (`normalized_entry_pupil_shift`,
  [ContrastAnalysis.java:63](../rayoptics/src/main/java/org/redukti/rayoptics/analysis/ContrastAnalysis.java:63))
  — displacement 2 at the incoherent cutoff `1/(λF#)`. `fod.fno` is the working F/#,
  so finite conjugates are handled.
- OPD is returned in waves, so residuals are commensurate across wavelengths.
- Gauss-Legendre quadrature weights sum to 1 and enter the residual as `√w`, with the
  goal weight contributing a second `√w` in `LMDerMeritFunction`, giving a correct
  weighted least squares `Σr² = Σ w·ΔW²`.
- Sharing the reference ray between the sagittal and tangential members of a triplet
  is a genuine 3-rays-per-sample economy over the naive 4.
- The pupil-shift direction convention (sagittal = x shear, tangential = y shear) is
  the standard one.
- `check_apertures = false` during the triplet trace is a deliberate and correct
  smoothness choice, consistent with how the wavefront fans are traced.
- Unlike the neighbouring `trace_grid` / `trace_rings`, `trace_contrast` correctly uses
  `wavelengths[wl]` rather than the central wavelength when a single wavelength index
  is requested.

**Measured speed claim (warm JIT, 20 repetitions):**

```
contrast analysis only            15.7 ms
contrast + ray-aberration fans    15.8 ms
contrast + fans + spot/MTF        64.4 ms
```

The paper's central claim holds — a contrast evaluation is **4.1× cheaper** than the
full spot/MTF analysis it replaces.

---

## 1. Vignetting silently rescales the shear — the off-axis merit runs at the wrong frequency

**Severity: high. This is a live bug in the test case, not a latent one.**

> **FIXED.** `trace_contrast` now traces with `apply_vignetting = false` and
> `generate_contrast_quadrature` maps the nominal pattern into the physical vignetted
> pupil, so the shear is added in absolute pupil coordinates and stays a rigid
> translation. Verified: the realized shear is 0.06743 — 40.0 cyc/mm — in both
> directions at all four fields. `tan@40` at full field went 0.567 → 0.706.
> `VigCalc.set_pupil` now also calls `set_vig` unconditionally, so the factors this
> depends on can never be left at zero on a freshly built model.

As reviewed, [Trace.java:768](../rayoptics/src/main/java/org/redukti/rayoptics/raytr/Trace.java:768)
set `apply_vignetting = true` while working in `REL_PUPIL` coordinates, so `Field.apply_vignetting`
rescales every pupil coordinate before the trace. But the shear is computed from the axial
F/# in *unvignetted* pupil fractions and then applied in *vignetted-relative* coordinates.

This lens is heavily vignetted (`vuy = 0.636`, `vly = 0.687`, `vux = vlx = 0.216` at full
field), so the shear that actually reaches the ray trace is:

| field | sagittal shear | → effective ν | tangential shear (upper/lower) | → effective ν |
| --- | --- | --- | --- | --- |
| 0.0 | 0.06743 | 40.0 | 0.06743 / 0.06743 | 40.0 / 40.0 |
| 0.3 | 0.06651 | 39.5 | 0.06222 / 0.05580 | 36.9 / 33.1 |
| 0.7 | 0.06167 | 36.6 | 0.04614 / 0.03707 | 27.4 / 22.0 |
| 1.0 | 0.05284 | **31.3** | 0.02456 / 0.02110 | **14.6 / 12.5** |

(requested shear for 40 cyc/mm = 0.06743 pupil radii)

At full field the tangential contrast goal is effectively optimizing ~13 cyc/mm while
asking for 40. Two further consequences:

- Because `apply_vignetting` applies a *different* scale to the upper and lower pupil,
  the shear is not a rigid translation — it kinks at `pupil.y = 0`, so a base point and
  its sheared partner straddling the axis get inconsistent displacements.
- The three-disk overlap geometry in `trace_contrast` is reasoned in the pre-vignetting
  coordinate system while the physics happens post-vignetting, so the sampled region no
  longer matches the real aperture.

**Symptom:** `tan@40` at field 3 is the worst number in every configuration tried
(0.539–0.567) and is the *only* metric that does not respond to better sampling. The
merit barely sees it.

**Suggested fix:** trace the triplet with `apply_vignetting = false` and construct the
sample region inside the vignetted aperture explicitly, so the shear remains a true
translation in physical pupil space. Rescaling the shift per axis is a partial patch —
it does not remove the upper/lower kink.

---

## 2. The default 3×6 sampling is too coarse, and the optimizer exploits it

**Severity: high (quality, not correctness).**

> **APPLIED.** `OptimizationBuilder` now defaults to 6×12 and `createContrastSetup`
> uses it. The Otus golden values were regenerated: spot RMS improved at all four
> fields and 7 of 8 MTF numbers improved. 12 spokes also fixes the x/y sampling
> asymmetry noted under "Smaller items". Cost is ~3-4× the solve time.

`ContrastOptions` defaults to 3 rings × 6 spokes = 18 points, and
`createContrastSetup` used `contrastSampling(3, 6)`. (`ContrastOptions` still defaults to
3×6; what changed is the builder default, which is what every optimization goes through.)

On the **starting** design 18 points are adequate — 0.2084 against a converged 0.2074,
0.5% error. On the **optimized** design they are not:

| field | σ(ΔW) at 3×6 (what the optimizer sees) | converged (20×40) | error |
| --- | --- | --- | --- |
| 0 | 0.0569 | 0.0629 | −10% |
| 1 | 0.0687 | 0.0962 | −29% |
| 2 | 0.0352 | 0.0644 | **−45%** |
| 3 | 0.0402 | 0.0416 | −3% |

Accurate before optimization, badly wrong after — that asymmetry is the signature of
fitting the quadrature grid rather than the wavefront.

Re-optimizing at 6×12 produces a genuinely better lens, measured on the *independent*
spot/MTF analysis:

| configuration | solve time | spot RMS | sag@40 | tan@40 |
| --- | --- | --- | --- | --- |
| 3×6, freqs 10/20/40 (current) | 33.5 s | 2.52, 5.73, 5.19, 5.11 | .908 .809 .792 .830 | .908 .751 .727 .567 |
| 6×12, freqs 10/20/40 | 140.0 s | 2.34, **3.76**, **4.26**, 5.19 | .915 .860 .819 .847 | .915 .822 .766 .539 |
| 6×12, freq 40 only | 55.6 s | 2.32, 3.94, 4.38, 5.14 | .918 .855 .815 .844 | .918 .821 .758 .552 |
| 8×16, freq 40 only | 93.8 s | 2.32, 3.95, 4.40, 5.19 | .918 .856 .813 .846 | .918 .822 .759 .539 |

Field-1 spot RMS improves 34%; sagittal MTF improves at every field. The single
regression is `tan@40` at field 3 (0.567 → 0.539) — the field crippled by issue 1.

8×16 reproduces 6×12, so **6×12 is converged**; there is no reason to go finer.

---

## 3. The residual is the un-centred second moment, not the variance

**Severity: medium.**

> **FIXED, opt-in.** `ContrastOptions.center_residuals(true)` /
> `OptimizationBuilder.centerContrastResiduals(true)` subtracts the reference wavelength's
> weighted mean per (frequency, field, orientation). Off by default because it changes
> every contrast residual. Measured on the Leica 75/2 at 40 cyc/mm: tangential
> sum-of-squares falls 1.046 → 0.511 at field 0.7 and 1.980 → 0.886 at full field, while
> sagittal is unchanged to five decimals. See
> [the sagittal/tangential balance](#why-this-may-bear-on-the-sagittal-drop) below.

The wording above is intentionally reference-relative. For the reference wavelength this
is exactly the centred variance. The same reference offset is applied to every other
wavelength, so the combined polychromatic block remains a second moment about the
reference image rather than a variance about a combined spectral mean. That is deliberate:
it matches Beam42's polychromatic spot convention and preserves lateral colour. Earlier
documentation described every wavelength block simply as a variance; `OPTIMIZER.md` now
records the narrower contract.

The modulus of the OTF is

```
|OTF| = |⟨exp(iΦ)⟩|  ≈  1 − Var(Φ)/2        where Φ = 2π·ΔW
```

i.e. it depends on the **variance** of the phase difference, not on `⟨Φ²⟩`. A constant
`ΔW` across the pupil is a pure image displacement (it moves the phase transfer
function, not the modulus) and costs no MTF at all. But
[ContrastAnalysisResult.java:20](../rayoptics/src/main/java/org/redukti/rayoptics/analysis/ContrastAnalysisResult.java:20)
forms `√w · ΔW` with no mean removal, so `Σr² = ⟨Φ²⟩ = Var + mean²`.

This is not negligible, and it concentrates:

- Overall the mean term is **5.2%** of the total contrast sum-of-squares.
- At field 1, tangential, 40 cyc/mm: mean = −0.220 waves against rms 0.337 — the mean
  accounts for **42%** of that block's residual.
- The mean scales exactly linearly with the shear (−0.0578 / −0.1145 / −0.2198 at
  10 / 20 / 40 cyc/mm), confirming it is pure wavefront tilt, i.e. image displacement.
- The sagittal mean is **identically** zero, not merely small. For a rotationally
  symmetric lens with the field in y the wavefront is even in x, so
  `ΔW = W(x+s,y) − W(x,y)` is odd about the shear centre and integrates to zero. Measured
  as 0.0% on every sagittal group of both test lenses. The whole effect therefore lands
  on the tangential residuals. (This was originally written "and this finding can never
  explain a sagittal symptom", which does not follow — see below.)
- On the Leica 75/2 the tangential mean share reaches **57%** (field 1.0, 50 cyc/mm),
  well above the Otus figures — so the size of the effect is strongly design-dependent.

**Suggested fix:** subtract the weighted mean per (frequency, field, wavelength,
orientation) group. The residual `√w_i·(ΔW_i − ΔW̄)` is still smooth and LM handles the
rank-1 deficiency per group without trouble.

If you want to retain a polychromatic lateral-color penalty, subtract the *reference
wavelength's* mean from every wavelength rather than each wavelength's own mean — that
keeps colour-dependent tilt (which does reduce polychromatic MTF) while discarding the
common tilt (which does not). This is what was implemented.

### Why this may bear on the sagittal drop

The bullet above concluded that a tangential-only term "can never explain a sagittal
symptom". That was wrong, and the error is worth keeping visible: the residual *values*
are unchanged for sagittal, but the *balance* between the two orientations is not, and
the balance is what the optimizer acts on.

Two mechanisms, the second being the one that matters:

- **Weighting.** With the mean² term left in, tangential sum-of-squares is inflated by up
  to 57% while sagittal's is exact. In a merit that sums both with equal weight that is an
  implicit tangential up-weighting.
- **A spurious gradient.** The mean² term is *reducible* — by adding wavefront tilt, which
  costs no MTF at all. So there is merit reduction available on the tangential side that
  corresponds to no optical improvement. Design freedom spent collecting it comes out of
  somewhere, and sagittal and tangential trade against each other through the astigmatic
  focus split: move toward the tangential focus and you move away from the sagittal one. A
  merit summing both is nearly indifferent to which is favoured, so a spurious term on one
  side decides it. Symptom: tangential up, sagittal down, merit improving, lens not.

Measured on the Leica starting design, the factor by which centring effectively raises
sagittal relative to tangential ([ContrastProbe18](../optimr2/src/test/java/org/redukti/examples/ContrastProbe18.java)):

| field | 10 cyc/mm | 20 cyc/mm | 40 cyc/mm |
| --- | --- | --- | --- |
| 0.00 | 1.00× | 1.00× | 1.00× |
| 0.50 | 1.06× | 1.10× | 1.21× |
| 0.70 | 1.66× | 1.80× | **2.05×** |
| 0.85 | 1.95× | 2.09× | **2.35×** |
| 1.00 | 2.04× | 2.12× | **2.26×** |

Three things line up. The bias is largest at the outer fields and at field 0.7, which is
where the drop lives. The hand-tuned `sagittalWeights = 2.0` in
`LeicaApo75mmMandler.createContrastSetup` is approximately what centring delivers from
first principles. And the Otus — which does not show the pathology — has a bias of 1.00×
at fields 0.7 through 1.0, its only significant value being 1.5× at field 0.3.

This establishes that the bias exists and is large where the symptom is. It does **not**
establish that the optimizer takes the bait; that needs an A/B solve with centring on.

---

## 4. The three frequencies are near-collinear — two thirds of the ray budget is wasted

**Severity: medium (performance).**

At 40 cyc/mm the shear is 0.067 pupil radii, i.e. **3.4% of the 1186 cyc/mm cutoff**. In
that limit `ΔW ≈ s·∂W/∂y`, so the residual is just a scaled OPD gradient and every
frequency produces the same direction:

```
cos(r10, r20) = 0.99974
cos(r20, r40) = 0.99932
cos(r10, r40) = 0.99823
```

The merit is therefore, to within 0.2%, `1.3125 × ‖r40‖²`. The 10 and 20 cyc/mm goals
add 2/3 of the ray-trace cost and essentially no new information.

Confirmed end to end (table in §2): dropping to a single 40 cyc/mm goal at 6×12 gives a
statistically indistinguishable lens in **55.6 s instead of 140.0 s**.

Worth noting as a property of the technique rather than a defect: at a few percent of
cutoff, contrast optimization degenerates into transverse-ray-aberration least squares.
The benefit realized here is dense, smooth, well-conditioned residuals — not
MTF-specific information. That is still the paper's main claim, but if you want the
genuinely MTF-specific behaviour you need a frequency where the shear is a large
fraction of the pupil (0.3–0.5 × cutoff).

**Recommended configuration:** 6×12 sampling, single frequency. Better than the current
baseline on 4/4 spot RMS and 7/8 MTF numbers, at 55.6 s vs 33.5 s.

---

## 5. High frequencies fail without a diagnostic

**Severity: low for this lens, high for LWIR / slow systems.**

> **PARTLY FIXED.** The rewritten `generate_contrast_quadrature` introduced a worse
> variant of this — a band where the overlap centre was still valid but the pattern
> collapsed onto it, so every triplet reported ΔW ≈ 0 and the merit read as *perfect*.
> A `MIN_CONTRAST_CONTRACTION` guard now throws there. The underlying ~0.707×cutoff
> ceiling is unchanged: the technique still cannot represent frequencies above it, it
> just says so now instead of failing silently or opaquely.

As reviewed, `trace_contrast` clamped the sampling radius:

```java
overlapDefinition.max_radius = Math.max(0.0, grid_rng.max_radius - enclosingRadius);
```

Once `enclosingRadius = 0.707·d` reaches 1, all quadrature points collapse onto the grid
centre `(−d/2, −d/2)` — which is itself **outside** the unit pupil. `trace_if_inside`
then returns null, every goal in that block returns `BIGVAL`, `buildJacobian` returns
false, and the solve aborts with no explanation.

That code is gone. `generate_contrast_quadrature`
([Trace.java:821](../rayoptics/src/main/java/org/redukti/rayoptics/raytr/Trace.java:821))
now maps the nominal pattern into the physical pupil and bisects for the largest
contraction at which every sample and both of its partners are valid, guarded by
`MIN_CONTRAST_CONTRACTION = 0.1`. Below that it throws, naming the frequency. The
failure mode described above is therefore reported rather than silent — but the ceiling
itself is unchanged, so the paragraphs below still describe what the technique can and
cannot represent.

The threshold is per-wavelength, so the longest wavelength fails first, at
`ν ≈ 0.707/(λ_max·F#)`:

- this lens: 751 cyc/mm — unreachable, so not an issue here;
- an f/8 LWIR lens at 10 µm: **8.8 cyc/mm** — entirely reachable.

Observed behaviour sweeping frequency on this lens, against the reviewed code (3×6
sampling; `maxSampleRadius` no longer exists):

```
freq=  40  shift=0.0674  maxSampleRadius=0.94334
freq= 400  shift=0.6743  maxSampleRadius=0.96462
freq= 800  shift=1.3486  maxSampleRadius=1.06512   <- outside the pupil
freq=1000  shift=1.6857  maxSampleRadius=1.33140   <- fully collapsed
```

`ContrastOptions` still validates only `frequency >= 0`. The check now happens late, in
`generate_contrast_quadrature`, and reports rather than degrades — but rejecting an
unrepresentable frequency where it is configured would still be better than discovering
it on the first trace.

~~**Related, lower priority:** the inscribed circle is a *valid* (conservative) subset of
the three-disk intersection~~ — **superseded.** The inscribed-circle construction was
replaced by the contraction search described above, which scales the whole pattern about
the overlap centre instead of inscribing a disk inside it. The observation that motivated
the note (the conservative radius costs little at 3% of cutoff) no longer applies to code
that exists.

---

## 6. The least-squares reduction only tracks MTF in the small-phase regime

~~**Severity: high. This is the only finding that produces a measurably worse lens.**~~
**Severity: medium.** The severity claim was withdrawn — see the
[postscript](#postscript-the-motivating-regression-was-defocus) at the end of this
section. The analysis below is unaffected and still correct; only its consequence
changed.

Found by running the generic contrast setup on
`Examples/jfotoptix/leica-r-apo-75mm-f2-mandler/specs.txt` (fields 0/0.3/0.7/1.0,
frequencies 10/30/50, `varyAllCurvatures`, 6×12). Mid-field sagittal MTF *drops*
across the optimization:

| field 0.7, 50 cyc/mm | before | after |
| --- | --- | --- |
| sagittal MTF | 0.0501 | **0.0410** |
| sagittal σ(ΔW) | 0.8726 | 0.3063 |
| sagittal mean share | 0.0% | 0.0% |

The residual improved 2.8× and the MTF got worse. This is **not** finding 3 — the mean
share is identically zero in the sagittal direction (see the symmetry argument there).

### The information is in the samples; the squaring discards it

Computing `|Σ w·e^{i2πΔW}| / Σw` — the actual OTF modulus — from the *same* rays,
against what the least-squares merit implies (`exp(−2π²σ²)`) and the geometric MTF:

```
AFTER          sigma   LSQ pred    phasor |  geo MTF
10  2 sag |   0.0741    0.8973    0.9000 |   0.8941   <- in regime, LSQ is right
50  2 sag |   0.3058    0.1578    0.0911 |   0.0410   <- LSQ 1.7x optimistic
50  3 sag |   0.2049    0.4367    0.6920 |   0.5498   <- LSQ 0.26 pessimistic
BEFORE
50  3 sag |   1.1505    0.0000    0.2061 |   0.1389   <- LSQ says zero; truth is 0.14
```

The phasor tracks the geometric MTF across all 24 field/frequency/direction
combinations, before and after. So the traced rays already encode the right answer —
`Σ w·ΔW²` throws it away.

### Why: σ does not determine MTF above ~0.1 waves

`|OTF| = |⟨e^{iΦ}⟩| ≈ 1 − Var(Φ)/2` requires Φ = 2π·ΔW ≪ 1 radian. The clean
demonstration, from the optimized design at 30 cyc/mm:

| | σ(ΔW) | actual MTF |
| --- | --- | --- |
| field 1.0 sagittal | 0.1882 | **0.764** |
| field 0.7 sagittal | 0.1926 | **0.451** |

Indistinguishable to the merit; 0.31 apart in reality. Above roughly 0.1 waves the
*shape* of the ΔW distribution matters, not just its spread — and the resulting error
is not even one-signed, being optimistic at one field and pessimistic at another.

### Why it produces a regression rather than just noise

> Read with the [postscript](#postscript-the-motivating-regression-was-defocus): the
> under-weighting described here is real and measured, but it is not what produced the
> regression. The second bullet below — no thicknesses among the variables — turned out
> to be the whole story, and not merely an amplifier.

At field 0.7 sagittal the merit sees σ = 0.306 against ~0.20 elsewhere — about **2.3×**
worse in sum-of-squares. In MTF terms it is 0.041 against 0.39–0.55, i.e. **10×** worse.
The squaring compresses a catastrophic failure into a mildly elevated residual, so the
optimizer never prioritizes it.

Two things amplify this:

- Residuals scale as ΔW ∝ ν, so the 50 cyc/mm block carries ~25× the sum-of-squares
  weight of the 10 cyc/mm block. The merit is dominated by exactly the frequency where
  the approximation is worst. At 10 cyc/mm every group is in regime and behaves.
- `GenericContrastOpt` offers only 14 variables (no thicknesses, no aspherics) against a
  badly-aberrated start, so the design never reaches the small-phase regime.

This is also why the Otus looks healthy: it converges to σ ≈ 0.06–0.10, comfortably in
regime. The Leica never gets field 0.7 sagittal below 0.31.

### Why not just add the existing `GeoMTF` goals?

The obvious cheap answer — no new code, reuse `GeoMTF` — was tested and does not work.
(`GeoMTF` throughout this section is the class `GoalGeoMTF`, reached through the builder
as `mtfGoals(mtf(...))`.)
Sweeping the weight on `mtf(10/30/50, target 100%)` added to the contrast merit:

| GeoMTF weight | njev | field 0.7 sag @50 | field 1.0 sag @50 | spot RMS |
| --- | --- | --- | --- | --- |
| none (contrast only) | 48 | 0.0410 | 0.5498 | 6.93 7.02 8.62 11.66 |
| 0.003 | 48 | 0.0219 | 0.4785 | 6.56 6.79 8.70 13.02 |
| 0.03 | 15 | 0.0734 | 0.5358 | 6.28 8.06 11.44 17.73 |
| 0.3 | 12 | 0.1095 | 0.5142 | 5.95 9.60 13.77 19.75 |
| 1.0 | 7 | 0.3482 | 0.2755 | 4.70 12.45 16.77 26.50 |

There is no sweet spot. Every weight that meaningfully repairs field 0.7 costs more
elsewhere than it gains — spot RMS degrades monotonically at fields 1–3 — and `njev`
collapses from 48 to 7 as the MTF goals gain influence. The solver is stalling, not
converging.

The cause is conditioning, and it is directly measurable. Sweeping one curvature across
the Jacobian step and watching successive increments of each candidate goal:

```
  delta |   GeoMTF    d(GeoMTF) |   phasor    d(phasor)
 -1.667 |  0.04537   +0.00512 |  0.03090   +0.00057
 -1.333 |  0.03706   -0.00832 |  0.03146   +0.00056
 -1.000 |  0.03988   +0.00282 |  0.03200   +0.00054
 -0.667 |  0.04585   +0.00597 |  0.03252   +0.00052
  0.000 |  0.05014   +0.00192 |  0.03352   +0.00049
  1.000 |  0.05418   +0.00191 |  0.03491   +0.00045
  2.000 |  0.06028   +0.00400 |  0.03619   +0.00041
```

`GeoMTF` jitters by ±0.008 — including a sign reversal — while the true change over a
third of a Jacobian step is ~0.0015. Signal-to-noise below 1: a central-difference
derivative of it is close to random. The phasor over the same sweep is monotone and
smooth to five decimals. This is the paper's own thesis reproduced numerically, and it
is the reason the phasor goal is worth writing rather than reusing `GeoMTF`.

### Suggested fix: a phasor-based MTF goal

Add a `GoalContrastMTF` **alongside** `GoalContrast`, not replacing it — the dense
least-squares residuals are what give the well-conditioned Jacobian that makes the
method fast; they just need a companion that sees what they cannot.

- One residual per (frequency, field, orientation) — 24 for `GenericContrastOpt`, against 5184
  contrast residuals. Negligible addition.
- Value `|Σ w·e^{i2πΔW}| / Σw` with all wavelengths pooled into one complex sum, which
  is the physically correct polychromatic OTF. Target and weight semantics identical to
  `GeoMTF`, so it can reuse the `MtfGoals` spec type.
- **Target 1.0.** `LMDerMeritFunction` forms `r = (value − target)·weight`, which is
  two-sided: any target below 1.0 penalises *exceeding* it as much as falling short. The
  Otus legacy targets (65/62/45/38 at 40 cyc/mm) are now beaten by 0.75–0.91, so wiring
  them in would drag the design back down. A target of 1.0 cannot be exceeded, so the
  two-sidedness becomes harmless and the residual is a clean "maximize" form whose
  gradient vanishes naturally as MTF → 1. Use a spec curve only with real requirements,
  and consider making the goal one-sided first — a fix that would benefit `GeoMTF` too.
- **Set the weight deliberately.** A handful of aggregate goals with residuals ~0.5 will
  swamp thousands of least-squares residuals at ~0.02: at the Leica optimum the contrast
  block carries SOS ≈ 1.74 against ≈ 6 for 24 unit-weight MTF goals. This bit the
  `GeoMTF` experiment above and applies equally here.
- **No extra ray tracing** — it reuses the triplets already traced. That makes it a cheap
  diffraction MTF operator: the spot/MTF path costs 64.4 ms per evaluation, this is
  effectively free.
- Compute the phasor once per group in `ContrastAnalysisResult`; do not recompute the
  trig inside every goal.
- Validation: the goal frequency must be one of the contrast frequencies, since the
  shear is per-frequency.

**Sampling constraint.** The phasor needs finer sampling than the LSQ residual at large
aberration. On the start design at 50 cyc/mm field 0.7 sagittal it reads 0.1379 at 6×12,
0.0335 at 12×24, and 0.0293 at both 24×48 and 40×80. So 6×12 is *not* adequate for this
goal; 12×24 is close and 24×48 converged. Either raise the sampling or only trust the
goal once the design is in regime.

### Outcome: implemented, evaluated, and reverted

`GoalContrastMTF` was written as described above and unit tested, then **backed out**
because it does not resolve the drop. The implementation is preserved in commit
`1438105c "add GoalContrastMTF"` if it is ever worth revisiting; everything below is the
record of why it was not kept. On the Leica it behaves essentially like `GeoMTF`:

| variant | njev | fld 0.7 sag@50 | fld 1.0 sag@50 | spot RMS |
| --- | --- | --- | --- | --- |
| contrast only | 48 | 0.0410 | 0.5498 | 6.93 7.02 8.62 11.66 |
| contrastMTF 0.03 | 25 | 0.0240 | 0.4507 | 6.66 6.81 9.37 14.96 |
| contrastMTF 0.3 | 24 | 0.0762 | 0.4579 | 5.79 11.10 13.66 19.74 |
| contrastMTF 1.0 | 4 | 0.3379 | 0.3047 | 3.77 17.26 16.59 26.65 |
| targeted at fld 0.7 sag only, 0.5 | 25 | 0.1561 | 0.5214 | 6.36 7.82 11.30 18.26 |

No weight repairs field 0.7 without paying for it elsewhere, and even a *single* active
MTF goal roughly halves `njev`. Aiming the goal only at the known blind spot helps
(0.041 → 0.156 at weight 0.5) but still costs field 0.7 tangential (0.517 → 0.313) and
outer-field spot RMS.

**The conditioning argument above was wrong, and is retained only as a record.** The
smoothness sweep measured `d(phasor)/d(one variable)` at a single point. That is
necessary but not sufficient, and two things escaped it:

- `|OTF|` is **non-differentiable where the OTF passes through zero** — `hypot(re, im)`
  has a V-shaped kink at the origin. Field 0.7 sagittal sits at ~0.03–0.04, right beside
  a zero; the sweep never crossed it, so it looked smooth. The kink is genuinely
  reachable on this design: the real part is negative at three groups on the start
  design (30 cyc/mm fld 1.0 tan, 50 fld 0.7 tan, 50 fld 1.0 tan), i.e. those have already
  passed through a contrast reversal.
- Local derivative quality says nothing about the landscape. A residual of 0.96 against
  a target of 1.0, with a small local gradient, invites exactly the large destructive
  steps observed.

**Reframing.** Both `GeoMTF` and the phasor *can* reach ~0.34 at field 0.7 — at the cost
of fields 1 and 3. Summed sagittal MTF at 50 cyc/mm is actually higher for those runs
(1.78 vs 1.40). So this is substantially a genuine Pareto trade in the design space,
with the equal-weight contrast merit choosing one balance, rather than purely an
optimizer failure. `GenericContrastOpt` offers only 14 curvature variables, which narrows the
frontier further.

What survives unchanged is the diagnosis, which rests on the σ/phasor/geometric table
and the non-monotonicity demonstration, not on the remedy: **the least-squares residual
stops tracking MTF above ~0.1 waves, and under-weights a catastrophic field by ~4×.**
What has *not* been found is a merit formulation that fixes it without collateral
damage.

### If this is retried: optimize the real part, not the modulus or its square

`|OTF|²` was suggested here earlier as a way to remove the kink. That was a mistake, and
the measured OTF components show why. Since the residual gradient scales as
`d(|OTF|²)/d|OTF| = 2·|OTF|`, squaring *suppresses* sensitivity exactly where the MTF is
near zero — the case that matters. On the Leica start design that factor is 0.067 at
50 cyc/mm field 0.7 sagittal against 1.38 at 10 cyc/mm on axis, so squaring would
de-emphasise the broken group by roughly twentyfold relative to the healthy ones. It
removes the kink by flattening the thing you are trying to fix.

The better route is the **real part of the OTF**, target 1.0. For the sagittal direction
this is exact rather than an approximation: `dW` is odd about the shear centre (the same
symmetry as in finding 3), so the sine terms cancel and the OTF is purely real —
measured `|im| < 0.0013` at every sagittal group and every frequency, against a real
part of 0.03–0.69. So `|OTF|_sag = |re|`, and `re` is:

- analytic in the OPDs, hence smooth through zero — no kink;
- undiminished in sensitivity near zero, unlike the square;
- correctly ordered through a contrast reversal, since negative `re` reads as worse than
  zero contrast rather than folding back up as the modulus does.

The caveat is the tangential direction, where `im` is substantial (0.16–0.48, being the
coma-induced image displacement). There `re` alone under-reports the modulus, and
driving `re` to 1 also drives `im` to 0 — reintroducing a penalty on image displacement
that finding 3 establishes is MTF-irrelevant. So the clean form is `re` sagittal and
something else tangential, which is untidy but at least well founded.

This is reasoning plus one measurement, not a validated result. The last confident
mechanism claim in this section was wrong; treat it accordingly and measure before
believing.

The reverted implementation was itself sound — a correct, cheap polychromatic
diffraction-MTF evaluator costing no extra ray tracing, which would be useful for
reporting or for goals on designs already in regime. It was removed because carrying an
API that does not solve the problem it was added for is worse than not having it, not
because it was wrong. Restore it from `1438105c` if a use appears.

### Postscript: the motivating regression was defocus

The Leica field-0.7 sagittal drop that prompted this whole section has a much more
ordinary explanation, found later while investigating the same case: the optimized design
sits about **50 µm out of focus** at that field. Shifting focus recovers sagittal MTF at
50 cyc/mm from 0.041 to **0.629** — better than the starting design, not worse.

The cause is the configuration, not the merit. The generic contrast setup varied
curvatures only; with no thickness among the variables, the solver had no way to move the
image plane, so it traded field 0.7 away to satisfy the rest. The contrast merit was
reporting a genuine improvement in what it was asked to improve. Once thicknesses are
varied — which now requires the design-preservation constraints of the
[changes section](#changes-since-this-review-was-written), or the layout collapses — the
case does not reproduce.

Three consequences for the rest of this section:

- **The severity claim is withdrawn.** No case is currently known where the contrast
  merit walks a design backwards. That was the basis for calling this the most
  consequential finding, and it does not hold.
- **The diagnosis is untouched.** The σ/phasor/geometric table, the two groups with equal
  σ and MTF 0.31 apart, and the ~0.1-wave validity limit were all measured directly and
  do not depend on the Leica regression. The merit genuinely stops tracking MTF above
  that limit; what is missing is a demonstration that it costs anything in practice.
- **The `GeoMTF` and `GoalContrastMTF` weight sweeps above were solving the wrong
  problem.** Both were being asked to repair a focus error with curvature variables.
  That explains why every weight that helped field 0.7 cost more elsewhere: the frontier
  they were exploring was the wrong one. Neither experiment says much about whether a
  phasor goal is useful, and a retry — if there is ever a reason for one — should start
  from a configuration that can focus.

Lesson recorded because it cost real effort: a merit that improves while an independent
metric degrades is evidence of *something*, but "the merit is a bad proxy" is only one
candidate, and it was the one this document reached for first. "The variable set cannot
express the fix" is more common and much cheaper to check.

The measurements in this postscript were made during the investigation and the probe was
not retained, so unlike the rest of this document they are not reproducible from a
committed program.

### Second postscript: defocus does not cover the current configuration

The postscript above was written against `GenericContrastOpt`, which varied curvatures
only. `LeicaApo75mmMandler.createContrastSetup` now varies all thicknesses under
design-preservation constraints, so the solver *can* move the image plane and a residual
focus error can no longer be the whole story. Sagittal softness has still been seen.

So the account stands for the run it was measured on and does not generalise. What
replaces it for current runs is not settled; [finding 8](#8-vignetting-is-a-moving-mode-dependent-reference-frame)
covers what has been measured since, and the [open questions](#open-questions) list what
has not.

---

## 7. The shear is applied at the entrance pupil, not the exit pupil

**Severity: moderate. Same class as finding 1, an order of magnitude smaller.**

> **MITIGATED, opt-in.** `calibrate_frequency` corrects the field-dependent part; see
> "The 90% correction" below. The exact treatment is with the upstream ray-optics
> project.

The OTF is the autocorrelation of the **exit** pupil function, and the paper computes the
shear there. `trace_contrast` works in `PupilType.REL_PUPIL`, and
`OpticalSpecs.ray_start_from_osp` builds those rays from `fod.enp_radius` and
`fod.enp_dist` — **entrance** pupil coordinates. So the displacement is derived from an
exit-pupil relation (`2·λ·F#·ν`, working F/#) but applied as a rigid translation in the
entrance pupil. Those agree only where the pupil imaging is aberration free; real pupil
aberration makes the entrance→exit mapping nonlinear.

### Measuring it without locating the exit pupil

Two rays produce image-plane fringes of frequency ν exactly when their image-space
direction cosines differ by `λ·ν`. That holds wherever the exit pupil lies, so it tests
the shear directly. Ratio of realised to requested frequency, Leica 75/2 at 50 cyc/mm
(min / mean / max across the pupil):

```
fld |   sag: min/mean/max      |   tan: min/mean/max
  0 | 0.9740  0.9956  1.0014   | 0.9740  0.9956  1.0014
  1 | 0.9699  0.9924  0.9975   | 0.9593  0.9886  0.9946
  2 | 0.9470  0.9746  0.9811   | 0.9414  0.9547  0.9589
  3 | 0.9315  0.9528  0.9589   | 0.9109  0.9157  0.9166
```

Two distinct errors:

- a **field-dependent bias** — the mean falls from 0.996 on axis to **0.916** at full
  field tangential, so that group is optimized at ~45.8 cyc/mm rather than 50;
- a **within-pupil variation** — even on axis the ratio spans 0.974 to 1.001, so the
  shear is not a rigid translation in the exit pupil and each sample is smeared slightly
  across frequency rather than sitting at one.

The ratio is nearly independent of ν (0.9936 / 0.9946 / 0.9956 on axis at 10 / 30 / 50),
as expected for a property of the pupil mapping rather than of the frequency.

### The 90% correction

Getting this exactly right needs each partner ray aimed iteratively until its direction
cosine differs from the base ray's by `λ·ν` — several extra traces per sample. The
field-dependent bias is the dominant term and is much cheaper to remove: probe it.

`ContrastOptions.calibrate_frequency(true)` traces one probe pair per field, wavelength
and direction at ±shift/2, measures the realised direction-cosine difference, and scales
the entrance-pupil shift by `λν / realised`. Four extra rays per field and wavelength
against roughly two hundred for the samples.

```
fld dir |  scale | ratio before | ratio after
  3 tan | 1.0914 |       0.9155 |      0.9992
  3 sag | 1.0441 |       0.9501 |      0.9920
  2 tan | 1.0462 |       0.9542 |      0.9983
  1 tan | 1.0085 |       0.9874 |      0.9958
  0 sag | 1.0000 |       0.9942 |      0.9942
```

Worst case 8.5% low becomes 0.08% low; the residual ≤0.8% is the within-pupil spread,
which is what the remaining 10% would buy.

The on-axis row shows the method's limit: the probe straddles the pupil centre where the
mapping is locally linear, so it returns a scale of 1.0000 and leaves the 0.6%
pupil-averaged offset in place. The correction targets the *field* bias, which it
captures well because that bias is nearly uniform across the pupil — the full-field
tangential spread is only 0.911–0.917.

Verified smooth: sweeping a curvature across the Jacobian step with calibration on gives
monotone, noise-free increments (+0.003045 rising to +0.003186), the same character as
with it off. It does not degrade the finite-difference Jacobian.

**Off by default**, because it changes every contrast residual and therefore every
committed golden value. Enable with `OptimizationBuilder.calibrateContrastFrequency(true)`,
`Analysis.calibrating_contrast_frequency(true)`, or `ContrastOptions.calibrate_frequency(true)`.

### Effect on the Otus

The Otus is worse affected than the Leica, as expected at f/1.4 — corrections needed at
40 cyc/mm:

| field | sagittal | tangential |
| --- | --- | --- |
| 0.0 | 1.0000 | 1.0000 |
| 0.3 | 1.0080 | 1.0184 |
| 0.7 | 1.0464 | 1.0998 |
| 1.0 | 1.0973 | **1.2040** |

So full-field tangential was being optimized at ~33 cyc/mm rather than 40 — a 17%
shortfall against the Leica's 8.5%. Re-optimizing with the correction on:

| | calibrate off (committed) | calibrate on |
| --- | --- | --- |
| spot RMS | 2.398 3.487 3.917 4.181 | 2.499 3.477 3.914 **4.042** |
| sag@40 | .9106 .8692 .7990 .8086 | .9040 .8667 .7904 .7960 |
| tan@40 | .9106 .8221 .8011 .7542 | .9040 .8242 .8128 **.8013** |

The correction gives the off-axis tangential groups the frequency they were asked for,
and the optimizer responds where it had been under-corrected: tangential MTF at 40 cyc/mm
rises at three of four fields, by 0.047 at full field, and full-field spot RMS improves.
It pays for that with a little axial performance and a uniform ~0.01 of sagittal MTF.

The better summary is the weakest link and the spread across the field:

| | worst of the eight MTF@40 numbers | max − min |
| --- | --- | --- |
| calibrate off | 0.7542 | 0.156 |
| calibrate on | **0.7904** | **0.114** |

So correcting the frequency produces a more uniform lens with a better worst case, which
is what one would expect from removing a field-dependent bias that had been quietly
under-weighting the outer field. Note the two `finalRms` values (0.0064364267 against
0.0069304768) are **not comparable** — they are different merit functions, since the
sampled frequencies differ.

### Kept clear of the upstream port

The ray tracing is a port of the upstream ray-optics project and needs to stay close to
it so bug fixes remain portable. The whole correction therefore lives in Beam43-only
files — `ContrastAnalysis`, `ContrastOptions`, `Analysis`, `OptimizationBuilder` — and
calls `Trace.setup_pupil_coords` and `Trace.trace_safe` purely as a consumer. Nothing
under `raytr/`, `specs/` or `seq/` was touched.

(Note for the same reason: the `Field.apply_vignetting` refactor introduced under
"Smaller items" *is* a deviation from upstream, albeit a behaviour-preserving one. It
could be reverted to reduce porting friction.)

---

## 8. Vignetting is a moving, mode-dependent reference frame

**Severity: moderate, and partly unresolved.** Added 2026-08-15, after §6's first
postscript failed to explain current runs.

Findings 1 and 7 both concerned getting the *shear* right inside the pupil. This one is
about the pupil itself: which region of it the merit looks at, how that region is
established, and the fact that it moves while the solver works.

### Paraxial vignetting leaves the sagittal pupil unvignetted

`Trace.apply_paraxial_vignetting` sets only `vuy` and `vly`. It cannot do otherwise - a
paraxial ray is meridional, so it has nothing to say about x - and
`Field.vignetting_scale` maps a factor of exactly `0.0` to a scale of `1.0`. Under
`VigType.Paraxial` the pupil is therefore full width in x at *every* field:

```
Leica 75/2      x half-width          y half-width
field    Paraxial   SetVig      Paraxial   SetVig
0.0      1.0000     1.0029      0.9432     1.0029
0.7      1.0000     0.9717      0.5980     0.6894
1.0      1.0000     0.8747      0.3782     0.4656
```

The consequence is a hard invariant violation. On axis, sagittal and tangential MTF must
be equal by rotational symmetry. Under the real-ray modes they are, to every printed
digit. Under `Paraxial` they are not:

| lens | mode | on-axis sag@40 | on-axis tan@40 | gap |
| --- | --- | --- | --- | --- |
| Leica 75/2 | Paraxial | 0.4610 | 0.6075 | **0.148** |
| Leica 75/2 | SetVig | 0.4094 | 0.4094 | 0.000 |
| Otus 50/1.4 | Paraxial | 0.5401 | 0.5500 | 0.010 |
| Otus 50/1.4 | SetVig | 0.5401 | 0.5401 | 0.000 |

The tangential direction gets an artificial advantage: its narrowed pupil clips aberrated
rays, so it reads high, while sagittal carries the full width. The size of the violation
tracks the on-axis paraxial `vuy` - 0.057 on the Leica against 0.010 on the Otus - which
is the order of difference between the lens that shows sagittal pathology and the ones
that do not.

**This never affected committed results.** `Analysis` has always built with
`VigType.SetPupil`; `Paraxial` appeared only in a deliberate experiment. `SetVig` and
`SetPupil` agree closely - working F/# 2.0403 against 2.0500, x half-widths within 0.005,
MTF identical to 3-4 decimals - so the committed default matches what LensTool2 reports.

### Optimizing under Paraxial is a margin in x and a deficit in y

Because x is never vignetted, the sagittal pupil under `Paraxial` is a **superset** of the
real one and the tangential pupil is a **subset** (0.3782 against 0.4656 at full field).
A design optimized on a superset and then measured on the real sub-aperture can only come
out the same or better in that direction - which is a principled reason, not luck, why a
`Paraxial`-optimized Leica evaluates well in LensTool2's sagittal MTF.

The exposure is the other axis: roughly 19% of the tangential aperture at full field, the
outermost and most aberrated zone, was never in the merit. Predicted, not yet checked.

### The pupil moves during the solve

Apertures are never variables, but vignetting is not therefore constant: it is where rays
land on fixed apertures. Probing every variable at its exact Jacobian step:

| mode | variables moving a factor | mean max change | worst |
| --- | --- | --- | --- |
| Paraxial | 28 of 29 | 3.8e-04 | 1.7e-03 |
| SetVig | 28 of 29 | 4.5e-04 | 2.5e-03 |
| SetPupil | 24 of 29 | 1.9e-04 | 1.4e-03 |

The drift is **smooth** - sweeping surface 0's radius across ±2 Jacobian steps gives
monotone increments with no steps or sign flips, and `SetVig` is linear to three digits -
so it does not corrupt the finite difference. An earlier guess in the other direction was
wrong.

What it does mean is that the solver differentiates the design and the pupil together. A
more heavily vignetted lens has less aberration and better MTF, so **shrinking the pupil
is a way to improve the merit that costs nothing in the merit and real light in the
lens.** Whether the solver actually takes that route is the main untested question here.

Two smaller observations from the same sweep: `SetVig` and `SetPupil` move `vuy` in
*opposite* directions for the same design change, so they are not interchangeable under
differentiation even though they agree statically; and `set_pupil` can throw from
`Wideangle.find_edge` on a perturbed trial geometry. The latter is caught by
`computeResiduals`, which fills `BIGVAL` and rejects the step, so it costs an iteration
rather than the solve.

### What was added

`Analysis` now takes the mode and can hold the factors fixed:

```java
.vignetting(VigType.SetPupil)   // default, unchanged behaviour
.freezeVignetting()
```

Freezing captures the four factors once from a reference build, then builds every later
model with `VigType.None` and stamps them on. It also holds the captured pupil value,
since factors measured at one working f/# do not describe another - which pins `fod.fno`
and makes a `GoalParax` on `Fno` inert in combination with `SetPupil`. The cost is
staleness; `discard_frozen_vignetting()` re-measures between restarts.

This is closer to how Zemax behaves than live re-measurement is: there, vignetting factors
are static field data that optimization does not update, while real ray blocking at
apertures stays dynamic and ray aiming re-solves the pupil mapping on every trace. The
Beam43 analogue of that arrangement would be frozen factors with `check_apertures` on,
which the contrast path deliberately does not do, for smoothness.

Worth noting that Beam43 currently holds both conventions at once: `ZemaxExporter` writes
`VDXN`/`VDYN`/`VCXN`/`VCYN`/`VANN` as all zeros, so exported models rely purely on real
apertures, while `trace_contrast` sets `check_apertures = false` and relies purely on the
vignetting map. Both are defensible; a design compared across the two is not being
measured the same way.

### Status: the freeze experiment is inconclusive

The first attempt on the Leica produced overlapping first and second surfaces - a layout
failure, orthogonal to vignetting, so the run says nothing either way. The likely cause is
that the same setup moved from 4 fields to 11, which grew the contrast block from 5184
residuals to 14256 while the constraint count stayed at ~29; see items 2 and 3 under
[open questions](#open-questions).

---

## 9. Frequency normalization uses ray direction, not Hopkins' exit-pupil shear

The frequency-measurement and normalization options added after finding 7 exposed useful
diagnostics, but the quantity named `realized_frequency` is not the spatial frequency
coordinate in Hopkins' OTF definition. The normalization option was subsequently removed;
the diagnostic measurement remains.

[Hopkins](https://doi.org/10.1080/713820605) writes the two-dimensional OTF as the
autocorrelation of the pupil function (10.22 and 10.26):

```
D(s,t) = (1/A) ∫∫ f(x+s/2,y+t/2) f*(x-s/2,y-t/2) dx dy
```

The independent variables `(s,t)` are separations in *reduced exit-pupil coordinates*.
The paper is explicit in (10.9), (10.28), and the discussion following (10.30) that these
coordinates are measured on the exit-pupil reference sphere relative to the principal
ray. Identifying them with a convenient pupil plane is not generally valid.

`PupilShear.exit_pupil_coord` is therefore the right foundation: it reconstructs the
equally-inclined-chord point on the reference sphere for each traced ray. However,
`PupilShear.realized_frequency` does not use the separation of those coordinates. It uses
the difference between the two rays' image-space direction cosines:

```java
double component = axis == Orientation.X ? delta.x : delta.y;
return n_img * Math.abs(component) / wavelength;
```

For an aberration-free paraxial system, direction difference is proportional to pupil
separation, so this produces a plausible answer. In an aberrated system the direction
difference also contains wavefront slope -- equivalently transverse ray aberration. The
reported independent frequency therefore changes when the aberration being optimized
changes. That is precisely the coupling the exit-pupil autocorrelation avoids.

This also narrows finding 7's conclusion. The reported 8.5% to 0.08% improvement showed
that `calibrate_frequency` made the entrance-pupil probe reproduce the requested
*direction-cosine metric*. It did not establish that the pair had the requested separation
in Hopkins' reduced exit-pupil coordinates. Both the field-level
`exit_pupil_frequency_calibration` and the per-sample measurement currently use the same
metric, so their agreement is internally consistent but is not an independent validation
of the OTF frequency.

### Scaling the OPD is only a first-order approximation

The reverted `normalize_frequency(true)` option changed each sampled wavefront difference by

```
dW <- dW * (frequency_requested / frequency_realized)
```

Even after replacing the frequency measurement with the correct exit-pupil separation,
this is exact only in the infinitesimal-shear limit:

```
W(p + ds) - W(p) ≈ ds . grad(W)
```

For the finite shears used at useful MTF frequencies, higher-order wavefront terms mean
that multiplying an already traced difference does not reconstruct the difference at a
different shear. A real pupil mapping can also rotate or skew a nominal x or y entrance-
pupil displacement. Retaining only the requested-axis component loses that cross-axis
part; the actual OTF sample is then at a two-dimensional frequency `(s,t)`, not exactly
at `(s,0)` or `(0,t)`.

### Recommended next steps

1. Keep the measurement facility, but derive the realized reduced shear from
   `shiftedExitPupilCoord - referenceExitPupilCoord`, normalized with Hopkins' exit-pupil
   scale. Retain both vector components.
2. Rename the current direction-cosine result to something explicit such as
   `imageRayFrequency` if it remains useful diagnostically; it should not be presented as
   the OTF frequency.
3. Initially make the exit-pupil result diagnostic only and add a regression test against
   the ideal paraxial case plus a pupil-aberrated off-axis case. The existing tests prove
   that the programmed ratio is applied, not that the ratio represents Hopkins' shear.
4. For an exact correction, solve or aim each displaced entrance-pupil ray until its
   exit-pupil reference-sphere coordinate has the requested vector separation. Then use
   the OPD of that ray directly rather than rescaling a finite OPD difference afterwards.

### Implemented opt-in correction

Step 4 is now implemented behind `OptimizationBuilder.aimContrastAtExitPupil()`. Each
partner ray is inverse-aimed in both entrance-pupil coordinates to the requested vector
separation on the exit-pupil reference sphere, and its traced OPD is used directly.
Regression coverage checks the achieved sagittal and tangential sphere-coordinate
separations, including the nominally zero cross-axis component. The option is mutually
exclusive with `calibrateContrastFrequency(true)`.

This follows the independent variables in Hopkins (10.22) and (10.26): `(s,t)` is the
separation of two pupil-function samples in the overlap of displaced pupils. The text
after (10.28) identifies `(x',y')` as differences of exit-pupil coordinates calculated
by ray tracing, and the conclusion after (10.30) explicitly identifies the pupil surface
with the reference sphere rather than the pupil plane. Beam43 therefore constructs the
target from traced sphere coordinates rather than from image-ray direction:

```text
delta = 2 lambda F# frequency
target = X(reference) + R_exit delta_vector
```

For the partner's normalized entrance-pupil coordinate `u`, the solver finds

```text
G(u) = X(u) - target = 0
```

using a full 2-by-2 finite-difference Newton Jacobian and backtracking. The initial guess
is the old rigid entrance-pupil displacement. Solving both components corrects rotation
and cross-axis shear as well as the requested-axis scale. On convergence the residual is
formed from the newly traced finite OPD difference; it is not obtained by multiplying an
OPD measured at the wrong shear. The solver targets `2e-7` of the paraxial exit-pupil
radius; a stalled result within `1e-6` is accepted, while larger misses retain the ordinary
contrast-ray failure context. The acceptance floor was added after the dense 6-by-12
Nikkor 18 mm pattern produced 117 false failures: the first reported non-convergence was
only `2.02e-6 mm` from its target, but numerical roundoff prevented another decreasing
Newton step.

Two deliberately wide-angle regressions now validate the construction at 40 cycles/mm:

| Model | Field | Rigid error | Calibrated error | Aimed error |
| --- | ---: | ---: | ---: | ---: |
| Nikkor Z 14-30 mm | 57.68 degrees | 0.69209 mm | - | 0.00000119 mm |
| US 3,549,241 Example 5 | 45 degrees | 0.63498 mm | 0.09335 mm | 0.00000129 mm |

The US patent case is also a direct comparison with the older calibration. Its sagittal
and tangential scale factors are approximately 1.358 and 1.969. Calibration removes
about 85 percent of the worst vector error but leaves a 0.093 mm across-pupil residual;
direct aiming is about 72,000 times more accurate. This is the expected distinction
between one centre-derived scalar per direction and solving the nonlinear vector map at
every quadrature sample.

The remaining qualification is quadrature: reference nodes and weights are still formed
in the entrance pupil, as is the common-overlap contraction. A complete Hopkins pupil
integral would map the integration measure and overlap into reduced exit-pupil
coordinates as well. The new option fixes the ray-pair frequency coordinate, not that
larger integration problem, so it remains opt-in pending comparisons on real designs.

`measure_frequency` remains useful as a diagnostic of the current ray-direction metric.
The normalization API and residual rescaling were removed rather than left available in
an unsafe configuration. `exit_pupil_sphere_coord` is now the coordinate foundation for
the implemented aiming path. `calibrate_frequency` remains a separate, opt-in block-level
approximation and carries the qualification above.

## Smaller items

- **`dLineOnly` is ignored for contrast goals.** *Still open.*
  [OptimizationBuilder.java:544](../optimr2/src/main/java/org/redukti/optim/OptimizationBuilder.java:544)
  loops all `prescription._wvls` unconditionally, while the ray-aberration loop honours
  the flag. Consistent in the `ZeissOtusML50mm` path only because the prescription itself
  is restricted upstream.

- ~~**There is no way to build a contrast-only merit.**~~ **Fixed.** Ray-aberration fans
  are opt-in now: `buildGoals` adds them only for `rayAberrationGoals()`, and a merit
  with fewer goals than variables is rejected with a message naming that method rather
  than being silently padded. `_compute_ray_aberrations` follows the goals.

  The frozen test fixture enables them deliberately, to preserve the golden values they
  were generated under. Its residual breakdown at the current 6×12 default is 5184
  `GoalContrast` + 240 `GoalRayAberration` + 2 `GoalParax` = 5426. (The 1296/1538 counts
  recorded here originally were for 3×6.)

- **`contrastSamples = rings × spokes` is an unchecked contract** between
  `OptimizationBuilder` and `generate_gaussian_quadrature`. It holds today, but
  `ContrastOptions.num_spokes(null)` is a legal call that yields `4·(rings+1)` spokes,
  and any mismatch surfaces as `BIGVAL` and a dead solve rather than an error. Consider
  deriving goal count from the first analysis result, or asserting the sizes match.

- ~~**6 spokes samples the x axis but not the y axis**~~ (θ = 60°, 120°, … 360°). On axis,
  where sagittal and tangential must be identical by symmetry, they differ in the 4th
  digit (0.09463 vs 0.09467 at 40 cyc/mm). A spoke count divisible by 4 makes the two
  directions sample-equivalent. **Fixed incidentally by the 6×12 default in finding 2.**

- **`ContrastAnalysis.opd(...)` is a no-op wrapper.** `WavefrontAberrationAnalysis.opd`
  ignores its `p` and `xy` arguments entirely, so the careful
  `rays.sagittal().input_pupil` vs `rays.pupil()` distinction has no effect. Either use
  it or delete the wrapper.

- **`WavefrontAberrationAnalysis.opd` returns `Double` and is auto-unboxed** into
  `double reference` in `ContrastAnalysis.sample`. Guarded today only because the
  default `rayerr_filter = null` makes `trace_safe` return a null pkg first — one
  `TraceOptions` change away from an NPE.

- ~~**`GoalContrast.value()` compares a `double` frequency to an `int`.**~~ **Fixed.**
  The goal now addresses its block by `_contrast_index` and does no frequency comparison;
  `_frequency` is carried for reporting only. `ContrastOptions` still accepts arbitrary
  frequencies while only integers are addressable from the optimizer path, but nothing
  depends on the comparison any more.

- **`traced.get(0)`** in `ContrastAnalysis.eval` assumes exactly one wavelength came
  back. True because `wavelengthIndex` is non-null, but brittle.

- **`SequentialModel.trace_contrast` leaves `field.chief_ray` / `field.ref_sphere`**
  pointing at the last wavelength's values. Contrast runs last in `Analysis.compute()`,
  so anything reading that shared state afterwards sees a contrast leftover.

- **Reference-sphere convention differs between analyses.** `trace_contrast` re-aims the
  chief ray per wavelength while pinning the image point to the central wavelength;
  `trace_grid` / `trace_rings` set the reference sphere once at the central wavelength
  and never re-set it. The surrogate and the metric used to validate it are therefore
  not referenced identically.

- ~~**`LMDerSolver.solve()` does not recompute the analysis at the final `x`.**~~
  **Fixed.** `solve()` now writes the accepted `x` back to the prescription and calls
  `analysis.compute()` before returning, so the goals and the prescription always
  describe the same design — including for `info < 0`.

- **Redundant setup work.** `ContrastAnalysis.eval` is called once per frequency and each
  call re-runs `setup_pupil_coords` per (field, wavelength), including the
  reference-wavelength setup that depends only on the field — 9× redundant per field
  across three frequencies. Minor next to the ray tracing, but free to fix by looping
  frequencies inside.

---

## On the test

- `assertTrue(finalRms < initialRms)` is close to vacuous for a converged LM run; the
  absolute assertions are doing the real work.
- Validating the surrogate against an **independent** spot/MTF analysis is exactly the
  right approach, and keeping the second hexapolar spot regression for LensTool2
  comparison is a good idea.
- The 1e-6 locks on a 44-variable nonlinear solve are brittle across JDK, FPU and
  library changes. Worth a comment saying they are regression locks rather than physics.
- The `finalRms` values in the two tests are **not comparable** — `getRMS()` divides by
  `functions.length` and the residual counts differ (5426 against 266). Worth a comment
  so nobody reads it as a quality comparison.
- **Since fixed: the test owns its configuration.** It used to build its setup from
  `ZeissOtusML50mm`, which is an example class and therefore a place to try things —
  frequencies, sampling, calibration. Every one of those changes the merit function and
  so every asserted number, and an experiment did in fact surface as a test failure.
  `frozenContrastSetup` and `frozenDirectSetup` now declare the configuration inside the
  test, with a comment saying to regenerate the values deliberately when changing it.

---

## Work completed

Everything still open is consolidated under [Open questions](#open-questions).

Done:

1. ~~**Fix the vignetting/shear coupling** (§1)~~ — it was why field 3 tangential was stuck.
2. ~~**Raise the sampling default to ~6×12**~~ (§2).
3. ~~**Stop the degenerate high-frequency case failing silently**~~ (§5).
3a. ~~**Correct the entrance-pupil shear bias** (§7)~~ — available, opt-in, pending the
    exact exit-pupil treatment from upstream.

## Open questions

Everything unresolved, in rough priority order. Split into questions that need a
measurement and defects that need a fix, because they want different kinds of effort.

### Blocking the current investigation

1. **Get a conclusive freeze run on the Leica.** The attempt collapsed the layout - first
   and second surfaces overlapping - so it says nothing about vignetting yet. Item 2
   probably has to be fixed first.
2. **Constraint strength does not scale with merit size.** `NOMINAL_CONSTRAINT_WEIGHT`
   holds one residual per varied parameter (~29 on the Leica) against an optical merit
   that grows with field count. Moving `createContrastSetup` from 4 fields to 11 took the
   contrast block from 5184 residuals to 14256 - 2.75x - while the constraint count did
   not move, so weight 1.0 no longer holds the line it was tuned to hold. Either scale the
   nominal weight by residual count, or have the builder warn when the constraint block
   falls below some fraction of the optical merit. Immediate workaround is an explicit
   weight, e.g. `applyThicknessConstraints(3.0)`.
3. **No edge-thickness constraint.** `ConstraintThickness` holds *axial centre* thickness.
   Two surfaces can keep their axial gap and still cross away from the axis when curvature
   moves, since the edge gap is `t + sag2(h) - sag1(h)` and nothing looks at it. This is
   the likely mechanism behind the overlap in item 1. `SurfaceProfile.sag(x, y)` already
   exists on `Spherical`, `EvenPolynomial` and `RadialPolynomial`, and the interfaces are
   reachable from `Analysis._opt_model.seq_model.ifcs`, so the residual is cheap to form
   in optimcommon without touching ported code.

### Measurements not yet made

4. **Does the solver buy MTF by vignetting harder?** §8 establishes that it *could* -
   the pupil is differentiated along with the design, and a smaller pupil means less
   aberration. Untested. The check is to record `vux`/`vuy` at the start and end of a live
   solve and see whether vignetting grew; freezing should then change the outcome.
5. **Is the `Paraxial` tangential deficit real?** §8 predicts that optimizing under
   `Paraxial` leaves roughly 19% of the full-field tangential aperture outside the merit.
   Check outer-field tangential MTF in a LensTool2 report on a `Paraxial`-optimized
   prescription.
6. **Does the `Paraxial` sagittal advantage generalise?** The superset-pupil argument is
   sound in principle but has been seen on one lens. If it holds elsewhere, deliberately
   inflating the sampled aperture on both axes would be the honest version - a margin
   rather than an accident of which factors the paraxial routine happens to set.
7. **Re-measure §4's frequency collinearity on a badly-aberrated design.** It was
   established on the Otus, where phases are small. On the Leica the three frequencies may
   carry genuinely different information, so re-measure before dropping any.

### Defects and gaps

8. ~~**Subtract the weighted mean** (§3)~~ - **implemented, opt-in.** What remains is the
   A/B: solve both lenses with `centerContrastResiduals(true)` and compare against the
   independent spot/MTF analysis. It removes 49% of the tangential block at Leica field
   0.7 and effectively doubles sagittal's relative weight there, so it is a candidate
   explanation for the sagittal drop - see
   [the balance argument](#why-this-may-bear-on-the-sagittal-drop). Flip the default only
   once that A/B is done, since it regenerates every golden value.
9. **Validate frequency against the cutoff** (§5) at configuration time rather than on the
   first trace.
10. **`dLineOnly` is ignored for contrast goals**, or document that it deliberately does
    not apply to them.
11. **Two vignetting conventions coexist.** `ZemaxExporter` writes all-zero vignetting
    factors and relies on real apertures; `trace_contrast` sets `check_apertures = false`
    and relies entirely on the vignetting map. A design compared across the two is not
    measured the same way.
12. **`SetVig` and `SetPupil` differentiate differently**, moving `vuy` in opposite
    directions for the same design change despite agreeing statically. Consequences
    unknown; noted so it is not rediscovered.

### Latent

13. **§6, if a case for it appears.** The least-squares residual stops tracking MTF above
    ~0.1 waves; that is measured and not in doubt. What is missing is any current
    demonstration that it costs a design anything. A phasor MTF goal was implemented and
    reverted - read the outcome subsection *and* both postscripts before retrying, since
    the weight sweeps there were aimed at a case that is no longer understood. Most
    promising untried variant: the **real part** of the OTF rather than its modulus (exact
    for sagittal, smooth through zero). Also untried: reachable targets rather than 1.0.

## Changes since this review was written

Work that landed after the findings above and does not belong to any one of them.
[OPTIMIZER.md](OPTIMIZER.md) is the reference for how these behave; this section records
only why they exist and what they change about the review.

### Reference-wavelength centring and contrast balance

Finding 3 is now implemented as an opt-in reference-relative centring operation. For each
(frequency, field, orientation), `ContrastAnalysis` measures the quadrature-weighted mean
of the reference-wavelength wavefront differences and subtracts that one offset from every
wavelength. Common image displacement disappears, while wavelength-dependent displacement
remains as lateral colour. The distinction matters in the documentation: this is the exact
centred variance of the reference block, but the polychromatic residual set is a second
moment about the reference image, not about a combined wavelength-weighted centroid.

Correcting the spurious tangential mean changes the relative sagittal/tangential pressure
without directly changing sagittal residual values. `GoalContrastBalance` was therefore
added as a separate, explicit design preference: for each enabled field and contrast
frequency it drives the difference between the two orientations' weighted residual-energy
contributions toward zero. It uses the same wavelength weights and the same per-field
sagittal/tangential weights as `GoalContrast`, so it cannot silently substitute its own
orientation policy for the merit it is balancing.

The balance value is quadratic in the wavefront differences; because LM squares every goal
residual, its contribution to the final least-squares merit is quartic. Its scale is unlike
the per-sample contrast residuals, so `NOMINAL_BALANCE_WEIGHT` is only a starting point and
the aggregate balance and contrast contributions should be measured for each prescription.
The builder enables it with `contrastBalanceGoals(...)`, one flag per field, and adds one
goal per enabled field per configured contrast frequency. This does not correct another
error in the merit: unlike centring, it deliberately expresses a preference that the two
meridians should carry comparable weighted contrast error.

### Design-preservation constraints

Varying every thickness is what §6's postscript needs — a solver that cannot move the
image plane cannot fix a focus error. But an optical merit has no opinion about mechanical
layout, and with every space free the Leica solve produced air gaps of **−0.365, −0.677
and −1.349 mm**: elements passed through one another and through the stop.

`ConstraintThickness` and `ConstraintCurvature` anchor each varied parameter to its
starting value as a soft penalty in the same least-squares merit, enabled per-category
through `applyThicknessConstraints()` / `applyCurvatureConstraints()`. With them every gap
in that solve stayed positive and the solve converged. They resist a *fractional* change,
which is what lets a 0.1 mm air gap and a 39 mm back focus be held equally by one weight;
the normalization is folded into the stored weight, so the weight a constraint holds is
not the number passed to the builder.

They are penalties, not bounds. Nothing prevents a strong enough optical gradient from
moving a parameter a long way, thickness constraints hold axial centre thickness rather
than edge separation, and a final prescription still needs a mechanical check.

### Per-ray spot goals

`GoalSpotRMS` exposes one aggregate radius per field; differentiating a single
square-rooted aggregate tells the solver much less than the signed ray deviations that
compose it. `GoalSpotDeviation` emits two residuals per (field, wavelength, pupil sample)
— the weighted signed x and y intercept errors in microns — so that minimizing the sum of
squares is equivalent to minimizing the same weighted RMS spot radius while keeping the
direction of every ray error in the Jacobian. Added through `spotDeviationGoals(weights)`.

This is the same decomposition `GoalContrast` performs for MTF, applied to spot size, and
it makes a direct spot merit competitive with the contrast merit on conditioning rather
than only on cost.

### A tolerant Jacobian

`buildJacobian` ([LMDerMeritFunction.java:116](../optimr2/src/main/java/org/redukti/optim/LMDerMeritFunction.java:116))
no longer abandons the solve the moment a probe ray fails. Per variable it retries with
the step halved up to 8 times, and per residual it falls back to a one-sided difference
when only one side of the central difference is usable. A failed probe restores the
prescription and analysis in a `finally` block, so a partial state can never leak to the
caller, and the Jacobian is reported invalid only when some residual has no usable value
on either side.

This softens §5: the degenerate high-frequency case is still unrepresentable and still
throws, but an isolated failed ray at an ordinary frequency is now survivable rather than
fatal to the solve.

### Configurable and freezable vignetting

`Analysis` no longer hardcodes `VigType.SetPupil`; the mode is settable and the factors
can be measured once and held. See [finding 8](#8-vignetting-is-a-moving-mode-dependent-reference-frame)
for what the modes do and why freezing is worth having.

```java
.vignetting(VigType.SetPupil)   // default, unchanged behaviour
.freezeVignetting()
```

### Naming

The goal and builder APIs were made consistent, which invalidates method names used
earlier in this document and in older examples:

| was | now |
| --- | --- |
| `curvatureSurfaces` / `allCurvatureSurfaces` | `varyCurvatures` / `varyAllCurvatures` |
| `thicknessSurfaces` / `allThicknessSurfaces` | `varyThicknesses` / `varyAllThicknesses` |
| `includeExistingAspherics(boolean)` | `varyExistingAspherics()` |
| `useHexapolarSpotPattern(int)` | `hexapolarSampling(int)` |
| `spotRmsRayGoals(weights)` | `spotDeviationGoals(weights)` |

`OptimizationBuilder` is now ordered into labelled sections — configuration, variables,
goals, constraints, build — so a reader can tell which kind of thing a method adds.

The duplicate sagittal/tangential constant pairs were replaced by a single
[Orientation](../rayoptics/src/main/java/org/redukti/rayoptics/util/Orientation.java) in
`org.redukti.rayoptics.util`, next to `ZDir`. It sits in `rayoptics` rather than
`optimcommon` because the meridian index is a ray-optics concept, not an optimization
one: the MTF and fan analyses, the plotting tools and the goals all encode it the same
way, usually spelled `xy`. Beam43-only code now uses the constants where it previously
wrote `0`, `1` or `2` directly; the upstream-ported files (`Trace`, `SequentialModel`,
`VigCalc`, `SpotAnalysis`) were deliberately left alone to keep future ports clean.

It also validates, which the goals did not: `GoalGeoMTF`, `GoalRayAberration` and
`GoalMTFProxy` used to accept any integer and silently read the sagittal fan for it.

## How the numbers were obtained

The probe programs that produced every figure in this document live in
`optimr2/src/test/java/org/redukti/examples/`. They are not JUnit tests — each has a
`main` method — but they sit in the test source root so they compile against the
module's classpath and can reach the package-private helpers on `ZeissOtusML50mm`.

| Probe | Evidence it produced |
| --- | --- |
| [ContrastProbe.java](../optimr2/src/test/java/org/redukti/examples/ContrastProbe.java) | goal counts and SOS by type; per-block mean/rms/variance of ΔW (§3) |
| [ContrastProbe2.java](../optimr2/src/test/java/org/redukti/examples/ContrastProbe2.java) | frequency-block cosines (§4); mean fraction 5.2% (§3); surrogate vs geometric MTF |
| [ContrastProbe3.java](../optimr2/src/test/java/org/redukti/examples/ContrastProbe3.java) | sampling convergence 3×6 → 20×40 (§2); warm cost split (Verdict) |
| [ContrastProbe4.java](../optimr2/src/test/java/org/redukti/examples/ContrastProbe4.java) | the four-way re-optimization table (§2, §4). Slowest — ~5 minutes |
| [ContrastProbe5.java](../optimr2/src/test/java/org/redukti/examples/ContrastProbe5.java) | frequency sweep showing sample collapse outside the pupil (§5) |
| [ContrastProbe6.java](../optimr2/src/test/java/org/redukti/examples/ContrastProbe6.java) | clear-aperture survival per field; the vignetting factors that exposed §1 |
| [ContrastProbe7.java](../optimr2/src/test/java/org/redukti/examples/ContrastProbe7.java) | **inconclusive**, superseded by Probe8 — kept as a record of a dead end |
| [ContrastProbe8.java](../optimr2/src/test/java/org/redukti/examples/ContrastProbe8.java) | the §1 shear table — direct proof via `Field.apply_vignetting` |
| [ContrastProbe9.java](../optimr2/src/test/java/org/redukti/examples/ContrastProbe9.java) | §6 — reproduces the Leica mid-field drop; mean decomposition that rules out §3 |
| [ContrastProbe10.java](../optimr2/src/test/java/org/redukti/examples/ContrastProbe10.java) | §6 core evidence — σ vs LSQ prediction vs phasor vs geometric MTF |
| [ContrastProbe11.java](../optimr2/src/test/java/org/redukti/examples/ContrastProbe11.java) | §6 — phasor sampling convergence, 6×12 → 40×80 |
| [ContrastProbe12.java](../optimr2/src/test/java/org/redukti/examples/ContrastProbe12.java) | §7 — realised vs requested frequency, exposing the entrance/exit pupil error |
| [ContrastProbe13.java](../optimr2/src/test/java/org/redukti/examples/ContrastProbe13.java) | §7 — verifies `calibrate_frequency` removes the field-dependent bias |
| [ContrastProbe14.java](../optimr2/src/test/java/org/redukti/examples/ContrastProbe14.java) | §8 — **refutes** its own hypothesis: contrast sampling coverage is 82-95% and identical in x and y, so the contraction does not starve the sagittal direction. Kept as the record of a ruled-out cause |
| [ContrastProbe15.java](../optimr2/src/test/java/org/redukti/examples/ContrastProbe15.java) | §8 — the three-mode comparison and the on-axis symmetry violation under `Paraxial` |
| [ContrastProbe16.java](../optimr2/src/test/java/org/redukti/examples/ContrastProbe16.java) | §8 — vignetting drift per Jacobian step, and the smoothness sweep showing it is differentiable |

`ContrastProbes.java` in the same package holds the shared prescription-path lookups.
Probes 1–8 use the Otus; 9–13 use the Leica 75/2; 14–16 run both. The Otus figures in §7
came from re-running the test case with calibration enabled rather than from a probe.

Probes 1–13 were updated for the builder renames listed under
[Changes since this review was written](#changes-since-this-review-was-written) and still
compile, but they have not been re-run since. They reproduce the *evidence* as of the
code at the time each finding was written; where a fix has since landed, expect the
numbers to have moved. Probes 14–16 were written and run against the code as of
2026-08-15, so their figures are current.

Run one with (from the repository root, after a build):

```
javac -cp "beam42/target/classes;mathlib/target/classes;optimcommon/target/classes;optimr2/target/classes;rayoptics/target/classes;render/target/classes;tools/target/classes" -d /tmp/probes optimr2/src/test/java/org/redukti/examples/ContrastProbe*.java
java -cp "/tmp/probes;beam42/target/classes;mathlib/target/classes;optimcommon/target/classes;optimr2/target/classes;rayoptics/target/classes;render/target/classes;tools/target/classes" org.redukti.examples.ContrastProbe8
```

Findings 1–5 were measured against
`Examples/jfotoptix/cosina-otus-ml-50mm-f1.4/JP2026-105585_Example01.txt` using the
`createContrastSetup` configuration; finding 6 against
`Examples/jfotoptix/leica-r-apo-75mm-f2-mandler/specs.txt` using `GenericContrastOpt`. Both on
JDK 25, running the compiled classes in `*/target/classes` directly. Timings are
wall-clock on a single machine and are indicative rather than benchmark-grade; the
sampling, collinearity, mean-fraction, shear and phasor measurements are deterministic
and reproducible.
