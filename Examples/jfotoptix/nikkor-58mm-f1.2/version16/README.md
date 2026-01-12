## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 81.78806958237104 | 6.885 | 50.4875 | 1.795 | 45.31 | Hikari | J-LASF017 |
| 2 | 0.0 | 0.1 | 50.4875 |  |  |  |
| 3 | 34.61119620278737 | 9.75 | 44.832 | 1.8485 | 43.79 | Hikari | J-LASFH22 |
| 4 | 73.71901665222416 | 1.56 | 44.832 |  |  |  |
| 5 | 132.412938893216 | 2.87 | 42.169 | 1.74 | 28.3 | Ohara | S-TIH3 |
| 6 | 23.096319530396965 | 8.44 | 32.12841 |  |  |  |
| 7 | AS | 7.95 | 31.227 |  |  |  |
| 8 | -22.924335210546094 | 1.64 | 31.445 | 1.74077 | 27.79 | Ohara | S-TIH13 |
| 9 | 306.809610965077 | 8.196 | 40.2 | 1.788 | 47.49 | Hoya | TAF4 |
| 10 | -35.459139177715244 | 0.15 | 40.2 |  |  |  |
| 11 | -394.26508987025437 | 6.147 | 39.5 | 1.7725 | 49.62 | Hikari | J-LASF016 |
| 12 | -56.94340780253313 | 0.0 | 39.5 |  |  |  |
| 13 | 226.43187654820784 | 4.016 | 38.275 | 1.795 | 45.31 | Hikari | J-LASF017 |
| 14 | -96.07135282405889 | 37.75 | 38.275 |  |  |  |
## Aspherical Data
| ID  | k   | P1  | P2  | P3  | P3 | P5 | P6 | P7 | P8 | P9 | P10 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 0.3700325489429075 | 0.0 | -2.438639185079319E-7 | 1.1204779531463593E-10 | 3.844441243010319E-13 | -4.515196460153482E-16 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
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
| effective_focal_length |58.039
| back_focal_length | 37.749
| optical_invariant | 9.018
| object_distance | 1.0E10
| image_distance | 37.749
| power | 0.017
| pp1_H | 50.836
| ppk_H' | -20.289
| ffl_F | -7.203
| fno | 1.2
| enp_dist_P | 35.345
| enp_radius | 24.183
| exp_dist_P' | -41.42
| exp_radius | 32.987
| m | -0
| red | -1.7229840741555038E8
| n_obj | 1
| n_img | 1
| img_ht | 21.642
| obj_ang | 20.45
| obj_na | 0
| img_na | -0.385|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 21.896 | 96.357|
 | Field(x=0.0, y=0.1) | 29.1 | 165.47|
 | Field(x=0.0, y=0.2) | 43.689 | 198.079|
 | Field(x=0.0, y=0.3) | 38.259 | 207.674|
 | Field(x=0.0, y=0.4) | 39.936 | 215.976|
 | Field(x=0.0, y=0.5) | 44.122 | 232.452|
 | Field(x=0.0, y=0.6) | 47.171 | 269.744|
 | Field(x=0.0, y=0.7) | 48.773 | 260.524|
 | Field(x=0.0, y=0.8) | 56.781 | 295.511|
 | Field(x=0.0, y=0.9) | 69.252 | 337.726|
 | Field(x=0.0, y=1.0) | 86.417 | 383.877|
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
* [OpticalBench Compatible Data File, tab delimited](./specs.txt)
* [Zemax file](./specs.zmx)
