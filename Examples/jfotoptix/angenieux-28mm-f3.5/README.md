# Angenieux 28mm f3.5 R11
## Patent Information
| Country | Patent Number | Example | Year of Application | Inventors | Organisation | Link |
| ---     | ---           | ---     | ---                 | ---       | ---          | ---  |
|US | US2696758 | 1 | 1950 | Pierre Angenieux | Angenieux | [link](https://patents.google.com/patent/US2696758A/en) |
## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 95.52 | 5.87 | 51.6 | 1.6751 | 32.3 |  |
| 2 | 805.67 | 0.3 | 48.46 |  |  |  |
| 3 | 98.18 | 1.47 | 41.48 | 1.62041 | 60.32 | Schott | SK16 |
| 4 | 18.66 | 31.21 | 30.64 |  |  |  |
| 5 | 37.99 | 4.4 | 14.62 | 1.62041 | 60.32 | Schott | SK16 |
| 6 | -90.62 | 0.15 | 13.66 |  |  |  |
| 7 | 20.36 | 4.29 | 14.0 | 1.62041 | 60.32 | Schott | SK16 |
| 8 | 55.25 | 1.37 | 11.46 |  |  |  |
| 9 | AS | 1.37 | 10.871 |  |  |  |
| 10 | -23.44 | 0.62 | 10.86 | 1.6287 | 35.3 |  |
| 11 | 18.18 | 1.97 | 11.74 |  |  |  |
| 12 | 90.62 | 2.49 | 10.94 | 1.62041 | 60.32 | Schott | SK16 |
| 13 | -15.14 | 37.62 | 12.0 |  |  |  |
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
| effective_focal_length |28.277
| back_focal_length | 37.621
| optical_invariant | 3.069
| object_distance | 1.0E10
| image_distance | 37.621
| power | 0.035
| pp1_H | 38.848
| ppk_H' | 9.343
| ffl_F | 10.57
| fno | 3.535
| enp_dist_P | 28.647
| enp_radius | 4
| exp_dist_P' | -6.614
| exp_radius | 6.257
| m | -0
| red | -3.536383024394787E8
| n_obj | 1
| n_img | 1
| img_ht | 21.698
| obj_ang | 37.5
| obj_na | 0
| img_na | -0.14|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 13.769 | 39.852|
 | Field(x=0.0, y=0.1) | 15.883 | 78.829|
 | Field(x=0.0, y=0.2) | 22.049 | 93.937|
 | Field(x=0.0, y=0.3) | 29.01 | 104.823|
 | Field(x=0.0, y=0.4) | 34.008 | 115.129|
 | Field(x=0.0, y=0.5) | 36.659 | 122.794|
 | Field(x=0.0, y=0.6) | 37.706 | 129.541|
 | Field(x=0.0, y=0.7) | 39.822 | 145.928|
 | Field(x=0.0, y=0.8) | 45.409 | 164.973|
 | Field(x=0.0, y=0.9) | 58.285 | 197.433|
 | Field(x=0.0, y=1.0) | 81.862 | 273.953|
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
* [Zemax file](./US002696758_Example01.zmx)

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-07-15
