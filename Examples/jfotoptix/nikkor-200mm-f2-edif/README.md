## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 200.0 | 14.0 | 102.05 | 1.49782 | 82.57 | Hikari | J-FKH1 |
| 2 | -540.0 | 0.3 | 102.05 |  |  |  |
| 3 | 112.869 | 15.5 | 97.66 | 1.49782 | 82.57 | Hikari | J-FKH1 |
| 4 | -600.0 | 0.65 | 97.66 |  |  |  |
| 5 | -480.0 | 5.0 | 95.69 | 1.7552 | 27.51 | Hikari | E-SF4 |
| 6 | 431.735 | 41.201 | 95.69 |  |  |  |
| 7 | -386.0 | 7.5 | 70.05 | 1.79504 | 28.54 | Hikari | E-LAF9 |
| 8 | -125.0 | 2.6 | 69.05 | 1.4645 | 65.77 | Hoya | FC3 |
| 9 | 286.185 | 21.5 | 69.05 |  |  |  |
| 10 | -161.2 | 3.4 | 55.9 | 1.4645 | 65.77 | Hoya | FC3 |
| 11 | 67.815 | 22.912 | 53.21 |  |  |  |
| 12 | 171.0 | 6.5 | 53.71 | 1.6935 | 53.2 | Hikari | E-LAK13 |
| 13 | -131.975 | 2.0 | 53.71 |  |  |  |
| 14 | -213.0 | 2.0 | 52.37 | 1.59507 | 35.6 |  |
| 15 | 61.0 | 11.0 | 50.69 | 1.6968 | 55.52 | Hikari | J-LAK14 |
| 16 | -193.237 | 11.2 | 50.69 |  |  |  |
| 17 | AS | 10.8 | 39.998 |  |  |  |
| 18 | -130.0 | 3.0 | 38.64 | 1.4645 | 65.77 | Hoya | FC3 |
| 19 | -311.705 | 65.15 | 35.74 |  |  |  |
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
| effective_focal_length |201.035
| back_focal_length | 65.21
| optical_invariant | 5.319
| object_distance | 1.0E10
| image_distance | 65.21
| power | 0.005
| pp1_H | 83.975
| ppk_H' | -135.824
| ffl_F | -117.06
| fno | 2.04
| enp_dist_P | 401.794
| enp_radius | 49.278
| exp_dist_P' | -12.622
| exp_radius | 19.093
| m | -0
| red | -4.974265499212322E7
| n_obj | 1
| n_img | 1
| img_ht | 21.697
| obj_ang | 6.16
| obj_na | 0
| img_na | -0.238|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 1.987 | 3.516|
 | Field(x=0.0, y=0.1) | 5.247 | 18.893|
 | Field(x=0.0, y=0.2) | 8.448 | 34.514|
 | Field(x=0.0, y=0.3) | 11.255 | 43.449|
 | Field(x=0.0, y=0.4) | 12.73 | 47.679|
 | Field(x=0.0, y=0.5) | 14.478 | 47.642|
 | Field(x=0.0, y=0.6) | 16.335 | 45.364|
 | Field(x=0.0, y=0.7) | 17.313 | 45.615|
 | Field(x=0.0, y=0.8) | 17.312 | 42.443|
 | Field(x=0.0, y=0.9) | 16.148 | 38.281|
 | Field(x=0.0, y=1.0) | 13.452 | 34.363|
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
* [Zemax file](./US004176913_Example02.zmx)

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-03-21
