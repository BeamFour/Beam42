## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | -80.26308131628097 | 1.75 | 34.36 | 1.60342 | 38.03 | Ohara | S-TIM5 |
| 2 | 24.474125956052408 | 7.35 | 30.26 | 2.001 | 29.14 | Ohara | S-LAH99 |
| 3 | -1577.3879436306047 | 0.35 | 30.26 |  |  |  |
| 4 | 41.125 | 7.0 | 28.18 | 1.7432 | 49.34 | Ohara | S-LAM60 |
| 5 | -47.11742965014956 | 1.4 | 26.96 | 1.7552 | 27.51 | Ohara | S-TIH4 |
| 6 | 24.569097134887972 | 4.9 | 24.34 |  |  |  |
| 7 | AS | 1.75 | 24.472 |  |  |  |
| 8 | 0.0 | 5.6 | 25.54 | 1.883 | 40.77 | Ohara | S-LAH58 |
| 9 | -23.46779830712738 | 1.4 | 25.54 | 1.738 | 32.26 | Ohara | S-NBH53 |
| 10 | 23.154435930764127 | 8.4 | 25.54 | 1.7432 | 49.34 | Ohara | S-LAM60 |
| 11 | -72.28422548299197 | 0.35 | 25.54 |  |  |  |
| 12 | 27.529677184223385 | 7.0 | 24.14 | 1.95375 | 32.32 | Ohara | S-LAH98 |
| 13 | -34.73001591629172 | 1.4 | 23.36 | 1.64769 | 33.79 | Ohara | S-TIM22 |
| 14 | 21.78368211962422 | 4.55 | 23.02 |  |  |  |
| 15 | -80.7679391637423 | 2.1 | 23.02 | 1.92119 | 23.96 | Hoya | FDS24 |
| 16 | -93.03643432683424 | 13.98 | 23.54 |  |  |  |
| 17 | 0.0 | 0.75 | 51.34 | 1.51633 | 64.14 | Ohara | S-BSL7 |
| 18 | 0.0 | 0.85 | 51.34 |  |  |  |
## Aspherical Data
| ID  | Type | k   | P1 | P2 | P3 | P4 | P5 | P6 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 4| EVEN | 0.0 | 0.0 | -1.0925652481383523E-5 | -1.8374237412229967E-8 | -1.5795584935116463E-12 | 0.0 | 0  |
| 11| EVEN | 0.0 | 0.0 | -3.6809173000093985E-6 | -1.3556194607345106E-8 | 2.119009067236045E-11 | 0.0 | 0  |
| 16| EVEN | 0.0 | 0.0 | 2.414047316795897E-5 | -2.0724973397557147E-8 | 1.0869721661783472E-9 | -5.736652276215599E-12 | 1.595009606707349E-14 |
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
| effective_focal_length |34.787
| back_focal_length | 0.846
| optical_invariant | 8.677
| object_distance | 1.0E10
| image_distance | 0.846
| power | 0.029
| pp1_H | 20.297
| ppk_H' | -33.941
| ffl_F | -14.49
| fno | 1.236
| enp_dist_P | 18.008
| enp_radius | 14.077
| exp_dist_P' | -36.395
| exp_radius | 15.069
| m | -0
| red | -2.874628494590819E8
| n_obj | 1
| n_img | 1
| img_ht | 21.443
| obj_ang | 31.65
| obj_na | 0
| img_na | -0.375|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 19.065 | 50.933|
 | Field(x=0.0, y=0.1) | 18.478 | 57.418|
 | Field(x=0.0, y=0.2) | 17.12 | 57.548|
 | Field(x=0.0, y=0.3) | 17.243 | 54.11|
 | Field(x=0.0, y=0.4) | 18.361 | 51.986|
 | Field(x=0.0, y=0.5) | 20.041 | 59.855|
 | Field(x=0.0, y=0.6) | 21.994 | 64.933|
 | Field(x=0.0, y=0.7) | 24.487 | 72.96|
 | Field(x=0.0, y=0.8) | 29.507 | 82.18|
 | Field(x=0.0, y=0.9) | 39.527 | 111.922|
 | Field(x=0.0, y=1.0) | 48.66 | 174.721|
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
* [Zemax file](./US20250271647_Example02g-optim.zmx)

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-03-06
