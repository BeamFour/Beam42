# AI Nikkor 50mm f1.2
## Patent Information
| Country | Patent Number | Example | Year of Application | Inventors | Organisation | Link |
| ---     | ---           | ---     | ---                 | ---       | ---          | ---  |
|US | US3738736 | EX 1 | 1972 | Yoshiyuki Shimizu | Nikon Corp | [link](https://patents.google.com/patent/US3738736A) |
## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 67.0 | 5.2 | 45.26 | 1.79631 | 40.8 |  |
| 2 | 769.3 | 0.1 | 45.26 |  |  |  |
| 3 | 28.5 | 8.3 | 39.86 | 1.744 | 44.8 | Hikari | J-LAF2 |
| 4 | 60.0 | 1.5 | 39.86 |  |  |  |
| 5 | 87.0 | 1.5 | 39.86 | 1.68893 | 31.08 | Ohara | S-TIM28 |
| 6 | 18.19 | 9.3 | 28.96 |  |  |  |
| 7 | AS | 8.5 | 28.352 |  |  |  |
| 8 | -18.2 | 1.1 | 28.96 | 1.7847 | 26.27 | Hikari | J-SFS3 |
| 9 | 0.0 | 8.7 | 37.48 | 1.74443 | 49.4 |  |
| 10 | -28.4 | 0.1 | 37.48 |  |  |  |
| 11 | -180.0 | 6.1 | 40.37 | 1.72 | 50.23 | Ohara | S-LAL10 |
| 12 | -34.7 | 0.1 | 40.37 |  |  |  |
| 13 | 92.14 | 3.2 | 36.99 | 1.76684 | 46.78 | Hikari | J-LASFH2 |
| 14 | -259.79 | 39.03 | 36.99 |  |  |  |
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
| effective_focal_length |51.6
| back_focal_length | 39.08
| optical_invariant | 9.126
| object_distance | 1.0E10
| image_distance | 39.08
| power | 0.019
| pp1_H | 55.86
| ppk_H' | -12.52
| ffl_F | 4.26
| fno | 1.2
| enp_dist_P | 32.309
| enp_radius | 21.5
| exp_dist_P' | -55.794
| exp_radius | 39.552
| m | -0
| red | -1.9379826885676596E8
| n_obj | 1
| n_img | 1
| img_ht | 21.903
| obj_ang | 23
| obj_na | 0
| img_na | -0.385|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 28.354 | 60.46|
 | Field(x=0.0, y=0.1) | 35.344 | 155.317|
 | Field(x=0.0, y=0.2) | 48.933 | 159.297|
 | Field(x=0.0, y=0.3) | 64.022 | 230.656|
 | Field(x=0.0, y=0.4) | 73.94 | 209.112|
 | Field(x=0.0, y=0.5) | 82.165 | 249.523|
 | Field(x=0.0, y=0.6) | 88.483 | 321.177|
 | Field(x=0.0, y=0.7) | 98.002 | 396.984|
 | Field(x=0.0, y=0.8) | 106.933 | 447.195|
 | Field(x=0.0, y=0.9) | 112.374 | 459.814|
 | Field(x=0.0, y=1.0) | 109.653 | 402.118|
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
* [OpticalBench Compatible Data File, tab delimited](./US003738736_Example01.txt)
* [Zemax file](./US003738736_Example01.zmx)
