## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | -80.43537907082809 | 1.75 | 34.36 | 1.60342 | 38.03 | Ohara | S-TIM5 |
| 2 | 24.436615229663893 | 7.35 | 30.26 | 2.001 | 29.14 | Ohara | S-LAH99 |
| 3 | -1577.4080554174977 | 0.35 | 30.26 |  |  |  |
| 4 | 41.125 | 7.0 | 28.18 | 1.7432 | 49.34 | Ohara | S-LAM60 |
| 5 | -44.591297803465174 | 1.4 | 26.96 | 1.7552 | 27.51 | Ohara | S-TIH4 |
| 6 | 24.281264763025618 | 4.9 | 24.34 |  |  |  |
| 7 | AS | 1.75 | 24.472 |  |  |  |
| 8 | 0.0 | 5.6 | 25.54 | 1.883 | 40.77 | Ohara | S-LAH58 |
| 9 | -23.17428803060984 | 1.4 | 25.54 | 1.738 | 32.26 | Ohara | S-NBH53 |
| 10 | 19.01616875255559 | 8.4 | 25.54 | 1.7432 | 49.34 | Ohara | S-LAM60 |
| 11 | -69.9331032031928 | 0.35 | 25.54 |  |  |  |
| 12 | 27.41750146438315 | 7.0 | 24.14 | 1.95375 | 32.32 | Ohara | S-LAH98 |
| 13 | -35.70496764130632 | 1.4 | 23.36 | 1.64769 | 33.79 | Ohara | S-TIM22 |
| 14 | 21.36650249385509 | 4.55 | 23.02 |  |  |  |
| 15 | -83.56628748872288 | 2.1 | 23.02 | 1.90366 | 31.34 | Ohara | S-LAH95 |
| 16 | -90.60565524159038 | 13.98 | 23.54 |  |  |  |
| 17 | 0.0 | 0.75 | 51.34 | 1.51633 | 64.14 | Ohara | S-BSL7 |
| 18 | 0.0 | 0.85 | 51.34 |  |  |  |
## Aspherical Data
| ID  | Type | k   | P1 | P2 | P3 | P4 | P5 | P6 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 4| EVEN | 0.0 | 0.0 | -1.1102717302000108E-5 | -1.805815325933077E-8 | -7.316846222738769E-13 | 0.0 | 0  |
| 11| EVEN | 0.0 | 0.0 | -3.60630061753672E-6 | -1.3482853898187443E-8 | 2.0010228354338994E-11 | 0.0 | 0  |
| 16| EVEN | 0.0 | 0.0 | 2.3920526940356175E-5 | -1.7355180821392688E-8 | 1.0754357295803E-9 | -5.7366623281612234E-12 | 1.5930131869285965E-14 |
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
| effective_focal_length |34.612
| back_focal_length | 0.834
| optical_invariant | 8.681
| object_distance | 1.0E10
| image_distance | 0.834
| power | 0.029
| pp1_H | 20.594
| ppk_H' | -33.777
| ffl_F | -14.018
| fno | 1.229
| enp_dist_P | 18.007
| enp_radius | 14.084
| exp_dist_P' | -36.588
| exp_radius | 15.221
| m | -0
| red | -2.889174776871476E8
| n_obj | 1
| n_img | 1
| img_ht | 21.335
| obj_ang | 31.65
| obj_na | 0
| img_na | -0.377|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 7.77 | 19.214|
 | Field(x=0.0, y=0.1) | 9.063 | 30.423|
 | Field(x=0.0, y=0.2) | 9.587 | 35.133|
 | Field(x=0.0, y=0.3) | 10.726 | 32.549|
 | Field(x=0.0, y=0.4) | 12.391 | 33.49|
 | Field(x=0.0, y=0.5) | 15.002 | 36.846|
 | Field(x=0.0, y=0.6) | 18.55 | 51.009|
 | Field(x=0.0, y=0.7) | 23.51 | 61.578|
 | Field(x=0.0, y=0.8) | 30.845 | 73.786|
 | Field(x=0.0, y=0.9) | 42.665 | 117.833|
 | Field(x=0.0, y=1.0) | 58.987 | 195.491|
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
* [Zemax file](./US20250271647_Example02d-optim.zmx)

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-03-06
