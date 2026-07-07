# Leica 50mm Noctilux-M f/1.0 Reverse Engineered
## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 60.3967 | 8.071 | 54.57 | 1.6779 | 55.34 | Ohara | S-LAL12 |
| 2 | 1756.45 | 0.1 | 54.57 |  |  |  |
| 3 | 30.595 | 8.0 | 46.571 | 1.883 | 40.77 | Ohara | S-LAH58 |
| 4 | 67.879 | 1.7857 | 44.644 |  |  |  |
| 5 | 120.79 | 4.0714 | 45.214 | 1.7847 | 26.29 | Ohara | S-TIH23 |
| 6 | 19.787 | 9.35 | 31.6 |  |  |  |
| 7 | AS | 7.1 | 30.6 |  |  |  |
| 8 | -23.04 | 1.357 | 31.0 | 1.72825 | 28.46 | Ohara | S-TIH10 |
| 9 | 90.5275 | 8.7143 | 37.643 | 1.883 | 40.77 | Ohara | S-LAH58 |
| 10 | -31.779 | 0.1 | 37.714 |  |  |  |
| 11 | 90.4594 | 4.0 | 35.286 | 1.788 | 47.37 | Ohara | S-LAH64 |
| 12 | 549.4488 | 0.1 | 35.286 |  |  |  |
| 13 | 80.26486 | 4.0 | 33.429 | 1.788 | 47.37 | Ohara | S-LAH64 |
| 14 | -197.008 | 27.9565 | 33.429 |  |  |  |
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
| effective_focal_length |52.177
| back_focal_length | 28
| optical_invariant | 10.514
| object_distance | 1.0E10
| image_distance | 28
| power | 0.019
| pp1_H | 49.716
| ppk_H' | -24.177
| ffl_F | -2.46
| fno | 1.028
| enp_dist_P | 42.576
| enp_radius | 25.383
| exp_dist_P' | -32.406
| exp_radius | 29.407
| m | -0
| red | -1.9165704016781798E8
| n_obj | 1
| n_img | 1
| img_ht | 21.612
| obj_ang | 22.5
| obj_na | 0
| img_na | -0.437|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 11.936 | 32.938|
 | Field(x=0.0, y=0.1) | 24.156 | 116.198|
 | Field(x=0.0, y=0.2) | 31.963 | 151.617|
 | Field(x=0.0, y=0.3) | 27.587 | 147.521|
 | Field(x=0.0, y=0.4) | 27.148 | 113.575|
 | Field(x=0.0, y=0.5) | 30.3 | 108.739|
 | Field(x=0.0, y=0.6) | 39.246 | 161.814|
 | Field(x=0.0, y=0.7) | 53.509 | 234.624|
 | Field(x=0.0, y=0.8) | 72.215 | 334.65|
 | Field(x=0.0, y=0.9) | 101.5 | 437.506|
 | Field(x=0.0, y=1.0) | 137.902 | 501.591|
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
* [Zemax file](./leica-noctilux-50mm-f1-11a.zmx)

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-07-07
