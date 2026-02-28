## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 408.18 | 13.0 | 112.06 | 1.49782 | 82.57 | Hikari | J-FKH1 |
| 2 | -435.3 | 8.0 | 112.06 |  |  |  |
| 3 | -403.2 | 8.0 | 112.06 | 1.744 | 44.8 | Hikari | J-LAF2 |
| 4 | 445.2 | 2.0 | 112.06 |  |  |  |
| 5 | 396.7 | 10.0 | 112.06 | 1.62374 | 47.01 | Hikari | J-BAF8 |
| 6 | -873.9 | 590.0 | 112.06 |  |  |  |
| 7 | AS | 69.0 | 30.988 |  |  |  |
| 8 | -195.0 | 2.0 | 47.7 | 1.51633 | 64.15 | Ohara | BSL7 |
| 9 | 439.2 | 7.0 | 47.7 | 1.66998 | 39.2 | Schott | BASF12 |
| 10 | -927.5 | 240.21 | 47.7 |  |  |  |
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
| effective_focal_length |1199.474
| back_focal_length | 240.303
| optical_invariant | 0.999
| object_distance | 1.0E10
| image_distance | 240.303
| power | 0.001
| pp1_H | -1283.284
| ppk_H' | -959.171
| ffl_F | -2482.758
| fno | 11.004
| enp_dist_P | 2178.769
| enp_radius | 54.501
| exp_dist_P' | -68.245
| exp_radius | 14.024
| m | -0
| red | -8336985.784082128
| n_obj | 1
| n_img | 1
| img_ht | 21.984
| obj_ang | 1.05
| obj_na | 0
| img_na | -0.045|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 7.309 | 19.3|
 | Field(x=0.0, y=0.1) | 7.496 | 23.905|
 | Field(x=0.0, y=0.2) | 7.373 | 24.184|
 | Field(x=0.0, y=0.3) | 7.566 | 24.415|
 | Field(x=0.0, y=0.4) | 7.678 | 24.621|
 | Field(x=0.0, y=0.5) | 7.766 | 24.791|
 | Field(x=0.0, y=0.6) | 7.934 | 24.933|
 | Field(x=0.0, y=0.7) | 8.195 | 25.05|
 | Field(x=0.0, y=0.8) | 8.522 | 25.313|
 | Field(x=0.0, y=0.9) | 8.829 | 25.515|
 | Field(x=0.0, y=1.0) | 9.271 | 25.695|
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

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-02-28
