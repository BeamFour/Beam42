# AI Nikkor 50mm f1.2
## Patent Information
| Country | Patent Number | Example | Year of Application | Inventors | Organisation | Link |
| ---     | ---           | ---     | ---                 | ---       | ---          | ---  |
|US | US3738736 | EX 1 | 1972 | Yoshiyuki Shimizu | Nikon Corp | [link](https://patents.google.com/patent/US3738736A) |
## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 67.0 | 5.2 | 45.26 | 1.7972 | 41.14 | Hoya | NBFD2 |
| 2 | 769.3 | 0.1 | 44.6213 |  |  |  |
| 3 | 28.5 | 8.3 | 39.7642 | 1.744 | 44.9 | Hoya | LAF2 |
| 4 | 60.0 | 1.5 | 36.3875 |  |  |  |
| 5 | 87.0 | 1.5 | 35.5908 | 1.68893 | 31.16 | Hoya | E-FD8 |
| 6 | 18.19 | 9.3 | 28.7881 |  |  |  |
| 7 | AS | 8.5 | 28.3444 |  |  |  |
| 8 | -18.2 | 1.1 | 28.2442 | 1.7847 | 26.1 | Hoya | FDS30 |
| 9 | 0.0 | 8.7 | 35.4448 | 1.74443 | 49.4 |  |
| 10 | -28.4 | 0.1 | 37.1413 |  |  |  |
| 11 | -180.0 | 6.1 | 39.902 | 1.72 | 50.23 | Ohara | S-LAL10 |
| 12 | -34.7 | 0.1 | 40.1929 |  |  |  |
| 13 | 92.14 | 3.2 | 36.99 | 1.76684 | 46.78 | Hikari | J-LASFH2 |
| 14 | -259.79 | 39.03 | 36.99 |  |  |  |
## Layouts
![Layout Elements](./layoutonly.svg)
![Layout](./layout.svg)
## Spot Diagrams
![Spot Diagram Field 0.0](./spot.svg)
![Spot Diagram Field 0.7](./spot-semi-skew.svg)
![Spot Diagram Field 1.0](./spot-skew.svg)
## Paraxial Parameters
| parameter | value |
| ---       | ---   |
| effective_focal_length |51.602
| back_focal_length | 39.048
| optical_invariant | 9.127
| object_distance | 1.0E10
| image_distance | 39.048
| power | 0.019
| pp1_H | 55.86
| ppk_H' | -12.554
| ffl_F | 4.258
| fno | 1.2
| enp_dist_P | 32.319
| enp_radius | 21.501
| exp_dist_P' | -55.826
| exp_radius | 39.538
| m | -0
| red | -1.9379103943764833E8
| n_obj | 1
| n_img | 1
| img_ht | 21.904
| obj_ang | 23
| obj_na | 0
| img_na | -0.417|
## Spot Analysis
| Field | Spot Mean Radius (µm) | Spot Max Radius (µm) |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 21.898 | 55.353|
 | Field(x=0.0, y=0.1) | 31.273 | 138.922|
 | Field(x=0.0, y=0.2) | 46.436 | 204.708|
 | Field(x=0.0, y=0.3) | 59.532 | 217.04|
 | Field(x=0.0, y=0.4) | 68.812 | 210.861|
 | Field(x=0.0, y=0.5) | 75.797 | 245.699|
 | Field(x=0.0, y=0.6) | 83.216 | 316.927|
 | Field(x=0.0, y=0.7) | 90.742 | 387.971|
 | Field(x=0.0, y=0.8) | 99.702 | 436.706|
 | Field(x=0.0, y=0.9) | 105.93 | 448.414|
 | Field(x=0.0, y=1.0) | 104.291 | 392.873|
## Polychromatic Geometric MTF
![Polychromatic Geometrical MTF](./mtf.svg)
* 10=red,30=blue,50=black cycles/mm
* Solid lines represent sagittal, dashed lines tangential
* To generate above, MTFs for wavelengths 587.5618(d), 486.1327(F), 656.2725(C) were calculated across 10 fields, and then averaged
## Polychromatic Geometric MTF (Weighted)
![Polychromatic Geometrical MTF Weighted](./mtf-w.svg)
* 10=red,30=blue,50=black cycles/mm
* Solid lines represent sagittal, dashed lines tangential
* To generate above, MTFs for wavelengths 587.5618(d) wt(1.0), 656.2725(C) wt(0.475), 546.074(e) wt(0.98), 486.1327(F) wt(0.49), 435.8343(g) wt(0.15) were calculated across 10 fields, and then combined using weighted average
## Resources
* [OpticalBench Compatible Data File, tab delimited](./prescription.txt)
* [Zemax file](./US003738736_Example01.zmx)

Generated from `US003738736_Example01.txt`, status **TODO**

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-10-17
