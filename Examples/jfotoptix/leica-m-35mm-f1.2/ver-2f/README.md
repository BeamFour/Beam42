## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | -81.01492339969609 | 1.75 | 34.36 | 1.60342 | 38.03 | Ohara | S-TIM5 |
| 2 | 24.30161032333682 | 7.35 | 30.26 | 2.001 | 29.14 | Ohara | S-LAH99 |
| 3 | -1577.1874696410719 | 0.35 | 30.26 |  |  |  |
| 4 | 41.125 | 7.0 | 28.18 | 1.7432 | 49.34 | Ohara | S-LAM60 |
| 5 | -45.769738400769384 | 1.4 | 26.96 | 1.7552 | 27.51 | Ohara | S-TIH4 |
| 6 | 24.409168348702423 | 4.9 | 24.34 |  |  |  |
| 7 | AS | 1.75 | 24.472 |  |  |  |
| 8 | 0.0 | 5.6 | 25.54 | 1.883 | 40.77 | Ohara | S-LAH58 |
| 9 | -25.704681350347578 | 1.4 | 25.54 | 1.738 | 32.26 | Ohara | S-NBH53 |
| 10 | 20.987378802679352 | 8.4 | 25.54 | 1.7432 | 49.34 | Ohara | S-LAM60 |
| 11 | -72.53165038439921 | 0.35 | 25.54 |  |  |  |
| 12 | 27.616744156576953 | 7.0 | 24.14 | 1.95375 | 32.32 | Ohara | S-LAH98 |
| 13 | -34.45299726543552 | 1.4 | 23.36 | 1.64769 | 33.79 | Ohara | S-TIM22 |
| 14 | 22.346724095869824 | 4.55 | 23.02 |  |  |  |
| 15 | -84.20288047344117 | 2.1 | 23.02 | 1.92286 | 18.9 | Ohara | S-NPH2 |
| 16 | -92.67506224163286 | 13.98 | 23.54 |  |  |  |
| 17 | 0.0 | 0.75 | 51.34 | 1.51633 | 64.14 | Ohara | S-BSL7 |
| 18 | 0.0 | 0.85 | 51.34 |  |  |  |
## Aspherical Data
| ID  | Type | k   | P1 | P2 | P3 | P4 | P5 | P6 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 4| EVEN | 0.0 | 0.0 | -1.0604741571932629E-5 | -1.8674751225807893E-8 | -1.9538797515293358E-12 | 0.0 | 0  |
| 11| EVEN | 0.0 | 0.0 | -3.741716989440411E-6 | -1.3303678404164756E-8 | 2.0100178947051555E-11 | 0.0 | 0  |
| 16| EVEN | 0.0 | 0.0 | 2.4454557089143025E-5 | -1.6263019355769125E-8 | 1.062943976087706E-9 | -5.7368666898071825E-12 | 1.5948908761604E-14 |
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
| effective_focal_length |34.577
| back_focal_length | 0.844
| optical_invariant | 8.713
| object_distance | 1.0E10
| image_distance | 0.844
| power | 0.029
| pp1_H | 20.859
| ppk_H' | -33.732
| ffl_F | -13.717
| fno | 1.223
| enp_dist_P | 18.057
| enp_radius | 14.136
| exp_dist_P' | -36.788
| exp_radius | 15.382
| m | -0
| red | -2.892126786428649E8
| n_obj | 1
| n_img | 1
| img_ht | 21.313
| obj_ang | 31.65
| obj_na | 0
| img_na | -0.378|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 15.168 | 46.728|
 | Field(x=0.0, y=0.1) | 13.542 | 50.098|
 | Field(x=0.0, y=0.2) | 12.259 | 49.526|
 | Field(x=0.0, y=0.3) | 12.794 | 52.853|
 | Field(x=0.0, y=0.4) | 14.411 | 62.824|
 | Field(x=0.0, y=0.5) | 16.399 | 71.552|
 | Field(x=0.0, y=0.6) | 17.732 | 74.586|
 | Field(x=0.0, y=0.7) | 18.191 | 67.987|
 | Field(x=0.0, y=0.8) | 19.368 | 54.339|
 | Field(x=0.0, y=0.9) | 26.528 | 60.082|
 | Field(x=0.0, y=1.0) | 37.675 | 109.941|
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
* [Zemax file](./US20250271647_Example02f-optim.zmx)

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-03-06
