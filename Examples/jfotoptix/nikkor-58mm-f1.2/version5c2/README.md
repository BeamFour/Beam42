## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 84.12001322169323 | 6.885 | 50.4875 | 1.795 | 45.31 | Hikari | J-LASF017 |
| 2 | 0.0 | 0.1 | 50.4875 |  |  |  |
| 3 | 34.22137487407864 | 9.75 | 44.832 | 1.8485 | 43.79 | Hikari | J-LASFH22 |
| 4 | 78.0494351773588 | 1.56 | 44.832 |  |  |  |
| 5 | 147.70007789597264 | 2.87 | 42.169 | 1.74 | 28.3 | Ohara | S-TIH3 |
| 6 | 22.87698972969248 | 8.44 | 32.12841 |  |  |  |
| 7 | AS | 7.95 | 31.227 |  |  |  |
| 8 | -22.676594585987573 | 1.64 | 31.445 | 1.74077 | 27.79 | Ohara | S-TIH13 |
| 9 | 302.63077698550035 | 8.196 | 40.2 | 1.788 | 47.49 | Hoya | TAF4 |
| 10 | -35.90100309212692 | 0.15 | 40.2 |  |  |  |
| 11 | -396.96493379438965 | 6.147 | 39.5 | 1.7725 | 49.62 | Hikari | J-LASF016 |
| 12 | -54.438513184455324 | 0.0 | 39.5 |  |  |  |
| 13 | 227.22754395681483 | 4.016 | 38.275 | 1.795 | 45.31 | Hikari | J-LASF017 |
| 14 | -96.78120780473958 | 37.78 | 38.275 |  |  |  |
## Aspherical Data
| ID  | k   | P1  | P2  | P3  | P3 | P5 | P6 | P7 | P8 | P9 | P10 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 3.423989046625126 | 0.0 | -4.939163408662723E-7 | -1.111253348153393E-9 | 1.416281327694923E-12 | -8.428293546127028E-16 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
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
| effective_focal_length |57.92
| back_focal_length | 38.027
| optical_invariant | 8.999
| object_distance | 1.0E10
| image_distance | 38.027
| power | 0.017
| pp1_H | 51.225
| ppk_H' | -19.894
| ffl_F | -6.696
| fno | 1.2
| enp_dist_P | 35.291
| enp_radius | 24.133
| exp_dist_P' | -41.628
| exp_radius | 33.292
| m | -0
| red | -1.726511909314396E8
| n_obj | 1
| n_img | 1
| img_ht | 21.598
| obj_ang | 20.45
| obj_na | 0
| img_na | -0.385|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 19.01 | 52.031|
 | Field(x=0.0, y=0.1) | 18.989 | 68.299|
 | Field(x=0.0, y=0.2) | 16.125 | 64.003|
 | Field(x=0.0, y=0.3) | 15.581 | 55.923|
 | Field(x=0.0, y=0.4) | 16.761 | 66.138|
 | Field(x=0.0, y=0.5) | 19.249 | 58.647|
 | Field(x=0.0, y=0.6) | 23.684 | 82.932|
 | Field(x=0.0, y=0.7) | 29.441 | 91.164|
 | Field(x=0.0, y=0.8) | 38.452 | 140.458|
 | Field(x=0.0, y=0.9) | 50.574 | 168.618|
 | Field(x=0.0, y=1.0) | 70.34 | 229.347|
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
* [OpticalBench Compatible Data File, tab delimited](./Noct-Nikkor-58mmf1.2.txt)
* [Zemax file](./Noct-Nikkor-58mmf1.2.zmx)
