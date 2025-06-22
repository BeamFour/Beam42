package org.redukti.jfotoptix.optim;

import org.redukti.jfotoptix.analysis.AnalysisRayFinder;
import org.redukti.jfotoptix.analysis.AnalysisSpot;
import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.jfotoptix.spec.Prescription;
import org.redukti.jfotoptix.tracing.RayTraceResults;

public class Analysis {

    public Prescription prescription;
    public OpticalSystem sys1;
    public double[] pfo;
    public AnalysisSpot sys1Spot;
    public OpticalSystem sys2;
    public AnalysisSpot sys2Spot;
    public OpticalSystem sys3;
    public AnalysisSpot sys3Spot;
    public RayTraceResults singleRayTraceResults;
    public double field2 = 0.7;
    public boolean enableField2 = false;
    public double field3 = 1.0;
    public boolean enableField3 = false;

    public Analysis(Prescription prescription) {
        this.prescription = prescription;
    }
    public Analysis field2(double value) {
        if (value > 0.0 && value <= 1.0) {
            field2 = value;
            enableField2 = true;
        }
        else
            throw new IllegalArgumentException();
        return this;
    }
    public Analysis field3(double value) {
        if (value > 0.0 && value <= 1.0) {
            field3 = value;
            enableField3 = true;
        }
        else
            throw new IllegalArgumentException();
        return this;
    }

    public void compute() {
        sys1 = prescription.buildSystem(true,0.0).build();
        sys1Spot = new AnalysisSpot(sys1,10).process_analysis();
        if (enableField2) {
            sys2 = prescription.buildSystem(true,field2).build();
            sys2Spot = new AnalysisSpot(sys2,10).process_analysis();
        }
        if (enableField3) {
            sys3 = prescription.buildSystem(true,field3).build();
            sys3Spot = new AnalysisSpot(sys3,10).process_analysis();
        }
        pfo = ParaxialFirstOrderInfo.compute(sys1).asArray();
        if (prescription.distribution.get_user_defined_points() != null) {
            singleRayTraceResults = new AnalysisRayFinder(sys1,prescription.distribution).compute();
        }
    }
}
