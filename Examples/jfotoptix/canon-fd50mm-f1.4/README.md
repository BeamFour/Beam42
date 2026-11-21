# Canon New FD 50mm f1.4
## Patent Information
| Country | Patent Number | Example | Year of Application | Inventors | Organisation | Link |
| ---     | ---           | ---     | ---                 | ---       | ---          | ---  |
|US | US4247171 | 3 | 1977 | Sadahiko Tsuji | Canon Inc | [link](https://patents.google.com/patent/US4247171A/en) |
## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 34.3705 | 5.525 | 38.96 | 1.7725 | 49.6 | Ohara | S-LAH66 |
| 2 | 153.5085 | 0.1455 | 38.3526 |  |  |  |
| 3 | 23.574 | 4.37 | 31.2238 | 1.6935 | 53.21 | Ohara | S-LAL13 |
| 4 | 36.529 | 1.081 | 28.94 |  |  |  |
| 5 | 55.6315 | 1.744 | 28.9318 | 1.64769 | 33.79 | Ohara | S-TIM22 |
| 6 | 15.334 | 7.0205 | 23.7459 |  |  |  |
| 7 | AS | 8.585 | 23.4587 |  |  |  |
| 8 | -16.636 | 1.938 | 23.2516 | 1.7552 | 27.51 | Ohara | S-TIH4 |
| 9 | -175.325 | 6.1825 | 27.7097 | 1.7725 | 49.6 | Ohara | S-LAH66 |
| 10 | -28.526 | 0.194 | 29.6641 |  |  |  |
| 11 | -88.505 | 3.8705 | 31.0531 | 1.7725 | 49.6 | Ohara | S-LAH66 |
| 12 | -28.6785 | 0.194 | 31.3021 |  |  |  |
| 13 | 112.5865 | 2.5445 | 29.88 | 1.7725 | 49.6 | Ohara | S-LAH66 |
| 14 | -192.7245 | 35.135 | 29.88 |  |  |  |
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
| effective_focal_length |50.001
| back_focal_length | 35.195
| optical_invariant | 7.473
| object_distance | 1.0E10
| image_distance | 35.195
| power | 0.02
| pp1_H | 41.698
| ppk_H' | -14.806
| ffl_F | -8.303
| fno | 1.42
| enp_dist_P | 24.398
| enp_radius | 17.606
| exp_dist_P' | -41.198
| exp_radius | 26.92
| m | -0
| red | -1.9999714320349154E8
| n_obj | 1
| n_img | 1
| img_ht | 21.224
| obj_ang | 23
| obj_na | 0
| img_na | -0.352|
## Spot Analysis
| Field | Spot Mean Radius (µm) | Spot Max Radius (µm) |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 16.435 | 55.78|
 | Field(x=0.0, y=0.1) | 22.813 | 113.544|
 | Field(x=0.0, y=0.2) | 37.649 | 196.333|
 | Field(x=0.0, y=0.3) | 46.691 | 209.337|
 | Field(x=0.0, y=0.4) | 50.322 | 212.365|
 | Field(x=0.0, y=0.5) | 53.636 | 230.782|
 | Field(x=0.0, y=0.6) | 57.449 | 275.389|
 | Field(x=0.0, y=0.7) | 64.548 | 321.577|
 | Field(x=0.0, y=0.8) | 69.213 | 323.171|
 | Field(x=0.0, y=0.9) | 64.299 | 280.548|
 | Field(x=0.0, y=1.0) | 51.012 | 185.256|
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
* [Zemax file](./US004247171_Example03.zmx)

Generated from `US004247171_Example03.txt`, status **Accepted**

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-11-21
