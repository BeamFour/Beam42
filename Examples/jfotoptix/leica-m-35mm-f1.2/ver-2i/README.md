## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | -81.89479080676603 | 1.75 | 34.36 | 1.59551 | 39.21 | Hikari | J-F8 |
| 2 | 24.780273573837057 | 7.35 | 30.26 | 2.001 | 29.14 | Ohara | S-LAH99 |
| 3 | -1577.3833848706697 | 0.35 | 30.26 |  |  |  |
| 4 | 41.125 | 7.0 | 28.18 | 1.7432 | 49.34 | Ohara | S-LAM60 |
| 5 | -47.07305547147899 | 1.4 | 26.96 | 1.7552 | 27.51 | Ohara | S-TIH4 |
| 6 | 24.814205369406505 | 4.9 | 24.34 |  |  |  |
| 7 | AS | 1.75 | 24.472 |  |  |  |
| 8 | 0.0 | 5.6 | 25.54 | 1.883 | 40.77 | Ohara | S-LAH58 |
| 9 | -22.87999602590484 | 1.4 | 25.54 | 1.738 | 32.26 | Ohara | S-NBH53 |
| 10 | 23.231167585068725 | 8.4 | 25.54 | 1.7432 | 49.34 | Ohara | S-LAM60 |
| 11 | -72.01550060079663 | 0.35 | 25.54 |  |  |  |
| 12 | 27.580395813554393 | 7.0 | 24.14 | 1.95375 | 32.32 | Ohara | S-LAH98 |
| 13 | -35.32722858664242 | 1.4 | 23.36 | 1.64769 | 33.79 | Ohara | S-TIM22 |
| 14 | 21.52541564867839 | 4.55 | 23.02 |  |  |  |
| 15 | -80.96669168781543 | 2.1 | 23.02 | 1.92286 | 18.9 | Ohara | S-NPH2 |
| 16 | -92.96710476797219 | 13.41 | 23.54 |  |  |  |
| 17 | 0.0 | 0.75 | 51.34 | 1.51633 | 64.14 | Ohara | S-BSL7 |
| 18 | 0.0 | 0.85 | 51.34 |  |  |  |
## Aspherical Data
| ID  | Type | k   | P1 | P2 | P3 | P4 | P5 | P6 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 4| EVEN | 0.0 | 0.0 | -1.1050838494391566E-5 | -1.8310680312835907E-8 | 1.1053416633689078E-12 | 0.0 | 0  |
| 11| EVEN | 0.0 | 0.0 | -3.72865216273772E-6 | -1.3171250965430446E-8 | 2.0190317742329E-11 | 0.0 | 0  |
| 16| EVEN | 0.0 | 0.0 | 2.4382352231371774E-5 | -2.0776345650425206E-8 | 1.0859846416849663E-9 | -5.736657945569047E-12 | 1.5950092191379997E-14 |
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
| effective_focal_length |34.635
| back_focal_length | 0.821
| optical_invariant | 8.723
| object_distance | 1.0E10
| image_distance | 0.821
| power | 0.029
| pp1_H | 19.933
| ppk_H' | -33.814
| ffl_F | -14.702
| fno | 1.224
| enp_dist_P | 18.138
| enp_radius | 14.152
| exp_dist_P' | -35.737
| exp_radius | 14.926
| m | -0
| red | -2.887223847477428E8
| n_obj | 1
| n_img | 1
| img_ht | 21.349
| obj_ang | 31.65
| obj_na | 0
| img_na | -0.378|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 18.585 | 42.918|
 | Field(x=0.0, y=0.1) | 18.376 | 43.513|
 | Field(x=0.0, y=0.2) | 17.219 | 41.641|
 | Field(x=0.0, y=0.3) | 16.207 | 38.69|
 | Field(x=0.0, y=0.4) | 15.86 | 38.155|
 | Field(x=0.0, y=0.5) | 15.835 | 42.433|
 | Field(x=0.0, y=0.6) | 15.644 | 42.556|
 | Field(x=0.0, y=0.7) | 15.568 | 46.953|
 | Field(x=0.0, y=0.8) | 17.467 | 51.621|
 | Field(x=0.0, y=0.9) | 23.397 | 68.234|
 | Field(x=0.0, y=1.0) | 29.093 | 109.311|
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
* [Zemax file](./US20250271647_Example02i-optim.zmx)

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-03-06
