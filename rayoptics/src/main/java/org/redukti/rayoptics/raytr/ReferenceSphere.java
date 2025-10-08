package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.math.Tfm3d;

public class ReferenceSphere {
    private final Vector3 imagePt;
    private final Vector3 refDir;
    private final double refSphereRadius;
    private final Tfm3d lclTfrmLast;

    public ReferenceSphere(Vector3 imagePt, Vector3 refDir, double refSphereRadius, Tfm3d lclTfrmLast) {
        this.imagePt = imagePt;
        this.refDir = refDir;
        this.refSphereRadius = refSphereRadius;
        this.lclTfrmLast = lclTfrmLast;
    }
}
