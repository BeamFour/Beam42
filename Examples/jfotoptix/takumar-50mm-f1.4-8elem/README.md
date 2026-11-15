# Asahi Super Takumar 50mm f1.4 (8 elements)
## Patent Information
| Country | Patent Number | Example | Year of Application | Inventors | Organisation | Link |
| ---     | ---           | ---     | ---                 | ---       | ---          | ---  |
|JP | JP-S40-028384 | EX 1 | 1963 | Saburo Matsumoto | Asahi Optical | [link](https://www.j-platpat.inpit.go.jp/c1801/PU/JP-S40-028384/12/en) |
## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 65.0 | 3.75 | 38.48 | 1.8061 | 40.73 | Hoya | NBFD13 |
| 2 | 560.365 | 0.1 | 37.8459 |  |  |  |
| 3 | 22.6 | 5.5 | 32.0 | 1.8061 | 40.73 | Hoya | NBFD13 |
| 4 | 37.9305 | 1.9 | 29.7277 |  |  |  |
| 5 | 42.5 | 1.25 | 27.869 | 1.74077 | 27.76 | Hoya | E-FD13 |
| 6 | 16.2885 | 6.5 | 23.5 |  |  |  |
| 7 | AS | 6.0 | 23.1844 |  |  |  |
| 8 | -18.4 | 1.0 | 22.8631 | 1.74077 | 27.76 | Hoya | E-FD13 |
| 9 | 110.0 | 8.75 | 26.241 | 1.7859 | 43.93 | Hoya | NBFD11 |
| 10 | -16.5 | 1.25 | 26.8397 | 1.70154 | 41.15 | Hoya | BAFD7 |
| 11 | -36.4595 | 0.05 | 29.2609 |  |  |  |
| 12 | -400.0 | 3.25 | 29.8569 | 1.7859 | 43.93 | Hoya | NBFD11 |
| 13 | -44.939 | 0.05 | 30.0 |  |  |  |
| 14 | 300.0 | 2.5 | 30.0 | 1.7859 | 43.93 | Hoya | NBFD11 |
| 15 | -99.2345 | 37.653 | 30.0 |  |  |  |
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
| effective_focal_length |49.998
| back_focal_length | 37.708
| optical_invariant | 7.58
| object_distance | 1.0E10
| image_distance | 37.708
| power | 0.02
| pp1_H | 36.941
| ppk_H' | -12.29
| ffl_F | -13.057
| fno | 1.4
| enp_dist_P | 22.454
| enp_radius | 17.856
| exp_dist_P' | -32.633
| exp_radius | 25.141
| m | -0
| red | -2.0000847578614724E8
| n_obj | 1
| n_img | 1
| img_ht | 21.223
| obj_ang | 23
| obj_na | 0
| img_na | -0.357|
## Spot Analysis
| Field | Spot Mean Radius (µm) | Spot Max Radius (µm) |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 16.294 | 32.479|
 | Field(x=0.0, y=0.1) | 19.271 | 71.821|
 | Field(x=0.0, y=0.2) | 32.342 | 148.537|
 | Field(x=0.0, y=0.3) | 54.465 | 248.518|
 | Field(x=0.0, y=0.4) | 77.239 | 329.072|
 | Field(x=0.0, y=0.5) | 99.053 | 385.252|
 | Field(x=0.0, y=0.6) | 112.295 | 432.475|
 | Field(x=0.0, y=0.7) | 120.518 | 452.562|
 | Field(x=0.0, y=0.8) | 126.978 | 483.846|
 | Field(x=0.0, y=0.9) | 132.002 | 500.71|
 | Field(x=0.0, y=1.0) | 133.211 | 484.93|
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
* [Zemax file](./JP-S40-028384_Example01.zmx)

Generated from `JP-S40-028384_Example01.txt`, status **Accepted**

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-11-15
