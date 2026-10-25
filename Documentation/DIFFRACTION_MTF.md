# Diffraction modulation transfer function

This note describes the scalar diffraction calculation used by `DiffractionMTF`.
It follows the calculation from a ray-traced wavefront to image contrast, derives
the frequency scale and circular-pupil reference, and states the approximations
needed to interpret the result. The final section extends the calculation to
the wavelength sum used by `PolyDiffractionMTF`.

## 1. What is being calculated

At one field point, wavelength and image plane, the point-spread function (PSF)
is the intensity image of an object point. For spatially incoherent imaging,
nearby object points contribute intensities independently. If the PSF is
approximately unchanged over the small object region under consideration,
the image is a convolution with that PSF.

Let $h(x,y)$ be the PSF normalized so that its integral is one. Define the
optical transfer function (OTF) by

$$
H(\nu_x,\nu_y)=\iint h(x,y)e^{-i2\pi(\nu_x x+\nu_y y)}\,dx\,dy.
$$

The modulation transfer function is

$$
\operatorname{MTF}(\nu_x,\nu_y)=|H(\nu_x,\nu_y)|.
$$

For example, convolving a sinusoidal intensity pattern with $h$ multiplies
its complex sinusoidal coefficient by $H$. Its contrast is multiplied by
$|H|$, and its phase changes by $\arg H$. Consequently,

$$
H(0,0)=1,\qquad \operatorname{MTF}(0,0)=1,\qquad 0\leq\operatorname{MTF}\leq1.
$$

The upper bound follows from the triangle inequality and $h\geq0$:
$\left|\int h\,e^{-i\theta}\right|\leq\int h=1$.
These are normalized contrast measures; they do not measure throughput.

Spatial frequencies are image-space cycles per system length unit. With
millimetre prescriptions, 40 cycles/mm means a sinusoidal period of 0.025 mm.
The wavelength in the equations below is the vacuum wavelength converted to
the same length unit as the prescription.

## 2. Trace the pupil and measure wavefront error

Choose the field, wavelength and axial image-plane displacement. Trace the
chief ray and establish a reference wavefront converging to its intersection
with the selected image plane. For a finite exit pupil this is a reference
sphere centred at that image point. The wavefront machinery also provides
an infinite-reference limit.

Trace a rectangular grid across the field's pupil bounds, retaining the
positions of blocked or failed samples as missing data. Aperture checking
determines which sampled rays pass through the system. Each successful ray
supplies an optical path difference (OPD) relative to the reference wavefront.
For the finite reference sphere, the path comparison and its stable geometric
evaluation are derived in the companion optical-path-difference note.

Use the chief-minus-test convention:

$$
\begin{aligned}
\operatorname{OPD}&=\text{chief optical path}-\text{test optical path},\\
W&=\frac{\operatorname{OPD}}{\lambda}.
\end{aligned}
$$

$W$ is in waves, so its phase is $2\pi W$. Moving the image plane changes
the reference wavefront and therefore the defocus in $W$; this calculation
does not search for best focus. For a chief-ray image segment $(\mathbf p,\mathbf d)$, an
axial shift $\mathrm{foc}$ moves its intersection to

$$
\mathbf p_{\mathrm{new}}=\mathbf p+\mathbf d\,\frac{\mathrm{foc}}{d_z}.
$$

A constant added to every OPD is piston. It multiplies the entire complex
field by one phase factor and has no effect on the PSF or MTF. Tilt generally
displaces the PSF; it must be retained when OTF phases from different
wavelengths are compared.

## 3. Construct the complex pupil

The generalized pupil is an amplitude transmission multiplied by a phase:

$$
P(u,v)=A(u,v)e^{i2\pi W(u,v)}.
$$

The present calculation uses a binary amplitude:

$$
A=\begin{cases}
1&\text{at samples with a finite wavefront value},\\
0&\text{at blocked, failed or non-finite samples and outside the grid}.
\end{cases}
$$

Zero wavefront error is illuminated, not missing data. Every valid sample has
equal amplitude. There is no Fresnel transmission, coating loss, absorption,
polarization, or ray-density amplitude correction in this pupil.

The grid is rectangular in entrance-pupil sampling coordinates, with $N$
samples across each axis. Its transmitted mask need not be circular: surface
apertures can remove samples. Field vignetting factors also affect the pupil
bounds and ray launches. A missing ray is treated as zero transmission even
when its failure was numerical rather than physical; ray validity must
therefore be checked separately.

## 4. From pupil to point-spread function

In the scalar focal-region Fourier approximation, the image amplitude is
a Fourier transform of the complex pupil. A useful physical coordinate is
the transverse propagation direction multiplied by the image-space index:

$$
q_x=n_{\mathrm{img}}l,\qquad q_y=n_{\mathrm{img}}m.
$$

where $l,m$ are direction cosines and $n_{\mathrm{img}}$ is the positive image-space
refractive index. Suppressing constant factors and an irrelevant phase,
one consistent representation is

$$
\begin{aligned}
U(x,y)&=\iint P(q_x,q_y)\exp\!\left[i2\pi\frac{q_x x+q_y y}{\lambda}\right]\,dq_x\,dq_y,\\
I(x,y)&=|U(x,y)|^2,\\
h(x,y)&=\frac{I(x,y)}{\iint I(x,y)\,dx\,dy}.
\end{aligned}
$$

All pupil contributions from one object point interfere as complex
amplitudes before taking the squared modulus. Independent object points
subsequently add as intensities. This distinction is why the calculation
includes diffraction even though rays supply the wavefront.

The Fourier relation requires a suitable pupil coordinate and amplitude
measure. Beam42 approximates $q_x,q_y$ as affine functions of the two sampled
pupil coordinates, calibrated by rim rays. It does not resample the wavefront
onto a uniform direction-cosine grid. The consequences are discussed below.

Numerically, place the $N\times N$ complex pupil in a zero-filled $M\times M$ array,
transform it, and square the amplitude modulus. The PSF is initially divided
by its peak. This common scale cancels when the OTF is normalized by its
zero-frequency value. A peak-normalized PSF does not by itself give Strehl
ratio, encircled energy, or absolute irradiance.

The negative-exponent FFT used with $\exp(+i2\pi W)$ represents the image
with reversed transverse coordinates under the convention above. With an
even transform size, an array index $a$ corresponds to

$$
x_a=-\left(a-\frac M2\right)\Delta x,\qquad y_b=-\left(b-\frac M2\right)\Delta y.
$$

Reversal does not change a monochromatic MTF magnitude, but it changes OTF
phase. The complex-OTF evaluation uses these reversed coordinates explicitly.

## 5. Derive the OTF as a pupil overlap

The PSF transform has an equivalent expression which avoids the intermediate
image intensity. Substituting the amplitude integral into $I=UU^*$ gives

$$
I(\mathbf r)=\iint\!\iint P(\mathbf q)P^*(\mathbf q^\prime)\exp\!\left[i2\pi\frac{(\mathbf q-\mathbf q^\prime)\cdot\mathbf r}{\lambda}\right]\,d^2q\,d^2q^\prime.
$$

Fourier transformation with respect to $\mathbf r=(x,y)$ imposes
$\mathbf q-\mathbf q^\prime=\lambda\boldsymbol\nu$, where $\boldsymbol\nu=(\nu_x,\nu_y)$. Normalizing at zero frequency
therefore gives

$$
H(\boldsymbol\nu)=\frac{\displaystyle\iint P\!\left(\mathbf q+\frac{\lambda\boldsymbol\nu}{2}\right)P^*\!\left(\mathbf q-\frac{\lambda\boldsymbol\nu}{2}\right)\,d^2q}{\displaystyle\iint|P(\mathbf q)|^2\,d^2q}.
$$

This is the normalized autocorrelation of the complex pupil. For a binary
pupil, only the overlap of the aperture and its shifted copy contributes:

$$
H(\boldsymbol\nu)=\frac1{\text{pupil area}}\iint_{\mathrm{overlap}}\exp\!\left\{i2\pi\left[W\!\left(\mathbf q+\frac{\lambda\boldsymbol\nu}{2}\right)-W\!\left(\mathbf q-\frac{\lambda\boldsymbol\nu}{2}\right)\right]\right\}\,d^2q.
$$

The denominator is the full transmitted pupil area, not the overlap area.
Normalizing by overlap area would remove the diffraction loss with frequency.
Equivalently, this is the overlap-area fraction multiplied by the average
phase factor over that overlap.

For a fixed nonnegative amplitude distribution, the triangle inequality gives

$$
|H(\boldsymbol\nu)|\leq\frac{\displaystyle\iint A\!\left(\mathbf q+\frac{\lambda\boldsymbol\nu}{2}\right)A\!\left(\mathbf q-\frac{\lambda\boldsymbol\nu}{2}\right)\,d^2q}{\displaystyle\iint A(\mathbf q)^2\,d^2q}.
$$

Thus the zero-phase pupil with the *same amplitude distribution* bounds the
aberrated MTF at each frequency. A clear-circle reference is such a bound
only when that is the actual pupil model. Multiplying a geometric MTF by a
circular diffraction curve is not the general overlap formula.

## 6. Derive the physical frequency scale

For two interfering pupil directions, the image intensity contains a term
with spatial frequency $(\mathbf q-\mathbf q^\prime)/\lambda$. The available pupil span therefore
sets the frequency support. Using the positive and negative axial rim rays,
the adopted axis scales are

$$
C_x=\frac{n_{\mathrm{img}}|l_{+x}-l_{-x}|}{\lambda},\qquad C_y=\frac{n_{\mathrm{img}}|m_{+y}-m_{-y}|}{\lambda}.
$$

These are `nu_cutoff_x` and `nu_cutoff_y`. If the half direction-cosine span
is called $a_x$, then $C_x=2n_{\mathrm{img}}a_x/\lambda$. In the usual convention
$\mathrm{NA}_x=n_{\mathrm{img}}a_x$, this is $2\,\mathrm{NA}_x/\lambda$.

For a centred circular cone with semi-angle $\alpha$,

$$
C=\frac{2n_{\mathrm{img}}\sin\alpha}{\lambda}.
$$

For a paraxial image in air, $\sin\alpha\approx D/(2f^\prime)$, so
$C\approx1/(\lambda F_{\mathrm{number}})$. At finite conjugates the image-side
cone, or working f-number, is the relevant quantity. Real rim rays avoid
assuming that the two off-axis or anamorphic scales are equal.

The four rim directions are scale estimates, not a search over all
transmitted directions. With distorted, rotated or irregular pupils they
need not determine the exact support of an axial OTF slice. Even the full
projected direction span is only a bound: an axial autocorrelation also
requires overlap in the other direction.

Under the assumed affine map, $N$ pupil samples span $N-1$ intervals:

$$
\Delta q_x=\frac{\lambda C_x}{N-1}.
$$

The DFT conjugacy condition is $M\,\Delta q_x\,\Delta x/\lambda=1$, giving

$$
\begin{aligned}
\Delta x&=\frac{N-1}{MC_x},\\
L_x&=M\Delta x=\frac{N-1}{C_x},\\
\Delta\nu_x&=\frac1{L_x}=\frac{C_x}{N-1}.
\end{aligned}
$$

The y expressions follow by replacing x with y. A pupil autocorrelation
shift of $k$ samples corresponds to $\nu_x=kC_x/(N-1)$.
The period $L_x$ is the image-window width, while $\Delta\nu_x$ is the OTF
bin spacing. These scales use the sampled endpoints; replacing $N-1$ by
$N$ changes the calibration.

## 7. Padding, sampling and finite windows

The autocorrelation of $N$ samples occupies at most $2N-1$ lags. An FFT
computes a circular correlation, so sufficient padding is needed to keep
positive and negative lags from overlapping. The calculation requires
$M\geq2N$, and an even $N\geq4$.

The sampling relations show two different effects:

- Increasing $M$ at fixed $N$ reduces image pixel spacing but leaves the
  image-window width and OTF bin spacing unchanged. It adds no wavefront data.
- Increasing $N$ refines the pupil, enlarges the image window and reduces
  the OTF bin spacing. $M$ must also satisfy the padding requirement.

Pupil phase must be resolved. An adjacent phase change approaching $\pi$,
equivalently an OPD change approaching half a wavelength, is a warning of
undersampling. This is a practical local check, not a proof of convergence;
rapid variation between samples and poorly resolved aperture edges can
still be missed. Large defocus and strong aberrations need finer sampling.

Sampling the pupil makes the image periodic with periods $L_x,L_y$. A broad
PSF can wrap into the opposite side of its window even when the pupil
autocorrelation has sufficient padding. Increasing padding alone does not
enlarge that window.

A geometric warning estimates whether the blur fits. For each successful
grid ray, measure its image displacement from the chief-ray image point:

$$
e=\max\!\left(\frac{|x-x_{\mathrm{chief}}|}{L_x/2},\frac{|y-y_{\mathrm{chief}}|}{L_y/2}\right).
$$

The reported `spot_to_window` is the 99th percentile of these values, using
equal ray counts. A value above one warns of overflow. The corresponding
sampling suggestion follows from $L\propto N-1$:

$$
N_{\mathrm{new}}\geq1+(N-1)\,\mathtt{spot\_to\_window},
$$

rounded upwards to an even integer. Rerun after increasing $N$; the newly
sampled rays can reveal a different extent.

This check is not an encircled-energy calculation or a rigorous error bound.
It excludes failed rays, permits one percent of successful rays outside the
window, and does not measure diffraction tails. A value below one does not
certify accuracy. With no traced rays the diagnostic returns zero, which
does not mean a usable PSF exists.

Use an even $M$ for the centred-coordinate and phase formulas in this note.
Although the constructor only checks its lower bound, the current centring
operations do not establish an odd-size phase convention. Padding and phase
resolution are separate from the physical validity of the Fourier model.

## 8. Obtain axial MTF and complex OTF

Fourier-transforming the PSF and dividing the transform modulus by its DC
value produces the sampled two-dimensional MTF. The calculation calls the
x-frequency slice sagittal and the y-frequency slice tangential. These
names assume the conventional meridional field orientation; arbitrary field
azimuths or rotated systems require interpreting the actual coordinate axes.
The frequency direction is perpendicular to the lines of a sinusoidal target.

The monochromatic magnitude accessors linearly interpolate neighbouring
MTF bins. They use $|\nu|$ and return zero at and beyond the adopted cutoff.
The zero beyond cutoff is imposed, not an independent physical validation
of the rim-ray estimate.

For complex OTF values, first integrate the PSF in the perpendicular direction:

$$
\begin{aligned}
\ell_x(x)&=\int h(x,y)\,dy,\\
H(\nu,0)&=\int\ell_x(x)e^{-i2\pi\nu x}\,dx.
\end{aligned}
$$

This follows directly by setting $\nu_y=0$ in the two-dimensional transform.
The discrete line spread is normalized to unit sum, so the actual evaluation is

$$
H_x(\nu)=\sum_{a=0}^{M-1}\ell_x[a]\exp\!\left[+i2\pi\nu\left(a-\frac M2\right)\Delta x\right].
$$

The positive sign accounts for the reversed image coordinates described
above. This finite sum evaluates the sampled PSF transform at the requested
frequency, without interpolating magnitudes. At FFT-bin frequencies its
modulus agrees with the two-dimensional FFT slice. Between bins it need not
equal the linearly interpolated MTF magnitude. It remains a finite-window
approximation to the continuous optical OTF, not an exact optical solution
at an arbitrary frequency. The complex accessor also imposes zero at and
beyond the adopted cutoff.

Because the PSF is real, $H(-\nu)=H(\nu)^*$; consequently its magnitude is
even. In two dimensions the general symmetry is simultaneous reversal
$\operatorname{MTF}(-\nu_x,-\nu_y)=\operatorname{MTF}(\nu_x,\nu_y)$, not necessarily reflection in each axis.

## 9. Derive the clear circular-pupil reference

For a circular pupil of radius $R$ with uniform amplitude and zero wavefront
error, the OTF is the overlap area of two equal disks divided by $\pi R^2$.
Let their separation be $d$, and define

$$
s=\frac d{2R}=\frac{|\nu|}{C},\qquad\phi=\arccos s.
$$

For $0\leq s<1$, the overlap consists of two circular segments. Each is a
sector of area $R^2\phi$ minus a triangle of area
$R^2\sin\phi\cos\phi$. Thus

$$
\begin{aligned}
\text{overlap}&=2R^2(\phi-\sin\phi\cos\phi),\\
\operatorname{MTF}_{\mathrm{clear}}(s)&=\frac2\pi\left[\arccos s-s\sqrt{1-s^2}\right].
\end{aligned}
$$

At $s=0$ the disks coincide and the result is one. At $s\geq1$ there is no
overlap of positive area and the result is zero. For example, at half cutoff,

$$
\operatorname{MTF}_{\mathrm{clear}}\!\left(\frac12\right)=\frac2\pi\left(\frac\pi3-\frac{\sqrt3}{4}\right)\approx0.3910.
$$

This is the reference returned for each adopted axis cutoff. It also describes
an ideal uniformly illuminated ellipse along its principal axes after the
corresponding coordinate scaling. It is not the diffraction-limited response
of an arbitrary vignetted, obscured, apodized or irregular pupil. For those
pupils the appropriate reference is their own zero-phase autocorrelation.

## 10. Reference position and polychromatic MTF

Changing the coordinate origin does not change monochromatic contrast. It
does change OTF phase. If a PSF is displaced by $d$ from a common reference,
substitute $t=x-d$ into its Fourier transform:

$$
\int h(x-d)e^{-i2\pi\nu x}\,dx=e^{-i2\pi\nu d}H(\nu).
$$

Each monochromatic PSF is calculated around its own chief-ray image point.
To refer its OTF to another image point, use $d=\text{chief point}-\text{reference point}$ and multiply by the phase
factor above. This avoids moving lateral
colour through a finite periodic image window. It does not remove intrinsic
blur, defocus or chromatic wavefront error.

For mutually incoherent wavelengths, intensities add. If $h_j$ has unit
integral and $w_j$ is its nonnegative detected-energy weight, then

$$
\begin{aligned}
h_{\mathrm{poly}}&=\frac{\sum_jw_jh_j}{\sum_jw_j},\\
H_{\mathrm{poly}}(\nu)&=\frac{\sum_jw_jH_j(\nu)}{\sum_jw_j},\\
\operatorname{MTF}_{\mathrm{poly}}(\nu)&=\frac{\left|\sum_jw_jH_j(\nu)\right|}{\sum_jw_j}.
\end{aligned}
$$

All $H_j$ must use the same image-plane origin, physical frequency and
physical image plane. Beam42 uses the reference wavelength's chief-ray
image point as origin. Each wavelength has its own cutoff and FFT-bin
spacing, so the line-spread sums evaluate every OTF at the requested physical
frequency before combining it. Wavelengths beyond their cutoff contribute
zero to the numerator but retain their weight in the denominator.

Averaging the monochromatic MTF magnitudes would discard relative phase:

$$
\left|\sum_jw_jH_j\right|\leq\sum_jw_j|H_j|.
$$

For two equally weighted, otherwise identical PSFs separated by $d$,

$$
\begin{aligned}
H_{\mathrm{poly}}&=H_0e^{-i\pi\nu d}\cos(\pi\nu d),\\
\operatorname{MTF}_{\mathrm{poly}}&=|H_0|\,|\cos(\pi\nu d)|.
\end{aligned}
$$

Both individual MTFs are $|H_0|$, yet their sum can have zero contrast.
This is the effect of lateral colour that a magnitude average misses.

The spectral weights must represent the desired source spectrum, detector
response and relative transmitted energy. Since each monochromatic PSF is
normalized independently, wavelength-dependent throughput is not recovered
automatically. Physical interpretation requires finite nonnegative weights
with a positive sum; the current wrapper does not fully validate these
conditions. A discrete wavelength sum also needs spectral convergence checks.

## 11. Physical limitations

The calculation is a scalar pupil-FFT model of locally incoherent imaging.
It includes interference from the sampled wavefront and binary aperture,
but has the following limits:

- **Pupil mapping.** Uniform entrance-pupil samples are treated as a uniform
  grid in image-side direction-cosine coordinates after independent x/y
  scaling. Nonlinear pupil aberration, shear and rotation are not corrected
  by measuring four rim rays. A coordinate change would require resampling
  and the appropriate amplitude and integration measure. Field vignetting
  factors and the sampled aperture mask do not establish that mapping.
- **Amplitude.** Equal-amplitude samples omit apodization, transmission
  variation and pupil-mapping amplitude effects. Normalization conceals
  total energy loss; it does not correct the omitted amplitude distribution.
- **Propagation.** A reference-wavefront Fourier calculation is a focal-region
  approximation, not general propagation through arbitrary image planes.
  Very high numerical aperture, strong off-axis pupil distortion or large
  defocus can require a suitably weighted Huygens integral or vector
  diffraction calculation. More samples cannot repair an unsuitable model.
- **Image coordinates.** Separate axis scales do not supply a general
  transformation to a tilted or curved detector surface.
- **Image formation.** Partial coherence, coherent extended-object transfer,
  polarization coupling, scatter, ghosts, detector pixel integration and
  detector sampling are outside this optical MTF calculation.
- **Locality.** A field-specific PSF describes a small neighbourhood. One OTF
  does not describe an entire image when blur or magnification varies
  substantially with field.

The need for nearly uniform exit-pupil sampling in cosine space is also
stated in Ansys's [FFT MTF documentation](https://ansyshelp.ansys.com/public/Views/Secured/Zemax/v26102/en/OpticStudio_User_Guide/OpticStudio_Help/topics/FFT_MTF.html).
It is a limitation of the Fourier pupil model, independent of FFT accuracy.

## 12. Independent checks

The following checks separate the optical mathematics from a particular
implementation:

1. A zero-phase filled circular pupil must converge to the disk-overlap
   formula, including one at DC, approximately 0.3910 at half cutoff, and
   zero at cutoff.
2. A direct discrete complex-pupil autocorrelation, divided by the full
   pupil energy, must agree with the PSF-transform result at FFT bins.
   This also tests the transmitted mask and padding.
3. Piston must leave both PSF intensity and OTF unchanged. A known image
   translation must preserve MTF and multiply OTF by
   $\exp(-i2\pi\nu d)$.
4. Complex OTFs must obey conjugate symmetry and agree in modulus with FFT
   slices at bin frequencies. A centred rotationally symmetric test must
   give equal x and y responses.
5. Increase pupil sampling and padding separately. Require stable results
   at the physical frequencies of interest, adequate phase and aperture
   resolution, and an image window large enough for the blur.
6. Check physical frequency calibration against a simple known cone or
   paraxial image-side f-number. Internal transform agreement alone cannot
   validate the pupil-to-image coordinate mapping.
7. For polychromatic results, verify a common image reference and plane,
   nonnegative energy weights, the two-displaced-PSF example, and stability
   as wavelengths are added.

The existing `DiffractionMTFTest` and `PolyDiffractionMTFTest` cover several
of these identities, including the circular reference, overlap comparison,
FFT-bin agreement, phase translations and finite-window warning. They are
regression checks, not validation for all pupil geometries or imaging regimes.
