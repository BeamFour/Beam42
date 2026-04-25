# Leica APO 75mm F2 Walter Mandler
## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 28.8597022829186 | 9.25 | 43.5 | 1.5522 | 67.06 | CORNING | B52-67 |
| 2 | 129.05471675699664 | 0.1 | 40.0 |  |  |  |
| 3 | 27.592697215866703 | 8.0 | 34.0 | 1.5522 | 67.06 | CORNING | B52-67 |
| 4 | -120.07963084031654 | 2.3 | 34.0 | 1.6134 | 44.29 | Schott | KZFSN4 |
| 5 | 14.533551486939635 | 3.0 | 23.0 |  |  |  |
| 6 | 19.20807199328744 | 5.25 | 24.0 | 1.5522 | 67.06 | CORNING | B52-67 |
| 7 | 21.513485885524403 | 3.75 | 20.0 |  |  |  |
| 8 | AS | 3.75 | 19.446 |  |  |  |
| 9 | -26.683444934278203 | 5.75 | 20.0 | 1.5522 | 67.06 | CORNING | B52-67 |
| 10 | -26.48259771844526 | 1.5 | 20.0 |  |  |  |
| 11 | -16.610844059649427 | 3.0 | 19.0 | 1.6134 | 44.29 | Schott | KZFSN4 |
| 12 | -141.99239809067916 | 6.5 | 24.0 | 1.5522 | 67.06 | CORNING | B52-67 |
| 13 | -20.382784792811346 | 0.25 | 24.0 |  |  |  |
| 14 | 87.77903859339958 | 6.0 | 30.0 | 1.5522 | 67.06 | CORNING | B52-67 |
| 15 | -69.38726068189341 | 39.38 | 30.0 |  |  |  |
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
| effective_focal_length |74.811
| back_focal_length | 39.463
| optical_invariant | 5.275
| object_distance | 1.0E10
| image_distance | 39.463
| power | 0.013
| pp1_H | 47.047
| ppk_H' | -35.348
| ffl_F | -27.764
| fno | 2.033
| enp_dist_P | 45.868
| enp_radius | 18.395
| exp_dist_P' | -36.463
| exp_radius | 18.689
| m | -0
| red | -1.3367076291457628E8
| n_obj | 1
| n_img | 1
| img_ht | 21.452
| obj_ang | 16
| obj_na | 0
| img_na | -0.239|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 4.998 | 12.985|
 | Field(x=0.0, y=0.1) | 11.154 | 51.227|
 | Field(x=0.0, y=0.2) | 17.51 | 74.627|
 | Field(x=0.0, y=0.3) | 20.515 | 84|
 | Field(x=0.0, y=0.4) | 20.865 | 77.744|
 | Field(x=0.0, y=0.5) | 20.536 | 85.853|
 | Field(x=0.0, y=0.6) | 21.523 | 96.623|
 | Field(x=0.0, y=0.7) | 23.633 | 116.424|
 | Field(x=0.0, y=0.8) | 27.598 | 152.745|
 | Field(x=0.0, y=0.9) | 33.533 | 176.179|
 | Field(x=0.0, y=1.0) | 39.504 | 185.5|
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
* [Zemax file](./specs.zmx)

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-04-25
