# Ai Nikkor ED-IF 200mm F2S
## Patent Information
| Country | Patent Number | Example | Year of Application | Inventors | Organisation | Link |
| ---     | ---           | ---     | ---                 | ---       | ---          | ---  |
|US | US4176913 | 2 | 1977 | Soichi Nakamura,Kiyoshi Hayashi | Nippon Kogaku KK | [link](https://patents.google.com/patent/US4176913A/en) |
## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 198.34680191700087 | 14.0 | 102.05 | 1.49782 | 82.57 | Hikari | J-FKH1 |
| 2 | -543.1632445214844 | 0.3 | 102.05 |  |  |  |
| 3 | 112.90472680999295 | 15.5 | 97.66 | 1.49782 | 82.57 | Hikari | J-FKH1 |
| 4 | -590.2386075131246 | 0.65 | 97.66 |  |  |  |
| 5 | -470.9177898300286 | 5.0 | 95.69 | 1.7552 | 27.51 | Hikari | E-SF4 |
| 6 | 428.59617032157286 | 41.201 | 95.69 |  |  |  |
| 7 | -379.8900940572674 | 7.5 | 70.05 | 1.79504 | 28.54 | Hikari | E-LAF9 |
| 8 | -124.76859398828077 | 2.6 | 69.05 | 1.4645 | 65.77 | Hoya | FC3 |
| 9 | 288.37691701092115 | 21.5 | 69.05 |  |  |  |
| 10 | -161.88699764572854 | 3.4 | 55.9 | 1.4645 | 65.77 | Hoya | FC3 |
| 11 | 66.86509267582343 | 22.912 | 53.21 |  |  |  |
| 12 | 167.91310014536583 | 6.5 | 53.71 | 1.6935 | 53.2 | Hikari | E-LAK13 |
| 13 | -129.95324173169246 | 2.0 | 53.71 |  |  |  |
| 14 | -205.54158241577284 | 2.0 | 52.37 | 1.59355 | 35.51 | Schott | TIFN5 |
| 15 | 58.69661505411463 | 11.0 | 50.69 | 1.6968 | 55.52 | Hikari | J-LAK14 |
| 16 | -184.51801112738244 | 11.2 | 50.69 |  |  |  |
| 17 | AS | 10.8 | 39.998 |  |  |  |
| 18 | -122.18062669079758 | 3.0 | 38.64 | 1.4645 | 65.77 | Hoya | FC3 |
| 19 | -318.3333244234881 | 64.81 | 35.74 |  |  |  |
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
| effective_focal_length |199.997
| back_focal_length | 64.858
| optical_invariant | 5.302
| object_distance | 1.0E10
| image_distance | 64.858
| power | 0.005
| pp1_H | 83.845
| ppk_H' | -135.139
| ffl_F | -116.152
| fno | 2.036
| enp_dist_P | 399.898
| enp_radius | 49.126
| exp_dist_P' | -12.604
| exp_radius | 19.039
| m | -0
| red | -5.000068986054931E7
| n_obj | 1
| n_img | 1
| img_ht | 21.585
| obj_ang | 6.16
| obj_na | 0
| img_na | -0.239|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 11.332 | 25.999|
 | Field(x=0.0, y=0.1) | 12.425 | 39.79|
 | Field(x=0.0, y=0.2) | 13.306 | 42.7|
 | Field(x=0.0, y=0.3) | 14.286 | 47.852|
 | Field(x=0.0, y=0.4) | 14.308 | 49.09|
 | Field(x=0.0, y=0.5) | 14.339 | 47.638|
 | Field(x=0.0, y=0.6) | 14.536 | 44.912|
 | Field(x=0.0, y=0.7) | 14.334 | 44.166|
 | Field(x=0.0, y=0.8) | 13.491 | 40.075|
 | Field(x=0.0, y=0.9) | 12.178 | 34.784|
 | Field(x=0.0, y=1.0) | 10.945 | 30.458|
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
* [Zemax file](./US004176913_Example02d.zmx)

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-03-29
