# Voigtlander NOKTON 50mm f1.0 Aspherical
## Patent Information
| Country | Patent Number | Example | Year of Application | Inventors | Organisation | Link |
| ---     | ---           | ---     | ---                 | ---       | ---          | ---  |
|JP | JP2023-063766 | EX 1 | 2021 | Kazuhiro Ogino | Cosina Co Ltd | [link](https://patents.google.com/patent/JP2023063766A/en) |
## Surface Data
Note that where glass types are shown the refractive index and abbe number is as per assigned glass type

| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |
| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |
| 1 | 40.765 | 4.89 | 48.12 | 1.90525 | 35.04 | Ohara | S-LAH93 |
| 2 | 65.488 | 1.07 | 46.9802 |  |  |  |
| 3 | 32.975 | 8.01 | 44.69 | 1.90043 | 37.37 | Hoya | TAFD37 |
| 4 | 84.15 | 0.87 | 42.6414 |  |  |  |
| 5 | 58.596 | 1.65 | 40.0556 | 1.80518 | 25.46 | Hoya | FD60 |
| 6 | 19.835 | 12.05 | 32.4987 |  |  |  |
| 7 | AS | 3.24 | 31.8813 |  |  |  |
| 8 | -40.995 | 1.55 | 31.8788 | 1.76182 | 26.61 | Hoya | FD140 |
| 9 | 26.578 | 10.8 | 34.865 | 1.883 | 40.69 | Hikari | J-LASF08A |
| 10 | -90.702 | 0.31 | 35.18 |  |  |  |
| 11 | 78.611 | 5.05 | 34.723 | 1.883 | 40.69 | Hikari | J-LASF08A |
| 12 | -125.199 | 0.31 | 34.2102 |  |  |  |
| 13 | 53.736 | 9.47 | 31.18 | 1.883 | 40.69 | Hikari | J-LASF08A |
| 14 | -78.407 | 1.55 | 28.2147 | 1.55298 | 55.07 | Hikari | J-KZFH4 |
| 15 | 45.846 | 1.42 | 25.18 |  |  |  |
| 16 | 3376.612 | 2.78 | 25.2059 | 1.80835 | 40.55 | Ohara | L-LAH84 |
| 17 | 120.496 | 18.74 | 25.76 |  |  |  |
## Aspherical Data
| ID  | Type | k   | P1 | P2 | P3 | P4 | P5 | P6 | P7 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1| EVEN | -0.02238 | 0.0 | -1.0281E-6 | 6.04388E-10 | -4.31143E-12 | 5.62572E-15 | -3.29805E-18 | -5.97602E-23 |
| 16| EVEN | -20.0 | 0.0 | 2.4371E-5 | -9.23649E-8 | 1.14851E-9 | -1.18851E-11 | 5.37423E-14 | -9.17045E-17 |
| 17| EVEN | 20.0 | 0.0 | 3.37528E-5 | -2.40831E-8 | -1.16539E-10 | 1.9524E-13 | -7.7965E-16 | 2.20734E-18 |
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
| effective_focal_length |49.998
| back_focal_length | 18.732
| optical_invariant | 10.712
| object_distance | 1.0E10
| image_distance | 18.732
| power | 0.02
| pp1_H | 28.84
| ppk_H' | -31.265
| ffl_F | -21.158
| fno | 1
| enp_dist_P | 38.303
| enp_radius | 24.999
| exp_dist_P' | -23.316
| exp_radius | 21.02
| m | -0
| red | -2.0000928296044186E8
| n_obj | 1
| n_img | 1
| img_ht | 21.424
| obj_ang | 23.195
| obj_na | 0
| img_na | -0.5|
## Spot Analysis
| Field | Spot Mean Radius (µm) | Spot Max Radius (µm) |
| ---   | ---              | ---             |
 | Field(x=0.0, y=0.0) | 14.539 | 26.381|
 | Field(x=0.0, y=0.1) | 16.362 | 42.948|
 | Field(x=0.0, y=0.2) | 17.621 | 61.796|
 | Field(x=0.0, y=0.3) | 17.813 | 65.053|
 | Field(x=0.0, y=0.4) | 18.563 | 67.687|
 | Field(x=0.0, y=0.5) | 18.705 | 68.563|
 | Field(x=0.0, y=0.6) | 18.704 | 64.563|
 | Field(x=0.0, y=0.7) | 20.524 | 61.205|
 | Field(x=0.0, y=0.8) | 25.866 | 88.113|
 | Field(x=0.0, y=0.9) | 33.021 | 123.034|
 | Field(x=0.0, y=1.0) | 35.918 | 124.022|
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
* [Zemax file](./JP2023-063766_Example01P.zmx)

Generated from `JP2023-063766_Example01P.txt`, status **TODO**

Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on 2026-11-07
