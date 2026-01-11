## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 29.0 | 4.75 | 30.6 | 1.62041 | 60.29 | Ohara | S-BSM16 |
| 2 | 190.0 | 0.25 | 30.6 |  |  |  |
| 3 | 17.85 | 7.25 | 26.16 | 1.6261 | 39.1 |  |
| 4 | -105.0 | 2.2 | 26.16 | 1.74 | 28.3 | Ohara | S-TIH3 |
| 5 | 12.05 | 4.4 | 17.63 |  |  |  |
| 6 | AS | 2.2 | 17.549 |  |  |  |
| 7 | -28.125 | 1.75 | 17.51 | 1.5014 | 56.5 |  |
| 8 | 21.9 | 8.25 | 23.06 | 1.63854 | 55.38 | Ohara | S-BSM18 |
| 9 | -40.0 | 0.15 | 23.06 |  |  |  |
| 10 | 85.0 | 3.5 | 25.0 | 1.63854 | 55.38 | Ohara | S-BSM18 |
| 11 | -62.85 | 22.62 | 25.0 |  |  |  |
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
| effective_focal_length |44.859
| back_focal_length | 22.819
| optical_invariant | 5.289
| object_distance | 1.0E10
| image_distance | 22.819
| power | 0.022
| pp1_H | 15.696
| ppk_H' | -22.04
| ffl_F | -29.163
| fno | 1.8
| enp_dist_P | 24.542
| enp_radius | 12.461
| exp_dist_P' | -14.452
| exp_radius | 10.408
| m | -0
| red | -2.2292179215701133E8
| n_obj | 1
| n_img | 1
| img_ht | 19.041
| obj_ang | 23
| obj_na | 0
| img_na | -0.268|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 69.256 | 110.129|
 | Field(x=0.0, y=0.1) | 67.181 | 126.436|
 | Field(x=0.0, y=0.2) | 64.157 | 129.627|
 | Field(x=0.0, y=0.3) | 61.137 | 148.194|
 | Field(x=0.0, y=0.4) | 68.114 | 270.6|
 | Field(x=0.0, y=0.5) | 90.279 | 425.679|
 | Field(x=0.0, y=0.6) | 113.868 | 546.256|
 | Field(x=0.0, y=0.7) | 102.074 | 420.464|
 | Field(x=0.0, y=0.8) | 80.444 | 250.828|
 | Field(x=0.0, y=0.9) | 59 | 162.778|
 | Field(x=0.0, y=1.0) | 49.918 | 136.586|
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
* [OpticalBench Compatible Data File, tab delimited](./US002681594_Example01.txt)
* [Zemax file](./US002681594_Example01.zmx)
