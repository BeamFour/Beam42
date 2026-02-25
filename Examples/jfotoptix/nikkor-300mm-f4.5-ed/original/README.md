# Nikkor 300mm f4.5 ED
## Patent Information
| Country | Patent Number | Example | Year of Application | Inventors | Organisation | Link |
| ---     | ---           | ---     | ---                 | ---       | ---          | ---  |
|US | US3774991 | EX 1 | 1971 | Y Shimizu | Nikon Corp | [link](https://patents.google.com/patent/US3774991A/en) |
## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 106.0 | 11.0 | 69.02 | 1.48606 | 81.49 | Schott | FK50 |
| 2 | -151.0 | 4.0 | 69.02 |  |  |  |
| 3 | -144.0 | 3.0 | 65.74 | 1.744 | 44.8 | Hikari | J-LAF2 |
| 4 | 130.0 | 12.0 | 65.54 | 1.6393 | 45.0 | Schott | BAF12-Old |
| 5 | 650.0 | 1.0 | 64.92 |  |  |  |
| 6 | 95.0 | 7.7 | 64.92 | 1.48606 | 81.49 | Schott | FK50 |
| 7 | 878.8 | 69.3 | 64.92 |  |  |  |
| 8 | AS | 50.0 | 34.722 |  |  |  |
| 9 | -43.0 | 1.0 | 29.2 | 1.62041 | 60.25 | Hikari | J-SK16 |
| 10 | 150.0 | 3.5 | 29.2 | 1.62004 | 36.4 | Hikari | J-F2 |
| 11 | -104.82 | 70.05 | 29.2 |  |  |  |
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
| effective_focal_length |300
| back_focal_length | 70.053
| optical_invariant | 2.381
| object_distance | 1.0E10
| image_distance | 70.053
| power | 0.003
| pp1_H | -339.303
| ppk_H' | -229.946
| ffl_F | -639.303
| fno | 4.5
| enp_dist_P | 184.466
| enp_radius | 33.335
| exp_dist_P' | -39.197
| exp_radius | 12.14
| m | -0
| red | -3.3333376044074573E7
| n_obj | 1
| n_img | 1
| img_ht | 21.425
| obj_ang | 4.085
| obj_na | 0
| img_na | -0.11|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 3.141 | 9.896|
 | Field(x=0.0, y=0.1) | 3.678 | 12.302|
 | Field(x=0.0, y=0.2) | 3.775 | 12.777|
 | Field(x=0.0, y=0.3) | 4.004 | 13.544|
 | Field(x=0.0, y=0.4) | 4.449 | 14.606|
 | Field(x=0.0, y=0.5) | 5.145 | 15.95|
 | Field(x=0.0, y=0.6) | 6.082 | 17.558|
 | Field(x=0.0, y=0.7) | 7.2 | 20.781|
 | Field(x=0.0, y=0.8) | 8.512 | 24.838|
 | Field(x=0.0, y=0.9) | 10.027 | 29.448|
 | Field(x=0.0, y=1.0) | 11.733 | 34.578|
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
* [Zemax file](./US3774991_Example01.zmx)

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-02-25
