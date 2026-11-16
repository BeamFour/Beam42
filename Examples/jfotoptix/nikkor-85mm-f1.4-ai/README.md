## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 66.55320885735235 | 7.497 | 60.79 | 1.7725 | 49.46 | Hoya | M-TAF1 |
| 2 | 346.596 | 1.496 | 60.79 |  |  |  |
| 3 | 37.128 | 9.6985 | 52.91 | 1.6968 | 55.53 | Ohara | LAL14 |
| 4 | 96.5005 | 1.496 | 51.06 |  |  |  |
| 5 | 167.28 | 6.5025 | 51.06 | 1.7847 | 26.1 | Hoya | FDS30 |
| 6 | -120.003 | 2.7965 | 51.06 | 1.7552 | 27.53 | Hoya | E-FD4 |
| 7 | 23.6385 | 11.799 | 36.26 |  |  |  |
| 8 | AS | 7.7 | 34.244 |  |  |  |
| 9 | -31.4075 | 1.496 | 35.14 | 1.58144 | 40.89 | Hoya | E-FL5 |
| 10 | 79.26724364105368 | 8.704 | 38.8 | 1.7433 | 49.4 | Schott | N-LAF35 |
| 11 | -39.74263526371718 | 0.3995 | 38.8 |  |  |  |
| 12 | 105.9076861099802 | 4.8025 | 34.04 | 1.7433 | 49.4 | Schott | N-LAF35 |
| 13 | -325.78124735748037 | 42.792303167036465 | 32.9 |  |  |  |
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
| effective_focal_length |85.221
| back_focal_length | 42.881
| optical_invariant | 7.7
| object_distance | 1.0E10
| image_distance | 42.881
| power | 0.012
| pp1_H | 37.921
| ppk_H' | -42.34
| ffl_F | -47.3
| fno | 1.405
| enp_dist_P | 62.016
| enp_radius | 30.32
| exp_dist_P' | -23.467
| exp_radius | 23.637
| m | -0
| red | -1.173418591628829E8
| n_obj | 1
| n_img | 1
| img_ht | 21.643
| obj_ang | 14.25
| obj_na | 0
| img_na | -0.356|
## Spot Analysis
| Field | Spot Mean Radius (µm) | Spot Max Radius (µm) |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 21.825 | 59.884|
 | Field(x=0.0, y=0.1) | 22.687 | 92.357|
 | Field(x=0.0, y=0.2) | 25.499 | 109.035|
 | Field(x=0.0, y=0.3) | 26.844 | 121.725|
 | Field(x=0.0, y=0.4) | 27.679 | 119.968|
 | Field(x=0.0, y=0.5) | 28.877 | 128.635|
 | Field(x=0.0, y=0.6) | 30.449 | 141.232|
 | Field(x=0.0, y=0.7) | 33.222 | 159.891|
 | Field(x=0.0, y=0.8) | 38.026 | 185.572|
 | Field(x=0.0, y=0.9) | 45.135 | 216.166|
 | Field(x=0.0, y=1.0) | 54.898 | 247.814|
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
* [Zemax file](./US004396256_Example01-opt.zmx)

Generated from `US004396256_Example01-opt.txt`, status **TODO**

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-11-16
