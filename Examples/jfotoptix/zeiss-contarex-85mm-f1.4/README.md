# Carl Zeiss f.Contarex 1.4/85mm Planar T*
## Patent Information
| Country | Patent Number | Example | Year of Application | Inventors | Organisation | Link |
| ---     | ---           | ---     | ---                 | ---       | ---          | ---  |
|US | US3948584 | EX 4 | 1973 | Heinrich Basista, Bernhard Sonnenberg | Carl Zeiss AG | [link](https://patents.google.com/patent/US3948584A/en) |
## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 57.902 | 11.0075 | 60.56 | 1.6516 | 58.4 | Hoya | LAC7 |
| 2 | 0.0 | 0.0102 | 58.8962 |  |  |  |
| 3 | 39.542 | 10.404 | 48.8157 | 1.6516 | 58.4 | Hoya | LAC7 |
| 4 | 93.1005 | 2.499 | 42.99 |  |  |  |
| 5 | 440.5125 | 10.71 | 41.9509 | 1.7618 | 26.91 | Hoya | FDS5 |
| 6 | 24.786 | 7.70525 | 27.78 |  |  |  |
| 7 | AS | 7.70525 | 27.0233 |  |  |  |
| 8 | -37.066 | 1.4025 | 29.4991 | 1.57501 | 41.49 | Schott | LF7 |
| 9 | 60.435 | 0.0 | 36.6 |  |  |  |
| 10 | 60.435 | 7.6075 | 36.6 | 1.744 | 44.87 | Hoya | LAF20 |
| 11 | -43.112 | 2.006 | 36.6 |  |  |  |
| 12 | 83.5805 | 5.304 | 36.6 | 1.713 | 53.94 | Hoya | LAC8 |
| 13 | -1000.54 | 38.198 | 36.6 |  |  |  |
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
| effective_focal_length |84.857
| back_focal_length | 38.269
| optical_invariant | 9.084
| object_distance | 1.0E10
| image_distance | 38.269
| power | 0.012
| pp1_H | 31.261
| ppk_H' | -46.588
| ffl_F | -53.596
| fno | 1.7
| enp_dist_P | 61.555
| enp_radius | 24.958
| exp_dist_P' | -24.193
| exp_radius | 18.392
| m | -0
| red | -1.1784501224149634E8
| n_obj | 1
| n_img | 1
| img_ht | 30.886
| obj_ang | 20
| obj_na | 0
| img_na | -0.294|
## Spot Analysis
| Field | Spot Mean Radius (µm) | Spot Max Radius (µm) |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 15.051 | 38.977|
 | Field(x=0.0, y=0.1) | 15.055 | 49.798|
 | Field(x=0.0, y=0.2) | 15.857 | 64.024|
 | Field(x=0.0, y=0.3) | 17.653 | 85.391|
 | Field(x=0.0, y=0.4) | 21.343 | 117.827|
 | Field(x=0.0, y=0.5) | 28.093 | 165.915|
 | Field(x=0.0, y=0.6) | 29.415 | 132.634|
 | Field(x=0.0, y=0.7) | 35.058 | 155.014|
 | Field(x=0.0, y=0.8) | 64.17 | 251.946|
 | Field(x=0.0, y=0.9) | 100.995 | 321.191|
 | Field(x=0.0, y=1.0) | 139.997 | 369.919|
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
* [Zemax file](./US3948584_Example04.zmx)

Generated from `US3948584_Example04.txt`, status **TODO**

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-11-17
