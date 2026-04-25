# Nikkor-N 5cm f1.1
## Patent Information
| Country | Patent Number | Example | Year of Application | Inventors | Organisation | Link |
| ---     | ---           | ---     | ---                 | ---       | ---          | ---  |
|US | US2828671 | 1 | 1957 | Murakami Saburo | Nippon Kogaku KK | [link](https://patents.google.com/patent/US2828671A/en) |
## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 83.8 | 4.35 | 52.62 | 1.60729 | 59.46 | Schott | SK7 |
| 2 | 214.15 | 0.3 | 52.62 |  |  |  |
| 3 | 46.5 | 4.25 | 45.39 | 1.60729 | 59.46 | Schott | SK7 |
| 4 | 81.9 | 0.7 | 45.39 |  |  |  |
| 5 | 26.75 | 9.7 | 39.75 | 1.717 | 47.97 | Hikari | J-LAF3 |
| 6 | 435.9 | 2.05 | 39.75 | 1.5927 | 35.45 | Hoya | FF5 |
| 7 | 16.65 | 7.5 | 27.512 |  |  |  |
| 8 | AS | 5.1 | 27.367 |  |  |  |
| 9 | -21.3 | 2.7 | 27.304 | 1.64831 | 33.84 | Schott | SF12 |
| 10 | 67.85 | 10.25 | 31.89 | 1.717 | 47.97 | Hikari | J-LAF3 |
| 11 | -29.05 | 0.3 | 31.89 |  |  |  |
| 12 | 58.15 | 5.5 | 32.14 | 1.717 | 47.97 | Hikari | J-LAF3 |
| 13 | 0.0 | 0.3 | 32.14 |  |  |  |
| 14 | 71.15 | 1.45 | 32.7 | 1.6259 | 35.61 | CORNING | C26-36 |
| 15 | 48.45 | 3.9 | 32.7 | 1.63854 | 55.34 | Hikari | J-SK18 |
| 16 | 258.95 | 22.94 | 32.7 |  |  |  |
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
| effective_focal_length |49.927
| back_focal_length | 23.055
| optical_invariant | 9.646
| object_distance | 1.0E10
| image_distance | 23.055
| power | 0.02
| pp1_H | 42.086
| ppk_H' | -26.872
| ffl_F | -7.841
| fno | 1.099
| enp_dist_P | 35.981
| enp_radius | 22.725
| exp_dist_P' | -33.713
| exp_radius | 25.89
| m | -0
| red | -2.002925651547201E8
| n_obj | 1
| n_img | 1
| img_ht | 21.193
| obj_ang | 23
| obj_na | 0
| img_na | -0.414|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 52.706 | 137.521|
 | Field(x=0.0, y=0.1) | 58.28 | 237.149|
 | Field(x=0.0, y=0.2) | 73.12 | 365.661|
 | Field(x=0.0, y=0.3) | 89.982 | 510.957|
 | Field(x=0.0, y=0.4) | 106.854 | 552.044|
 | Field(x=0.0, y=0.5) | 122.526 | 522.023|
 | Field(x=0.0, y=0.6) | 121.089 | 556.472|
 | Field(x=0.0, y=0.7) | 118.967 | 528.486|
 | Field(x=0.0, y=0.8) | 122.576 | 575.693|
 | Field(x=0.0, y=0.9) | 127.779 | 636.887|
 | Field(x=0.0, y=1.0) | 126.872 | 595.342|
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
* [Zemax file](./US002828671_Example01.zmx)

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-04-25
