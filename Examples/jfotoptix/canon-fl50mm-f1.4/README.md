# Canon FL 50mm f1.4 v2
## Patent Information
| Country | Patent Number | Example | Year of Application | Inventors | Organisation | Link |
| ---     | ---           | ---     | ---                 | ---       | ---          | ---  |
|JP | JP 48-010083 | EX 1 | 1967 | Kikuo Momiyama | Canon Inc  | [link](https://www.j-platpat.inpit.go.jp/c1801/PU/JP-S48-010083/12/en) |
## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 35.375 | 6.4 | 42.0 | 1.62041 | 60.25 | Hikari | J-SK16 |
| 2 | 182.85 | 0.15 | 42.0 |  |  |  |
| 3 | 25.415 | 4.35 | 33.43 | 1.6935 | 53.21 | Ohara | S-LAL13 |
| 4 | 44.595 | 0.7 | 33.43 |  |  |  |
| 5 | 58.0 | 4.35 | 31.19 | 1.58215 | 42.03 | Hoya | FL3 |
| 6 | 14.47 | 8.82 | 23.26 |  |  |  |
| 7 | AS | 7.48 | 22.637 |  |  |  |
| 8 | -14.62 | 1.0 | 22.54 | 1.7552 | 27.57 | Hikari | J-SF4 |
| 9 | 740.0 | 5.8 | 29.44 | 1.6935 | 53.21 | Ohara | S-LAL13 |
| 10 | -24.035 | 0.15 | 29.44 |  |  |  |
| 11 | -72.5 | 4.45 | 31.62 | 1.8061 | 40.93 | Ohara | S-LAH53 |
| 12 | -25.02 | 0.15 | 31.62 |  |  |  |
| 13 | 104.0 | 3.0 | 32.59 | 1.6935 | 53.21 | Ohara | S-LAL13 |
| 14 | -129.725 | 35.435 | 32.59 |  |  |  |
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
| effective_focal_length |50
| back_focal_length | 35.49
| optical_invariant | 7.58
| object_distance | 1.0E10
| image_distance | 35.49
| power | 0.02
| pp1_H | 48.732
| ppk_H' | -14.51
| ffl_F | -1.269
| fno | 1.4
| enp_dist_P | 32.5
| enp_radius | 17.857
| exp_dist_P' | -38.488
| exp_radius | 26.44
| m | -0
| red | -1.999993859354017E8
| n_obj | 1
| n_img | 1
| img_ht | 21.224
| obj_ang | 23
| obj_na | 0
| img_na | -0.336|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 18.014 | 46.518|
 | Field(x=0.0, y=0.1) | 21.976 | 75.304|
 | Field(x=0.0, y=0.2) | 40.28 | 167.674|
 | Field(x=0.0, y=0.3) | 51.12 | 224.88|
 | Field(x=0.0, y=0.4) | 64.091 | 192.253|
 | Field(x=0.0, y=0.5) | 78.652 | 256.97|
 | Field(x=0.0, y=0.6) | 86.789 | 354.954|
 | Field(x=0.0, y=0.7) | 97.583 | 462.756|
 | Field(x=0.0, y=0.8) | 113.441 | 566.314|
 | Field(x=0.0, y=0.9) | 127.77 | 657.736|
 | Field(x=0.0, y=1.0) | 135.068 | 630.835|
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
* [OpticalBench Compatible Data File, tab delimited](./JP1973-010083_Example01.txt)
* [Zemax file](./JP1973-010083_Example01.zmx)
