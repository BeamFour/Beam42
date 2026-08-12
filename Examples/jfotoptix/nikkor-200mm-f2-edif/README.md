# Ai Nikkor ED-IF 200mm F2S
## Patent Information
| Country | Patent Number | Example | Year of Application | Inventors | Organisation | Link |
| ---     | ---           | ---     | ---                 | ---       | ---          | ---  |
|US | US4176913 | 2 | 1977 | Soichi Nakamura,Kiyoshi Hayashi | Nippon Kogaku KK | [link](https://patents.google.com/patent/US4176913A/en) |
## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 200.0 | 14.0 | 102.05 | 1.49782 | 82.57 | Hikari | J-FKH1 |
| 2 | -540.0 | 0.3 | 102.05 |  |  |  |
| 3 | 112.869 | 15.5 | 97.66 | 1.49782 | 82.57 | Hikari | J-FKH1 |
| 4 | -600.0 | 0.4763414595637902 | 97.66 |  |  |  |
| 5 | -480.0 | 5.0 | 95.69 | 1.7552 | 27.51 | Hikari | E-SF4 |
| 6 | 431.735 | 46.24140839615228 | 95.69 |  |  |  |
| 7 | -386.0 | 7.5 | 70.05 | 1.79504 | 28.54 | Hikari | E-LAF9 |
| 8 | -125.0 | 2.6 | 69.05 | 1.4645 | 65.77 | Hoya | FC3 |
| 9 | 286.185 | 18.98283778475241 | 69.05 |  |  |  |
| 10 | -161.2 | 3.4 | 55.9 | 1.4645 | 65.77 | Hoya | FC3 |
| 11 | 67.815 | 18.883185601635763 | 53.21 |  |  |  |
| 12 | 171.0 | 6.5 | 53.71 | 1.6935 | 53.2 | Hikari | E-LAK13 |
| 13 | -131.975 | 1.533476773185139 | 53.71 |  |  |  |
| 14 | -213.0 | 2.0 | 52.37 | 1.5995 | 35.2 | Schott | F16 |
| 15 | 61.0 | 11.0 | 50.69 | 1.6968 | 55.52 | Hikari | J-LAK14 |
| 16 | -193.237 | 6.922214892339323 | 50.69 |  |  |  |
| 17 | AS | 22.345546925975615 | 39.998 |  |  |  |
| 18 | -130.0 | 3.0 | 38.64 | 1.4645 | 65.77 | Hoya | FC3 |
| 19 | -311.705 | 53.828803705363526 | 35.74 |  |  |  |
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
| effective_focal_length |200.061
| back_focal_length | 53.898
| optical_invariant | 5.184
| object_distance | 1.0E10
| image_distance | 53.898
| power | 0.005
| pp1_H | 63.807
| ppk_H' | -146.163
| ffl_F | -136.255
| fno | 2.083
| enp_dist_P | 380.606
| enp_radius | 48.028
| exp_dist_P' | -23.47
| exp_radius | 18.59
| m | -0
| red | -4.998464363112712E7
| n_obj | 1
| n_img | 1
| img_ht | 21.592
| obj_ang | 6.16
| obj_na | 0
| img_na | -0.233|
## Spot Analysis
| Field | Spot Mean Radius | Spot Max Radius |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 8.899 | 20.325|
 | Field(x=0.0, y=0.1) | 9.268 | 28.606|
 | Field(x=0.0, y=0.2) | 9.819 | 31.663|
 | Field(x=0.0, y=0.3) | 10.225 | 33.824|
 | Field(x=0.0, y=0.4) | 10.225 | 34.863|
 | Field(x=0.0, y=0.5) | 9.914 | 33.985|
 | Field(x=0.0, y=0.6) | 9.564 | 32.39|
 | Field(x=0.0, y=0.7) | 9.538 | 30.52|
 | Field(x=0.0, y=0.8) | 9.998 | 27.698|
 | Field(x=0.0, y=0.9) | 11.246 | 29.024|
 | Field(x=0.0, y=1.0) | 13.956 | 41.175|
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
* [Zemax file](./US004176913_Example02-optimized.zmx)

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-08-12
