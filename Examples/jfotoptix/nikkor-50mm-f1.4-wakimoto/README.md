# Nikkor-S Auto 50mm F1.4
## Patent Information
| Country | Patent Number | Example | Year of Application | Inventors | Organisation | Link |
| ---     | ---           | ---     | ---                 | ---       | ---          | ---  |
|US | US3560079 | EX 2 | 1965 | Zenji Wakimoto, Yoshiyuki Simizu | Nikon Corp | [link](https://patents.google.com/patent/US3560079A) |
## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 40.7 | 4.65 | 40.0 | 1.7495 | 34.95 | Schott | LAFN7 |
| 2 | 132.56 | 0.095 | 40.0 |  |  |  |
| 3 | 28.1 | 6.01 | 32.9 | 1.744 | 44.8 | Hikari | J-LAF2 |
| 4 | 135.66 | 3.295 | 31.5 | 1.69895 | 30.13 | Hikari | J-SF15 |
| 5 | 16.705 | 8.165 | 24.6 |  |  |  |
| 6 | AS | 8.5 | 23.9 |  |  |  |
| 7 | -16.135 | 2.325 | 23.3 | 1.71736 | 29.5 | Hoya | E-FD1 |
| 8 | -428.635 | 8.235 | 28.5 | 1.6779 | 55.52 | Hoya | LAC12 |
| 9 | -24.765 | 0.29 | 30.8 |  |  |  |
| 10 | -116.28 | 4.845 | 32.0 | 1.713 | 53.94 | Hoya | LAC8 |
| 11 | -33.05 | 0.095 | 33.5 |  |  |  |
| 12 | 79.455 | 3.1 | 36.0 | 1.713 | 53.94 | Hoya | LAC8 |
| 13 | -445.14 | 37.15 | 36.0 |  |  |  |
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
| effective_focal_length |50.06
| back_focal_length | 37.279
| optical_invariant | 7.573
| object_distance | 1.0E10
| image_distance | 37.279
| power | 0.02
| pp1_H | 50.206
| ppk_H' | -12.782
| ffl_F | 0.146
| fno | 1.403
| enp_dist_P | 26.418
| enp_radius | 17.841
| exp_dist_P' | -57.98
| exp_radius | 33.995
| m | -0
| red | -1.9975857076021975E8
| n_obj | 1
| n_img | 1
| img_ht | 21.249
| obj_ang | 23
| obj_na | 0
| img_na | -0.336|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 46.033 | 131.939|
 | Field(x=0.0, y=0.1) | 42.019 | 146.983|
 | Field(x=0.0, y=0.2) | 44.382 | 158.361|
 | Field(x=0.0, y=0.3) | 54.248 | 218.679|
 | Field(x=0.0, y=0.4) | 65.202 | 259.923|
 | Field(x=0.0, y=0.5) | 75.713 | 265.041|
 | Field(x=0.0, y=0.6) | 82.427 | 323.592|
 | Field(x=0.0, y=0.7) | 90.127 | 419.014|
 | Field(x=0.0, y=0.8) | 96.186 | 449.803|
 | Field(x=0.0, y=0.9) | 101.39 | 448.173|
 | Field(x=0.0, y=1.0) | 101.234 | 398.233|
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
* [Zemax file](./US003560079_Example02.zmx)

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-07-17
