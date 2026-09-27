// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.rayoptics.raytr;

public class TraceRingsDef {
    public double cx = 0;
    public double cy = 0;
    public int num_rings = 21;
    /** Normalized inner radius; zero selects a filled circular pupil. */
    public double min_radius = 0.0;
    public double max_radius = 1.0;
    public int num_points_in_ring_one = 6;
    public boolean hexapolar = true;
}
