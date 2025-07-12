package org.redukti.jfotoptix.optim;

import org.redukti.jfotoptix.analysis.AnalysisRayFinder;
import org.redukti.jfotoptix.analysis.AnalysisSpot;
import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.jfotoptix.spec.Prescription;
import org.redukti.jfotoptix.tracing.RayTraceResults;

public class Analysis {

    public Prescription prescription;
    /**
     * By default, the sys1 system is for parallel rays (field 0)
     * but if being used to find rays then the angle of view is variable
     * and set by the optimizer
     */
    public OpticalSystem sys1;
    public double[] pfo;
    public AnalysisSpot sys1Spot;

    public OpticalSystem[] systems;
    public AnalysisSpot[] spots;
    public double[] fields;

    /**
     * Results for tracing a single ray
     * used for ray finding
     */
    public RayTraceResults singleRayTraceResults;

    public Analysis(Prescription prescription, double[] fields) {
        this.prescription = prescription;
        this.fields = fields;
    }
    public Analysis(Prescription prescription) {
        this(prescription,new double[0]);
    }

    public void compute() {
        sys1 = prescription.buildSystem(true,0.0).build();
        sys1Spot = new AnalysisSpot(sys1,10).process_analysis();
        systems = new OpticalSystem[fields.length];
        spots = new AnalysisSpot[fields.length];
        for (int i = 0; i < fields.length; i++) {
            systems[i] = prescription.buildSystem(true,fields[i]).build();
            spots[i] = new AnalysisSpot(systems[i],10).process_analysis();
        }
        pfo = ParaxialFirstOrderInfo.compute(sys1).asArray();
        if (prescription.distribution.get_user_defined_points() != null) {
            singleRayTraceResults = new AnalysisRayFinder(sys1,prescription.distribution).compute();
        }
    }
}
