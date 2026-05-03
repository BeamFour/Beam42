## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 98.253 | 12.0006 | 66.132 | 1.497 | 81.61 | Hoya | FCD1 |
| 2 | -140.0634 | 1.9998 | 66.132 |  |  |  |
| 3 | -138.0564 | 3.7008 | 65.52 | 1.7495 | 34.95 | Schott | LAFN7 |
| 4 | 372.9996 | 5.5008 | 65.52 |  |  |  |
| 5 | 77.7744 | 9.1998 | 61.452 | 1.65844 | 50.85 | Hoya | BACED5 |
| 6 | 241.9992 | 48.9816 | 61.452 |  |  |  |
| 7 | AS | 42.219 | 35.8 |  |  |  |
| 8 | -35.0316 | 1.9998 | 30.708 | 1.51823 | 58.98 | Schott | K3 |
| 9 | -550.0008 | 0.1008 | 30.708 |  |  |  |
| 10 | 221.1966 | 3.9996 | 30.888 | 1.7945 | 45.39 | Hoya | TAF2 |
| 11 | -160.699 | 41.1516 | 30.888 |  |  |  |
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
| effective_focal_length |180.222
| back_focal_length | 41.204
| optical_invariant | 3.836
| object_distance | 1.0E10
| image_distance | 41.204
| power | 0.006
| pp1_H | -88.415
| ppk_H' | -139.019
| ffl_F | -268.638
| fno | 2.801
| enp_dist_P | 130.49
| enp_radius | 32.166
| exp_dist_P' | -40.122
| exp_radius | 14.524
| m | -0
| red | -5.548700481328532E7
| n_obj | 1
| n_img | 1
| img_ht | 21.49
| obj_ang | 6.8
| obj_na | 0
| img_na | -0.176|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 24.295 | 54.403|
 | Field(x=0.0, y=0.1) | 24.94 | 63.644|
 | Field(x=0.0, y=0.2) | 24.316 | 67.256|
 | Field(x=0.0, y=0.3) | 24.272 | 66.943|
 | Field(x=0.0, y=0.4) | 24.465 | 65.216|
 | Field(x=0.0, y=0.5) | 24.831 | 65.751|
 | Field(x=0.0, y=0.6) | 24.335 | 65.01|
 | Field(x=0.0, y=0.7) | 24.277 | 64.408|
 | Field(x=0.0, y=0.8) | 24.157 | 63.35|
 | Field(x=0.0, y=0.9) | 24.301 | 63.105|
 | Field(x=0.0, y=1.0) | 24.102 | 60.685|
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
* [Zemax file](./US004338001_Example01.zmx)

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-05-03
