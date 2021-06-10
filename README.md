# Beam42 Optical Ray Tracer

This is a fork of the BEAM FOUR Optical Ray Tracer, by www.StellarSoftware.com.

Original author is [Michael Lampton](https://www.ssl.berkeley.edu/~mlampton/).

Copyright (c) M.Lampton, 2003-2020, STELLAR SOFTWARE all rights reserved.

For any inquiries regarding this version, please raise issues here and do not contact StellarSoftware.com, as this
version is not maintained by Mike Lampton.
  
## Changes / Plan

* The original implementation makes it difficult to use the backend calculators independently of the GUI. I am working on improving the de-coupling of the UI from the calculators.
* The original implementation assumes that there is a single workspace that a user is owrking in, and therefore uses static data structures - this design is not as friendly to server side use cases where multiple simultaneous workspaces can be in use. I am modifying the system so that all the data structures are encapsulated in objects.
* I plan to merge the code from my Java port of GNU Optical into this project.
* I plan to add some features that are more specific to photo lenses - such as paraxial calculations.

