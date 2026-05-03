## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 99.021 | 11.5 | 65.93 | 1.49782 | 82.57 | Hikari | J-FKH1 |
| 2 | -140.839 | 2.1 | 65.93 |  |  |  |
| 3 | -138.056 | 3.7 | 62.88 | 1.7495 | 35.04 | Hoya | E-LAF7 |
| 4 | 373.0 | 6.3 | 62.88 |  |  |  |
| 5 | 77.774 | 9.2 | 61.38 | 1.65844 | 50.84 | Hikari | J-SSK5 |
| 6 | 240.0 | 58.1 | 61.38 |  |  |  |
| 7 | AS | 32.8 | 31.503 |  |  |  |
| 8 | -35.5 | 1.8 | 22.48 | 1.51454 | 54.63 | Hoya | CF3 |
| 9 | -550.0 | 0.5 | 22.48 |  |  |  |
| 10 | 220.0 | 5.0 | 22.14 | 1.795 | 45.31 | Hikari | J-LASF017 |
| 11 | -162.193 | 42.65 | 22.14 |  |  |  |
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
| effective_focal_length |182.135
| back_focal_length | 42.646
| optical_invariant | 3.809
| object_distance | 1.0E10
| image_distance | 42.646
| power | 0.005
| pp1_H | -81.482
| ppk_H' | -139.489
| ffl_F | -263.618
| fno | 2.851
| enp_dist_P | 165.714
| enp_radius | 31.943
| exp_dist_P' | -34.625
| exp_radius | 13.551
| m | -0
| red | -5.490418598128545E7
| n_obj | 1
| n_img | 1
| img_ht | 21.718
| obj_ang | 6.8
| obj_na | 0
| img_na | -0.173|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 8.633 | 26.541|
 | Field(x=0.0, y=0.1) | 9.088 | 34.173|
 | Field(x=0.0, y=0.2) | 8.662 | 33.877|
 | Field(x=0.0, y=0.3) | 8.307 | 33.197|
 | Field(x=0.0, y=0.4) | 7.239 | 30.287|
 | Field(x=0.0, y=0.5) | 6.848 | 26.699|
 | Field(x=0.0, y=0.6) | 6.985 | 22.659|
 | Field(x=0.0, y=0.7) | 7.391 | 18.449|
 | Field(x=0.0, y=0.8) | 8.092 | 14.371|
 | Field(x=0.0, y=0.9) | 9.076 | 18.352|
 | Field(x=0.0, y=1.0) | 10.003 | 22.655|
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
* [Zemax file](./US004514051_ExampleML01P.zmx)

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-05-03
