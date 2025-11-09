// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.parax;

public class Seidel_FieldCurv {
    // TCV = curvature of the tangential image surface
    public final double TCV;
    // SCV = curvature of the sagittal image surface
    public final double SCV;
    // PCV = curvature of the Petzval surface
    public final double PCV;

    public Seidel_FieldCurv(double TCV, double SCV, double PCV) {
        this.TCV = TCV;
        this.SCV = SCV;
        this.PCV = PCV;
    }
}
