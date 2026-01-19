# Canon EF 35mm f2
## Patent Information
| Country | Patent Number | Example | Year of Application | Inventors | Organisation | Link |
| ---     | ---           | ---     | ---                 | ---       | ---          | ---  |
|JP | JP,04-015611,A(1992) | EX 1 | 1990 | TANAKA TSUNEFUMI | Canon Inc | [link](https://www.j-platpat.inpit.go.jp/c1801/PU/JP-H04-015611/11/en) |
## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 28.25 | 1.5 | 30.03 | 1.58913 | 61.14 | Ohara | S-BAL35 |
| 2 | 15.79 | 15.28 | 24.94 |  |  |  |
| 3 | 37.84 | 10.54 | 24.58 | 1.8061 | 40.93 | Ohara | S-LAH53 |
| 4 | -19.77 | 1.53 | 24.58 | 1.64769 | 33.79 | Ohara | S-TIM22 |
| 5 | -142.05 | 3.09 | 24.58 |  |  |  |
| 6 | AS | 3.54 | 19.759 |  |  |  |
| 7 | -27.54 | 1.94 | 19.93 | 1.6727 | 32.1 | Ohara | S-TIM25 |
| 8 | 45.81 | 2.0 | 19.05 |  |  |  |
| 9 | -46.16 | 1.1 | 19.05 | 1.78472 | 25.68 | Ohara | S-TIH11 |
| 10 | 276.83 | 3.58 | 19.93 | 1.7725 | 49.6 | Ohara | S-LAH66 |
| 11 | -24.15 | 0.1 | 20.35 |  |  |  |
| 12 | 134.67 | 3.2 | 22.51 | 1.7725 | 49.6 | Ohara | S-LAH66 |
| 13 | -46.19 | 37.74 | 22.51 |  |  |  |
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
| effective_focal_length |35.508
| back_focal_length | 37.828
| optical_invariant | 5.407
| object_distance | 1.0E10
| image_distance | 37.828
| power | 0.028
| pp1_H | 33.988
| ppk_H' | 2.32
| ffl_F | -1.52
| fno | 2
| enp_dist_P | 21.091
| enp_radius | 8.875
| exp_dist_P' | -17.844
| exp_radius | 13.937
| m | -0
| red | -2.8162798071719176E8
| n_obj | 1
| n_img | 1
| img_ht | 21.632
| obj_ang | 31.35
| obj_na | 0
| img_na | -0.242|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 6.363 | 15.963|
 | Field(x=0.0, y=0.1) | 6.53 | 23.522|
 | Field(x=0.0, y=0.2) | 7.251 | 33.524|
 | Field(x=0.0, y=0.3) | 7.672 | 28.525|
 | Field(x=0.0, y=0.4) | 10.525 | 40.447|
 | Field(x=0.0, y=0.5) | 16.608 | 63.469|
 | Field(x=0.0, y=0.6) | 26.271 | 96.489|
 | Field(x=0.0, y=0.7) | 37.255 | 142.163|
 | Field(x=0.0, y=0.8) | 52.879 | 204.637|
 | Field(x=0.0, y=0.9) | 77.441 | 289.687|
 | Field(x=0.0, y=1.0) | 109.099 | 405.152|
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
* [OpticalBench Compatible Data File, tab delimited](./JP1992-015611_Example01P.txt)
* [Zemax file](./JP1992-015611_Example01P.zmx)
