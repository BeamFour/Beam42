# Leitz / Leica Summilux-R 50mm f1.4 v1
## Patent Information
| Country | Patent Number | Example | Year of Application | Inventors | Organisation | Link |
| ---     | ---           | ---     | ---                 | ---       | ---          | ---  |
|US | US3552829 | EX 1 | 1967 | Heinz Marquardt | Ernst Leitz Wetzlar GmbH | [link](https://patents.google.com/patent/US3552829A/en) |
## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 56.655 | 5.24 | 38.5 | 1.816 | 46.62 | Ohara | S-LAH59 |
| 2 | 600.0 | 0.565 | 37.415 |  |  |  |
| 3 | 25.68 | 6.56 | 32.0396 | 1.69101 | 54.79 | CORNING | C90-55 |
| 4 | 51.875 | 0.8 | 28.716 |  |  |  |
| 5 | 84.6 | 2.255 | 28.7153 | 1.62588 | 35.7 | Ohara | S-TIM1 |
| 6 | 17.295 | 5.865 | 24.1664 |  |  |  |
| 7 | AS | 5.565 | 23.8461 |  |  |  |
| 8 | -19.475 | 2.855 | 24.0 | 1.7847 | 26.08 | Schott | SF56A |
| 9 | 246.02 | 6.185 | 28.5009 | 1.79619 | 43.22 | CORNING | D96-43 |
| 10 | -35.965 | 0.095 | 30.0039 |  |  |  |
| 11 | -125.895 | 4.05 | 30.9406 | 1.79619 | 43.22 | CORNING | D96-43 |
| 12 | -37.625 | 0.095 | 31.5 |  |  |  |
| 13 | 198.13 | 4.05 | 31.5 | 1.713 | 53.83 | Schott | N-LAK8 |
| 14 | -63.025 | 36.412 | 31.5 |  |  |  |
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
| effective_focal_length |49.953
| back_focal_length | 36.486
| optical_invariant | 7.702
| object_distance | 1.0E10
| image_distance | 36.486
| power | 0.02
| pp1_H | 37.178
| ppk_H' | -13.466
| ffl_F | -12.774
| fno | 1.4
| enp_dist_P | 24.078
| enp_radius | 17.84
| exp_dist_P' | -31.15
| exp_radius | 24.182
| m | -0
| red | -2.0018938458346564E8
| n_obj | 1
| n_img | 1
| img_ht | 21.565
| obj_ang | 23.35
| obj_na | 0
| img_na | -0.357|
## Spot Analysis
| Field | Spot Mean Radius (µm) | Spot Max Radius (µm) |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 14.678 | 51.826|
 | Field(x=0.0, y=0.1) | 20.576 | 111.282|
 | Field(x=0.0, y=0.2) | 35.836 | 192.91|
 | Field(x=0.0, y=0.3) | 51.082 | 251.891|
 | Field(x=0.0, y=0.4) | 62.647 | 269.536|
 | Field(x=0.0, y=0.5) | 66.75 | 254.928|
 | Field(x=0.0, y=0.6) | 69.056 | 232.616|
 | Field(x=0.0, y=0.7) | 71.808 | 277.722|
 | Field(x=0.0, y=0.8) | 79.334 | 367.436|
 | Field(x=0.0, y=0.9) | 91.866 | 433.116|
 | Field(x=0.0, y=1.0) | 109.235 | 478.521|
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
* [Zemax file](./US003552829_Example01.zmx)

Generated from `US003552829_Example01.txt`, status **TODO**

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-10-17
