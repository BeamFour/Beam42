# Review: Contrast Optimization implementation

Review of the contrast-optimization implementation on branch `contrast_opt`, against:

> *Contrast Optimization: A faster and better technique for optimizing on MTF*
> Ken Moore, Erin Elliott, Mark Nicholson, Chris Normanshire, Shawn Gay, Jade Aiona — Zemax, LLC

Test case: `ZeissOtusML50mmTest.optimizesPatentPrescriptionUsingContrast()`

Findings 1–7 were written against the state of the branch at the time of the review.
Everything below has been revisited as of 2026-08-14; see
[Changes since this review was written](#changes-since-this-review-was-written) for the
work that landed afterwards and does not belong to any single finding.

## Status

| # | Finding | Status |
| --- | --- | --- |
| [1](#1-vignetting-silently-rescales-the-shear--the-off-axis-merit-runs-at-the-wrong-frequency) | Vignetting rescales the shear | **Fixed** — shear now 40.0 cyc/mm at every field |
| [2](#2-the-default-3x6-sampling-is-too-coarse-and-the-optimizer-exploits-it) | 3×6 sampling too coarse | **Applied** — builder default now 6×12 |
| [3](#3-the-residual-is-the-un-centred-second-moment-not-the-variance) | Un-centred second moment | **Open** — tangential only, see the refinement below |
| [4](#4-the-three-frequencies-are-near-collinear--two-thirds-of-the-ray-budget-is-wasted) | Frequencies near-collinear | **Open** |
| [5](#5-high-frequencies-fail-without-a-diagnostic) | High-frequency degeneracy | **Partly fixed** — the silent collapse now throws; the ~0.707×cutoff ceiling remains |
| [6](#6-the-least-squares-reduction-only-tracks-mtf-in-the-small-phase-regime) | Least-squares only valid at small phase | **Open, but downgraded.** The diagnosis stands; the regression that motivated it turned out to be defocus — see the [postscript](#postscript-the-motivating-regression-was-defocus) |
| [7](#7-the-shear-is-applied-at-the-entrance-pupil-not-the-exit-pupil) | Shear applied at entrance, not exit, pupil | **Mitigated, opt-in.** `calibrate_frequency` removes the field bias (8.5% → 0.08%); exact fix pending upstream |

Finding 6 was added after the first four fixes landed, prompted by a mid-field MTF drop
running the generic contrast setup on the Leica 75/2. It was described here as the most
consequential open item, on the grounds that it produced a measurably worse design rather
than merely a wasteful merit. That framing has since been withdrawn: the drop was a
focus error the configuration had no variable to correct. The blind spot in the merit is
real and still unfixed, but no case is currently known where it walks a design backwards.

## Code under review

Line references are as of 2026-08-14.

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
mid-field drop. That case turned out to be defocus rather than a merit failure; the
[postscript to §6](#postscript-the-motivating-regression-was-defocus) has the detail. The
non-monotonicity is measured and real, but the claim that it costs a design anything in
practice is currently unsupported.)

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

The modulus of the OTF is

```
|OTF| = |⟨exp(iΦ)⟩|  ≈  1 − Var(Φ)/2        where Φ = 2π·ΔW
```

i.e. it depends on the **variance** of the phase difference, not on `⟨Φ²⟩`. A constant
`ΔW` across the pupil is a pure image displacement (it moves the phase transfer
function, not the modulus) and costs no MTF at all. But
[ContrastAnalysisResult.java:20](../rayoptics/src/main/java/org/redukti/rayoptics/analysis/ContrastAnalysisResult.java:20)
forms `√w · ΔW` with no mean removal, so `Σr² = ⟨Φ²⟩ = Var + mean²`. Still the case.

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
  on the tangential residuals, and this finding can never explain a sagittal symptom.
- On the Leica 75/2 the tangential mean share reaches **57%** (field 1.0, 50 cyc/mm),
  well above the Otus figures — so the size of the effect is strongly design-dependent.

**Suggested fix:** subtract the weighted mean per (frequency, field, wavelength,
orientation) group. The residual `√w_i·(ΔW_i − ΔW̄)` is still smooth and LM handles the
rank-1 deficiency per group without trouble.

If you want to retain a polychromatic lateral-color penalty, subtract the *reference
wavelength's* mean from every wavelength rather than each wavelength's own mean — that
keeps colour-dependent tilt (which does reduce polychromatic MTF) while discarding the
common tilt (which does not).

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

## Suggested order of work

Done:

1. ~~**Fix the vignetting/shear coupling** (§1)~~ — it was why field 3 tangential was stuck.
2. ~~**Raise the sampling default to ~6×12**~~ (§2).
3. ~~**Stop the degenerate high-frequency case failing silently**~~ (§5).
3a. ~~**Correct the entrance-pupil shear bias** (§7)~~ — available, opt-in, pending the
    exact exit-pupil treatment from upstream.

Remaining, in priority order. §6 has dropped down the list since the postscript: with no
known case where the merit costs a design anything, it is a latent limitation rather than
an active problem.

4. **Subtract the weighted mean** (§3) — tangential only; worth doing, but it cannot
   explain a sagittal symptom. Now the highest-value open item, being a real and
   quantified 5% of the merit spent on image displacement that costs no MTF.
5. **Drop to a single contrast frequency** (§4) — recovers most of the cost that the
   6×12 sampling change added. Note the collinearity was measured on the Otus, where
   phases are small; on a badly-aberrated design like the Leica the frequencies may be
   less redundant, so re-measure before removing any.
6. **Validate frequency against the cutoff** (§5, remaining half) — at configuration
   time rather than on the first trace.
7. **Honour `dLineOnly` for contrast goals**, or document that it deliberately does not
   apply to them.
8. **§6, if a case for it appears.** A phasor MTF goal was implemented and reverted; read
   the outcome subsection *and* the postscript before retrying, since both weight sweeps
   there were aimed at a defocus. Most promising untried variant: the **real part** of
   the OTF rather than its modulus (exact for sagittal, smooth through zero). Also
   untried: reachable targets rather than 1.0.

## Changes since this review was written

Work that landed after the findings above and does not belong to any one of them.
[OPTIMIZER.md](OPTIMIZER.md) is the reference for how these behave; this section records
only why they exist and what they change about the review.

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
The four duplicate sagittal/tangential constant pairs were replaced by a single
`Orientation` class, which also validates: `GoalGeoMTF`, `GoalRayAberration` and
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

`ContrastProbes.java` in the same package holds the shared prescription-path lookups.
Probes 1–8 use the Otus; 9–13 use the Leica 75/2. The Otus figures in §7 came from
re-running the test case with calibration enabled rather than from a probe.

The probes were updated for the builder renames listed under
[Changes since this review was written](#changes-since-this-review-was-written) and still
compile, but they have not been re-run since. They reproduce the *evidence* as of the
code at the time each finding was written; where a fix has since landed, expect the
numbers to have moved.

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
