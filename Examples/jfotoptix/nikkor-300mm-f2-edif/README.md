## Patent Information
| Country | Patent Number | Example | Year of Application | Inventors | Organisation | Link |
| ---     | ---           | ---     | ---                 | ---       | ---          | ---  |
|US | US4732459 | 4 | 1983 | Kiyoshi Hayashi | Nikon Corp | [link](https://patents.google.com/patent/US4732459A/en) |
## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 443.373 | 14.5 | 160.7 | 1.49782 | 82.57 | Hikari | J-FKH1 |
| 2 | -745.663 | 0.5 | 160.7 |  |  |  |
| 3 | 235.41 | 21.0 | 154.86 | 1.49782 | 82.57 | Hikari | J-FKH1 |
| 4 | -599.816 | 6.85 | 154.86 |  |  |  |
| 5 | -527.848 | 6.0 | 151.54 | 1.7495 | 35.25 | Hikari | J-LAF7 |
| 6 | 473.787 | 1.7 | 151.54 |  |  |  |
| 7 | 176.9 | 15.0 | 143.2 | 1.49782 | 82.57 | Hikari | J-FKH1 |
| 8 | 558.189 | 102.701 | 143.2 |  |  |  |
| 9 | 749.672 | 8.0 | 77.78 | 1.79504 | 28.69 | Hikari | J-LAFH3 |
| 10 | -189.496 | 3.65 | 76.12 | 1.51454 | 54.7 | Schott | KF3 |
| 11 | 125.271 | 12.0 | 67.78 |  |  |  |
| 12 | -148.765 | 4.8 | 66.12 | 1.4645 | 65.77 | Schott | FK3 |
| 13 | 97.985 | 27.388 | 66.12 |  |  |  |
| 14 | AS | 1.0 | 60.292 |  |  |  |
| 15 | -1754.216 | 1.5 | 65.7 | 1.68893 | 31.16 | Hikari | J-SF8 |
| 16 | 110.0 | 7.75 | 65.7 | 1.6935 | 53.2 | Hikari | J-LAK13 |
| 17 | -317.94 | 2.0 | 65.7 |  |  |  |
| 18 | 169.454 | 2.4 | 64.44 | 1.69895 | 30.13 | Hikari | J-SF15 |
| 19 | 113.525 | 8.0 | 62.78 | 1.6968 | 55.52 | Hikari | J-LAK14 |
| 20 | -254.06 | 112.10174 | 62.78 |  |  |  |
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
| effective_focal_length |300.001
| back_focal_length | 112.222
| optical_invariant | 5.356
| object_distance | 1.0E10
| image_distance | 112.222
| power | 0.003
| pp1_H | 220.349
| ppk_H' | -187.778
| ffl_F | -79.652
| fno | 2
| enp_dist_P | 624.243
| enp_radius | 75.001
| exp_dist_P' | -15.518
| exp_radius | 31.965
| m | -0
| red | -3.3333258442158017E7
| n_obj | 1
| n_img | 1
| img_ht | 21.425
| obj_ang | 4.085
| obj_na | 0
| img_na | -0.243|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 15.491 | 40.119|
 | Field(x=0.0, y=0.1) | 15.874 | 48.206|
 | Field(x=0.0, y=0.2) | 16.107 | 51.373|
 | Field(x=0.0, y=0.3) | 14.543 | 49.922|
 | Field(x=0.0, y=0.4) | 13.186 | 48.114|
 | Field(x=0.0, y=0.5) | 12.076 | 42.358|
 | Field(x=0.0, y=0.6) | 11.347 | 39.568|
 | Field(x=0.0, y=0.7) | 11.273 | 34.188|
 | Field(x=0.0, y=0.8) | 12.089 | 25.548|
 | Field(x=0.0, y=0.9) | 13.767 | 29.608|
 | Field(x=0.0, y=1.0) | 16.052 | 35.952|
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
* [Zemax file](./US004732459_Example04P.zmx)

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-03-20
