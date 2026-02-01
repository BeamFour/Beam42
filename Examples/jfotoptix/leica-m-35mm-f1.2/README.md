## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | -82.425 | 1.75 | 34.36 | 1.60342 | 38.03 | Ohara | S-TIM5 |
| 2 | 24.465 | 7.35 | 30.26 | 2.001 | 29.14 | Ohara | S-LAH99 |
| 3 | -1577.38 | 0.35 | 30.26 |  |  |  |
| 4 | 41.125 | 7.0 | 28.18 | 1.741 | 52.64 | Ohara | S-LAL61 |
| 5 | -47.145 | 1.4 | 26.96 | 1.76 | 28.0 |  |
| 6 | 24.5 | 4.9 | 24.34 |  |  |  |
| 7 | AS | 1.75 | 24.472 |  |  |  |
| 8 | 0.0 | 5.6 | 25.54 | 1.883 | 40.77 | Ohara | S-LAH58 |
| 9 | -23.625 | 1.4 | 25.54 | 1.738 | 32.26 | Ohara | S-NBH53 |
| 10 | 23.135 | 8.4 | 25.54 | 1.741 | 52.64 | Ohara | S-LAL61 |
| 11 | -72.625 | 0.35 | 25.54 |  |  |  |
| 12 | 27.405 | 7.0 | 24.14 | 1.95375 | 32.32 | Ohara | S-LAH98 |
| 13 | -34.65 | 1.4 | 23.36 | 1.6398 | 34.47 | Ohara | S-TIM27 |
| 14 | 21.49 | 4.55 | 23.02 |  |  |  |
| 15 | -80.115 | 2.1 | 23.02 | 1.90366 | 31.34 | Ohara | S-LAH95 |
| 16 | -93.555 | 15.1 | 23.54 |  |  |  |
## Aspherical Data
| ID  | Type | k   | P1 | P2 | P3 | P4 | P5 | P6 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 4| EVEN | 0.0 | 0.0 | -1.08991E-5 | -1.84152E-8 | 0.0 | 0.0 | 0  |
| 11| EVEN | 0.0 | 0.0 | -3.68047E-6 | -1.32097E-8 | 2.13384E-11 | 0.0 | 0  |
| 16| EVEN | 0.0 | 0.0 | 2.36781E-5 | -2.0361E-8 | 1.09176E-9 | -5.73665E-12 | 1.59501E-14 |
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
| effective_focal_length |34.926
| back_focal_length | 15.462
| optical_invariant | 8.815
| object_distance | 1.0E10
| image_distance | 15.462
| power | 0.029
| pp1_H | 20.365
| ppk_H' | -19.465
| ffl_F | -14.562
| fno | 1.238
| enp_dist_P | 18.054
| enp_radius | 14.108
| exp_dist_P' | -21.578
| exp_radius | 15.107
| m | -0
| red | -2.8631751656196946E8
| n_obj | 1
| n_img | 1
| img_ht | 21.824
| obj_ang | 32
| obj_na | 0
| img_na | -0.375|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 82.677 | 120.155|
 | Field(x=0.0, y=0.1) | 82.405 | 123.528|
 | Field(x=0.0, y=0.2) | 79.336 | 128.168|
 | Field(x=0.0, y=0.3) | 73.866 | 127.802|
 | Field(x=0.0, y=0.4) | 67.196 | 122.681|
 | Field(x=0.0, y=0.5) | 60.568 | 117.259|
 | Field(x=0.0, y=0.6) | 55.56 | 114.32|
 | Field(x=0.0, y=0.7) | 55.02 | 118.503|
 | Field(x=0.0, y=0.8) | 63.559 | 167.143|
 | Field(x=0.0, y=0.9) | 85.919 | 231.009|
 | Field(x=0.0, y=1.0) | 106.339 | 381.816|
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
* [Zemax file](./US20250271647_Example02.zmx)

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-02-01
