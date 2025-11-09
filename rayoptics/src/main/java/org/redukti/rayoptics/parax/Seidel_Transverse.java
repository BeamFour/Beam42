// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.parax;

public class Seidel_Transverse {
    // TSA = transverse spherical aberration
    public final double TSA;
    // TCO = tangential coma
    public final double TCO;
    // TAS = tangential astigmatism
    public final double TAS;
    // SAS = sagittal astigmatism
    public final double SAS;
    // PTB = Petzval blur
    public final double PTB;
    // DST = distortion
    public final double DST;

    public Seidel_Transverse(double TSA, double TCO, double TAS, double SAS, double PTB, double DST) {
        this.TSA = TSA;
        this.TCO = TCO;
        this.TAS = TAS;
        this.SAS = SAS;
        this.PTB = PTB;
        this.DST = DST;
    }
}
