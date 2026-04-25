# Leica APO 75mm F2 Walter Mandler
## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 28.790197061851032 | 9.25 | 43.5 | 1.5522 | 67.06 | CORNING | B52-67 |
| 2 | 128.9271684921586 | 0.1 | 40.0 |  |  |  |
| 3 | 27.64913526595679 | 8.0 | 34.0 | 1.5522 | 67.06 | CORNING | B52-67 |
| 4 | -120.04636508006878 | 2.3 | 34.0 | 1.6134 | 44.29 | Schott | KZFSN4 |
| 5 | 14.478186598514508 | 3.0 | 23.0 |  |  |  |
| 6 | 19.102771623401654 | 5.25 | 24.0 | 1.5522 | 67.06 | CORNING | B52-67 |
| 7 | 21.662297959312568 | 3.75 | 20.0 |  |  |  |
| 8 | AS | 3.75 | 19.446 |  |  |  |
| 9 | -26.42625115622753 | 5.75 | 20.0 | 1.5522 | 67.06 | CORNING | B52-67 |
| 10 | -26.59227370910273 | 1.5 | 20.0 |  |  |  |
| 11 | -16.597900996704112 | 3.0 | 19.0 | 1.6134 | 44.29 | Schott | KZFSN4 |
| 12 | -141.9909284764685 | 6.5 | 24.0 | 1.5522 | 67.06 | CORNING | B52-67 |
| 13 | -20.324067896160823 | 0.25 | 24.0 |  |  |  |
| 14 | 87.76269056596293 | 6.0 | 30.0 | 1.5522 | 67.06 | CORNING | B52-67 |
| 15 | -69.39343410646416 | 39.38 | 30.0 |  |  |  |
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
| effective_focal_length |74.852
| back_focal_length | 39.422
| optical_invariant | 5.279
| object_distance | 1.0E10
| image_distance | 39.422
| power | 0.013
| pp1_H | 47.032
| ppk_H' | -35.43
| ffl_F | -27.82
| fno | 2.033
| enp_dist_P | 45.894
| enp_radius | 18.409
| exp_dist_P' | -36.544
| exp_radius | 18.693
| m | -0
| red | -1.3359615074229608E8
| n_obj | 1
| n_img | 1
| img_ht | 21.464
| obj_ang | 16
| obj_na | 0
| img_na | -0.239|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 9.86 | 23.804|
 | Field(x=0.0, y=0.1) | 11.902 | 57.92|
 | Field(x=0.0, y=0.2) | 15.971 | 75.679|
 | Field(x=0.0, y=0.3) | 18.005 | 78.203|
 | Field(x=0.0, y=0.4) | 18.643 | 75.465|
 | Field(x=0.0, y=0.5) | 19.297 | 82.987|
 | Field(x=0.0, y=0.6) | 21.666 | 93.246|
 | Field(x=0.0, y=0.7) | 24.712 | 120.268|
 | Field(x=0.0, y=0.8) | 28.84 | 154.661|
 | Field(x=0.0, y=0.9) | 33.235 | 175.033|
 | Field(x=0.0, y=1.0) | 37.137 | 181.46|
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
