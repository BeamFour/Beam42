## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 412.0 | 13.0 | 112.06 | 1.50032 | 81.9 |  |
| 2 | -435.3 | 8.0 | 112.06 |  |  |  |
| 3 | -403.2 | 8.0 | 112.06 | 1.744 | 44.9 |  |
| 4 | 445.2 | 2.0 | 112.06 |  |  |  |
| 5 | 396.7 | 10.0 | 112.06 | 1.62374 | 47.0 |  |
| 6 | -873.9 | 590.0 | 112.06 |  |  |  |
| 7 | AS | 69.0 | 30.988 |  |  |  |
| 8 | -195.0 | 2.0 | 47.7 | 1.51632 | 64.2 |  |
| 9 | 439.2 | 7.0 | 47.7 | 1.66854 | 39.2 |  |
| 10 | -927.5 | 239.96 | 47.7 |  |  |  |
## Layouts
![Layout Only](./layoutonly.svg)
![Layout Field 0.0](./layout.svg)
![Layout Field 0.7](./layout-semi-skew.svg)
![Layout Field 1.0](./layout-skew.svg)
## Spot Diagrams
![Spot Diagram Field 0.0](./spot.svg)
![Spot Diagram Field 0.7](./spot-semi-skew.svg)
![Spot Diagram Field 1.0](./spot-skew.svg)
## Paraxial Parameters
| parameter | value |
| ---       | ---   |
| effective_focal_length |1199.701
| back_focal_length | 240.06
| optical_invariant | 1
| object_distance | 1.0E10
| image_distance | 240.061
| power | 0.001
| pp1_H | -1287.325
| ppk_H' | -959.641
| ffl_F | -2487.026
| fno | 10.997
| enp_dist_P | 2180.179
| enp_radius | 54.545
| exp_dist_P' | -68.221
| exp_radius | 14.021
| m | -0
| red | -8335407.711428385
| n_obj | 1
| n_img | 1
| img_ht | 21.988
| obj_ang | 1.05
| obj_na | 0
| img_na | -0.045|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 0.607 | 1.027|
 | Field(x=0.0, y=0.1) | 0.633 | 1.094|
 | Field(x=0.0, y=0.2) | 0.676 | 1.181|
 | Field(x=0.0, y=0.3) | 0.743 | 1.292|
 | Field(x=0.0, y=0.4) | 0.854 | 1.543|
 | Field(x=0.0, y=0.5) | 1.008 | 1.826|
 | Field(x=0.0, y=0.6) | 1.212 | 2.09|
 | Field(x=0.0, y=0.7) | 1.464 | 2.349|
 | Field(x=0.0, y=0.8) | 1.76 | 2.659|
 | Field(x=0.0, y=0.9) | 2.098 | 3.236|
 | Field(x=0.0, y=1.0) | 2.441 | 3.95|
## Polychromatic Geometric MTF
![Polychromatic Geometrical MTF](./mtf.svg)
* 10,30,50 cycles/mm
* Black lines represent sagittal, blue tangential
* To generate above, MTFs for wavelengths 587.5618(d), 486.1327(F), 656.2725(C) were calculated across 10 fields, and then averaged
## Polychromatic Geometric MTF (Weighted)
![Polychromatic Geometrical MTF Weighted](./mtf-w.svg)
* 10,30,50 cycles/mm
* Black lines represent sagittal, blue tangential
* To generate above, MTFs for wavelengths 587.5618(d) wt(1.0), 656.2725(C) wt(0.475), 546.074(e) wt(0.98), 486.1327(F) wt(0.49), 435.8343(g) wt(0.15) were calculated across 10 fields, and then combined using weighted average
## Resources
* [OpticalBench Compatible Data File, tab delimited](./prescription.txt)
* [Zemax file](./JP1976-043920_Example01P.zmx)

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-02-27
